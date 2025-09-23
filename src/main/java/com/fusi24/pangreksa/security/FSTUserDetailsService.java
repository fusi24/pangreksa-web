package com.fusi24.pangreksa.security;

import com.fusi24.pangreksa.web.model.entity.FwAppUser;
import com.fusi24.pangreksa.web.model.entity.VwAppUserRole;
import com.fusi24.pangreksa.web.repo.FwAppUserRepository;
import com.fusi24.pangreksa.web.repo.VwAppUserRoleRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
final class FSTUserDetailsService implements UserDetailsService {

    private static final PasswordEncoder PASSWORD_ENCODER = PasswordEncoderFactories
            .createDelegatingPasswordEncoder();

    @Autowired
    private FwAppUserRepository appUserRepository;
    @Autowired
    private VwAppUserRoleRepository appUserRoleRepository;

    FSTUserDetailsService() {
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Optional<FwAppUser> appUserOptional = appUserRepository.findByUsername(username);

        // Check if fwAppUser exists
        if (appUserOptional.isEmpty()) {
            throw new UsernameNotFoundException(username);
        } else {
            // Roles
            List<VwAppUserRole> appUserRoleList = appUserRoleRepository.findByUsername(username);

            Collection<GrantedAuthority> roles = appUserRoleList.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getRole())) // Prefix roles with "ROLE_" diperlukan oleh Spring Security untuk GrantedAuthority.
                    .collect(Collectors.toList());

            log.info("User {} has roles: {}", username, roles.stream()
                    .map(GrantedAuthority::getAuthority).collect(Collectors.joining(", ")));

            FSTUser user = new FSTUser(appUserOptional.get(), roles,
                    getPossiblePassword(appUserOptional.get()));

            return user;
        }
    }

    private String getPossiblePassword(FwAppUser appUser) {

        if (appUser.getPassword() == null || appUser.getPassword().isBlank()) {
            return appUser.getPasswordHash();
        } else {

            String encodedPassword = PASSWORD_ENCODER.encode(appUser.getPassword());

            appUser.setPasswordHash(encodedPassword);
            appUser.setPassword(null);
            appUserRepository.save(appUser);

            return encodedPassword;
        }
    }
}
