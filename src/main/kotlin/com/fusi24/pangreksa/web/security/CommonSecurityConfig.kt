package com.fusi24.pangreksa.web.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextHolderStrategy
import org.springframework.security.web.SecurityFilterChain


@Configuration
@EnableWebSecurity
class CommonSecurityConfig {

    @Bean
    fun securityContextHolderStrategy(): SecurityContextHolderStrategy {
        return SecurityContextHolder.getContextHolderStrategy()
    }

    @Bean
    fun currentUser(securityContextHolderStrategy: SecurityContextHolderStrategy): CurrentUser {
        return CurrentUser(securityContextHolderStrategy)
    }
}