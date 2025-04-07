package com.github.yangtuooc.codeaudit.model

import com.intellij.psi.PsiMethod
import java.util.*

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
     * 返回按调用层次有序的方法列表
     */
    fun getAllMethods(): List<PsiMethod> {
        // 如果没有调用链，则只返回入口点方法
        if (rootCall == null) {
            return listOf(entryPoint)
        }
        
        // 使用LinkedHashSet保持插入顺序并去重
        val methods = mutableListOf<PsiMethod>()
        
        // 首先添加入口点方法
        methods.add(entryPoint)
        
        // 然后按广度优先搜索(BFS)的顺序收集其他方法
        val queue = LinkedList<MethodCall>()
        val visited = HashSet<String>()
        
        if (rootCall != null) {
            queue.add(rootCall!!)
            visited.add(rootCall!!.method.toString())
        }
        
        while (queue.isNotEmpty()) {
            val current = queue.poll()
            
            // 避免重复添加入口点方法
            if (current.method != entryPoint) {
                methods.add(current.method)
            }
            
            // 添加子方法到队列
            for (child in current.children) {
                val childMethodKey = child.method.toString()
                if (!visited.contains(childMethodKey)) {
                    visited.add(childMethodKey)
                    queue.add(child)
                }
            }
        }
        
        return methods
    }
    
    /**
     * 递归收集调用链中的所有方法 (已废弃，改用BFS方式收集)
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