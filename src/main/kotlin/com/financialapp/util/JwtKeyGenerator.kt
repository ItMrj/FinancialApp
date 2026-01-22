package com.financialapp.util

import io.jsonwebtoken.security.Keys
import io.jsonwebtoken.SignatureAlgorithm
import java.util.*

/**
 * JWT 密钥生成工具
 *
 * 用于生成符合 HS512 算法安全要求的密钥
 */
object JwtKeyGenerator {

    /**
     * 生成符合 HS512 安全要求的密钥（Base64 编码）
     */
    fun generateSecureKey(): String {
        val secretKey = Keys.secretKeyFor(SignatureAlgorithm.HS512)
        val keyBytes = secretKey.encoded
        return Base64.getEncoder().encodeToString(keyBytes)
    }

    /**
     * 生成并打印密钥配置
     */
    @JvmStatic
    fun main(args: Array<String>) {
        val separator = "=".repeat(80)
        val dashSeparator = "-".repeat(80)

        println(separator)
        println("JWT 密钥生成工具")
        println(separator)
        println()

        val secureKey = generateSecureKey()

        println("✅ 已生成安全的 HS512 密钥")
        println()
        println("密钥长度: ${secureKey.length} 字符 (${secureKey.length * 8} 位)")
        println("算法: HS512")
        println("编码: Base64")
        println()
        println(dashSeparator)
        println("请将以下密钥复制到 application.yml 的 app.jwt.secret 字段：")
        println(dashSeparator)
        println()
        println("jwt:")
        println("  secret: $secureKey")
        println("  expiration: 86400000  # 24小时，单位毫秒")
        println("  refresh-expiration: 604800000  # 7天")
        println()
        println(separator)
        println("安全提示：")
        println("1. 生产环境中，请使用环境变量存储此密钥")
        println("2. 定期轮换密钥以提高安全性")
        println("3. 不要将密钥提交到版本控制系统")
        println(separator)
    }
}
