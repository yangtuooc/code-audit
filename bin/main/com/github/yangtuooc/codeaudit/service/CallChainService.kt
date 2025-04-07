package com.github.yangtuooc.codeaudit.service

import com.github.yangtuooc.codeaudit.api.CallChainListener
import com.github.yangtuooc.codeaudit.api.CallChainService
import com.github.yangtuooc.codeaudit.model.ApiEndpoint
import com.github.yangtuooc.codeaudit.model.CallChain
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod

/**
 * 调用链服务实现类
 */
@Service(Service.Level.PROJECT)
class CallChainServiceImpl(private val project: Project) : CallChainService {
    private val log = logger<CallChainServiceImpl>()
    private val listeners = mutableListOf<CallChainListener>()
    private val callChainCache = mutableMapOf<String, CallChain>()

    override fun buildCallChain(entryPoint: PsiMethod): CallChain {
        // TODO: 实现调用链构建逻辑
        log.info("Building call chain for method: ${entryPoint.name}")
        return CallChain(
            id = "${entryPoint.containingClass?.qualifiedName}.${entryPoint.name}",
            entryPoint = entryPoint
        )
    }

    override fun getAllApiEndpoints(): List<ApiEndpoint> {
        // TODO: 实现获取所有API端点的逻辑
        log.info("Getting all API endpoints")
        return emptyList()
    }

    override fun addCallChainListener(listener: CallChainListener) {
        listeners.add(listener)
    }

    override fun refreshEndpoints() {
        // TODO: 实现刷新端点的逻辑
        log.info("Refreshing endpoints")
    }

    /**
     * 通知所有监听器调用链发生变化
     */
    private fun notifyListeners(newCallChain: CallChain, oldCallChain: CallChain?) {
        listeners.forEach { it.onCallChainChanged(newCallChain, oldCallChain) }
    }
} 