package com.financialapp.controller

import com.financialapp.dto.request.LoginRequest
import com.financialapp.dto.request.RegisterRequest
import com.financialapp.dto.response.ApiResponse
import com.financialapp.dto.response.AuthResponse
import com.financialapp.dto.response.UserResponse
import com.financialapp.security.UserDetailsImpl
import com.financialapp.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.*

@Tag(name = "认证管理", description = "用户登录、注册、登出等认证相关接口")
@RestController
@RequestMapping("/auth")
class AuthController(
    private val authService: AuthService
) {

    @Operation(summary = "用户登录", description = "使用用户名和密码登录系统")
    @PostMapping("/login")
    fun login(
        @Valid @RequestBody request: LoginRequest,
        httpRequest: HttpServletRequest
    ): ResponseEntity<ApiResponse<AuthResponse>> {
        val authResponse = authService.login(request, httpRequest)
        return ResponseEntity.ok(ApiResponse.success(authResponse, "登录成功"))
    }

    @Operation(summary = "用户注册", description = "注册新用户")
    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<ApiResponse<UserResponse>> {
        val userResponse = authService.register(request)
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(userResponse, "注册成功"))
    }

    @Operation(summary = "用户登出", description = "退出登录")
    @PostMapping("/logout")
    @PreAuthorize("isAuthenticated()")
    fun logout(@AuthenticationPrincipal userDetails: UserDetailsImpl): ResponseEntity<ApiResponse<Void>> {
        authService.logout(userDetails.id)
        return ResponseEntity.ok(ApiResponse.success(message = "登出成功"))
    }

    @Operation(summary = "刷新Token", description = "使用刷新Token获取新的访问Token")
    @PostMapping("/refresh")
    @PreAuthorize("isAuthenticated()")
    fun refreshToken(@AuthenticationPrincipal userDetails: UserDetailsImpl): ResponseEntity<ApiResponse<AuthResponse>> {
        val authResponse = authService.refreshToken(userDetails as org.springframework.security.core.Authentication)
        return ResponseEntity.ok(ApiResponse.success(authResponse, "刷新Token成功"))
    }

    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    fun getCurrentUser(@AuthenticationPrincipal userDetails: UserDetailsImpl): ResponseEntity<ApiResponse<UserResponse>> {
        val userResponse = authService.getCurrentUser(userDetails.id)
        return ResponseEntity.ok(ApiResponse.success(userResponse))
    }
}
