package com.financialapp.dto.request

import jakarta.validation.constraints.NotNull
import org.springframework.web.multipart.MultipartFile

/**
 * 头像上传请求
 */
data class AvatarUploadRequest(
    @field:NotNull(message = "头像文件不能为空")
    val avatar: MultipartFile
)
