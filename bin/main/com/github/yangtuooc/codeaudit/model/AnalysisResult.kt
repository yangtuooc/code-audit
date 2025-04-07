package com.github.yangtuooc.codeaudit.model

import com.intellij.psi.PsiElement

/**
 * 代码分析结果
 */
data class AnalysisResult(
    /**
     * 分析结果的唯一标识符
     */
    val id: String,
    
    /**
     * 分析过程中发现的问题列表
     */
    val issues: List<CodeIssue> = mutableListOf(),
    
    /**
     * 分析的状态
     */
    val status: AnalysisStatus = AnalysisStatus.COMPLETED,
    
    /**
     * 分析持续的时间（毫秒）
     */
    val duration: Long = 0,
    
    /**
     * 分析结果的摘要
     */
    val summary: String = "",
    
    /**
     * 分析的详细信息
     */
    val details: String = "",
    
    /**
     * 分析相关的元数据
     */
    val metadata: Map<String, Any> = mutableMapOf()
)

/**
 * 代码问题
 */
data class CodeIssue(
    /**
     * 问题的唯一标识符
     */
    val id: String,
    
    /**
     * 问题的严重程度
     */
    val severity: IssueSeverity,
    
    /**
     * 问题类型
     */
    val type: IssueType,
    
    /**
     * 问题描述
     */
    val description: String,
    
    /**
     * 问题相关的代码元素
     */
    val element: PsiElement,
    
    /**
     * 问题影响的代码范围
     */
    val range: IntRange? = null,
    
    /**
     * 问题可能的修复建议
     */
    val fixSuggestions: List<FixSuggestion> = emptyList(),
    
    /**
     * 问题的上下文信息
     */
    val context: String = "",
    
    /**
     * 问题的元数据
     */
    val metadata: Map<String, Any> = mutableMapOf()
)

/**
 * 修复建议
 */
data class FixSuggestion(
    /**
     * 建议的唯一标识符
     */
    val id: String,
    
    /**
     * 建议描述
     */
    val description: String,
    
    /**
     * 建议的代码片段
     */
    val codeSnippet: String,
    
    /**
     * 是否为自动修复
     */
    val autoFix: Boolean = false,
    
    /**
     * 修复建议的元数据
     */
    val metadata: Map<String, Any> = mutableMapOf()
)

/**
 * 分析状态
 */
enum class AnalysisStatus {
    WAITING,
    RUNNING,
    COMPLETED,
    FAILED,
    TIMEOUT
}

/**
 * 问题严重程度
 */
enum class IssueSeverity {
    INFO,
    WARNING,
    ERROR,
    CRITICAL
}

/**
 * 问题类型
 */
enum class IssueType {
    SECURITY,
    PERFORMANCE,
    MAINTAINABILITY,
    BUG,
    CODE_SMELL,
    VULNERABILITY,
    DESIGN_FLAW,
    OTHER
} 