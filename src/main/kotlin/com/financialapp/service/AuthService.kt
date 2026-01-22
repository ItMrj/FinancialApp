package com.financialapp.service

import com.financialapp.dto.request.LoginRequest
import com.financialapp.dto.request.RegisterRequest
import com.financialapp.dto.response.AuthResponse
import com.financialapp.dto.response.UserResponse
import com.financialapp.entity.UserStatus
import com.financialapp.exception.BusinessException
import com.financialapp.repository.UserRepository
import com.financialapp.security.JwtTokenProvider
import com.financialapp.security.UserDetailsImpl
import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class AuthService(
    private val authenticationManager: AuthenticationManager,
    private val userRepository: UserRepository,
    private val userService: UserService,
    private val jwtTokenProvider: JwtTokenProvider,
    private val passwordEncoder: PasswordEncoder,
    private val rsaService: RSAService
) {

    companion object {
        private val logger = LoggerFactory.getLogger(AuthService::class.java)
    }

    fun login(request: LoginRequest, httpRequest: HttpServletRequest): AuthResponse {
        try {
            logger.debug("登录请求 - 用户名: {}, 密码是否加密: {}", request.username, request.encrypted)

            // 根据加密标志决定是否解密密码
            val password = if (request.encrypted) {
                logger.debug("尝试解密密码...")
                try {
                    val decrypted = rsaService.decryptPassword(request.password)
                    logger.debug("密码解密成功，长度: {}", decrypted.length)
                    decrypted
                } catch (e: Exception) {
                    logger.error("密码解密失败: {}", e.message)
                    throw IllegalArgumentException("密码解密失败，请检查前端是否正确使用RSA加密")
                }
            } else {
                // 明文密码（向后兼容，不推荐使用）
                logger.debug("使用明文密码（向后兼容模式）")
                request.password
            }

            logger.debug("准备认证用户: {}", request.username)

            // 执行认证
            val authentication = authenticationManager.authenticate(
                UsernamePasswordAuthenticationToken(request.username, password)
            )

            logger.debug("认证成功: {}", request.username)

            // 获取用户信息
            val userDetails = authentication.principal as UserDetailsImpl
            val user = userRepository.findById(userDetails.id)
                .orElseThrow { BusinessException.userNotFound() }

            // 检查用户状态
            logger.debug("用户状态检查 - enabled: {}, status: {}", user.enabled, user.status)
            if (user.status != UserStatus.ACTIVE) {
                logger.error("用户 {} 状态异常: {}", request.username, user.status)
                throw BusinessException.userDisabled()
            }

            // 更新最后登录时间和IP
            userService.updateLastLogin(user.id!!, getClientIp(httpRequest))

            // 生成Token
            val accessToken = jwtTokenProvider.generateToken(authentication)
            val refreshToken = jwtTokenProvider.generateRefreshToken(authentication)

            logger.info("用户 {} 登录成功", request.username)

            return AuthResponse(
                accessToken = accessToken,
                refreshToken = refreshToken,
                tokenType = "Bearer",
                expiresIn = 86400000,
                user = userService.getUserById(user.id)
            )
        } catch (e: BusinessException) {
            // 重新抛出业务异常
            throw e
        } catch (e: IllegalArgumentException) {
            // RSA解密失败
            logger.error("登录失败 - 用户名: {}, 错误: {}", request.username, e.message)
            throw BusinessException(BusinessException.invalidCredentials().code, "密码解密失败：${e.message}")
        } catch (e: Exception) {
            // 其他异常
            logger.error("登录失败 - 用户名: {}, 错误类型: {}, 错误信息: {}",
                request.username, e.javaClass.simpleName, e.message)
            throw BusinessException.invalidCredentials()
        }
    }

    fun register(request: RegisterRequest): UserResponse {
        return userService.register(request)
    }

    @Transactional
    fun logout(userId: Long) {
        // 在实际应用中，可以将Token加入黑名单（Redis）
        // 这里仅做演示
    }

    fun refreshToken(authentication: Authentication): AuthResponse {
        val userDetails = authentication.principal as UserDetailsImpl
        val user = userRepository.findById(userDetails.id)
            .orElseThrow { BusinessException.userNotFound() }

        val newAccessToken = jwtTokenProvider.generateToken(authentication)
        val newRefreshToken = jwtTokenProvider.generateRefreshToken(authentication)

        return AuthResponse(
            accessToken = newAccessToken,
            refreshToken = newRefreshToken,
            tokenType = "Bearer",
            expiresIn = 86400000,
            user = userService.getUserById(user.id!!)
        )
    }

    fun getCurrentUser(userId: Long): UserResponse {
        return userService.getUserById(userId)
    }

    private fun getClientIp(request: HttpServletRequest): String {
        var ip = request.getHeader("X-Forwarded-For")
        if (ip.isNullOrBlank() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("Proxy-Client-IP")
        }
        if (ip.isNullOrBlank() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.getHeader("WL-Proxy-Client-IP")
        }
        if (ip.isNullOrBlank() || "unknown".equals(ip, ignoreCase = true)) {
            ip = request.remoteAddr
        }
        return ip ?: "unknown"
    }
}
