package com.financialapp.util

import org.springframework.http.HttpStatus

data class ApiResponse<T>(
    val success: Boolean,
    val message: String,
    val data: T? = null,
    val code: Int = HttpStatus.OK.value()
) {
    companion object {
        fun <T> success(data: T?, message: String = "操作成功"): ApiResponse<T> {
            return ApiResponse(true, message, data)
        }

        fun <T> success(message: String = "操作成功"): ApiResponse<T> {
            return ApiResponse(true, message, null)
        }

        fun <T> error(message: String, code: Int = HttpStatus.INTERNAL_SERVER_ERROR.value()): ApiResponse<T> {
            return ApiResponse(false, message, null, code)
        }
    }
}
