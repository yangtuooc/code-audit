package com.github.yangtuooc.codeaudit.framework

import com.github.yangtuooc.codeaudit.api.JaxRsFramework
import com.github.yangtuooc.codeaudit.model.ApiParameter
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter

/**
 * JAX-RS框架的抽象实现，包含共用逻辑
 */
abstract class AbstractJaxRsFramework : JaxRsFramework {
    override val name = "JAX-RS"

    override val annotations: List<String>
        get() = listOf(
            pathAnnotation,
            getAnnotation,
            postAnnotation,
            putAnnotation,
            deleteAnnotation
        )

    override fun isControllerAnnotation(annotationName: String): Boolean {
        return annotationName.endsWith("Path") || annotationName.endsWith("Resource")
    }

    override fun extractBasePathFromController(controllerClass: PsiClass): String {
        // 查找类上的Path注解
        val pathAnnotation = controllerClass.getAnnotation(pathAnnotation)
        var basePath = ""

        if (pathAnnotation != null) {
            basePath = getAnnotationAttributeValue(pathAnnotation, "value") ?: ""
        }

        // 确保路径格式正确
        return PathUtils.formatPath(basePath)
    }

    override fun extractEndpointInfo(method: PsiMethod, basePath: String): Pair<String, String>? {
        var httpMethod = ""
        var path = ""

        // 检查JAX-RS注解
        when {
            method.hasAnnotation(getAnnotation) -> {
                httpMethod = "GET"
            }

            method.hasAnnotation(postAnnotation) -> {
                httpMethod = "POST"
            }

            method.hasAnnotation(putAnnotation) -> {
                httpMethod = "PUT"
            }

            method.hasAnnotation(deleteAnnotation) -> {
                httpMethod = "DELETE"
            }
        }

        // 获取Path注解的值
        val pathAnnotation = method.getAnnotation(pathAnnotation)
        if (pathAnnotation != null) {
            path = getAnnotationAttributeValue(pathAnnotation, "value") ?: ""
        }

        // 如果没有找到HTTP方法，则不是API端点
        if (httpMethod.isEmpty()) {
            return null
        }

        // 确保路径格式正确
        path = PathUtils.formatPath(path)

        // 组合基础路径和方法路径
        val fullPath = PathUtils.combinePaths(basePath, path)

        return Pair(httpMethod, fullPath)
    }

    override fun extractParameterInfo(parameter: PsiParameter): ApiParameter? {
        // 处理JAX-RS参数注解
        // 检查@QueryParam注解
        val queryParamAnnotation = parameter.getAnnotation(queryParamAnnotation)
        if (queryParamAnnotation != null) {
            val paramName = getAnnotationAttributeValue(queryParamAnnotation, "value") ?: parameter.name

            return ApiParameter(
                name = paramName,
                type = parameter.type.presentableText,
                required = false,  // JAX-RS没有required属性
                description = "Query parameter"
            )
        }

        // 检查@PathParam注解
        val pathParamAnnotation = parameter.getAnnotation(pathParamAnnotation)
        if (pathParamAnnotation != null) {
            val paramName = getAnnotationAttributeValue(pathParamAnnotation, "value") ?: parameter.name

            return ApiParameter(
                name = paramName,
                type = parameter.type.presentableText,
                required = true,  // 路径参数总是必需的
                description = "Path parameter"
            )
        }

        return null
    }

    // 辅助方法：获取注解属性值
    protected fun getAnnotationAttributeValue(annotation: PsiAnnotation?, attributeName: String): String? {
        if (annotation == null) return null

        val value = annotation.findAttributeValue(attributeName) ?: return null

        // 去除引号和大括号
        return value.text.trim()
            .removePrefix("\"").removeSuffix("\"")  // 移除字符串引号
            .removePrefix("{").removeSuffix("}")    // 移除数组大括号
    }
} 