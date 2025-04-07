package com.github.yangtuooc.codeaudit.model

import com.intellij.psi.PsiMethod

/**
 * 表示API端点的模型类
 */
data class ApiEndpoint(
    /**
     * 端点的唯一标识符
     */
    val id: String,
    
    /**
     * 端点的URL路径
     */
    val path: String,
    
    /**
     * HTTP方法（GET, POST, PUT等）
     */
    val httpMethod: String,
    
    /**
     * 对应的PSI方法
     */
    val psiMethod: PsiMethod,
    
    /**
     * 端点所属的控制器类名
     */
    val controllerName: String,
    
    /**
     * 端点方法名
     */
    val methodName: String,
    
    /**
     * 端点的参数列表
     */
    val parameters: List<ApiParameter> = emptyList(),
    
    /**
     * 端点的返回类型描述
     */
    val returnType: String,
    
    /**
     * 端点的调用链
     */
    var callChain: CallChain? = null
)

/**
 * API端点参数
 */
data class ApiParameter(
    /**
     * 参数名
     */
    val name: String,
    
    /**
     * 参数类型
     */
    val type: String,
    
    /**
     * 参数是否必须
     */
    val required: Boolean = false,
    
    /**
     * 参数默认值
     */
    val defaultValue: String? = null,
    
    /**
     * 参数描述
     */
    val description: String? = null
) 