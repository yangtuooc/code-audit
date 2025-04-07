package com.github.yangtuooc.codeaudit.service

import com.github.yangtuooc.codeaudit.api.CodeParsingService
import com.github.yangtuooc.codeaudit.api.Framework
import com.github.yangtuooc.codeaudit.framework.FrameworkFactory
import com.github.yangtuooc.codeaudit.model.ApiEndpoint
import com.github.yangtuooc.codeaudit.model.ApiParameter
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.*
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

    override fun discoverApiEndpoints(): List<ApiEndpoint> {
        log.info("开始发现API端点")
        val allEndpoints = mutableListOf<ApiEndpoint>()

        // 对于每个支持的框架，查找其API端点
        FrameworkFactory.getAllFrameworks().forEach { framework ->
            log.info("在框架 ${framework.name} 中查找端点")
            val endpoints = findApiEndpointsByFramework(framework)
            allEndpoints.addAll(endpoints)
        }

        log.info("共发现 ${allEndpoints.size} 个API端点")
        return allEndpoints
    }

    private fun findApiEndpointsByFramework(framework: Framework): List<ApiEndpoint> {
        log.info("查找框架 ${framework.name} 的API端点")
        val endpoints = mutableListOf<ApiEndpoint>()

        // 所有PSI操作都应该在读操作中执行
        val psiFacade = JavaPsiFacade.getInstance(project)
        val searchScope = GlobalSearchScope.projectScope(project)

        // 查找带有控制器注解的类
        for (controllerAnnotation in framework.annotations.filter { framework.isControllerAnnotation(it) }) {
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
            }

            // 对于每个控制器类，查找其API端点方法
            for (controllerClass in controllerClasses) {
                val basePath = ReadAction.compute<String, Throwable> {
                    framework.extractBasePathFromController(controllerClass)
                }

                // 处理类中的每个方法
                val methods = ReadAction.compute<Array<PsiMethod>, Throwable> {
                    controllerClass.methods
                }

                for (method in methods) {
                    val endpointInfo = ReadAction.compute<ApiEndpoint?, Throwable> {
                        val info = framework.extractEndpointInfo(method, basePath)
                        if (info != null) {
                            val (httpMethod, fullPath) = info

                            // 提取参数信息
                            val parameters = extractParameters(method, framework)

                            // 获取返回类型
                            val returnType = method.returnType?.presentableText ?: "void"

                            // 创建API端点对象
                            ApiEndpoint(
                                id = UUID.randomUUID().toString(),
                                path = fullPath,
                                httpMethod = httpMethod,
                                psiMethod = method,
                                controllerName = method.containingClass?.qualifiedName ?: "",
                                methodName = method.name,
                                parameters = parameters,
                                returnType = returnType
                            )
                        } else {
                            null
                        }
                    }

                    if (endpointInfo != null) {
                        endpoints.add(endpointInfo)
                    }
                }
            }
        }

        log.info("在框架 ${framework.name} 中发现了 ${endpoints.size} 个API端点")
        return endpoints
    }

    override fun findApiEndpointsByFramework(frameworkName: String): List<ApiEndpoint> {
        // 根据框架名称查找对应的框架实现
        val framework = FrameworkFactory.getFramework(frameworkName)
        return if (framework != null) {
            findApiEndpointsByFramework(framework)
        } else {
            log.warn("未支持的框架: $frameworkName")
            emptyList()
        }
    }

    override fun getSupportedFrameworks(): List<String> {
        return FrameworkFactory.getAllFrameworkNames()
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

    // 辅助方法：提取方法参数信息
    private fun extractParameters(method: PsiMethod, framework: Framework): List<ApiParameter> {
        val parameters = mutableListOf<ApiParameter>()

        // 遍历方法的所有参数
        for (parameter in method.parameterList.parameters) {
            // 尝试使用框架特定的参数解析逻辑
            val paramInfo = framework.extractParameterInfo(parameter)

            // 如果框架解析成功，添加该参数
            if (paramInfo != null) {
                parameters.add(paramInfo)
            } else {
                // 否则，添加为普通参数
                parameters.add(
                    ApiParameter(
                        name = parameter.name,
                        type = parameter.type.presentableText,
                        required = true,  // 默认为必需
                        description = null
                    )
                )
            }
        }

        return parameters
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