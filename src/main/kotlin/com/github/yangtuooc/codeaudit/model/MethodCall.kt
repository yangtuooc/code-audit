package com.github.yangtuooc.codeaudit.model

import com.intellij.psi.PsiMethod

/**
 * 表示方法调用的模型类
 */
data class MethodCall(
    /**
     * 调用的唯一标识符
     */
    val id: String,
    
    /**
     * 被调用的方法
     */
    val method: PsiMethod,
    
    /**
     * 调用该方法的父调用，入口方法的父调用为null
     */
    val parent: MethodCall? = null,
    
    /**
     * 该方法调用的子方法列表
     */
    val children: MutableList<MethodCall> = mutableListOf(),
    
    /**
     * 调用的元数据，可用于存储额外信息
     */
    val metadata: MutableMap<String, Any> = mutableMapOf()
) 