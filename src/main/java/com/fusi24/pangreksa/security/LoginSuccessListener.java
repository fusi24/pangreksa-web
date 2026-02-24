package com.fusi24.pangreksa.security;

import com.fusi24.pangreksa.web.model.entity.FwAppUser;
import com.fusi24.pangreksa.web.repo.FwAppUserRepository;
import org.springframework.context.event.EventListener;
import org.springframework.security.authentication.event.AuthenticationSuccessEvent;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;


@Component
public class LoginSuccessListener {

    private final FwAppUserRepository userRepository;



    public LoginSuccessListener(FwAppUserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @EventListener
    public void handleLoginSuccess(AuthenticationSuccessEvent event) {

        String username = event.getAuthentication().getName();
        FwAppUser user = userRepository.findByUsername(username).orElse(null);

        if (user != null) {
            user.setLastLogin(
                    ZonedDateTime.now(ZoneId.of("Asia/Jakarta")).toLocalDateTime()
            );
            userRepository.save(user);
        }
    }
}