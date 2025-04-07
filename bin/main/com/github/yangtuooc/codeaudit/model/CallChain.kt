package com.github.yangtuooc.codeaudit.model

import com.intellij.psi.PsiMethod

/**
 * 表示方法调用链的模型类
 */
data class CallChain(
    /**
     * 调用链的唯一标识符
     */
    val id: String,
    
    /**
     * 调用链的入口点方法
     */
    val entryPoint: PsiMethod,
    
    /**
     * 调用链的节点列表，表示方法调用关系
     */
    val nodes: List<CallNode> = mutableListOf(),
    
    /**
     * 调用链的深度
     */
    val depth: Int = 0,
    
    /**
     * 最近一次构建调用链的时间戳
     */
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * 调用链中的节点，表示单个方法调用
 */
data class CallNode(
    /**
     * 节点的唯一标识符
     */
    val id: String,
    
    /**
     * 节点对应的方法
     */
    val method: PsiMethod,
    
    /**
     * 调用该方法的父节点，入口节点的父节点为null
     */
    val parent: CallNode? = null,
    
    /**
     * 该方法调用的子节点列表
     */
    val children: MutableList<CallNode> = mutableListOf(),
    
    /**
     * 节点在调用链中的深度
     */
    val depth: Int = 0,
    
    /**
     * 节点的元数据，可用于存储额外信息
     */
    val metadata: Map<String, Any> = mutableMapOf()
) 