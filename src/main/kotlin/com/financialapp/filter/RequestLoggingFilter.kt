package com.financialapp.filter

import jakarta.servlet.FilterChain
import jakarta.servlet.ReadListener
import jakarta.servlet.ServletInputStream
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletRequestWrapper
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpServletResponseWrapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import org.springframework.web.util.ContentCachingRequestWrapper
import org.springframework.web.util.ContentCachingResponseWrapper
import java.io.BufferedReader
import java.io.ByteArrayInputStream
import java.io.IOException
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets
import java.util.*

/**
 * 请求日志过滤器
 * 记录所有HTTP请求的详细信息，包括：
 * - 请求方法和URL
 * - 查询参数
 * - 请求头
 * - 请求体
 * - 响应状态码
 * - 请求耗时
 */
@Component
class RequestLoggingFilter : OncePerRequestFilter() {

    companion object {
        private val logger = LoggerFactory.getLogger(RequestLoggingFilter::class.java)

        // 不需要记录日志的路径
        private val EXCLUDE_PATHS = listOf(
            "/health",
            "/actuator",
            "/swagger-ui",
            "/api-docs",
            "/webjars"
        )
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {
        // 检查是否需要排除此路径
        val requestURI = request.requestURI
        if (shouldSkip(requestURI)) {
            filterChain.doFilter(request, response)
            return
        }

        val startTime = System.currentTimeMillis()

        // 使用包装器来缓存请求和响应内容
        val wrappedRequest = ContentCachingRequestWrapper(request)
        val wrappedResponse = ContentCachingResponseWrapper(response)

        try {
            // 执行过滤器链
            filterChain.doFilter(wrappedRequest, wrappedResponse)

            // 记录请求和响应信息
            logRequest(wrappedRequest)
            logResponse(wrappedResponse)

            // 计算并记录请求耗时
            val duration = System.currentTimeMillis() - startTime
            logDuration(wrappedRequest, wrappedResponse, duration)

            // 必须将缓存的响应内容写回到原始响应中
            wrappedResponse.copyBodyToResponse()
        } catch (e: Exception) {
            logger.error("Error in request logging filter", e)
            throw e
        }
    }

    /**
     * 检查是否应该跳过日志记录
     */
    private fun shouldSkip(uri: String): Boolean {
        return EXCLUDE_PATHS.any { uri.contains(it) }
    }

    /**
     * 记录请求信息
     */
    private fun logRequest(request: ContentCachingRequestWrapper) {
        logger.info("========== 请求开始 ==========")

        // 请求基本信息
        logger.info("请求方法: ${request.method}")
        logger.info("请求URL: ${request.requestURL}")
        logger.info("请求URI: ${request.requestURI}")
        logger.info("查询字符串: ${request.queryString ?: "无"}")
        logger.info("远程地址: ${request.remoteAddr}")
        logger.info("用户代理: ${request.getHeader("User-Agent") ?: "未知"}")
        logger.info("Content-Type: ${request.contentType ?: "未知"}")
        logger.info("Content-Length: ${request.contentLength}")

        // 记录查询参数
        logQueryParameters(request)

        // 记录请求头
        logRequestHeaders(request)

        // 记录请求体
        logRequestBody(request)

        logger.info("===========================")
    }

    /**
     * 记录查询参数
     */
    private fun logQueryParameters(request: HttpServletRequest) {
        val params = request.parameterMap
        if (params.isEmpty()) {
            logger.info("查询参数: 无")
        } else {
            logger.info("查询参数:")
            params.forEach { (name, values) ->
                val valueStr = if (values.size > 1) {
                    values.joinToString(", ")
                } else {
                    values[0]
                }
                logger.info("  $name = $valueStr")
            }
        }
    }

    /**
     * 记录请求头
     */
    private fun logRequestHeaders(request: HttpServletRequest) {
        logger.info("请求头:")
        val headerNames = request.headerNames

        val headersMap = mutableMapOf<String, String>()
        while (headerNames.hasMoreElements()) {
            val headerName = headerNames.nextElement()
            val headerValue = request.getHeader(headerName)

            // 脱敏处理敏感信息
            val displayValue = when (headerName.lowercase()) {
                "authorization" -> "Bearer ***REDACTED***"
                "cookie" -> "***REDACTED***"
                "set-cookie" -> "***REDACTED***"
                "x-api-key" -> "***REDACTED***"
                else -> headerValue ?: ""
            }

            headersMap[headerName] = displayValue
        }

        headersMap.forEach { (name, value) ->
            logger.info("  $name: $value")
        }
    }

    /**
     * 记录请求体
     */
    private fun logRequestBody(request: ContentCachingRequestWrapper) {
        val content = request.contentAsByteArray

        if (content.isEmpty()) {
            logger.info("请求体: 无")
            return
        }

        val contentType = request.contentType ?: ""
        val bodyString = String(content, StandardCharsets.UTF_8)

        // 根据Content-Type决定如何记录
        when {
            contentType.contains("application/json") -> {
                logger.info("请求体 (JSON):")
                logger.info("  $bodyString")
            }
            contentType.contains("application/x-www-form-urlencoded") -> {
                logger.info("请求体 (Form Data):")
                logger.info("  $bodyString")
            }
            contentType.contains("multipart/form-data") -> {
                logger.info("请求体: Multipart Form Data (内容略)")
            }
            contentType.contains("text/") -> {
                logger.info("请求体 (Text):")
                logger.info("  $bodyString")
            }
            else -> {
                logger.info("请求体: 二进制数据 (${content.size} bytes)")
            }
        }
    }

    /**
     * 记录响应信息
     */
    private fun logResponse(response: ContentCachingResponseWrapper) {
        logger.info("---------- 响应信息 ----------")
        logger.info("响应状态码: ${response.status}")
        logger.info("Content-Type: ${response.contentType ?: "未知"}")
        logger.info("Content-Length: ${response.contentSize}")

        // 记录响应头
        logger.info("响应头:")
        response.headerNames.forEach { headerName ->
            val headerValue = response.getHeader(headerName)
            logger.info("  $headerName: $headerValue")
        }

        // 记录响应体（仅对JSON或文本内容）
        val content = response.contentAsByteArray
        if (content.isNotEmpty()) {
            val contentType = response.contentType ?: ""
            when {
                contentType.contains("application/json") -> {
                    val bodyString = String(content, StandardCharsets.UTF_8)
                    // 限制响应体长度，避免日志过长
                    val displayBody = if (bodyString.length > 1000) {
                        bodyString.take(1000) + "... (截断)"
                    } else {
                        bodyString
                    }
                    logger.info("响应体 (JSON):")
                    logger.info("  $displayBody")
                }
                contentType.contains("text/") -> {
                    val bodyString = String(content, StandardCharsets.UTF_8)
                    val displayBody = if (bodyString.length > 500) {
                        bodyString.take(500) + "... (截断)"
                    } else {
                        bodyString
                    }
                    logger.info("响应体 (Text):")
                    logger.info("  $displayBody")
                }
                else -> {
                    logger.info("响应体: 二进制数据 (${content.size} bytes)")
                }
            }
        } else {
            logger.info("响应体: 无")
        }
    }

    /**
     * 记录请求耗时
     */
    private fun logDuration(
        request: ContentCachingRequestWrapper,
        response: ContentCachingResponseWrapper,
        duration: Long
    ) {
        val status = response.status
        val statusEmoji = when {
            status in 200..299 -> "✅"
            status in 300..399 -> "🔄"
            status in 400..499 -> "⚠️"
            status in 500..599 -> "❌"
            else -> "❓"
        }

        val durationStr = if (duration < 1000) {
            "${duration}ms"
        } else {
            String.format("%.2fs", duration / 1000.0)
        }

        logger.info("========== 请求结束 ==========")
        logger.info("$statusEmoji 状态: $status | 耗时: $durationStr | ${request.method} ${request.requestURI}")
        logger.info("==============================")
    }
}
