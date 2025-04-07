package com.github.yangtuooc.codeaudit.ui.toolwindow

import com.github.yangtuooc.codeaudit.api.AIAnalysisService
import com.github.yangtuooc.codeaudit.api.CallChainService
import com.github.yangtuooc.codeaudit.api.ContextManager
import com.github.yangtuooc.codeaudit.model.ApiEndpoint
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.psi.PsiMethod
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.treeStructure.Tree
import java.awt.*
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.*
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * 代码审计工具窗口工厂类
 */
class CodeAuditToolWindowFactory : ToolWindowFactory, DumbAware {
    private val log = logger<CodeAuditToolWindowFactory>()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val codeAuditToolWindow = CodeAuditToolWindow(project, toolWindow)
        val content = ContentFactory.getInstance().createContent(
            codeAuditToolWindow.getContent(),
            "API调用链分析",
            false
        )
        toolWindow.contentManager.addContent(content)
    }

    override fun shouldBeAvailable(project: Project): Boolean = true
}

/**
 * 代码审计工具窗口
 */
class CodeAuditToolWindow(private val project: Project, toolWindow: ToolWindow) {
    private val callChainService = project.service<CallChainService>()
    private val aiAnalysisService = project.service<AIAnalysisService>()
    private val contextManager = project.service<ContextManager>()
    private val log = logger<CodeAuditToolWindow>()

    private val rootNode = DefaultMutableTreeNode("API Endpoints")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = Tree(treeModel)

    // 创建主面板和内容面板
    private val mainPanel = JPanel(BorderLayout())
    private val contentPanel = JPanel(BorderLayout())

    // 存储端点映射，用于快速查找
    private val endpointMap = mutableMapOf<String, ApiEndpoint>()

    init {
        setupUI()
        // 延迟加载端点，直到索引准备就绪
        scheduleRefreshWhenReady()
        registerListeners()
    }

    /**
     * 设置UI组件
     */
    private fun setupUI() {
        // 设置树控件
        tree.isRootVisible = true
        tree.showsRootHandles = true

        // 添加到内容面板
        contentPanel.add(JScrollPane(tree), BorderLayout.CENTER)

        // 创建顶部工具栏
        val toolbarPanel = JPanel(BorderLayout())
        val refreshButton = JButton("刷新端点")
        refreshButton.addActionListener { scheduleRefreshWhenReady() }
        toolbarPanel.add(refreshButton, BorderLayout.EAST)

        // 添加状态标签
        val statusLabel = JLabel("双击API端点查看调用链")
        statusLabel.border = BorderFactory.createEmptyBorder(0, 10, 0, 0)
        toolbarPanel.add(statusLabel, BorderLayout.WEST)

        // 添加到主面板
        mainPanel.add(toolbarPanel, BorderLayout.NORTH)
        mainPanel.add(contentPanel, BorderLayout.CENTER)

        // 设置初始状态
        rootNode.removeAllChildren()
        rootNode.userObject = "正在加载API端点..."
        treeModel.reload()
    }

    /**
     * 注册监听器
     */
    private fun registerListeners() {
        tree.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val path = tree.getPathForLocation(e.x, e.y) ?: return
                    val node = path.lastPathComponent as? DefaultMutableTreeNode ?: return
                    val userObject = node.userObject as? String ?: return

                    // 处理端点节点点击
                    if (node.parent == rootNode) {
                        handleEndpointClick(userObject)
                    }
                }
            }
        })
    }

    /**
     * 当索引就绪时调度刷新操作
     */
    private fun scheduleRefreshWhenReady() {
        log.info("Scheduling endpoint refresh when indices are ready")
        // 显示加载状态
        rootNode.removeAllChildren()
        rootNode.userObject = "正在加载API端点..."
        treeModel.reload()

        // 使用DumbService确保在索引就绪后执行
        DumbService.getInstance(project).runWhenSmart {
            log.info("Indices are ready, refreshing endpoints")
            refreshEndpoints()
        }
    }

    /**
     * 处理端点点击事件
     */
    private fun handleEndpointClick(nodeText: String) {
        val endpoint = endpointMap[nodeText] ?: return
        log.info("Endpoint clicked: ${endpoint.httpMethod} ${endpoint.path}")

        try {
            // 确保调用链已经构建
            var callChain = endpoint.callChain
            if (callChain == null) {
                log.info("为端点构建调用链: ${endpoint.path}")
                callChain = callChainService.buildCallChain(endpoint.psiMethod)
                endpoint.callChain = callChain
            }

            // 创建弹出窗口内容
            if (callChain != null) {
                // 创建一个弹出窗口
                val frame = JFrame("API调用链: ${endpoint.path}")
                frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
                frame.size = Dimension(1000, 700)
                frame.setLocationRelativeTo(null)  // 居中显示

                // 创建内容面板
                val contentPanel = JPanel(BorderLayout(10, 10))
                contentPanel.border = BorderFactory.createEmptyBorder(10, 10, 10, 10)

                // 顶部信息面板
                val infoPanel = JPanel(BorderLayout())
                val infoTextArea = JTextArea().apply {
                    isEditable = false
                    font = Font("Monospaced", Font.PLAIN, 12)
                    text = buildString {
                        appendLine("API端点: ${endpoint.httpMethod} ${endpoint.path}")
                        appendLine("控制器: ${endpoint.controllerName}")
                        appendLine("处理方法: ${endpoint.methodName}")
                        appendLine("返回类型: ${endpoint.returnType}")

                        if (endpoint.parameters.isNotEmpty()) {
                            appendLine("\n请求参数:")
                            endpoint.parameters.forEach { param ->
                                appendLine("- ${param.name}: ${param.type}${if (param.required) " (必须)" else " (可选)"}")
                                if (param.description != null) {
                                    appendLine("  描述: ${param.description}")
                                }
                            }
                        }
                    }
                }
                infoPanel.add(JScrollPane(infoTextArea), BorderLayout.CENTER)
                infoPanel.border = BorderFactory.createTitledBorder("API端点信息")
                infoPanel.preferredSize = Dimension(1000, 150)

                // 主内容区域（调用链和源码视图）
                val mainContentPanel = JSplitPane(JSplitPane.HORIZONTAL_SPLIT)
                mainContentPanel.dividerLocation = 450
                mainContentPanel.resizeWeight = 0.4

                // 左侧调用链面板
                val callChainPanel = JPanel(BorderLayout())
                val methods = callChain.getAllMethods()

                if (methods.isEmpty()) {
                    callChainPanel.add(JLabel("该API端点没有检测到调用链", JLabel.CENTER), BorderLayout.CENTER)
                } else {
                    // 创建调用链列表
                    val callChainListModel = DefaultListModel<CallChainItem>()
                    methods.forEachIndexed { index, method ->
                        val className = method.containingClass?.qualifiedName ?: "Unknown"
                        val displayName = "${index + 1}. ${method.name}() - ${className.substringAfterLast(".")}"
                        callChainListModel.addElement(CallChainItem(method, displayName, className))
                    }

                    val callChainList = JList(callChainListModel)
                    callChainList.cellRenderer = CallChainCellRenderer()
                    callChainList.selectionMode = ListSelectionModel.SINGLE_SELECTION

                    // 调用链统计信息
                    val statsPanel = JPanel(BorderLayout())
                    val statsTextArea = JTextArea().apply {
                        isEditable = false
                        font = Font("Monospaced", Font.PLAIN, 12)
                        text = buildString {
                            appendLine("调用链深度: ${methods.size} 个方法")

                            // 分组展示不同包的调用情况
                            val packageCounts = methods
                                .mapNotNull { it.containingClass?.qualifiedName }
                                .groupBy {
                                    it.split(".").let { parts ->
                                        if (parts.size > 2) parts.take(3).joinToString(".")
                                        else it
                                    }
                                }
                                .mapValues { it.value.size }
                                .toList()
                                .sortedByDescending { it.second }

                            appendLine("\n包调用分布:")
                            packageCounts.forEach { (pkg, count) ->
                                val percentage = count * 100 / methods.size
                                appendLine("• $pkg: $count 次 ($percentage%)")
                            }
                        }
                    }
                    statsPanel.add(JScrollPane(statsTextArea), BorderLayout.CENTER)
                    statsPanel.border = BorderFactory.createTitledBorder("调用链统计")

                    // 组合调用链面板
                    val callChainSplitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
                    callChainSplitPane.topComponent = JScrollPane(callChainList)
                    callChainSplitPane.bottomComponent = statsPanel
                    callChainSplitPane.dividerLocation = 350
                    callChainSplitPane.resizeWeight = 0.7

                    callChainPanel.add(callChainSplitPane, BorderLayout.CENTER)

                    // 右侧源码面板
                    val sourceCodePanel = JPanel(BorderLayout())
                    sourceCodePanel.border = BorderFactory.createTitledBorder("源代码")

                    val sourceTextArea = JTextArea().apply {
                        isEditable = false
                        font = Font("Monospaced", Font.PLAIN, 12)
                        text = "请在左侧选择一个方法查看源代码"
                    }

                    sourceCodePanel.add(JScrollPane(sourceTextArea), BorderLayout.CENTER)

                    // 为调用链列表添加选择监听器，显示选中方法的源码
                    callChainList.addListSelectionListener { event ->
                        if (!event.valueIsAdjusting) {
                            val selectedItem = callChainList.selectedValue as? CallChainItem
                            if (selectedItem != null) {
                                val method = selectedItem.method

                                // 获取方法的源代码文本
                                sourceTextArea.text = try {
                                    val sourceText = method.text
                                    val className = method.containingClass?.qualifiedName ?: "Unknown"
                                    val fileName = method.containingFile.name

                                    buildString {
                                        appendLine("文件: $fileName")
                                        appendLine("类: $className")
                                        appendLine("方法: ${method.name}")
                                        appendLine("行号: ${getLineNumber(method)}")
                                        appendLine("\n源代码:")
                                        appendLine(sourceText)
                                    }
                                } catch (e: Exception) {
                                    "无法获取源代码: ${e.message}"
                                }

                                // 滚动到文本区域的顶部
                                sourceTextArea.caretPosition = 0
                            }
                        }
                    }

                    // 添加定位到源码按钮
                    val actionPanel = JPanel(FlowLayout(FlowLayout.LEFT))
                    val navigateButton = JButton("在IDE中打开源码").apply {
                        addActionListener {
                            val selectedItem = callChainList.selectedValue as? CallChainItem
                            if (selectedItem != null) {
                                navigateToSource(selectedItem.method)
                            } else {
                                JOptionPane.showMessageDialog(
                                    frame,
                                    "请先选择一个方法",
                                    "提示",
                                    JOptionPane.INFORMATION_MESSAGE
                                )
                            }
                        }
                    }
                    actionPanel.add(navigateButton)
                    sourceCodePanel.add(actionPanel, BorderLayout.NORTH)

                    // 设置主分割面板
                    mainContentPanel.leftComponent = callChainPanel
                    mainContentPanel.rightComponent = sourceCodePanel

                    // 自动选择第一个项目
                    if (callChainListModel.size > 0) {
                        callChainList.selectedIndex = 0
                    }
                }

                // 底部按钮面板
                val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT))
                val closeButton = JButton("关闭").apply {
                    addActionListener { frame.dispose() }
                }
                buttonPanel.add(closeButton)

                // 添加到主面板
                contentPanel.add(infoPanel, BorderLayout.NORTH)
                contentPanel.add(mainContentPanel, BorderLayout.CENTER)
                contentPanel.add(buttonPanel, BorderLayout.SOUTH)

                frame.contentPane = contentPanel
                frame.isVisible = true
            } else {
                JOptionPane.showMessageDialog(
                    mainPanel,
                    "无法构建调用链，请检查代码或重新刷新端点。",
                    "警告",
                    JOptionPane.WARNING_MESSAGE
                )
            }
        } catch (e: Exception) {
            log.error("处理端点点击时发生错误", e)
            JOptionPane.showMessageDialog(
                mainPanel,
                "显示调用链失败: ${e.message}",
                "错误",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    /**
     * 获取方法在文件中的行号
     */
    private fun getLineNumber(method: PsiMethod): Int {
        return try {
            val document = method.containingFile?.viewProvider?.document
            if (document != null) {
                document.getLineNumber(method.textOffset) + 1
            } else {
                -1
            }
        } catch (e: Exception) {
            -1
        }
    }

    /**
     * 在IDE中导航到源码位置
     */
    private fun navigateToSource(method: PsiMethod) {
        method.navigate(true)
    }

    /**
     * 调用链项目数据类，用于在列表中显示
     */
    private data class CallChainItem(
        val method: PsiMethod,
        val displayName: String,
        val className: String
    )

    /**
     * 自定义列表渲染器，美化调用链展示
     */
    private inner class CallChainCellRenderer : DefaultListCellRenderer() {
        override fun getListCellRendererComponent(
            list: JList<*>?,
            value: Any?,
            index: Int,
            isSelected: Boolean,
            cellHasFocus: Boolean
        ): Component {
            val component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus)

            if (value is CallChainItem) {
                text = value.displayName

                // 设置图标标识不同的方法类型
                val method = value.method
                icon = when {
                    method.isConstructor -> UIManager.getIcon("FileView.fileIcon")
                    method.hasModifierProperty("private") -> UIManager.getIcon("Tree.leafIcon")
                    method.hasModifierProperty("public") -> UIManager.getIcon("FileChooser.detailsViewIcon")
                    else -> UIManager.getIcon("FileView.fileIcon")
                }

                // 添加工具提示
                toolTipText = "${value.className}.${method.name}()\n${
                    method.parameterList.parameters.joinToString(", ") {
                        "${it.name}: ${it.type.presentableText}"
                    }
                }"
            }

            return component
        }
    }

    /**
     * 刷新API端点列表
     */
    private fun refreshEndpoints() {
        try {
            log.info("开始刷新API端点")
            rootNode.removeAllChildren()
            rootNode.userObject = "API Endpoints" // 重置根节点文本
            endpointMap.clear()

            val endpoints = callChainService.getAllApiEndpoints()

            endpoints.forEach { endpoint: ApiEndpoint ->
                val nodeText = "${endpoint.httpMethod} ${endpoint.path}"
                val endpointNode = DefaultMutableTreeNode(nodeText)
                rootNode.add(endpointNode)

                // 存储端点映射
                endpointMap[nodeText] = endpoint

                // 添加一个说明节点，指示需要双击查看调用链
                endpointNode.add(DefaultMutableTreeNode("(双击端点查看调用链)"))
            }

            if (endpoints.isEmpty()) {
                rootNode.add(DefaultMutableTreeNode("未找到API端点"))
            }

            log.info("API端点刷新完成，共 ${endpoints.size} 个端点")
            treeModel.reload()
            tree.expandRow(0)
        } catch (e: Exception) {
            log.error("刷新端点失败", e)
            rootNode.removeAllChildren()
            rootNode.userObject = "API Endpoints"
            rootNode.add(DefaultMutableTreeNode("刷新端点失败: ${e.message}"))
            treeModel.reload()

            JOptionPane.showMessageDialog(
                mainPanel,
                "刷新端点失败: ${e.message}",
                "错误",
                JOptionPane.ERROR_MESSAGE
            )
        }
    }

    /**
     * 获取工具窗口内容面板
     */
    fun getContent(): JPanel {
        return mainPanel
    }
} 