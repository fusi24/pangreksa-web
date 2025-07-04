package com.fusi24.pangreksa.web.security

import org.slf4j.LoggerFactory
import org.springframework.security.authentication.AnonymousAuthenticationToken
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolderStrategy
import java.util.Optional

/**
 * Service for retrieving the currently authenticated user from the Spring Security context.
 */
class CurrentUser(
    private val securityContextHolderStrategy: SecurityContextHolderStrategy
) {

    private val log = LoggerFactory.getLogger(CurrentUser::class.java)

    /**
     * Returns the currently authenticated user from the security context.
     */
    fun get(): Optional<AppUserInfo> {
        return getPrincipal().map { it.appUser }
    }

    /**
     * Returns the currently authenticated principal from the security context.
     */
    fun getPrincipal(): Optional<AppUserPrincipal> {
        val auth = securityContextHolderStrategy.context.authentication
        return Optional.ofNullable(getPrincipalFromAuthentication(auth))
    }

    /**
     * Extracts the principal from the provided authentication object.
     */
    private fun getPrincipalFromAuthentication(authentication: Authentication?): AppUserPrincipal? {
        if (authentication == null ||
            authentication.principal == null ||
            authentication is AnonymousAuthenticationToken
        ) {
            return null
        }

        val principal = authentication.principal

        return if (principal is AppUserPrincipal) {
            principal
        } else {
            log.warn("Unexpected principal type: {}", principal::class.java.name)
            null
        }
    }

    /**
     * Returns the currently authenticated user or throws if unavailable.
     */
    fun require(): AppUserInfo {
        return get().orElseThrow {
            AuthenticationCredentialsNotFoundException("No current user")
        }
    }

    fun requirePrincipal(): AppUserPrincipal {
        return getPrincipal().orElseThrow {
            AuthenticationCredentialsNotFoundException("No current user")
        }
    }
}
