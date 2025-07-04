package com.fusi24.pangreksa.web.security.dev

import com.fusi24.pangreksa.web.security.AppUserInfo
import com.fusi24.pangreksa.web.security.AppUserPrincipal
import com.fusi24.pangreksa.web.security.domain.UserId
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.AuthorityUtils
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.crypto.factory.PasswordEncoderFactories
import org.springframework.security.crypto.password.PasswordEncoder
import java.time.ZoneId
import java.util.*

class DevUser(
    override val appUser: AppUserInfo,
    val authorities: MutableSet<GrantedAuthority>,
    private val password: String
) : AppUserPrincipal, UserDetails {

    //override fun getAuthorities(): Collection<GrantedAuthority> = authorities

    override fun getPassword(): String = password

    override fun getUsername(): String = appUser.preferredUsername

    override fun equals(other: Any?): Boolean {
        return other is DevUser && this.appUser.userId == other.appUser.userId
    }

    override fun hashCode(): Int = appUser.userId.hashCode()

    companion object {
        fun builder(): DevUserBuilder = DevUserBuilder()
    }

    class DevUserBuilder {
        private val passwordEncoder: PasswordEncoder = PasswordEncoderFactories.createDelegatingPasswordEncoder()

        private var userId: UserId? = null
        private var preferredUsername: String? = null
        private var fullName: String? = null
        private var email: String? = null
        private var profileUrl: String? = null
        private var pictureUrl: String? = null
        private var zoneInfo: ZoneId = ZoneId.systemDefault()
        private var locale: Locale = Locale.getDefault()
        private var authorities: List<GrantedAuthority> = emptyList()
        private var password: String? = null

        fun userId(userId: UserId) = apply { this.userId = userId }

        fun preferredUsername(username: String) = apply { this.preferredUsername = username }

        fun fullName(name: String?) = apply { this.fullName = name }

        fun email(email: String?) = apply { this.email = email }

        fun profileUrl(url: String?) = apply { this.profileUrl = url }

        fun pictureUrl(url: String?) = apply { this.pictureUrl = url }

        fun zoneInfo(zoneId: ZoneId) = apply { this.zoneInfo = zoneId }

        fun locale(locale: Locale) = apply { this.locale = locale }

        fun roles(vararg roles: String) = apply {
            this.authorities = roles.map { SimpleGrantedAuthority("ROLE_$it") }
        }

        fun authorities(vararg authorities: String) = apply {
            this.authorities = AuthorityUtils.createAuthorityList(*authorities)
        }

        fun authorities(authorities: Collection<GrantedAuthority>) = apply {
            this.authorities = ArrayList(authorities)
        }

        fun password(password: String) = apply { this.password = password }

        fun build(): DevUser {
            val username = preferredUsername ?: throw IllegalStateException("Preferred username must be set")
            val rawPassword = password ?: throw IllegalStateException("Password must be set")
            val encodedPassword = passwordEncoder.encode(rawPassword)
            val id = userId ?: UserId.of(UUID.randomUUID().toString())
            val name = fullName ?: username

            val info = DevUserInfo(
                id, username, name, profileUrl, pictureUrl, email, zoneInfo, locale
            )

            return DevUser(info, authorities.toMutableSet(), encodedPassword)
        }
    }
}
