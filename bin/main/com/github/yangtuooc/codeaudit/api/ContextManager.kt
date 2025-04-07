package com.github.yangtuooc.codeaudit.api

import com.github.yangtuooc.codeaudit.model.AnalysisContext
import com.github.yangtuooc.codeaudit.model.CallChain
import com.github.yangtuooc.codeaudit.model.ElementContext
import com.intellij.psi.PsiElement

/**
 * 上下文管理接口，负责管理代码分析的上下文信息
 */
interface ContextManager {
    /**
     * 为调用链收集上下文信息
     * @param callChain 调用链
     * @return 分析上下文
     */
    fun buildContext(callChain: CallChain): AnalysisContext
    
    /**
     * 更新上下文(当代码变化时)
     * @param element 发生变化的代码元素
     */
    fun updateContext(element: PsiElement)
    
    /**
     * 获取元素的上下文信息
     * @param element 代码元素
     * @return 元素上下文
     */
    fun getElementContext(element: PsiElement): ElementContext
    
    /**
     * 清除所有缓存的上下文
     */
    fun clearContext()
} 