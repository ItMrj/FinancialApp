package com.financialapp.security

import com.financialapp.entity.User
import com.financialapp.entity.UserRole
import com.financialapp.entity.UserStatus
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails

data class UserDetailsImpl(
    val id: Long,
    private val username: String,
    private val email: String,
    private val password: String,
    private val role: UserRole,
    private val status: UserStatus,
    private val enabled: Boolean
) : UserDetails {

    override fun getAuthorities(): Collection<GrantedAuthority> {
        return listOf(SimpleGrantedAuthority("ROLE_${role.name}"))
    }

    override fun getPassword(): String = password

    override fun getUsername(): String = username

    override fun isAccountNonExpired(): Boolean = true

    override fun isAccountNonLocked(): Boolean = status != UserStatus.LOCKED

    override fun isCredentialsNonExpired(): Boolean = true

    override fun isEnabled(): Boolean = enabled && status == UserStatus.ACTIVE

    // 公共方法用于获取私有属性
    fun getEmail(): String = email

    fun getRole(): UserRole = role

    fun getUsernamePublic(): String = username

    companion object {
        fun build(user: User): UserDetailsImpl {
            return UserDetailsImpl(
                id = user.id!!,
                username = user.username,
                email = user.email,
                password = user.password,
                role = user.role,
                status = user.status,
                enabled = user.enabled
            )
        }
    }
}
