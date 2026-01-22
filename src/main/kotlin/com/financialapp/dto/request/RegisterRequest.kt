package com.financialapp.dto.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank(message = "用户名不能为空")
    @field:Size(min = 3, max = 50, message = "用户名长度必须在3-50之间")
    @field:Pattern(regexp = "^[a-zA-Z0-9_]+$", message = "用户名只能包含字母、数字和下划线")
    val username: String,

    @field:NotBlank(message = "密码不能为空")
    @field:Size(min = 6, max = 500, message = "密码长度必须在6-500之间")
    val password: String,

    @field:NotBlank(message = "确认密码不能为空")
    @field:Size(min = 6, max = 500, message = "密码长度必须在6-500之间")
    val confirmPassword: String,

    @field:NotBlank(message = "邮箱不能为空")
    @field:Email(message = "邮箱格式不正确")
    val email: String,

    @field:Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    val phone: String? = null,

    @field:NotBlank(message = "姓不能为空")
    @field:Size(max = 50, message = "姓长度不能超过50")
    val firstName: String,

    @field:NotBlank(message = "名不能为空")
    @field:Size(max = 50, message = "名长度不能超过50")
    val lastName: String,

    /**
     * 密码是否已使用RSA加密
     * true: 密码已加密，需要解密后验证
     * false: 密码为明文（向后兼容）
     * 默认为 true（推荐使用加密）
     */
    val encrypted: Boolean = true
)
