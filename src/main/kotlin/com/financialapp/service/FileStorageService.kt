package com.financialapp.service

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.multipart.MultipartFile
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.util.*

@Service
class FileStorageService {

    private val logger = LoggerFactory.getLogger(FileStorageService::class.java)

    @Value("\${app.upload.location}")
    private lateinit var uploadLocation: String

    @Value("\${app.upload.url-prefix}")
    private lateinit var urlPrefix: String

    @Value("\${app.upload.max-file-size:5242880}")
    private var maxFileSize: Long = 5242880 // 默认5MB

    /**
     * 存储文件到指定目录
     */
    fun storeFile(file: MultipartFile, subDir: String = "general"): String {
        try {
            // 验证文件
            validateFile(file)

            // 创建目录
            val uploadPath = Paths.get(uploadLocation, subDir)
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath)
            }

            // 生成唯一文件名
            val originalFileName = file.originalFilename ?: throw IllegalArgumentException("文件名不能为空")
            val fileExtension = getFileExtension(originalFileName)
            val uniqueFileName = "${UUID.randomUUID()}.$fileExtension"

            // 存储文件
            val targetPath = uploadPath.resolve(uniqueFileName)
            Files.copy(file.inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)

            logger.info("文件存储成功: $uniqueFileName in $subDir")

            // 返回访问URL
            return "$urlPrefix/$subDir/$uniqueFileName"

        } catch (e: IOException) {
            logger.error("文件存储失败", e)
            throw RuntimeException("文件存储失败: ${e.message}")
        }
    }

    /**
     * 存储头像
     */
    fun storeAvatar(file: MultipartFile, userId: Long): String {
        try {
            // 验证头像文件
            validateAvatarFile(file)

            // 创建头像目录
            val avatarPath = Paths.get(uploadLocation, "avatars")
            if (!Files.exists(avatarPath)) {
                Files.createDirectories(avatarPath)
            }

            // 生成文件名：userId_timestamp.extension
            val originalFileName = file.originalFilename ?: throw IllegalArgumentException("文件名不能为空")
            val fileExtension = getFileExtension(originalFileName)
            val timestamp = System.currentTimeMillis()
            val fileName = "user_${userId}_$timestamp.$fileExtension"

            // 存储文件
            val targetPath = avatarPath.resolve(fileName)
            Files.copy(file.inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING)

            logger.info("头像存储成功: $fileName for user $userId")

            // 返回访问URL
            return "$urlPrefix/avatars/$fileName"

        } catch (e: IOException) {
            logger.error("头像存储失败", e)
            throw RuntimeException("头像存储失败: ${e.message}")
        }
    }

    /**
     * 删除文件
     */
    fun deleteFile(fileUrl: String): Boolean {
        return try {
            val relativePath = fileUrl.substringAfter(urlPrefix).trim('/')
            val filePath = Paths.get(uploadLocation, relativePath)
            if (Files.exists(filePath)) {
                Files.delete(filePath)
                logger.info("文件删除成功: $fileUrl")
                true
            } else {
                logger.warn("文件不存在: $fileUrl")
                false
            }
        } catch (e: IOException) {
            logger.error("文件删除失败", e)
            false
        }
    }

    /**
     * 验证文件
     */
    private fun validateFile(file: MultipartFile) {
        // 检查文件是否为空
        if (file.isEmpty) {
            throw IllegalArgumentException("文件不能为空")
        }

        // 检查文件大小
        if (file.size > maxFileSize) {
            throw IllegalArgumentException("文件大小超过限制: ${maxFileSize / 1024 / 1024}MB")
        }

        // 检查文件类型
        val contentType = file.contentType ?: throw IllegalArgumentException("无法确定文件类型")
        if (!isAllowedContentType(contentType)) {
            throw IllegalArgumentException("不支持的文件类型: $contentType")
        }
    }

    /**
     * 验证头像文件
     */
    private fun validateAvatarFile(file: MultipartFile) {
        // 检查文件是否为空
        if (file.isEmpty) {
            throw IllegalArgumentException("头像文件不能为空")
        }

        // 检查文件大小（头像限制2MB）
        val maxAvatarSize = 2 * 1024 * 1024L // 2MB
        if (file.size > maxAvatarSize) {
            throw IllegalArgumentException("头像文件大小不能超过2MB")
        }

        // 检查文件类型（只允许图片）
        val contentType = file.contentType ?: throw IllegalArgumentException("无法确定文件类型")
        if (!isImageContentType(contentType)) {
            throw IllegalArgumentException("只支持图片格式: $contentType")
        }
    }

    /**
     * 检查是否为允许的文件类型
     */
    private fun isAllowedContentType(contentType: String): Boolean {
        val allowedTypes = listOf(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
        )
        return allowedTypes.contains(contentType.lowercase())
    }

    /**
     * 检查是否为图片类型
     */
    private fun isImageContentType(contentType: String): Boolean {
        val imageTypes = listOf(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/gif",
            "image/webp",
            "image/bmp"
        )
        return imageTypes.contains(contentType.lowercase())
    }

    /**
     * 获取文件扩展名
     */
    private fun getFileExtension(fileName: String): String {
        val lastDotIndex = fileName.lastIndexOf('.')
        return if (lastDotIndex != -1 && lastDotIndex < fileName.length - 1) {
            fileName.substring(lastDotIndex + 1).lowercase()
        } else {
            "bin"
        }
    }

    /**
     * 获取文件完整路径
     */
    fun getFullPath(fileUrl: String): Path {
        val relativePath = fileUrl.substringAfter(urlPrefix).trim('/')
        return Paths.get(uploadLocation, relativePath)
    }
}
