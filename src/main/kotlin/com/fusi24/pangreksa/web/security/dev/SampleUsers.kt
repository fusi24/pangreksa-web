package com.fusi24.pangreksa.web.security.dev

import com.fusi24.pangreksa.web.security.AppRoles
import com.fusi24.pangreksa.web.security.domain.UserId
import java.util.*

/**
 * Provides predefined sample users for development and testing environments.
 *
 * This object contains constants and preconfigured [DevUser] instances
 * that can be reused across dev/test configurations.
 */
object SampleUsers {

    /**
     * The raw, unencoded password used by all sample users.
     */
    const val SAMPLE_PASSWORD: String = "123"

    /**
     * The user ID for the admin sample user.
     */
    val ADMIN_ID: UserId = UserId.of(UUID.randomUUID().toString())

    /**
     * The preferred username of the admin sample user.
     */
    const val ADMIN_USERNAME: String = "admin"

    /**
     * The admin sample user with "ADMIN" and "USER" roles.
     */
    val ADMIN: DevUser = DevUser.builder()
        .preferredUsername(ADMIN_USERNAME)
        .fullName("Alice Administrator")
        .userId(ADMIN_ID)
        .password(SAMPLE_PASSWORD)
        .email("alice@example.com")
        .roles(AppRoles.ADMIN, AppRoles.USER)
        .build()

    /**
     * The user ID for the regular sample user.
     */
    val USER_ID: UserId = UserId.of(UUID.randomUUID().toString())

    /**
     * The preferred username of the regular sample user.
     */
    const val USER_USERNAME: String = "user"

    /**
     * The regular sample user with "USER" role only.
     */
    val USER: DevUser = DevUser.builder()
        .preferredUsername(USER_USERNAME)
        .fullName("Ursula User")
        .userId(USER_ID)
        .password(SAMPLE_PASSWORD)
        .email("ursula@example.com")
        .roles(AppRoles.USER)
        .build()

    /**
     * List of all sample users.
     */
    val ALL_USERS: List<DevUser> = listOf(USER, ADMIN)
}
