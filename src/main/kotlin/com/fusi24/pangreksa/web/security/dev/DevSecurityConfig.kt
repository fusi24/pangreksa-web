package com.fusi24.pangreksa.web.security.dev

import com.vaadin.flow.router.RouteConfiguration
import com.vaadin.flow.server.VaadinServiceInitListener
import com.vaadin.flow.spring.security.VaadinAwareSecurityContextHolderStrategyConfiguration
import com.vaadin.flow.spring.security.VaadinSecurityConfigurer
import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.Import
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.SecurityFilterChain

/**
 * Security configuration for the development environment.
 */
@EnableWebSecurity
@Configuration
@Import(VaadinAwareSecurityContextHolderStrategyConfiguration::class)
class DevSecurityConfig {

    private val log = LoggerFactory.getLogger(DevSecurityConfig::class.java)

    init {
        log.warn("Using DEVELOPMENT security configuration. This should not be used in production environments!")
    }

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        return http.with(VaadinSecurityConfigurer.vaadin()) {
            it.loginView(DevLoginView.LOGIN_PATH)
        }.build()
    }

    @Bean
    fun userDetailsService(): UserDetailsService {
        return DevUserDetailsService(SampleUsers.ALL_USERS)
    }

    @Bean
    fun developmentLoginConfigurer(): VaadinServiceInitListener {
        return VaadinServiceInitListener { serviceInitEvent ->
            if (serviceInitEvent.source.deploymentConfiguration.isProductionMode) {
                throw IllegalStateException(
                    "Development profile is active but Vaadin is running in production mode. " +
                            "This indicates a configuration error – development profile should not be used in production."
                )
            }

            val routeConfiguration = RouteConfiguration.forApplicationScope()
            routeConfiguration.setRoute(DevLoginView.LOGIN_PATH, DevLoginView::class.java)
        }
    }
}
