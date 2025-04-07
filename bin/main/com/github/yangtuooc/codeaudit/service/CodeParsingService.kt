package com.github.yangtuooc.codeaudit.service

import com.github.yangtuooc.codeaudit.api.CodeParsingService
import com.github.yangtuooc.codeaudit.model.ApiEndpoint
import com.github.yangtuooc.codeaudit.model.ApiParameter
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.impl.source.PsiClassReferenceType
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.ClassInheritorsSearch
import com.intellij.psi.search.searches.ReferencesSearch
import com.intellij.psi.util.PsiTreeUtil
import java.util.*

/**
 * 代码解析服务实现类
 */
@Service(Service.Level.PROJECT)
class CodeParsingServiceImpl(private val project: Project) : CodeParsingService {
    private val log = logger<CodeParsingServiceImpl>()

    // 框架相关的注解和接口映射
    private val frameworkMappings = mapOf(
        "Spring" to listOf(
            "org.springframework.web.bind.annotation.RestController",
            "org.springframework.web.bind.annotation.Controller",
            "org.springframework.web.bind.annotation.RequestMapping",
            "org.springframework.web.bind.annotation.GetMapping",
            "org.springframework.web.bind.annotation.PostMapping",
            "org.springframework.web.bind.annotation.PutMapping",
            "org.springframework.web.bind.annotation.DeleteMapping",
            "org.springframework.web.bind.annotation.PatchMapping"
        ),
        "JAX-RS" to listOf(
            "javax.ws.rs.Path",
            "javax.ws.rs.GET",
            "javax.ws.rs.POST",
            "javax.ws.rs.PUT",
            "javax.ws.rs.DELETE"
        ),
        "Micronaut" to listOf(
            "io.micronaut.http.annotation.Controller",
            "io.micronaut.http.annotation.Get",
            "io.micronaut.http.annotation.Post",
            "io.micronaut.http.annotation.Put",
            "io.micronaut.http.annotation.Delete"
        )
    )

    override fun discoverApiEndpoints(): List<ApiEndpoint> {
        log.info("开始发现API端点")
        val allEndpoints = mutableListOf<ApiEndpoint>()

        // 对于每个支持的框架，查找其API端点
        frameworkMappings.keys.forEach { framework ->
            log.info("在框架 $framework 中查找端点")
            val endpoints = findApiEndpointsByFramework(framework)
            allEndpoints.addAll(endpoints)
        }

        log.info("共发现 ${allEndpoints.size} 个API端点")
        return allEndpoints
    }

    override fun findApiEndpointsByFramework(framework: String): List<ApiEndpoint> {
        log.info("查找框架 $framework 的API端点")
        val endpoints = mutableListOf<ApiEndpoint>()

        // 获取该框架的注解列表
        val annotations = frameworkMappings[framework] ?: return emptyList()

        // 所有PSI操作都应该在读操作中执行
        val psiFacade = JavaPsiFacade.getInstance(project)
        val searchScope = GlobalSearchScope.projectScope(project)

        // 查找带有控制器注解的类
        for (controllerAnnotation in annotations.filter { isControllerAnnotation(it) }) {
            val annotationClass = ReadAction.compute<PsiClass?, Throwable> { 
                psiFacade.findClass(controllerAnnotation, searchScope) 
            } ?: continue
            
            // 查找使用该注解的类
            val controllerClasses = mutableListOf<PsiClass>()
            ReadAction.compute<Unit, Throwable> {
                ReferencesSearch.search(annotationClass, searchScope).forEach { reference ->
                    val element = reference.element.parent
                    if (element is PsiModifierListOwner) {
                        val psiClass = element.parent
                        if (psiClass is PsiClass) {
                            controllerClasses.add(psiClass)
                        }
                    }
                }
                Unit
            }
            
            // 对于每个控制器类，查找其API端点方法
            for (controllerClass in controllerClasses) {
                val basePath = ReadAction.compute<String, Throwable> {
                    getBasePathFromController(controllerClass, framework)
                }
                
                // 处理类中的每个方法
                val methods = ReadAction.compute<Array<PsiMethod>, Throwable> {
                    controllerClass.methods
                }
                
                for (method in methods) {
                    val endpointInfo = ReadAction.compute<ApiEndpoint?, Throwable> {
                        extractEndpointInfo(method, basePath, framework)
                    }
                    if (endpointInfo != null) {
                        endpoints.add(endpointInfo)
                    }
                }
            }
        }
        
        log.info("在框架 $framework 中发现了 ${endpoints.size} 个API端点")
        return endpoints
    }

    override fun findCallers(method: PsiMethod): List<PsiMethod> {
        val callers = mutableListOf<PsiMethod>()
        val searchScope = GlobalSearchScope.projectScope(project)
        
        // 查找引用该方法的元素
        ReadAction.compute<Unit, Throwable> {
            ReferencesSearch.search(method, searchScope).forEach { reference ->
                val element = reference.element
                // 找到包含该引用的方法
                val caller = PsiTreeUtil.getParentOfType(element, PsiMethod::class.java)
                if (caller != null && !callers.contains(caller)) {
                    callers.add(caller)
                }
            }
            Unit
        }
        
        return callers
    }

    override fun findCallees(method: PsiMethod): List<PsiMethod> {
        val callees = mutableListOf<PsiMethod>()
        
        // 查找方法体中的所有方法调用表达式
        ReadAction.compute<Unit, Throwable> {
            PsiTreeUtil.findChildrenOfType(method, PsiMethodCallExpression::class.java).forEach { call ->
                val resolvedMethod = call.resolveMethod()
                if (resolvedMethod != null && !callees.contains(resolvedMethod) && !isJdkMethod(resolvedMethod)) {
                    callees.add(resolvedMethod)
                }
            }
            Unit
        }
        
        return callees
    }

    override fun getImplementedInterfaces(clazz: PsiClass): List<PsiClass> {
        val interfaces = mutableListOf<PsiClass>()
        
        ReadAction.compute<Unit, Throwable> {
            // 获取直接实现的接口
            clazz.interfaces.forEach {
                interfaces.add(it)
            }
            
            // 获取通过父类继承的接口
            var superClass = clazz.superClass
            while (superClass != null) {
                superClass.interfaces.forEach {
                    if (!interfaces.contains(it)) {
                        interfaces.add(it)
                    }
                }
                superClass = superClass.superClass
            }
            Unit
        }
        
        return interfaces
    }

    override fun getSubclasses(clazz: PsiClass): List<PsiClass> {
        val subclasses = mutableListOf<PsiClass>()
        val searchScope = GlobalSearchScope.projectScope(project)
        
        // 查找直接子类
        ReadAction.compute<Unit, Throwable> {
            ClassInheritorsSearch.search(clazz, searchScope, true).forEach {
                subclasses.add(it)
            }
            Unit
        }
        
        return subclasses
    }
    
    // 辅助方法：判断是否是控制器注解
    private fun isControllerAnnotation(annotation: String): Boolean {
        return annotation.endsWith("Controller") ||
                annotation.endsWith("RestController") ||
                annotation.endsWith("Resource") ||
                annotation.endsWith("Path")
    }
    
    // 辅助方法：获取控制器的基础路径
    private fun getBasePathFromController(controllerClass: PsiClass, framework: String): String {
        // 默认基础路径为空
        var basePath = ""
        
        // 根据不同框架获取基础路径
        when (framework) {
            "Spring" -> {
                // 查找类上的RequestMapping注解
                val requestMappingAnnotation = controllerClass.getAnnotation("org.springframework.web.bind.annotation.RequestMapping")
                if (requestMappingAnnotation != null) {
                    basePath = getAnnotationAttributeValue(requestMappingAnnotation, "value") ?: 
                              getAnnotationAttributeValue(requestMappingAnnotation, "path") ?: ""
                }
            }
            "JAX-RS" -> {
                // 查找类上的Path注解
                val pathAnnotation = controllerClass.getAnnotation("javax.ws.rs.Path")
                if (pathAnnotation != null) {
                    basePath = getAnnotationAttributeValue(pathAnnotation, "value") ?: ""
                }
            }
            "Micronaut" -> {
                // 查找类上的Controller注解
                val controllerAnnotation = controllerClass.getAnnotation("io.micronaut.http.annotation.Controller")
                if (controllerAnnotation != null) {
                    basePath = getAnnotationAttributeValue(controllerAnnotation, "value") ?: ""
                }
            }
        }
        
        // 确保路径以/开头且不以/结尾
        if (basePath.isNotEmpty()) {
            if (!basePath.startsWith("/")) {
                basePath = "/$basePath"
            }
            if (basePath.endsWith("/")) {
                basePath = basePath.substring(0, basePath.length - 1)
            }
        }
        
        return basePath
    }
    
    // 辅助方法：从方法中提取端点信息
    private fun extractEndpointInfo(method: PsiMethod, basePath: String, framework: String): ApiEndpoint? {
        var httpMethod = ""
        var path = ""
        
        // 根据框架类型解析不同的注解
        when (framework) {
            "Spring" -> {
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
                        path = getPathFromSpringMappingAnnotation(method, "org.springframework.web.bind.annotation.DeleteMapping")
                    }
                    method.hasAnnotation("org.springframework.web.bind.annotation.PatchMapping") -> {
                        httpMethod = "PATCH"
                        path = getPathFromSpringMappingAnnotation(method, "org.springframework.web.bind.annotation.PatchMapping")
                    }
                    method.hasAnnotation("org.springframework.web.bind.annotation.RequestMapping") -> {
                        val annotation = method.getAnnotation("org.springframework.web.bind.annotation.RequestMapping")
                        if (annotation != null) {
                            path = getPathFromSpringMappingAnnotation(method, "org.springframework.web.bind.annotation.RequestMapping")
                            val methodAttr = getAnnotationAttributeValue(annotation, "method")
                            if (methodAttr != null && methodAttr.contains("GET")) {
                                httpMethod = "GET"
                            } else if (methodAttr != null && methodAttr.contains("POST")) {
                                httpMethod = "POST"
                            } else if (methodAttr != null && methodAttr.contains("PUT")) {
                                httpMethod = "PUT"
                            } else if (methodAttr != null && methodAttr.contains("DELETE")) {
                                httpMethod = "DELETE"
                            } else if (methodAttr != null && methodAttr.contains("PATCH")) {
                                httpMethod = "PATCH"
                            } else {
                                // 默认为GET
                                httpMethod = "GET"
                            }
                        }
                    }
                }
            }
            "JAX-RS" -> {
                // 检查JAX-RS注解
                when {
                    method.hasAnnotation("javax.ws.rs.GET") -> {
                        httpMethod = "GET"
                    }
                    method.hasAnnotation("javax.ws.rs.POST") -> {
                        httpMethod = "POST"
                    }
                    method.hasAnnotation("javax.ws.rs.PUT") -> {
                        httpMethod = "PUT"
                    }
                    method.hasAnnotation("javax.ws.rs.DELETE") -> {
                        httpMethod = "DELETE"
                    }
                    method.hasAnnotation("javax.ws.rs.HEAD") -> {
                        httpMethod = "HEAD"
                    }
                }
                
                // 获取Path注解的值
                val pathAnnotation = method.getAnnotation("javax.ws.rs.Path")
                if (pathAnnotation != null) {
                    path = getAnnotationAttributeValue(pathAnnotation, "value") ?: ""
                }
            }
            "Micronaut" -> {
                // 检查Micronaut注解
                when {
                    method.hasAnnotation("io.micronaut.http.annotation.Get") -> {
                        httpMethod = "GET"
                        path = getAnnotationAttributeValue(method.getAnnotation("io.micronaut.http.annotation.Get"), "value") ?: ""
                    }
                    method.hasAnnotation("io.micronaut.http.annotation.Post") -> {
                        httpMethod = "POST"
                        path = getAnnotationAttributeValue(method.getAnnotation("io.micronaut.http.annotation.Post"), "value") ?: ""
                    }
                    method.hasAnnotation("io.micronaut.http.annotation.Put") -> {
                        httpMethod = "PUT"
                        path = getAnnotationAttributeValue(method.getAnnotation("io.micronaut.http.annotation.Put"), "value") ?: ""
                    }
                    method.hasAnnotation("io.micronaut.http.annotation.Delete") -> {
                        httpMethod = "DELETE"
                        path = getAnnotationAttributeValue(method.getAnnotation("io.micronaut.http.annotation.Delete"), "value") ?: ""
                    }
                }
            }
        }
        
        // 如果没有找到HTTP方法或路径，则不是API端点
        if (httpMethod.isEmpty()) {
            return null
        }
        
        // 确保路径以/开头
        if (path.isNotEmpty() && !path.startsWith("/")) {
            path = "/$path"
        }
        
        // 组合基础路径和方法路径
        val fullPath = if (path.isEmpty()) basePath else "$basePath$path"
        
        // 提取参数信息
        val parameters = extractParameters(method, framework)
        
        // 获取返回类型
        val returnType = method.returnType?.presentableText ?: "void"
        
        // 创建并返回API端点对象
        return ApiEndpoint(
            id = UUID.randomUUID().toString(),
            path = fullPath,
            httpMethod = httpMethod,
            psiMethod = method,
            controllerName = method.containingClass?.qualifiedName ?: "",
            methodName = method.name,
            parameters = parameters,
            returnType = returnType
        )
    }
    
    // 辅助方法：从Spring的映射注解中获取路径
    private fun getPathFromSpringMappingAnnotation(method: PsiMethod, annotationName: String): String {
        val annotation = method.getAnnotation(annotationName) ?: return ""
        return getAnnotationAttributeValue(annotation, "value") ?: 
               getAnnotationAttributeValue(annotation, "path") ?: ""
    }
    
    // 辅助方法：提取方法参数信息
    private fun extractParameters(method: PsiMethod, framework: String): List<ApiParameter> {
        val parameters = mutableListOf<ApiParameter>()
        
        // 遍历方法的所有参数
        for (parameter in method.parameterList.parameters) {
            var required = true
            var description: String? = null
            
            // 根据不同框架处理参数注解
            when (framework) {
                "Spring" -> {
                    // 检查@RequestParam注解
                    val requestParamAnnotation = parameter.getAnnotation("org.springframework.web.bind.annotation.RequestParam")
                    if (requestParamAnnotation != null) {
                        val paramName = getAnnotationAttributeValue(requestParamAnnotation, "name") ?: 
                                        getAnnotationAttributeValue(requestParamAnnotation, "value") ?: 
                                        parameter.name
                        val defaultValue = getAnnotationAttributeValue(requestParamAnnotation, "defaultValue")
                        val requiredValue = getAnnotationAttributeValue(requestParamAnnotation, "required")
                        required = requiredValue?.toBoolean() ?: true
                        
                        parameters.add(ApiParameter(
                            name = paramName ?: parameter.name ?: "",
                            type = parameter.type.presentableText,
                            required = required,
                            defaultValue = defaultValue,
                            description = description
                        ))
                        continue
                    }
                    
                    // 检查@PathVariable注解
                    val pathVariableAnnotation = parameter.getAnnotation("org.springframework.web.bind.annotation.PathVariable")
                    if (pathVariableAnnotation != null) {
                        val paramName = getAnnotationAttributeValue(pathVariableAnnotation, "name") ?: 
                                        getAnnotationAttributeValue(pathVariableAnnotation, "value") ?: 
                                        parameter.name
                        val requiredValue = getAnnotationAttributeValue(pathVariableAnnotation, "required")
                        required = requiredValue?.toBoolean() ?: true
                        
                        parameters.add(ApiParameter(
                            name = paramName ?: parameter.name ?: "",
                            type = parameter.type.presentableText,
                            required = required,
                            description = "Path variable"
                        ))
                        continue
                    }
                    
                    // 检查@RequestBody注解
                    val requestBodyAnnotation = parameter.getAnnotation("org.springframework.web.bind.annotation.RequestBody")
                    if (requestBodyAnnotation != null) {
                        val requiredValue = getAnnotationAttributeValue(requestBodyAnnotation, "required")
                        required = requiredValue?.toBoolean() ?: true
                        
                        parameters.add(ApiParameter(
                            name = parameter.name ?: "",
                            type = parameter.type.presentableText,
                            required = required,
                            description = "Request body"
                        ))
                        continue
                    }
                }
                "JAX-RS" -> {
                    // 处理JAX-RS参数注解
                    // 检查@QueryParam注解
                    val queryParamAnnotation = parameter.getAnnotation("javax.ws.rs.QueryParam")
                    if (queryParamAnnotation != null) {
                        val paramName = getAnnotationAttributeValue(queryParamAnnotation, "value") ?: parameter.name
                        
                        parameters.add(ApiParameter(
                            name = paramName ?: parameter.name ?: "",
                            type = parameter.type.presentableText,
                            required = false,  // JAX-RS没有required属性
                            description = "Query parameter"
                        ))
                        continue
                    }
                    
                    // 检查@PathParam注解
                    val pathParamAnnotation = parameter.getAnnotation("javax.ws.rs.PathParam")
                    if (pathParamAnnotation != null) {
                        val paramName = getAnnotationAttributeValue(pathParamAnnotation, "value") ?: parameter.name
                        
                        parameters.add(ApiParameter(
                            name = paramName ?: parameter.name ?: "",
                            type = parameter.type.presentableText,
                            required = true,  // 路径参数总是必需的
                            description = "Path parameter"
                        ))
                        continue
                    }
                }
                // 可以根据需要添加其他框架的处理逻辑
            }
            
            // 如果没有特殊注解，添加为普通参数
            parameters.add(ApiParameter(
                name = parameter.name ?: "",
                type = parameter.type.presentableText,
                required = true,  // 默认为必需
                description = null
            ))
        }
        
        return parameters
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
    
    // 辅助方法：检查类是否有指定注解
    private fun PsiClass.hasAnnotation(qualifiedName: String): Boolean {
        return this.getAnnotation(qualifiedName) != null
    }
    
    // 辅助方法：检查方法是否有指定注解
    private fun PsiMethod.hasAnnotation(qualifiedName: String): Boolean {
        return this.getAnnotation(qualifiedName) != null
    }
    
    // 辅助方法：判断是否是JDK方法
    private fun isJdkMethod(method: PsiMethod): Boolean {
        val containingClass = method.containingClass ?: return false
        val qualifiedName = containingClass.qualifiedName ?: return false
        return qualifiedName.startsWith("java.") || 
               qualifiedName.startsWith("javax.") || 
               qualifiedName.startsWith("sun.") ||
               qualifiedName.startsWith("com.sun.") ||
               qualifiedName.startsWith("oracle.")
    }
} 