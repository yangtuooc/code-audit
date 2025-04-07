package com.github.yangtuooc.codeaudit.api

import com.github.yangtuooc.codeaudit.model.ApiParameter
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter

/**
 * 框架抽象，包含注解路径映射和特定框架的逻辑处理
 */
interface Framework {
    /**
     * 获取框架名称
     */
    val name: String
    
    /**
     * 获取框架的所有相关注解全限定名
     */
    val annotations: List<String>
    
    /**
     * 判断指定注解是否是控制器注解
     */
    fun isControllerAnnotation(annotationName: String): Boolean
    
    /**
     * 从控制器类中提取基础路径
     */
    fun extractBasePathFromController(controllerClass: PsiClass): String
    
    /**
     * 从方法中提取端点信息
     * @return Pair<httpMethod, path> 或 null
     */
    fun extractEndpointInfo(method: PsiMethod, basePath: String): Pair<String, String>?
    
    /**
     * 从方法参数中提取API参数信息
     */
    fun extractParameterInfo(parameter: PsiParameter): ApiParameter?
}

/**
 * JAX-RS框架接口，定义JAX-RS相关的注解和处理逻辑
 */
interface JaxRsFramework : Framework {
    val pathAnnotation: String
    val getAnnotation: String
    val postAnnotation: String
    val putAnnotation: String
    val deleteAnnotation: String
    val queryParamAnnotation: String
    val pathParamAnnotation: String
} 