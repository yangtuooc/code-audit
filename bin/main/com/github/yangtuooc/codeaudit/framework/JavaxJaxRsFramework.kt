package com.github.yangtuooc.codeaudit.framework

/**
 * javax版本的JAX-RS框架实现
 */
class JavaxJaxRsFramework : AbstractJaxRsFramework() {
    override val pathAnnotation = "javax.ws.rs.Path"
    override val getAnnotation = "javax.ws.rs.GET"
    override val postAnnotation = "javax.ws.rs.POST"
    override val putAnnotation = "javax.ws.rs.PUT"
    override val deleteAnnotation = "javax.ws.rs.DELETE"
    override val queryParamAnnotation = "javax.ws.rs.QueryParam"
    override val pathParamAnnotation = "javax.ws.rs.PathParam"
} 