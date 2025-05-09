package com.github.yangtuooc.codeaudit.startup

import com.github.yangtuooc.codeaudit.api.CallChainService
import com.github.yangtuooc.codeaudit.api.CodeParsingService
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.DumbService
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/**
 * 插件启动活动，在项目打开时初始化所有服务
 */
class CodeAuditStartupActivity : ProjectActivity {
    private val log = logger<CodeAuditStartupActivity>()

    override suspend fun execute(project: Project) {
        log.info("Initializing AI Code Audit plugin for project: ${project.name}")

        // 获取服务实例
        val codeParsingService = project.service<CodeParsingService>()
        val callChainService = project.service<CallChainService>()

        try {
            // 使用DumbService确保在索引就绪后执行
            DumbService.getInstance(project).runWhenSmart {
                log.info("Running initialization when indices are ready")

                // 在后台线程中执行耗时操作
                GlobalScope.launch {
                    try {
                        // 发现API端点并构建调用链
                        log.info("Starting API endpoint discovery")
                        val endpoints =
                            ReadAction.compute<List<com.github.yangtuooc.codeaudit.model.ApiEndpoint>, Throwable> {
                                codeParsingService.discoverApiEndpoints()
                            }
                        log.info("Discovered ${endpoints.size} API endpoints")

                        // 为每个端点构建调用链
                        endpoints.forEach { endpoint ->
                            ReadAction.compute<Unit, Throwable> {
                                callChainService.buildCallChain(endpoint.psiMethod)
                                Unit
                            }
                        }

                        log.info("AI Code Audit plugin initialized successfully")
                    } catch (e: Exception) {
                        log.error("Failed to initialize AI Code Audit plugin during smart mode", e)
                    }
                }
            }

            log.info("AI Code Audit plugin startup activity completed")
        } catch (e: Exception) {
            log.error("Failed to schedule initialization for AI Code Audit plugin", e)
        }
    }
} 