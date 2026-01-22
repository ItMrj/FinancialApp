package com.financialapp.util

import java.security.*
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*
import javax.crypto.Cipher
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi

/**
 * RSA加密解密工具类
 * 用于密码的RSA加解密
 */
object RSAUtil {

    private const val ALGORITHM = "RSA"
    private const val TRANSFORMATION = "RSA/ECB/PKCS1Padding"
    private const val KEY_SIZE = 2048

    /**
     * 生成RSA密钥对
     */
    fun generateKeyPair(): KeyPair {
        val keyPairGenerator = KeyPairGenerator.getInstance(ALGORITHM)
        keyPairGenerator.initialize(KEY_SIZE)
        return keyPairGenerator.generateKeyPair()
    }

    /**
     * 生成RSA密钥对并转换为Base64字符串
     */
    fun generateKeyPairBase64(): Pair<String, String> {
        val keyPair = generateKeyPair()
        val publicKeyBase64 = encodeKeyToBase64(keyPair.public)
        val privateKeyBase64 = encodeKeyToBase64(keyPair.private)
        return Pair(publicKeyBase64, privateKeyBase64)
    }

    /**
     * 将公钥转换为Base64字符串（PEM格式）
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun encodeKeyToBase64(key: Key): String {
        val base64 = Base64.encode(key.encoded)
        // 返回 PEM 格式：包含 BEGIN/END 标记
        return if (key is java.security.PublicKey) {
            """-----BEGIN PUBLIC KEY-----
$base64
-----END PUBLIC KEY-----""".trimIndent()
        } else {
            base64
        }
    }

    /**
     * 从Base64字符串恢复公钥
     * 支持纯 Base64 格式和 PEM 格式
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun decodePublicKeyFromBase64(publicKeyBase64: String): PublicKey {
        // 移除 PEM 格式的头尾标记和换行符
        val cleanedKey = publicKeyBase64
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\n", "")
            .trim()

        val keyBytes = Base64.decode(cleanedKey)
        val keySpec = X509EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance(ALGORITHM)
        return keyFactory.generatePublic(keySpec)
    }

    /**
     * 从Base64字符串恢复私钥
     * 支持纯 Base64 格式和 PEM 格式
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun decodePrivateKeyFromBase64(privateKeyBase64: String): PrivateKey {
        // 移除 PEM 格式的头尾标记和换行符
        val cleanedKey = privateKeyBase64
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replace("-----BEGIN RSA PRIVATE KEY-----", "")
            .replace("-----END RSA PRIVATE KEY-----", "")
            .replace("\n", "")
            .trim()

        val keyBytes = Base64.decode(cleanedKey)
        val keySpec = PKCS8EncodedKeySpec(keyBytes)
        val keyFactory = KeyFactory.getInstance(ALGORITHM)
        return keyFactory.generatePrivate(keySpec)
    }

    /**
     * 使用公钥加密
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun encrypt(plainText: String, publicKey: PublicKey): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, publicKey)
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        return Base64.encode(encryptedBytes)
    }

    /**
     * 使用公钥加密（Base64公钥）
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun encryptWithPublicKeyBase64(plainText: String, publicKeyBase64: String): String {
        val publicKey = decodePublicKeyFromBase64(publicKeyBase64)
        return encrypt(plainText, publicKey)
    }

    /**
     * 使用私钥解密
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun decrypt(encryptedText: String, privateKey: PrivateKey): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.DECRYPT_MODE, privateKey)
        val decodedBytes = Base64.decode(encryptedText)
        val decryptedBytes = cipher.doFinal(decodedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * 使用私钥解密（Base64私钥）
     */
    @OptIn(ExperimentalEncodingApi::class)
    fun decryptWithPrivateKeyBase64(encryptedText: String, privateKeyBase64: String): String {
        val privateKey = decodePrivateKeyFromBase64(privateKeyBase64)
        return decrypt(encryptedText, privateKey)
    }

    /**
     * 保存密钥对到配置属性（用于开发环境）
     */
    fun generateAndPrintKeys() {
        val (publicKey, privateKey) = generateKeyPairBase64()
        println("=====================================")
        println("RSA 密钥对已生成")
        println("=====================================")
        println("公钥 (添加到 application.yml):")
        println("rsa.public-key: $publicKey")
        println()
        println("私钥 (添加到 application.yml):")
        println("rsa.private-key: $privateKey")
        println("=====================================")
    }
}
