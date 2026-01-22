package com.financialapp.repository

import com.financialapp.entity.User
import com.financialapp.entity.UserStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.util.Optional

@Repository
interface UserRepository : JpaRepository<User, Long> {

    fun findByUsername(username: String): Optional<User>

    fun findByEmail(email: String): Optional<User>

    fun findByPhone(phone: String): Optional<User>

    fun existsByUsername(username: String): Boolean

    fun existsByEmail(email: String): Boolean

    fun existsByPhone(phone: String): Boolean

    @Query("SELECT u FROM User u WHERE u.username = :username AND u.enabled = true AND u.status = :status")
    fun findActiveUser(@Param("username") username: String, @Param("status") status: UserStatus): Optional<User>

    @Query("SELECT u FROM User u WHERE u.username = :username OR u.email = :email OR u.phone = :phone")
    fun findByAny(@Param("username") username: String, @Param("email") email: String, @Param("phone") phone: String): Optional<User>
}
