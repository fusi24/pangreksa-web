package com.fusi24.pangreksa.web.security.dev

import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException

class DevUserDetailsService(
    users: Collection<DevUser>
) : UserDetailsService {

    private val userByUsername: Map<String, UserDetails> = users.associateBy { it.appUser.preferredUsername }

    override fun loadUserByUsername(username: String): UserDetails {
        return userByUsername[username] ?: throw UsernameNotFoundException(username)
    }
}
