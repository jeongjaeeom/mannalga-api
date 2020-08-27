package com.taskforce.superinvention.app.domain.user

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException
import org.springframework.stereotype.Service

@Service
class UserDetailsService(
        private val userRepository: UserRepository
): UserDetailsService {

    override fun loadUserByUsername(userId: String): UserDetails {
        val user: User? = userRepository.findByUserId(userId)

        if(user == null) {
            throw UsernameNotFoundException("User $userId is not Exist")
        }
        return user
    }
}