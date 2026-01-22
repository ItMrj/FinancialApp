package com.financialapp.controller

import com.financialapp.dto.response.ApiResponse
import com.financialapp.service.RSAService
import com.financialapp.util.RSAUtil
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * RSA加密相关接口
 */
@RestController
@RequestMapping("/rsa")
@Tag(name = "RSA加密", description = "RSA加密相关接口")
class RSAController(
    private val rsaService: RSAService
) {

    @GetMapping("/public-key")
    @Operation(summary = "获取RSA公钥", description = "获取RSA公钥供前端加密密码使用")
    fun getPublicKey(): ApiResponse<Map<String, String>> {
        return rsaService.getPublicKey()
    }

    @GetMapping("/public-key/pem", produces = [MediaType.TEXT_PLAIN_VALUE])
    @Operation(summary = "获取RSA公钥(PEM格式)", description = "以纯文本格式返回RSA公钥，保留原始换行符")
    @OptIn(ExperimentalEncodingApi::class)
    fun getPublicKeyPem(): ResponseEntity<String> {
        // 从 RSAService 的响应中提取公钥
        val publicKeyPEM = rsaService.getPublicKey().data!!["publicKey"]!!
        return ResponseEntity.ok()
            .header("Content-Type", "text/plain; charset=utf-8")
            .body(publicKeyPEM)
    }

    @GetMapping("/test-encryption")
    @Operation(summary = "测试加密解密", description = "验证加密解密功能是否正常")
    @OptIn(ExperimentalEncodingApi::class)
    fun testEncryption(): ApiResponse<Map<String, Any>> {
        val testPassword = "test123"

        // 从公钥 PEM 字符串中提取公钥对象
        val publicKeyPEM = rsaService.getPublicKey().data!!["publicKey"]!!
        val publicKey = RSAUtil.decodePublicKeyFromBase64(publicKeyPEM)

        // 测试加密
        val encrypted = RSAUtil.encrypt(testPassword, publicKey)

        // 测试解密
        val decrypted = rsaService.decryptPassword(encrypted)

        return ApiResponse.success(
            mapOf(
                "originalPassword" to testPassword,
                "encryptedPassword" to encrypted,
                "encryptedLength" to encrypted.length,
                "decryptedPassword" to decrypted,
                "decryptionSuccess" to (testPassword == decrypted),
                "publicKeyFormat" to if (publicKeyPEM.contains("\n")) "contains_newline" else "no_newline",
                "publicKeyLength" to publicKeyPEM.length,
                "status" to "encryption_test_completed"
            )
        )
    }

    @GetMapping("/debug-info")
    @Operation(summary = "获取调试信息", description = "获取当前密钥对的调试信息")
    @OptIn(ExperimentalEncodingApi::class)
    fun getDebugInfo(): ApiResponse<Map<String, Any>> {
        val publicKeyPEM = rsaService.getPublicKey().data!!["publicKey"]!!

        return ApiResponse.success(
            mapOf(
                "timestamp" to System.currentTimeMillis(),
                "publicKeyLength" to publicKeyPEM.length,
                "publicKeyHasNewlines" to publicKeyPEM.contains("\n"),
                "publicKeyFormat" to if (publicKeyPEM.contains("-----BEGIN")) "PEM" else "RAW_BASE64",
                "publicKeyPreview" to publicKeyPEM.take(100) + "...",
                "algorithm" to "RSA/ECB/PKCS1Padding",
                "keySize" to "2048 bits"
            )
        )
    }
}
