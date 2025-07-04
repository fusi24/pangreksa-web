package com.fusi24.pangreksa.web.security.dev

import com.fusi24.pangreksa.web.security.AppUserInfo
import com.fusi24.pangreksa.web.security.domain.UserId
import java.time.ZoneId
import java.util.Locale

data class DevUserInfo(
    override val userId: UserId,
    override val preferredUsername: String,
    override val fullName: String,
    override val profileUrl: String?,
    override val pictureUrl: String?,
    override val email: String?,
    override val zoneId: ZoneId,
    override val locale: Locale
) : AppUserInfo {

    init {
        requireNotNull(userId) { "userId must not be null" }
        requireNotNull(preferredUsername) { "preferredUsername must not be null" }
        requireNotNull(fullName) { "fullName must not be null" }
        requireNotNull(zoneId) { "zoneId must not be null" }
        requireNotNull(locale) { "locale must not be null" }
    }

    fun getUserId(): UserId = userId
    fun getPreferredUsername(): String = preferredUsername
    fun getFullName(): String = fullName
    fun getProfileUrl(): String? = profileUrl
    fun getPictureUrl(): String? = pictureUrl
    fun getEmail(): String? = email
    fun getZoneId(): ZoneId = zoneId
    fun getLocale(): Locale = locale
}
