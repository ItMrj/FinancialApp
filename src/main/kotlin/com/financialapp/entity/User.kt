package com.financialapp.entity

import jakarta.persistence.*
import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.jpa.domain.support.AuditingEntityListener
import java.time.LocalDateTime

@Entity
@Table(name = "users", indexes = [
    Index(name = "idx_username", columnList = "username", unique = true),
    Index(name = "idx_email", columnList = "email", unique = true),
    Index(name = "idx_phone", columnList = "phone", unique = true)
])
@EntityListeners(AuditingEntityListener::class)
data class User(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(nullable = false, unique = true, length = 50)
    val username: String,

    @Column(nullable = false, length = 255)
    val password: String,

    @Column(nullable = false, unique = true, length = 100)
    val email: String,

    @Column(length = 20)
    val phone: String? = null,

    @Column(nullable = false, length = 50)
    val firstName: String,

    @Column(nullable = false, length = 50)
    val lastName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    val role: UserRole = UserRole.USER,

    @Column(nullable = false)
    val enabled: Boolean = true,

    @Column(nullable = false, length = 100)
    val status: UserStatus = UserStatus.ACTIVE,

    @Column(nullable = false, length = 500)
    val avatar: String = "default-avatar.png",

    @CreatedDate
    @Column(nullable = false, updatable = false)
    var createdAt: LocalDateTime? = null,

    @LastModifiedDate
    @Column(nullable = false)
    var updatedAt: LocalDateTime? = null,

    @Column
    val lastLoginAt: LocalDateTime? = null,

    @Column(length = 100)
    val lastLoginIp: String? = null
)

enum class UserRole {
    ADMIN,
    MANAGER,
    USER
}

enum class UserStatus {
    ACTIVE,
    INACTIVE,
    LOCKED,
    PENDING
}
