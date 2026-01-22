package com.financialapp.controller

import com.financialapp.dto.response.ApiResponse
import com.financialapp.dto.response.UserResponse
import com.financialapp.service.FileStorageService
import com.financialapp.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@Tag(name = "用户管理", description = "用户信息查询、更新等接口")
@RestController
@RequestMapping("/users")
class UserController(
    private val userService: UserService,
    private val fileStorageService: FileStorageService
) {

    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun getCurrentUser(@AuthenticationPrincipal username: String): ResponseEntity<ApiResponse<UserResponse>> {
        val userResponse = userService.getCurrentUserInfo(username)
        return ResponseEntity.ok(ApiResponse.success(userResponse))
    }

    @Operation(summary = "根据ID获取用户信息", description = "根据用户ID查询用户详细信息（仅管理员）")
    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun getUserById(@PathVariable id: Long): ResponseEntity<ApiResponse<UserResponse>> {
        val userResponse = userService.getUserById(id)
        return ResponseEntity.ok(ApiResponse.success(userResponse))
    }

    @Operation(summary = "上传头像", description = "上传用户头像")
    @PostMapping("/avatar")
    @PreAuthorize("isAuthenticated()")
    fun uploadAvatar(
        @AuthenticationPrincipal username: String,
        @RequestParam("avatar") file: MultipartFile
    ): ResponseEntity<ApiResponse<Map<String, String>>> {
        val avatarUrl = userService.updateAvatar(username, file)
        return ResponseEntity.ok(
            ApiResponse.success(
                data = mapOf("avatar" to avatarUrl),
                message = "头像上传成功"
            )
        )
    }

    @Operation(summary = "删除头像", description = "删除用户头像并恢复默认头像")
    @DeleteMapping("/avatar")
    @PreAuthorize("isAuthenticated()")
    fun deleteAvatar(@AuthenticationPrincipal username: String): ResponseEntity<ApiResponse<Void>> {
        userService.deleteAvatar(username)
        return ResponseEntity.ok(ApiResponse.success(message = "头像删除成功"))
    }
}
