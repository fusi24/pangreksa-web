package com.fusi24.pangreksa.web.security

import com.fusi24.pangreksa.web.security.domain.UserId
import java.time.ZoneId
import java.util.Locale

interface AppUserInfo {

    val userId: UserId?
    val preferredUsername: String
    val fullName: String
        get() = this.preferredUsername
    val profileUrl: String?
    val pictureUrl: String?
    val email: String?
        get() = null
    val zoneId: ZoneId
        get() = ZoneId.systemDefault()
    val locale: Locale
        get() = Locale.getDefault()
}
