package com.financialapp.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.BadCredentialsException
import org.springframework.security.core.AuthenticationException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import java.time.LocalDateTime

@RestControllerAdvice
class GlobalExceptionHandler {

    companion object {
        private val logger = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)
    }

    @ExceptionHandler(BusinessException::class)
    fun handleBusinessException(ex: BusinessException): ResponseEntity<Map<String, Any>> {
        logger.error("Business exception: {}", ex.message)
        val body = mapOf(
            "timestamp" to LocalDateTime.now(),
            "status" to ex.code,
            "error" to "Business Error",
            "message" to ex.message
        )
        return ResponseEntity(body, HttpStatus.valueOf(ex.code))
    }

    @ExceptionHandler(BadCredentialsException::class)
    fun handleBadCredentialsException(ex: BadCredentialsException): ResponseEntity<Map<String, Any>> {
        logger.error("Bad credentials exception: {}", ex.message)
        val body = mapOf(
            "timestamp" to LocalDateTime.now(),
            "status" to HttpStatus.UNAUTHORIZED.value(),
            "error" to "Unauthorized",
            "message" to "用户名或密码错误"
        )
        return ResponseEntity(body, HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ResponseEntity<Map<String, Any>> {
        logger.error("Authentication exception: {}", ex.message)
        val body = mapOf<String, Any>(
            "timestamp" to LocalDateTime.now(),
            "status" to HttpStatus.UNAUTHORIZED.value(),
            "error" to "Unauthorized",
            "message" to (ex.message ?: "认证失败")
        )
        return ResponseEntity(body, HttpStatus.UNAUTHORIZED)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(ex: MethodArgumentNotValidException): ResponseEntity<Map<String, Any>> {
        val errors = ex.bindingResult.allErrors.associateWith { error ->
            when (error) {
                is FieldError -> error.defaultMessage ?: "验证失败"
                else -> error.defaultMessage ?: "验证失败"
            }
        }
        logger.error("Validation exception: {}", errors)
        val body = mapOf(
            "timestamp" to LocalDateTime.now(),
            "status" to HttpStatus.BAD_REQUEST.value(),
            "error" to "Validation Error",
            "message" to "请求参数验证失败",
            "errors" to errors
        )
        return ResponseEntity(body, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<Map<String, Any>> {
        logger.error("Unexpected exception: ", ex)
        val body = mapOf(
            "timestamp" to LocalDateTime.now(),
            "status" to HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "error" to "Internal Server Error",
            "message" to "服务器内部错误"
        )
        return ResponseEntity(body, HttpStatus.INTERNAL_SERVER_ERROR)
    }
}
