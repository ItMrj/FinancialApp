package com.financialapp.util

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder

/**
 * BCrypt 密码生成工具
 * 用于将明文密码转换为 BCrypt 加密格式
 *
 * 使用方法：
 * 1. 直接运行此类的 main 方法
 * 2. 输入要加密的密码
 * 3. 获取 BCrypt 加密后的密码
 *
 * 生成的密码可以直接用于数据库更新：
 * UPDATE users SET password = '生成的BCrypt哈希' WHERE username = 'abc';
 */
object BCryptPasswordGenerator {

    private val encoder = BCryptPasswordEncoder()

    /**
     * 生成 BCrypt 加密密码
     *
     * @param plainPassword 明文密码
     * @return BCrypt 加密后的密码
     */
    fun generatePassword(plainPassword: String): String {
        return encoder.encode(plainPassword)
    }

    /**
     * 验证密码是否正确
     *
     * @param plainPassword 明文密码
     * @param hashedPassword BCrypt 加密后的密码
     * @return 是否匹配
     */
    fun verifyPassword(plainPassword: String, hashedPassword: String): Boolean {
        return encoder.matches(plainPassword, hashedPassword)
    }

    /**
     * 检查密码是否为 BCrypt 格式
     *
     * @param password 要检查的密码
     * @return 是否为 BCrypt 格式
     */
    fun isBCryptFormat(password: String): Boolean {
        return password.startsWith("\$2a\$") || password.startsWith("\$2b\$")
    }

    @JvmStatic
    fun main(args: Array<String>) {
        println("=" * 60)
        println("BCrypt 密码生成工具".padStart(40))
        println("=" * 60)
        println()

        // 生成常见测试密码的 BCrypt 哈希
        val testPasswords = listOf(
            "123456",
            "admin123",
            "abc123",
            "password",
            "test123"
        )

        println("常见测试密码的 BCrypt 哈希：")
        println("-" * 60)
        testPasswords.forEach { password ->
            val hashed = generatePassword(password)
            println("明文: ${password.padEnd(12)} -> BCrypt: $hashed")
        }
        println()

        // 交互式输入密码
        println("=" * 60)
        println("交互式密码生成".padStart(45))
        println("=" * 60)
        println()

        while (true) {
            print("请输入要加密的密码（输入 'q' 或 'quit' 退出）: ")
            val input = readlnOrNull()?.trim() ?: continue

            if (input.lowercase() == "q" || input.lowercase() == "quit") {
                println("退出程序...")
                break
            }

            if (input.isEmpty()) {
                println("密码不能为空，请重新输入。")
                println()
                continue
            }

            val hashed = generatePassword(input)
            println()
            println("明文密码: $input")
            println("BCrypt 哈希: $hashed")
            println()

            // 验证生成的密码是否正确
            val verified = verifyPassword(input, hashed)
            println("验证结果: ${if (verified) "✅ 密码验证通过" else "❌ 密码验证失败"}")
            println()

            // SQL 更新语句
            println("SQL 更新语句：")
            println("-" * 60)
            println("UPDATE users")
            println("SET password = '$hashed',")
            println("    enabled = TRUE,")
            println("    status = 'ACTIVE',")
            println("    updated_at = NOW()")
            println("WHERE username = 'your_username';")
            println("-" * 60)
            println()
        }

        println("程序结束。")
    }
}
