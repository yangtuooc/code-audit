package com.github.yangtuooc.codeaudit.api

import com.github.yangtuooc.codeaudit.model.ApiEndpoint
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod

/**
 * 代码解析服务接口，负责解析项目代码结构
 */
interface CodeParsingService {
    /**
     * 发现项目中的所有API端点
     * @return API端点列表
     */
    fun discoverApiEndpoints(): List<ApiEndpoint>

    /**
     * 根据特定框架或注解模式查找API端点
     * @param frameworkName 框架名称（如"Spring", "JAX-RS"等）
     * @return 符合条件的API端点列表
     */
    fun findApiEndpointsByFramework(frameworkName: String): List<ApiEndpoint>

    /**
     * 获取所有支持的框架名称
     * @return 支持的框架名称列表
     */
    fun getSupportedFrameworks(): List<String>

    /**
     * 查找方法的调用者
     * @param method 目标方法
     * @return 调用该方法的方法列表
     */
    fun findCallers(method: PsiMethod): List<PsiMethod>

    /**
     * 查找方法调用的其他方法
     * @param method 源方法
     * @return 被调用的方法列表
     */
    fun findCallees(method: PsiMethod): List<PsiMethod>

    /**
     * 获取类实现的所有接口
     * @param clazz 目标类
     * @return 实现的接口列表
     */
    fun getImplementedInterfaces(clazz: PsiClass): List<PsiClass>

    /**
     * 获取类的所有子类
     * @param clazz 目标类
     * @return 子类列表
     */
    fun getSubclasses(clazz: PsiClass): List<PsiClass>
} 