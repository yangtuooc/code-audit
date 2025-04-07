package com.github.yangtuooc.codeaudit.model

import com.intellij.psi.PsiElement

/**
 * 代码分析上下文，包含分析过程中需要的上下文信息
 */
data class AnalysisContext(
    /**
     * 上下文的唯一标识符
     */
    val id: String,
    
    /**
     * 分析的调用链
     */
    val callChain: CallChain? = null,
    
    /**
     * 元素上下文映射，键为元素ID，值为元素上下文
     */
    val elementContexts: Map<String, ElementContext> = mutableMapOf(),
    
    /**
     * 是否包含数据流分析信息
     */
    val hasDataFlowInfo: Boolean = false,
    
    /**
     * 上下文相关的元数据
     */
    val metadata: Map<String, Any> = mutableMapOf()
)

/**
 * 元素上下文，包含代码元素的上下文信息
 */
data class ElementContext(
    /**
     * 元素的唯一标识符
     */
    val id: String,
    
    /**
     * 关联的代码元素
     */
    val element: PsiElement,
    
    /**
     * 元素的类型描述
     */
    val elementType: String,
    
    /**
     * 如果元素是方法，则为方法的参数类型列表
     */
    val parameterTypes: List<String> = emptyList(),
    
    /**
     * 如果元素是方法，则为方法的返回类型
     */
    val returnType: String? = null,
    
    /**
     * 变量依赖关系 (用于数据流分析)
     */
    val variableDependencies: Map<String, Set<String>> = mutableMapOf(),
    
    /**
     * 相关的注解信息
     */
    val annotations: List<AnnotationInfo> = emptyList(),
    
    /**
     * 上下文相关的元数据
     */
    val metadata: Map<String, Any> = mutableMapOf()
)

/**
 * 注解信息
 */
data class AnnotationInfo(
    /**
     * 注解名称
     */
    val name: String,
    
    /**
     * 注解参数
     */
    val attributes: Map<String, String> = mutableMapOf()
) 