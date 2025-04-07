package com.github.yangtuooc.codeaudit.service

import com.github.yangtuooc.codeaudit.api.CallChainListener
import com.github.yangtuooc.codeaudit.api.CallChainService
import com.github.yangtuooc.codeaudit.api.CodeParsingService
import com.github.yangtuooc.codeaudit.model.ApiEndpoint
import com.github.yangtuooc.codeaudit.model.CallChain
import com.github.yangtuooc.codeaudit.model.MethodCall
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import java.util.*

/**
 * 调用链服务实现类
 */
@Service(Service.Level.PROJECT)
class CallChainServiceImpl(project: Project) : CallChainService {
    private val log = logger<CallChainServiceImpl>()
    private val listeners = mutableListOf<CallChainListener>()
    private val callChainCache = mutableMapOf<String, CallChain>()
    private val codeParsingService = project.service<CodeParsingService>()
    private var apiEndpoints: List<ApiEndpoint> = emptyList()

    init {
        // 初始化时加载所有API端点
        refreshEndpoints()
    }

    override fun buildCallChain(entryPoint: PsiMethod): CallChain {
        log.info("Building call chain for method: ${entryPoint.name}")

        // 检查缓存
        val cacheKey = getMethodKey(entryPoint)
        callChainCache[cacheKey]?.let {
            log.info("Using cached call chain for method: ${entryPoint.name}")
            return it
        }

        // 创建新的调用链
        val callChain = CallChain(
            id = UUID.randomUUID().toString(),
            entryPoint = entryPoint
        )

        // 构建方法调用层次结构，最大深度限制为5
        val rootCall = buildMethodCallHierarchy(entryPoint, hashSetOf(), 0, 5)
        callChain.rootCall = rootCall

        // 缓存调用链
        callChainCache[cacheKey] = callChain

        // 通知监听器
        notifyListeners(callChain)

        return callChain
    }

    /**
     * 递归构建方法调用层次结构
     *
     * @param method 当前处理的方法
     * @param visited 已访问过的方法集合，用于防止循环调用
     * @param currentDepth 当前递归深度
     * @param maxDepth 最大递归深度限制
     * @return 构建的方法调用对象
     */
    private fun buildMethodCallHierarchy(
        method: PsiMethod,
        visited: HashSet<String>,
        currentDepth: Int,
        maxDepth: Int
    ): MethodCall? {
        val methodKey = getMethodKey(method)

        // 防止循环调用和超过最大深度
        if (visited.contains(methodKey) || currentDepth >= maxDepth) {
            return null
        }

        // 标记为已访问
        visited.add(methodKey)

        // 创建当前方法的调用节点
        val methodCall = MethodCall(
            id = UUID.randomUUID().toString(),
            method = method,
            children = mutableListOf()
        )

        // 获取该方法调用的其他方法
        val callees = codeParsingService.findCallees(method)

        // 对于每个被调用的方法，递归构建调用层次
        for (callee in callees) {
            val childCall = buildMethodCallHierarchy(callee, HashSet(visited), currentDepth + 1, maxDepth)
            if (childCall != null) {
                methodCall.children.add(childCall)
            }
        }

        return methodCall
    }

    /**
     * 获取方法的唯一键
     */
    private fun getMethodKey(method: PsiMethod): String {
        return "${method.containingClass?.qualifiedName}.${method.name}"
    }

    override fun getAllApiEndpoints(): List<ApiEndpoint> {
        // 如果还没有加载API端点，则刷新
        if (apiEndpoints.isEmpty()) {
            refreshEndpoints()
        }
        return apiEndpoints
    }

    override fun addCallChainListener(listener: CallChainListener) {
        listeners.add(listener)
        log.info("Added call chain listener: ${listener.javaClass.simpleName}")
    }

    override fun refreshEndpoints() {
        log.info("Refreshing API endpoints...")

        // 使用CodeParsingService发现所有API端点
        apiEndpoints = ReadAction.compute<List<ApiEndpoint>, Throwable> {
            codeParsingService.discoverApiEndpoints()
        }

        // 清除缓存，以便重新构建调用链
        callChainCache.clear()

        log.info("Refreshed ${apiEndpoints.size} API endpoints")
    }

    /**
     * 通知所有监听器调用链发生变化
     */
    private fun notifyListeners(newCallChain: CallChain) {
        listeners.forEach { it.onCallChainChanged(newCallChain, null) }
    }
} 