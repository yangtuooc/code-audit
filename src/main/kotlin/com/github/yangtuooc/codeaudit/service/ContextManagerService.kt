package com.github.yangtuooc.codeaudit.service

import com.github.yangtuooc.codeaudit.api.ContextManager
import com.github.yangtuooc.codeaudit.model.AnalysisContext
import com.github.yangtuooc.codeaudit.model.CallChain
import com.github.yangtuooc.codeaudit.model.ElementContext
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 上下文管理服务实现类
 */
@Service(Service.Level.PROJECT)
class ContextManagerServiceImpl(private val project: Project) : ContextManager {
    
    private val log = logger<ContextManagerServiceImpl>()
    private val elementContextCache = ConcurrentHashMap<String, ElementContext>()
    private val callChainContextCache = ConcurrentHashMap<String, AnalysisContext>()
    
    override fun buildContext(callChain: CallChain): AnalysisContext {
        log.info("Building context for call chain: ${callChain.id}")
        
        // 检查缓存
        callChainContextCache[callChain.id]?.let {
            log.info("Using cached context for call chain: ${callChain.id}")
            return it
        }
        
        // 创建新的上下文
        val context = AnalysisContext(
            id = UUID.randomUUID().toString(),
            callChain = callChain,
            elementContexts = collectElementContexts(callChain)
        )
        
        // 缓存上下文
        callChainContextCache[callChain.id] = context
        
        return context
    }
    
    override fun updateContext(element: PsiElement) {
        log.info("Updating context for element: ${element.text?.take(20)}...")
        
        // 清除该元素的上下文缓存
        val elementId = getElementId(element)
        elementContextCache.remove(elementId)
        
        // 检查并更新受影响的调用链上下文
        callChainContextCache.values.forEach { context ->
            if (context.elementContexts.containsKey(elementId)) {
                log.info("Clearing call chain context ${context.id} affected by element change")
                callChainContextCache.remove(context.id)
            }
        }
    }
    
    override fun getElementContext(element: PsiElement): ElementContext {
        val elementId = getElementId(element)
        
        // 检查缓存
        elementContextCache[elementId]?.let {
            return it
        }
        
        // 创建新的元素上下文
        val context = createElementContext(element)
        elementContextCache[elementId] = context
        
        return context
    }
    
    override fun clearContext() {
        log.info("Clearing all context caches")
        elementContextCache.clear()
        callChainContextCache.clear()
    }
    
    /**
     * 为调用链收集所有元素的上下文
     */
    private fun collectElementContexts(callChain: CallChain): Map<String, ElementContext> {
        val contexts = mutableMapOf<String, ElementContext>()
        
        // 处理入口点方法
        val entryPointId = getElementId(callChain.entryPoint)
        contexts[entryPointId] = getElementContext(callChain.entryPoint)
        
        // 处理调用链中的所有节点
        callChain.nodes.forEach { node ->
            val nodeId = getElementId(node.method)
            contexts[nodeId] = getElementContext(node.method)
        }
        
        return contexts
    }
    
    /**
     * 创建元素的上下文
     */
    private fun createElementContext(element: PsiElement): ElementContext {
        return when (element) {
            is PsiMethod -> createMethodContext(element)
            else -> createGenericElementContext(element)
        }
    }
    
    /**
     * 创建方法的上下文
     */
    private fun createMethodContext(method: PsiMethod): ElementContext {
        val parameterTypes = method.parameterList.parameters.map { it.type.presentableText }
        val returnType = method.returnType?.presentableText ?: "void"
        
        return ElementContext(
            id = getElementId(method),
            element = method,
            elementType = "METHOD",
            parameterTypes = parameterTypes,
            returnType = returnType
        )
    }
    
    /**
     * 创建通用元素的上下文
     */
    private fun createGenericElementContext(element: PsiElement): ElementContext {
        return ElementContext(
            id = getElementId(element),
            element = element,
            elementType = element.javaClass.simpleName
        )
    }
    
    /**
     * 获取元素的唯一标识符
     */
    private fun getElementId(element: PsiElement): String {
        return when (element) {
            is PsiMethod -> {
                val containingClass = element.containingClass?.qualifiedName ?: "unknown"
                "$containingClass.${element.name}"
            }
            else -> "${element.javaClass.simpleName}_${element.hashCode()}"
        }
    }
} 