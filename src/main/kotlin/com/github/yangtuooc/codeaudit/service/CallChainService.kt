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
import kotlin.collections.HashSet

/**
 * 调用链服务实现类
 */
@Service(Service.Level.PROJECT)
class CallChainServiceImpl(private val project: Project) : CallChainService {
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
        val cacheKey = "${entryPoint.containingClass?.qualifiedName}.${entryPoint.name}"
        
        // 如果缓存中已有此调用链，则直接返回
        if (callChainCache.containsKey(cacheKey)) {
            log.info("Found call chain in cache for: $cacheKey")
            return callChainCache[cacheKey]!!
        }
        
        log.info("Building call chain for method: ${entryPoint.name}")
        
        // 创建新的调用链
        val newCallChain = CallChain(
            id = cacheKey,
            entryPoint = entryPoint
        )
        
        // 已访问过的方法，用于避免循环调用
        val visited = HashSet<String>()
        // 构建调用链
        buildMethodCallHierarchy(entryPoint, newCallChain, null, visited, 0)
        
        // 将调用链添加到缓存
        callChainCache[cacheKey] = newCallChain
        
        // 通知监听器调用链已创建
        val oldCallChain = callChainCache[cacheKey]
        if (oldCallChain != newCallChain) {
            notifyListeners(newCallChain, oldCallChain)
        }
        
        return newCallChain
    }
    
    /**
     * 递归构建方法调用层次结构
     * 
     * @param method 当前方法
     * @param callChain 当前构建的调用链
     * @param parentCall 父调用节点
     * @param visited 已访问过的方法集合，避免循环调用
     * @param depth 当前深度，防止过深递归
     */
    private fun buildMethodCallHierarchy(
        method: PsiMethod, 
        callChain: CallChain, 
        parentCall: MethodCall?,
        visited: HashSet<String>,
        depth: Int
    ) {
        // 防止过深递归，最多递归10层
        if (depth > 10) {
            log.warn("Reached maximum recursion depth (10) when building call chain")
            return
        }
        
        val methodId = "${method.containingClass?.qualifiedName}.${method.name}"
        
        // 如果该方法已被访问过，则防止循环
        if (visited.contains(methodId)) {
            return
        }
        
        visited.add(methodId)
        
        // 创建当前方法的调用节点
        val currentCall = MethodCall(
            id = UUID.randomUUID().toString(),
            method = method,
            parent = parentCall
        )
        
        // 将当前调用添加到调用链
        if (parentCall == null) {
            // 如果是入口方法，则直接设置为调用链的根节点
            callChain.rootCall = currentCall
        } else {
            // 否则添加为父调用的子节点
            parentCall.children.add(currentCall)
        }
        
        // 找出此方法调用的其他方法
        val callees = ReadAction.compute<List<PsiMethod>, Throwable> {
            codeParsingService.findCallees(method)
        }
        
        // 递归处理每个被调用的方法
        for (callee in callees) {
            buildMethodCallHierarchy(callee, callChain, currentCall, visited, depth + 1)
        }
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
    private fun notifyListeners(newCallChain: CallChain, oldCallChain: CallChain?) {
        listeners.forEach { it.onCallChainChanged(newCallChain, oldCallChain) }
    }
} 