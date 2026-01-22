package com.financialapp.config

import com.financialapp.util.RSAUtil
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Primary
import java.io.File
import java.security.PrivateKey
import java.security.PublicKey

/**
 * RSA配置类
 * 用于加载和管理RSA密钥对
 */
@Configuration
@EnableConfigurationProperties(RSAConfig.RSAProperties::class)
class RSAConfig {

    private val logger = LoggerFactory.getLogger(RSAConfig::class.java)

    /**
     * RSA属性配置
     */
    @ConfigurationProperties(prefix = "rsa")
    data class RSAProperties(
        var publicKey: String? = null,
        var privateKey: String? = null
    )

    /**
     * RSA公钥 Bean
     */
    @Bean
    fun rsaPublicKey(rsaProperties: RSAProperties): PublicKey {
        ensureKeysExist(rsaProperties)
        return RSAUtil.decodePublicKeyFromBase64(rsaProperties.publicKey!!)
    }

    /**
     * RSA私钥 Bean
     */
    @Bean
    fun rsaPrivateKey(rsaProperties: RSAProperties): PrivateKey {
        ensureKeysExist(rsaProperties)
        return RSAUtil.decodePrivateKeyFromBase64(rsaProperties.privateKey!!)
    }

    /**
     * 确保密钥存在，如果不存在或为空则生成临时密钥对
     */
    private fun ensureKeysExist(rsaProperties: RSAProperties) {
        if (rsaProperties.publicKey.isNullOrEmpty() || rsaProperties.privateKey.isNullOrEmpty()) {
            logger.warn("RSA密钥未配置或为空，使用临时生成的密钥对")
            val (publicKey, privateKey) = RSAUtil.generateKeyPairBase64()
            rsaProperties.publicKey = publicKey
            rsaProperties.privateKey = privateKey

            logger.info("临时RSA密钥对已生成")
            logger.info("公钥: {}", publicKey)
            logger.info("私钥: {}", privateKey)
            logger.info("请将这些密钥添加到 application.yml 的 rsa 配置中")

            // 保存密钥对到文件，方便复制
            saveKeysToFile(publicKey, privateKey)
        }
    }

    /**
     * 将密钥对保存到文件
     */
    private fun saveKeysToFile(publicKey: String, privateKey: String) {
        try {
            val keysDir = File("rsa-keys")
            if (!keysDir.exists()) {
                keysDir.mkdirs()
            }

            val timestamp = System.currentTimeMillis()
            val keysFile = File(keysDir, "keys_$timestamp.yml")

            val content = """
# RSA 密钥对配置
# 生成时间: ${java.time.Instant.now()}

rsa:
  # RSA公钥（用于前端加密密码）
  public-key: |
${publicKey.prependIndent("    ")}

  # RSA私钥（用于后端解密密码）
  private-key: |
${privateKey.prependIndent("    ")}

# 使用说明：
# 1. 复制上面的公钥和私钥到 application.yml
# 2. 重启应用使配置生效
# 3. 前端重新获取公钥
            """.trimIndent()

            keysFile.writeText(content, Charsets.UTF_8)
            logger.info("✅ 密钥对已保存到文件: {}", keysFile.absolutePath)
        } catch (e: Exception) {
            logger.error("保存密钥对到文件失败", e)
        }
    }
}
