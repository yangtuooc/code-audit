package com.github.yangtuooc.codeaudit.framework

/**
 * 路径处理工具类
 */
object PathUtils {
    /**
     * 格式化路径，确保以/开头且不以/结尾
     */
    fun formatPath(path: String): String {
        if (path.isEmpty()) return ""

        var formattedPath = path
        if (!formattedPath.startsWith("/")) {
            formattedPath = "/$formattedPath"
        }
        if (formattedPath.endsWith("/")) {
            formattedPath = formattedPath.substring(0, formattedPath.length - 1)
        }

        return formattedPath
    }

    /**
     * 组合两个路径
     */
    fun combinePaths(basePath: String, path: String): String {
        if (path.isEmpty()) return basePath
        if (basePath.isEmpty()) return path

        return basePath + path
    }
} 