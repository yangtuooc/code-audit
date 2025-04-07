package com.github.yangtuooc.codeaudit.api

import com.github.yangtuooc.codeaudit.model.*
import com.intellij.psi.PsiMethod

/**
 * AI代码分析服务接口，负责调用AI进行代码分析
 */
interface AIAnalysisService {
    /**
     * 分析单个方法
     * @param method 要分析的方法
     * @param context 分析上下文
     * @return 分析结果
     */
    fun analyzeMethod(method: PsiMethod, context: AnalysisContext): AnalysisResult

    /**
     * 分析完整调用链
     * @param callChain 要分析的调用链
     * @return 分析结果
     */
    fun analyzeCallChain(callChain: CallChain): AnalysisResult

    /**
     * 分析API端点
     * @param endpoint API端点
     * @param context 分析上下文
     * @return 分析结果
     */
    fun analyzeApiEndpoint(endpoint: ApiEndpoint, context: AnalysisContext): AnalysisResult

    /**
     * 获取AI生成的修复建议
     * @param issue 代码问题
     * @return 修复建议列表
     */
    fun getFixSuggestions(issue: CodeIssue): List<FixSuggestion>

    /**
     * 设置AI模型参数
     * @param key 参数名
     * @param value 参数值
     */
    fun setModelParameter(key: String, value: String)
} 