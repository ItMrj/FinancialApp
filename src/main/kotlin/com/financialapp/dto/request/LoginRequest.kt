package com.financialapp.dto.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class LoginRequest(
    @field:NotBlank(message = "用户名不能为空")
    @field:Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
    val username: String,

    @field:NotBlank(message = "密码不能为空")
    @field:Size(min = 6, max = 500, message = "密码长度必须在6-500之间")
    val password: String,

    /**
     * 密码是否已使用RSA加密
     * true: 密码已加密，需要解密后验证
     * false: 密码为明文（向后兼容）
     * 默认为 true（推荐使用加密）
     */
    val encrypted: Boolean = true
)
