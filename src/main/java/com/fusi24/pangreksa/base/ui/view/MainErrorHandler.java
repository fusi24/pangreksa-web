package com.fusi24.pangreksa.base.ui.view;

import com.fusi24.pangreksa.base.ui.notification.AppNotification;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class MainErrorHandler {

    private static final Logger log = LoggerFactory.getLogger(MainErrorHandler.class);

    @Bean
    public VaadinServiceInitListener errorHandlerInitializer() {
        return (event) -> event.getSource().addSessionInitListener(
                sessionInitEvent -> sessionInitEvent.getSession().setErrorHandler(errorEvent -> {

                    log.error("Terjadi kesalahan yang tidak terduga", errorEvent.getThrowable());

                    errorEvent.getComponent()
                            .flatMap(Component::getUI)
                            .ifPresent(ui -> ui.access(() ->
                                    AppNotification.error(
                                            "Terjadi kesalahan sistem. Silakan coba beberapa saat lagi."
                                    )
                            ));
                }));
    }
}