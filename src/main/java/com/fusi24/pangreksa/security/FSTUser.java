package com.fusi24.pangreksa.security;

import com.fusi24.pangreksa.security.domain.UserId;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.ZoneId;
import java.util.*;

import static java.util.Objects.requireNonNull;

final class FSTUser implements AppUserPrincipal, UserDetails {

    private final AppUserInfo appUser;
    private final Set<GrantedAuthority> authorities;
    private final String password;

    FSTUser(AppUserInfo appUser, Collection<GrantedAuthority> authorities, String password) {
        this.appUser = requireNonNull(appUser);
        this.authorities = Set.copyOf(authorities);
        this.password = requireNonNull(password);
    }

    @Override
    public AppUserInfo getAppUser() {
        return appUser;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return appUser.getPreferredUsername();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof FSTUser user) {
            return this.appUser.getUserId().equals(user.appUser.getUserId());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return this.appUser.getUserId().hashCode();
    }

}
