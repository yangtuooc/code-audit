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
     * 调用链的根节点
     */
    var rootCall: MethodCall? = null,
    
    /**
     * 最近一次构建调用链的时间戳
     */
    val timestamp: Long = System.currentTimeMillis()
) {
    /**
     * 获取调用链的深度
     */
    fun getDepth(): Int {
        return calculateDepth(rootCall, 0)
    }
    
    /**
     * 递归计算调用深度
     */
    private fun calculateDepth(call: MethodCall?, currentDepth: Int): Int {
        if (call == null || call.children.isEmpty()) {
            return currentDepth
        }
        
        var maxDepth = currentDepth
        for (child in call.children) {
            val childDepth = calculateDepth(child, currentDepth + 1)
            if (childDepth > maxDepth) {
                maxDepth = childDepth
            }
        }
        
        return maxDepth
    }
    
    /**
     * 获取调用链中的所有方法
     */
    fun getAllMethods(): List<PsiMethod> {
        val methods = mutableListOf<PsiMethod>()
        collectMethods(rootCall, methods)
        return methods
    }
    
    /**
     * 递归收集调用链中的所有方法
     */
    private fun collectMethods(call: MethodCall?, methods: MutableList<PsiMethod>) {
        if (call == null) {
            return
        }
        
        methods.add(call.method)
        for (child in call.children) {
            collectMethods(child, methods)
        }
    }
} 