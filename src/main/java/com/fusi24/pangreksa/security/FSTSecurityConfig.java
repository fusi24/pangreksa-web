package com.fusi24.pangreksa.security;

import com.fusi24.pangreksa.security.controlcenter.ControlCenterSecurityConfig;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.VaadinServiceInitListener;
import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategyConfiguration;
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.SecurityFilterChain;

@EnableWebSecurity
@Configuration
@Import({ VaadinAwareSecurityContextHolderStrategyConfiguration.class })
@ConditionalOnMissingBean(ControlCenterSecurityConfig.class)
class FSTSecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(FSTSecurityConfig.class);

    FSTSecurityConfig() {
        log.warn("Using DEVELOPMENT security configuration. This should not be used in production environments!");
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.headers(headers -> headers.frameOptions().disable())
            .csrf(csrf -> csrf.ignoringRequestMatchers("/h2-console/**"))
            .authorizeHttpRequests(authz -> authz
                            .requestMatchers("/h2-console/**").permitAll()
                    // Jangan tambahkan .anyRequest() di sini
            );

        http.with(VaadinSecurityConfigurer.vaadin(),

                configurer -> configurer.loginView(FSTLoginView.LOGIN_PATH));

        return http.build();
    }

    @Bean
    UserDetailsService userDetailsService() {
        return new FSTUserDetailsService();
    }

    @Bean
    VaadinServiceInitListener developmentLoginConfigurer() {
        return (serviceInitEvent) -> {
            if (serviceInitEvent.getSource().getDeploymentConfiguration().isProductionMode()) {
                throw new IllegalStateException(
                        "Development profile is active but Vaadin is running in production mode. This indicates a configuration error - development profile should not be used in production.");
            }
            var routeConfiguration = RouteConfiguration.forApplicationScope();
            routeConfiguration.setRoute(FSTLoginView.LOGIN_PATH, FSTLoginView.class);
        };
    }
}
