package com.financialapp.service

import com.financialapp.dto.response.ApiResponse
import com.financialapp.util.RSAUtil
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * RSA加密服务
 * 提供RSA公钥和加密解密功能
 */
@Service
class RSAService(
    private val rsaPublicKey: java.security.PublicKey,
    private val rsaPrivateKey: java.security.PrivateKey
) {

    private val logger = LoggerFactory.getLogger(RSAService::class.java)

    /**
     * 获取公钥供前端使用
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun getPublicKey(): ApiResponse<Map<String, String>> {
        return ApiResponse.success(
            mapOf(
                "publicKey" to RSAUtil.encodeKeyToBase64(rsaPublicKey),
                "algorithm" to "RSA",
                "keySize" to "2048"
            )
        )
    }

    /**
     * 解密密码（用于登录/注册）
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun decryptPassword(encryptedPassword: String): String {
        return try {
            logger.debug("开始解密密码，密文长度: {}", encryptedPassword.length)
            logger.debug("密文前50个字符: {}", encryptedPassword.take(50))

            // 检查密文是否为空或格式异常
            if (encryptedPassword.isBlank()) {
                throw IllegalArgumentException("加密密码不能为空")
            }

            val decrypted = RSAUtil.decrypt(encryptedPassword, rsaPrivateKey)
            logger.debug("密码解密成功，明文长度: {}", decrypted.length)
            decrypted
        } catch (e: javax.crypto.BadPaddingException) {
            logger.error("BadPaddingException: 密钥不匹配或密文被篡改")
            logger.error("错误详情: {}", e.message, e)
            throw IllegalArgumentException("密码解密失败：密钥不匹配，请重新获取公钥并加密")
        } catch (e: javax.crypto.IllegalBlockSizeException) {
            logger.error("IllegalBlockSizeException: 密文格式错误")
            logger.error("错误详情: {}", e.message, e)
            throw IllegalArgumentException("密码解密失败：密文格式错误")
        } catch (e: Exception) {
            logger.error("密码解密失败: {}", e.message)
            logger.error("异常类型: {}", e.javaClass.name)
            throw IllegalArgumentException("密码解密失败：${e.message}")
        }
    }

    /**
     * 加密密码（用于测试）
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun encryptPassword(plainPassword: String): String {
        return try {
            RSAUtil.encrypt(plainPassword, rsaPublicKey)
        } catch (e: Exception) {
            logger.error("密码加密失败: ${e.message}", e)
            throw IllegalArgumentException("密码加密失败")
        }
    }
}
