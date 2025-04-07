package com.github.yangtuooc.codeaudit.framework

/**
 * jakarta版本的JAX-RS框架实现
 */
class JakartaJaxRsFramework : AbstractJaxRsFramework() {
    override val pathAnnotation = "jakarta.ws.rs.Path"
    override val getAnnotation = "jakarta.ws.rs.GET"
    override val postAnnotation = "jakarta.ws.rs.POST"
    override val putAnnotation = "jakarta.ws.rs.PUT"
    override val deleteAnnotation = "jakarta.ws.rs.DELETE"
    override val queryParamAnnotation = "jakarta.ws.rs.QueryParam"
    override val pathParamAnnotation = "jakarta.ws.rs.PathParam"
} 