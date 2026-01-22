package com.financialapp.dto.response

/**
 * 统一API响应包装类
 * @param T 数据类型
 * @property success 是否成功
 * @property message 响应消息
 * @property data 响应数据
 * @property code HTTP状态码
 */
data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T?,
    val code: Int
) {
    companion object {
        /**
         * 创建成功响应
         * @param data 响应数据
         * @param message 响应消息，默认"操作成功"
         * @return 成功响应对象
         */
        fun <T> success(data: T? = null, message: String = "操作成功"): ApiResponse<T> {
            return ApiResponse(true, message, data, 200)
        }

        /**
         * 创建失败响应
         * @param message 响应消息
         * @param code HTTP状态码，默认400
         * @return 失败响应对象
         */
        fun <T> error(message: String, code: Int = 400): ApiResponse<T> {
            return ApiResponse(false, message, null, code)
        }
    }
}
