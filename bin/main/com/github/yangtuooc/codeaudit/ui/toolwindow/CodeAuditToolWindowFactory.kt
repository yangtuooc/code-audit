package com.github.yangtuooc.codeaudit.ui.toolwindow

import com.github.yangtuooc.codeaudit.api.AIAnalysisService
import com.github.yangtuooc.codeaudit.api.CallChainService
import com.github.yangtuooc.codeaudit.model.ApiEndpoint
import com.github.yangtuooc.codeaudit.model.CallNode
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.treeStructure.Tree
import java.awt.BorderLayout
import javax.swing.JPanel
import javax.swing.tree.DefaultMutableTreeNode
import javax.swing.tree.DefaultTreeModel

/**
 * 代码审计工具窗口工厂类
 */
class CodeAuditToolWindowFactory : ToolWindowFactory {
    private val log = logger<CodeAuditToolWindowFactory>()

    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val codeAuditToolWindow = CodeAuditToolWindow(project, toolWindow)
        val content = ContentFactory.getInstance().createContent(
            codeAuditToolWindow.getContent(),
            "API Endpoints",
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
    private val log = logger<CodeAuditToolWindow>()

    private val rootNode = DefaultMutableTreeNode("API Endpoints")
    private val treeModel = DefaultTreeModel(rootNode)
    private val tree = Tree(treeModel)

    init {
        refreshEndpoints()
    }

    /**
     * 刷新API端点列表
     */
    private fun refreshEndpoints() {
        try {
            rootNode.removeAllChildren()
            
            val endpoints = callChainService.getAllApiEndpoints()
            endpoints.forEach { endpoint: ApiEndpoint ->
                val endpointNode = DefaultMutableTreeNode("${endpoint.httpMethod} ${endpoint.path}")
                rootNode.add(endpointNode)
                
                // 如果有调用链，添加调用链节点
                endpoint.callChain?.nodes?.forEach { node: CallNode ->
                    val methodName = node.method.name
                    val className = node.method.containingClass?.qualifiedName ?: "Unknown"
                    val nodeText = "$methodName() - $className"
                    endpointNode.add(DefaultMutableTreeNode(nodeText))
                }
            }
            
            treeModel.reload()
            tree.expandRow(0)
        } catch (e: Exception) {
            log.error("Failed to refresh endpoints", e)
        }
    }

    /**
     * 获取工具窗口内容面板
     */
    fun getContent(): JPanel {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        panel.add(JBScrollPane(tree), BorderLayout.CENTER)
        return panel
    }
} 