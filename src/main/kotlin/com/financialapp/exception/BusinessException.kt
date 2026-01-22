package com.financialapp.exception

class BusinessException(
    val code: Int,
    override val message: String
) : RuntimeException(message) {

    companion object {
        fun userNotFound(): BusinessException = BusinessException(404, "用户不存在")
        fun invalidCredentials(): BusinessException = BusinessException(401, "用户名或密码错误")
        fun userAlreadyExists(): BusinessException = BusinessException(400, "用户已存在")
        fun tokenExpired(): BusinessException = BusinessException(401, "Token已过期")
        fun invalidToken(): BusinessException = BusinessException(401, "无效的Token")
        fun userDisabled(): BusinessException = BusinessException(403, "账户已被禁用")
        fun passwordMismatch(): BusinessException = BusinessException(400, "两次输入的密码不一致")
    }
}
