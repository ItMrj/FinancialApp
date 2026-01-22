package com.financialapp.security

import com.financialapp.util.JwtKeyValidator
import io.jsonwebtoken.*
import io.jsonwebtoken.security.Keys
import jakarta.annotation.PostConstruct
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.core.Authentication
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtTokenProvider {

    companion object {
        private val logger = LoggerFactory.getLogger(JwtTokenProvider::class.java)
    }

    @Value("\${app.jwt.secret}")
    private lateinit var jwtSecret: String

    @Value("\${app.jwt.expiration}")
    private var jwtExpiration: Long = 86400000

    @Value("\${app.jwt.refresh-expiration}")
    private var refreshExpiration: Long = 604800000

    /**
     * 应用启动时验证 JWT 密钥
     */
    @PostConstruct
    fun init() {
        logger.info("正在验证 JWT 密钥...")

        val validationResult = JwtKeyValidator.validateSecret(jwtSecret)

        if (!validationResult.valid) {
            val separator = "=".repeat(80)
            logger.error(separator)
            logger.error("❌ JWT 密钥验证失败！")
            logger.error(separator)
            logger.error("密钥长度: {} 字节 ({} 位)", validationResult.keySizeBytes, validationResult.keySizeBits)
            logger.error("HS512 要求: 至少 512 位 (64 字节)")
            logger.error(separator)
            validationResult.errors.forEach { logger.error(it) }
            logger.error(separator)
            throw IllegalStateException("JWT 密钥不符合安全要求，请检查配置")
        } else {
            val separator = "=".repeat(80)
            logger.info(separator)
            logger.info("✅ JWT 密钥验证通过")
            logger.info(separator)
            logger.info("密钥长度: {} 字节 ({} 位)", validationResult.keySizeBytes, validationResult.keySizeBits)
            logger.info("算法: HS512")
            validationResult.successes.forEach { logger.info(it) }
            if (validationResult.warnings.isNotEmpty()) {
                logger.info("安全建议:")
                validationResult.warnings.forEach { logger.warn(it) }
            }
            logger.info(separator)
        }

        // 预加载密钥对象，提前发现问题
        try {
            val key = getSigningKey()
            logger.info("✅ JWT 签名密钥已成功加载")
        } catch (e: Exception) {
            logger.error("❌ 加载 JWT 签名密钥失败", e)
            throw IllegalStateException("无法加载 JWT 签名密钥", e)
        }
    }

    private fun getSigningKey(): SecretKey {
        val keyBytes = jwtSecret.toByteArray()
        return Keys.hmacShaKeyFor(keyBytes)
    }

    fun generateToken(authentication: Authentication): String {
        val userPrincipal = authentication.principal as UserDetailsImpl
        val now = Date()
        val expiryDate = Date(now.time + jwtExpiration)

        return Jwts.builder()
            .setSubject(userPrincipal.id.toString())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .claim("username", userPrincipal.getUsernamePublic())
            .claim("email", userPrincipal.getEmail())
            .claim("role", userPrincipal.getRole())
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact()
    }

    fun generateRefreshToken(authentication: Authentication): String {
        val userPrincipal = authentication.principal as UserDetailsImpl
        val now = Date()
        val expiryDate = Date(now.time + refreshExpiration)

        return Jwts.builder()
            .setSubject(userPrincipal.id.toString())
            .setIssuedAt(now)
            .setExpiration(expiryDate)
            .claim("type", "refresh")
            .signWith(getSigningKey(), SignatureAlgorithm.HS512)
            .compact()
    }

    fun getUserIdFromToken(token: String): Long? {
        return try {
            val claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .payload
            claims.subject.toLongOrNull()
        } catch (ex: Exception) {
            logger.error("Could not get user ID from token", ex)
            null
        }
    }

    fun validateToken(authToken: String): Boolean {
        return try {
            Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(authToken)
            true
        } catch (ex: SignatureException) {
            logger.error("Invalid JWT signature")
            false
        } catch (ex: MalformedJwtException) {
            logger.error("Invalid JWT token")
            false
        } catch (ex: ExpiredJwtException) {
            logger.error("Expired JWT token")
            false
        } catch (ex: UnsupportedJwtException) {
            logger.error("Unsupported JWT token")
            false
        } catch (ex: IllegalArgumentException) {
            logger.error("JWT claims string is empty")
            false
        }
    }
}
