package com.financialapp.util

import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.SignatureAlgorithm
import javax.crypto.SecretKey

/**
 * JWT 密钥验证工具
 *
 * 用于验证 JWT 密钥是否符合 HS512 安全要求
 */
object JwtKeyValidator {

    /**
     * 验证密钥是否符合 HS512 要求
     *
     * @param secret 密钥字符串
     * @return 验证结果
     */
    fun validateSecret(secret: String): ValidationResult {
        val result = ValidationResult()

        // 1. 检查密钥是否为空
        if (secret.isEmpty()) {
            result.valid = false
            result.errors.add("❌ 密钥不能为空")
            return result
        }

        // 2. 计算密钥长度（位）
        val keyBytes = secret.toByteArray()
        val keySizeBits = keyBytes.size * 8
        val requiredSizeBits = 512

        result.keySizeBytes = keyBytes.size
        result.keySizeBits = keySizeBits

        // 3. 检查是否满足 HS512 最小长度
        if (keySizeBits < requiredSizeBits) {
            result.valid = false
            result.errors.add(
                "❌ 密钥长度不足：$keySizeBits 位（HS512 要求至少 $requiredSizeBits 位）"
            )
            result.errors.add(
                "💡 当前密钥长度：${keyBytes.size} 字节，建议至少 64 字节"
            )
        } else {
            result.valid = true
            result.successes.add("✅ 密钥长度符合要求：$keySizeBits 位")
        }

        // 4. 尝试创建 SecretKey（验证密钥格式）
        try {
            val secretKey: SecretKey = Keys.hmacShaKeyFor(keyBytes)
            result.algorithm = "HS512"
            result.successes.add("✅ 密钥格式正确，可以用于 HMAC-SHA512 签名")
        } catch (e: Exception) {
            result.valid = false
            result.errors.add("❌ 密钥格式错误：${e.message}")
        }

        // 5. 安全建议
        if (keyBytes.size == 64) {
            result.warnings.add("⚠️  密钥刚好满足最小要求（64 字节），建议使用更长的密钥（128 字节或更多）以提高安全性")
        } else if (keyBytes.size >= 128) {
            result.successes.add("✅ 密钥长度优秀（${keyBytes.size} 字节），符合最佳安全实践")
        }

        return result
    }

    /**
     * 打印验证结果
     */
    fun printValidationResult(result: ValidationResult) {
        val separator = "=".repeat(80)
        println(separator)
        println("JWT 密钥验证结果")
        println(separator)
        println()

        if (result.valid) {
            println("✅ 验证通过：密钥符合 HS512 安全要求")
        } else {
            println("❌ 验证失败：密钥不符合 HS512 安全要求")
        }

        println()
        println("📊 密钥信息：")
        println("  长度: ${result.keySizeBytes} 字节 (${result.keySizeBits} 位)")
        if (result.algorithm != null) {
            println("  算法: ${result.algorithm}")
        }

        if (result.successes.isNotEmpty()) {
            println()
            println("✅ 成功项：")
            result.successes.forEach { println("  $it") }
        }

        if (result.warnings.isNotEmpty()) {
            println()
            println("⚠️  警告项：")
            result.warnings.forEach { println("  $it") }
        }

        if (result.errors.isNotEmpty()) {
            println()
            println("❌ 错误项：")
            result.errors.forEach { println("  $it") }
        }

        println()
        println(separator)
        if (!result.valid) {
            println("建议：使用 JwtKeyGenerator 生成安全的密钥")
        } else {
            println("建议：定期轮换密钥以提高安全性")
        }
        println(separator)
    }

    /**
     * 验证结果数据类
     */
    data class ValidationResult(
        var valid: Boolean = false,
        var keySizeBytes: Int = 0,
        var keySizeBits: Int = 0,
        var algorithm: String? = null,
        val successes: MutableList<String> = mutableListOf(),
        val warnings: MutableList<String> = mutableListOf(),
        val errors: MutableList<String> = mutableListOf()
    )
}
