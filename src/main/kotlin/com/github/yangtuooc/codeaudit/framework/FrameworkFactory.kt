package com.github.yangtuooc.codeaudit.framework

import com.github.yangtuooc.codeaudit.api.Framework

/**
 * 框架工厂类，负责创建和管理框架实现
 */
object FrameworkFactory {
    // 支持的所有框架实现
    private val frameworks = listOf(
        SpringFramework(),
        JavaxJaxRsFramework(),
        JakartaJaxRsFramework(),
    )

    /**
     * 获取所有支持的框架
     */
    fun getAllFrameworks(): List<Framework> {
        return frameworks
    }

    /**
     * 获取所有支持的框架名称
     */
    fun getAllFrameworkNames(): List<String> {
        return frameworks.map { it.name }
    }

    /**
     * 通过名称获取框架实现
     */
    fun getFramework(name: String): Framework? {
        return frameworks.find { it.name == name }
    }
} 