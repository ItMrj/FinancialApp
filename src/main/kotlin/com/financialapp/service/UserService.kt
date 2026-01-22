package com.financialapp.service

import com.financialapp.dto.request.RegisterRequest
import com.financialapp.dto.response.UserResponse
import com.financialapp.entity.User
import com.financialapp.entity.UserStatus
import com.financialapp.exception.BusinessException
import com.financialapp.repository.UserRepository
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile

@Service
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: PasswordEncoder,
    private val fileStorageService: FileStorageService,
    private val rsaService: RSAService
) {

    @Transactional
    fun register(request: RegisterRequest): UserResponse {
        // 根据加密标志解密密码
        val password = if (request.encrypted) {
            rsaService.decryptPassword(request.password)
        } else {
            // 明文密码（向后兼容，不推荐使用）
            request.password
        }

        val confirmPassword = if (request.encrypted) {
            rsaService.decryptPassword(request.confirmPassword)
        } else {
            request.confirmPassword
        }

        // 验证密码是否一致
        if (password != confirmPassword) {
            throw BusinessException.passwordMismatch()
        }

        // 检查用户名是否已存在
        if (userRepository.existsByUsername(request.username)) {
            throw BusinessException.userAlreadyExists()
        }

        // 检查邮箱是否已存在
        if (userRepository.existsByEmail(request.email)) {
            throw BusinessException.userAlreadyExists()
        }

        // 检查手机号是否已存在
        if (request.phone != null && userRepository.existsByPhone(request.phone)) {
            throw BusinessException.userAlreadyExists()
        }

        // 创建新用户
        val user = User(
            username = request.username,
            password = passwordEncoder.encode(password),
            email = request.email,
            phone = request.phone,
            firstName = request.firstName,
            lastName = request.lastName,
            role = com.financialapp.entity.UserRole.USER,
            enabled = true,
            status = UserStatus.ACTIVE
        )

        val savedUser = userRepository.save(user)
        return toUserResponse(savedUser)
    }

    fun getUserById(id: Long): UserResponse {
        val user = userRepository.findById(id)
            .orElseThrow { BusinessException.userNotFound() }
        return toUserResponse(user)
    }

    fun getCurrentUserInfo(username: String): UserResponse {
        val user = userRepository.findByUsername(username)
            .orElseThrow { BusinessException.userNotFound() }
        return toUserResponse(user)
    }

    @Transactional
    fun updateAvatar(username: String, file: MultipartFile): String {
        val user = userRepository.findByUsername(username)
            .orElseThrow { BusinessException.userNotFound() }

        // 删除旧头像（如果不是默认头像）
        if (!user.avatar.equals("default-avatar.png", ignoreCase = true)) {
            fileStorageService.deleteFile(user.avatar)
        }

        // 上传新头像
        val avatarUrl = fileStorageService.storeAvatar(file, user.id!!)

        // 更新用户头像
        val updatedUser = user.copy(avatar = avatarUrl)
        userRepository.save(updatedUser)

        return avatarUrl
    }

    @Transactional
    fun deleteAvatar(username: String) {
        val user = userRepository.findByUsername(username)
            .orElseThrow { BusinessException.userNotFound() }

        // 删除旧头像（如果不是默认头像）
        if (!user.avatar.equals("default-avatar.png", ignoreCase = true)) {
            fileStorageService.deleteFile(user.avatar)
        }

        // 恢复默认头像
        val updatedUser = user.copy(avatar = "default-avatar.png")
        userRepository.save(updatedUser)
    }

    @Transactional
    fun updateLastLogin(userId: Long, ip: String) {
        userRepository.findById(userId).ifPresent { user ->
            userRepository.save(user.copy(lastLoginAt = java.time.LocalDateTime.now(), lastLoginIp = ip))
        }
    }

    private fun toUserResponse(user: User): UserResponse {
        return UserResponse(
            id = user.id,
            username = user.username,
            email = user.email,
            phone = user.phone,
            firstName = user.firstName,
            lastName = user.lastName,
            fullName = "${user.firstName}${user.lastName}",
            role = user.role.name,
            status = user.status.name,
            avatar = user.avatar,
            createdAt = user.createdAt
        )
    }
}
