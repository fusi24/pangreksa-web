package com.fusi24.pangreksa.web.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.core.context.SecurityContextHolderStrategy


@Configuration
@EnableWebSecurity
open class CommonSecurityConfig {

    @Bean
    open fun securityContextHolderStrategy(): SecurityContextHolderStrategy {
        return SecurityContextHolder.getContextHolderStrategy()
    }

    @Bean
    open fun currentUser(securityContextHolderStrategy: SecurityContextHolderStrategy): CurrentUser {
        return CurrentUser(securityContextHolderStrategy)
    }
}