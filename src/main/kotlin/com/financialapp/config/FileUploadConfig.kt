package com.financialapp.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer
import java.io.File

@Configuration
class FileUploadConfig : WebMvcConfigurer {

    @Value("\${app.upload.location}")
    private lateinit var uploadLocation: String

    @Value("\${app.upload.url-prefix}")
    private lateinit var urlPrefix: String

    override fun addResourceHandlers(registry: ResourceHandlerRegistry) {
        // 确保上传目录存在
        val uploadDir = File(uploadLocation)
        if (!uploadDir.exists()) {
            uploadDir.mkdirs()
            // 创建子目录
            File(uploadDir, "avatars").mkdirs()
            File(uploadDir, "documents").mkdirs()
        }

        // 配置静态资源访问
        registry.addResourceHandler("$urlPrefix/**")
            .addResourceLocations("file:$uploadLocation/")
            .setCachePeriod(86400) // 缓存24小时
    }
}
