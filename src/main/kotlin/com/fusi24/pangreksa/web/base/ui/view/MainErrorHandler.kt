package com.fusi24.pangreksa.web.base.ui.view

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.notification.Notification
import com.vaadin.flow.component.notification.NotificationVariant
import com.vaadin.flow.server.ErrorEvent
import com.vaadin.flow.server.ErrorHandler
import com.vaadin.flow.server.VaadinServiceInitListener
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
open class MainErrorHandler {

    companion object {
        private val log: Logger = LoggerFactory.getLogger(MainErrorHandler::class.java)
    }

    @Bean
    open fun errorHandlerInitializer(): VaadinServiceInitListener {
        return VaadinServiceInitListener { event ->
            event.source.addSessionInitListener { sessionInitEvent ->
                sessionInitEvent.session.errorHandler = { errorEvent: ErrorEvent ->
                    log.error("An unexpected error occurred", errorEvent.throwable)
                    errorEvent.component.flatMap { component: Component? ->
                                    component?.ui
                                }.ifPresent { ui ->
                                    val notification = Notification(
                                        "An unexpected error has occurred. Please try again later."
                                    )
                                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR)
                                    notification.position = Notification.Position.TOP_CENTER
                                    notification.duration = 3000
                                    ui.access { notification.open() }
                                }
                } as ErrorHandler
            }
        }
    }
}