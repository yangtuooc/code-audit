package com.github.yangtuooc.codeaudit.api

import com.github.yangtuooc.codeaudit.model.ApiEndpoint
import com.github.yangtuooc.codeaudit.model.CallChain
import com.intellij.psi.PsiMethod

/**
 * 服务接口，负责构建和管理代码调用链
 */
interface CallChainService {
    /**
     * 从给定入口点构建调用链
     * @param entryPoint 入口方法（如API端点）
     * @return 构建的调用链
     */
    fun buildCallChain(entryPoint: PsiMethod): CallChain
    
    /**
     * 获取所有API端点及其调用链
     * @return API端点列表
     */
    fun getAllApiEndpoints(): List<ApiEndpoint>
    
    /**
     * 添加调用链变化监听器
     * @param listener 调用链监听器
     */
    fun addCallChainListener(listener: CallChainListener)
    
    /**
     * 刷新项目中的所有API端点和调用链
     */
    fun refreshEndpoints()
}

/**
 * 调用链变化监听器接口
 */
interface CallChainListener {
    /**
     * 当调用链发生变化时调用此方法
     * @param newCallChain 新的调用链
     * @param oldCallChain 旧的调用链，如为首次构建则为null
     */
    fun onCallChainChanged(newCallChain: CallChain, oldCallChain: CallChain?)
} 