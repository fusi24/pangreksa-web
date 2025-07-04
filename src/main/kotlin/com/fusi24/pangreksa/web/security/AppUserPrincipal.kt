package com.fusi24.pangreksa.web.security

import org.springframework.security.core.GrantedAuthority
import java.security.Principal


interface AppUserPrincipal : Principal {
    val appUser: AppUserInfo?
    override fun getName(): String {
        return this.appUser!!.userId.toString()
    }
    val authorities: MutableCollection<out GrantedAuthority>?
}
