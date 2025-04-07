package com.github.yangtuooc.codeaudit.service

import com.github.yangtuooc.codeaudit.api.AIAnalysisService
import com.github.yangtuooc.codeaudit.model.*
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiMethod
import java.util.*

/**
 * AI代码分析服务实现类
 */
@Service(Service.Level.PROJECT)
class AIAnalysisServiceImpl(private val project: Project) : AIAnalysisService {

    private val log = logger<AIAnalysisServiceImpl>()
    private val modelParameters = mutableMapOf<String, String>()

    init {
        // 设置默认的AI模型参数
        modelParameters["temperature"] = "0.7"
        modelParameters["maxTokens"] = "2000"
    }

    override fun analyzeMethod(method: PsiMethod, context: AnalysisContext): AnalysisResult {
        // TODO: 实现方法级别的AI分析
        log.info("Analyzing method: ${method.name}")

        return AnalysisResult(
            id = UUID.randomUUID().toString(),
            summary = "Method analysis of ${method.name}",
            status = AnalysisStatus.COMPLETED,
            duration = 0
        )
    }

    override fun analyzeCallChain(callChain: CallChain): AnalysisResult {
        // TODO: 实现完整调用链的AI分析
        log.info("Analyzing call chain with entry point: ${callChain.entryPoint.name}")

        return AnalysisResult(
            id = UUID.randomUUID().toString(),
            summary = "Call chain analysis starting from ${callChain.entryPoint.name}",
            status = AnalysisStatus.COMPLETED,
            duration = 0
        )
    }

    override fun analyzeApiEndpoint(endpoint: ApiEndpoint, context: AnalysisContext): AnalysisResult {
        log.info("Analyzing API endpoint: ${endpoint.httpMethod} ${endpoint.path}")

        val startTime = System.currentTimeMillis()

        // 构建API端点的描述
        val endpointDescription = buildApiEndpointDescription(endpoint)

        // 模拟AI分析 (实际实现会调用AI服务)
        val analysisDetails = """
            |## API端点安全分析
            |
            |### 基本信息
            |- 路径: ${endpoint.path}
            |- HTTP方法: ${endpoint.httpMethod}
            |- 控制器: ${endpoint.controllerName}
            |
            |### 潜在安全风险
            |1. **输入验证** - 检查是否有充分的输入验证逻辑，防止SQL注入和XSS攻击
            |2. **认证与授权** - 验证此端点是否有适当的访问控制机制
            |3. **参数处理** - 分析如何处理用户输入参数，特别是敏感数据
            |
            |### 性能考虑
            |1. **数据库交互** - 检查是否存在N+1查询问题
            |2. **资源消耗** - 评估资源密集型操作
            |
            |### 代码结构
            |1. **责任划分** - 控制器逻辑是否分层合理
            |2. **异常处理** - 是否有完善的错误处理机制
            |
            |### 建议改进
            |- 考虑为敏感操作添加日志记录
            |${if (endpoint.parameters.isNotEmpty()) "- 参数${endpoint.parameters.first().name}可能需要额外验证" else ""}
            |
            |### 调用链分析
            |该API端点的调用链深度为: ${endpoint.callChain?.getDepth() ?: 0}层，包含${endpoint.callChain?.getAllMethods()?.size ?: 0}个方法调用。
        """.trimMargin()

        val duration = System.currentTimeMillis() - startTime

        return AnalysisResult(
            id = UUID.randomUUID().toString(),
            summary = "API endpoint ${endpoint.httpMethod} ${endpoint.path} analysis",
            details = analysisDetails,
            status = AnalysisStatus.COMPLETED,
            duration = duration
        )
    }

    override fun getFixSuggestions(issue: CodeIssue): List<FixSuggestion> {
        // TODO: 实现获取AI修复建议的逻辑
        log.info("Getting fix suggestions for issue: ${issue.id}")

        return emptyList()
    }

    override fun setModelParameter(key: String, value: String) {
        modelParameters[key] = value
        log.info("Set AI model parameter: $key = $value")
    }

    /**
     * 构建API端点的文本描述
     */
    private fun buildApiEndpointDescription(endpoint: ApiEndpoint): String {
        return buildString {
            appendLine("API Endpoint: ${endpoint.httpMethod} ${endpoint.path}")
            appendLine("Controller: ${endpoint.controllerName}")
            appendLine("Handler Method: ${endpoint.methodName}")
            appendLine("Return Type: ${endpoint.returnType}")

            if (endpoint.parameters.isNotEmpty()) {
                appendLine("\nParameters:")
                endpoint.parameters.forEach { param ->
                    appendLine("- ${param.name} (${param.type}): ${if (param.required) "Required" else "Optional"}")
                }
            }

            endpoint.callChain?.let { callChain ->
                appendLine("\nCall Chain:")
                callChain.getAllMethods().forEach { method ->
                    val className = method.containingClass?.qualifiedName ?: "Unknown"
                    appendLine("- ${method.name}() - $className")
                }
            }
        }
    }

    /**
     * 构建代码的文本表示，用于发送给AI进行分析
     */
    private fun buildCodeTextRepresentation(method: PsiMethod): String {
        // TODO: 实现代码文本表示构建逻辑
        val code = method.text ?: ""

        return buildString {
            append("Method: ${method.name}\n")
            append("Parameters: ${method.parameterList.parameters.joinToString(", ") { it.type.presentableText + " " + it.name }}\n")
            append("Return type: ${method.returnType?.presentableText ?: "void"}\n")
            append("Code:\n$code")
        }
    }
} 