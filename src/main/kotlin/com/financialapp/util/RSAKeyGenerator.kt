package com.financialapp.util

import java.io.File

/**
 * RSA密钥生成工具
 * 用于生成密钥对并输出到控制台，方便复制到配置文件
 */
object RSAKeyGenerator {
    @JvmStatic
    fun main(args: Array<String>) {
        println("=" .repeat(80))
        println("RSA 密钥对生成工具")
        println("=" .repeat(80))
        println()

        val (publicKey, privateKey) = RSAUtil.generateKeyPairBase64()

        println("✅ 密钥对生成成功！")
        println()

        println("=" .repeat(80))
        println("📋 公钥 (复制到 application.yml 的 rsa.public-key)")
        println("=" .repeat(80))
        println(publicKey)
        println()

        println("=" .repeat(80))
        println("🔐 私钥 (复制到 application.yml 的 rsa.private-key)")
        println("=" .repeat(80))
        println(privateKey)
        println()

        println("=" .repeat(80))
        println("📝 配置示例")
        println("=" .repeat(80))
        println("""
rsa:
  # RSA公钥（用于前端加密密码）
  public-key: |
${publicKey.prependIndent("    ")}
  
  # RSA私钥（用于后端解密密码）
  private-key: |
${privateKey.prependIndent("    ")}
        """.trimIndent())

        println()
        println("💡 提示：")
        println("   1. 复制上面的公钥和私钥到 application.yml")
        println("   2. 使用 | 符号保留多行格式")
        println("   3. 重启应用使配置生效")
        println("   4. 前端重新获取公钥")
        println()
        println("=" .repeat(80))
    }
}
