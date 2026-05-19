package com.fusi24.pangreksa;

import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.shared.communication.PushMode;
import com.vaadin.flow.theme.aura.Aura;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.time.Clock;

@SpringBootApplication(scanBasePackages = {"com.fusi24.pangreksa", "com.pangreksa.service"})
@EntityScan(basePackages = {"com.pangreksa.service.model.entity", "com.fusi24.pangreksa.taskmanagement.domain"})
@EnableJpaRepositories(basePackages = {"com.pangreksa.service.model.repo", "com.fusi24.pangreksa.taskmanagement.domain"})
@StyleSheet(Aura.STYLESHEET)
@StyleSheet("styles.css")
@Push(PushMode.AUTOMATIC)
public class Application implements AppShellConfigurator {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone(); // You can also use Clock.systemUTC()
    }

    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
