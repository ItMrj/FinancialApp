package com.financialapp.security

import com.financialapp.entity.UserStatus
import com.financialapp.repository.UserRepository
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class CustomUserDetailsService(
    private val userRepository: UserRepository
) : UserDetailsService {

    @Transactional
    override fun loadUserByUsername(username: String): UserDetails {
        val user = userRepository.findByUsername(username)
            .orElseThrow { UsernameNotFoundException("User not found with username: $username") }
        return UserDetailsImpl.build(user)
    }

    @Transactional
    fun loadUserByUsername(username: String, status: UserStatus): UserDetails {
        val user = userRepository.findActiveUser(username, status)
            .orElseThrow { UsernameNotFoundException("User not found with username: $username") }
        return UserDetailsImpl.build(user)
    }
}
