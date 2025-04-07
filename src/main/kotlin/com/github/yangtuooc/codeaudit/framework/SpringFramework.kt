package com.github.yangtuooc.codeaudit.framework

import com.github.yangtuooc.codeaudit.api.Framework
import com.github.yangtuooc.codeaudit.model.ApiParameter
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiParameter

/**
 * Spring框架实现
 */
class SpringFramework : Framework {
    override val name = "Spring"

    override val annotations = listOf(
        "org.springframework.web.bind.annotation.RestController",
        "org.springframework.web.bind.annotation.Controller",
        "org.springframework.web.bind.annotation.RequestMapping",
        "org.springframework.web.bind.annotation.GetMapping",
        "org.springframework.web.bind.annotation.PostMapping",
        "org.springframework.web.bind.annotation.PutMapping",
        "org.springframework.web.bind.annotation.DeleteMapping",
        "org.springframework.web.bind.annotation.PatchMapping"
    )

    override fun isControllerAnnotation(annotationName: String): Boolean {
        return annotationName.endsWith("Controller") || annotationName.endsWith("RestController")
    }

    override fun extractBasePathFromController(controllerClass: PsiClass): String {
        // 查找类上的RequestMapping注解
        val requestMappingAnnotation =
            controllerClass.getAnnotation("org.springframework.web.bind.annotation.RequestMapping")
        var basePath = ""

        if (requestMappingAnnotation != null) {
            basePath = getAnnotationAttributeValue(requestMappingAnnotation, "value") ?: getAnnotationAttributeValue(
                requestMappingAnnotation,
                "path"
            ) ?: ""
        }

        // 确保路径格式正确
        return PathUtils.formatPath(basePath)
    }

    override fun extractEndpointInfo(method: PsiMethod, basePath: String): Pair<String, String>? {
        var httpMethod = ""
        var path = ""

        // 检查各种映射注解
        when {
            method.hasAnnotation("org.springframework.web.bind.annotation.GetMapping") -> {
                httpMethod = "GET"
                path = getPathFromSpringMappingAnnotation(method, "org.springframework.web.bind.annotation.GetMapping")
            }

            method.hasAnnotation("org.springframework.web.bind.annotation.PostMapping") -> {
                httpMethod = "POST"
                path = getPathFromSpringMappingAnnotation(method, "org.springframework.web.bind.annotation.PostMapping")
            }

            method.hasAnnotation("org.springframework.web.bind.annotation.PutMapping") -> {
                httpMethod = "PUT"
                path = getPathFromSpringMappingAnnotation(method, "org.springframework.web.bind.annotation.PutMapping")
            }

            method.hasAnnotation("org.springframework.web.bind.annotation.DeleteMapping") -> {
                httpMethod = "DELETE"
                path =
                    getPathFromSpringMappingAnnotation(method, "org.springframework.web.bind.annotation.DeleteMapping")
            }

            method.hasAnnotation("org.springframework.web.bind.annotation.PatchMapping") -> {
                httpMethod = "PATCH"
                path =
                    getPathFromSpringMappingAnnotation(method, "org.springframework.web.bind.annotation.PatchMapping")
            }

            method.hasAnnotation("org.springframework.web.bind.annotation.RequestMapping") -> {
                val annotation = method.getAnnotation("org.springframework.web.bind.annotation.RequestMapping")
                if (annotation != null) {
                    path = getPathFromSpringMappingAnnotation(
                        method,
                        "org.springframework.web.bind.annotation.RequestMapping"
                    )
                    val methodAttr = getAnnotationAttributeValue(annotation, "method")
                    httpMethod = if (methodAttr != null && methodAttr.contains("GET")) {
                        "GET"
                    } else if (methodAttr != null && methodAttr.contains("POST")) {
                        "POST"
                    } else if (methodAttr != null && methodAttr.contains("PUT")) {
                        "PUT"
                    } else if (methodAttr != null && methodAttr.contains("DELETE")) {
                        "DELETE"
                    } else if (methodAttr != null && methodAttr.contains("PATCH")) {
                        "PATCH"
                    } else {
                        // 默认为GET
                        "GET"
                    }
                }
            }
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

    private fun getPathFromSpringMappingAnnotation(method: PsiMethod, annotationName: String): String {
        val annotation = method.getAnnotation(annotationName) ?: return ""
        return getAnnotationAttributeValue(annotation, "value") ?: getAnnotationAttributeValue(annotation, "path") ?: ""
    }

    override fun extractParameterInfo(parameter: PsiParameter): ApiParameter? {
        // 检查@RequestParam注解
        val requestParamAnnotation = parameter.getAnnotation("org.springframework.web.bind.annotation.RequestParam")
        if (requestParamAnnotation != null) {
            val paramName = getAnnotationAttributeValue(requestParamAnnotation, "name") ?: getAnnotationAttributeValue(
                requestParamAnnotation,
                "value"
            ) ?: parameter.name
            val defaultValue = getAnnotationAttributeValue(requestParamAnnotation, "defaultValue")
            val requiredValue = getAnnotationAttributeValue(requestParamAnnotation, "required")
            val required = requiredValue?.toBoolean() ?: true

            return ApiParameter(
                name = paramName,
                type = parameter.type.presentableText,
                required = required,
                defaultValue = defaultValue,
                description = "Request parameter"
            )
        }

        // 检查@PathVariable注解
        val pathVariableAnnotation = parameter.getAnnotation("org.springframework.web.bind.annotation.PathVariable")
        if (pathVariableAnnotation != null) {
            val paramName = getAnnotationAttributeValue(pathVariableAnnotation, "name") ?: getAnnotationAttributeValue(
                pathVariableAnnotation,
                "value"
            ) ?: parameter.name
            val requiredValue = getAnnotationAttributeValue(pathVariableAnnotation, "required")
            val required = requiredValue?.toBoolean() ?: true

            return ApiParameter(
                name = paramName,
                type = parameter.type.presentableText,
                required = required,
                description = "Path variable"
            )
        }

        // 检查@RequestBody注解
        val requestBodyAnnotation = parameter.getAnnotation("org.springframework.web.bind.annotation.RequestBody")
        if (requestBodyAnnotation != null) {
            val requiredValue = getAnnotationAttributeValue(requestBodyAnnotation, "required")
            val required = requiredValue?.toBoolean() ?: true

            return ApiParameter(
                name = parameter.name ?: "",
                type = parameter.type.presentableText,
                required = required,
                description = "Request body"
            )
        }

        return null
    }

    // 辅助方法：获取注解属性值
    private fun getAnnotationAttributeValue(annotation: PsiAnnotation?, attributeName: String): String? {
        if (annotation == null) return null

        val value = annotation.findAttributeValue(attributeName) ?: return null

        // 去除引号和大括号
        return value.text.trim()
            .removePrefix("\"").removeSuffix("\"")  // 移除字符串引号
            .removePrefix("{").removeSuffix("}")    // 移除数组大括号
    }
} 