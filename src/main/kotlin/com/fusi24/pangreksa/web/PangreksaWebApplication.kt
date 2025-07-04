package com.fusi24.pangreksa.web

import com.vaadin.flow.component.page.AppShellConfigurator
import com.vaadin.flow.theme.Theme
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import java.time.Clock;



@SpringBootApplication
@Theme("default")
class PangreksaWebApplication: AppShellConfigurator

fun clock(): Clock {
	return Clock.systemDefaultZone()
}

fun main(args: Array<String>) {
	runApplication<PangreksaWebApplication>(*args)
}
