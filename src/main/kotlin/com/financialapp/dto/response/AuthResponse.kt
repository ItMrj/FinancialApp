package com.financialapp.dto.response

import java.time.LocalDateTime

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresIn: Long,
    val user: UserResponse
)

data class UserResponse(
    val id: Long?,
    val username: String,
    val email: String,
    val phone: String?,
    val firstName: String,
    val lastName: String,
    val fullName: String,
    val role: String,
    val status: String,
    val avatar: String,
    val createdAt: LocalDateTime?
)
