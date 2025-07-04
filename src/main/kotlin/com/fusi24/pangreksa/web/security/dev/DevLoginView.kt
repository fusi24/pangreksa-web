package com.fusi24.pangreksa.web.security.dev

import com.vaadin.flow.component.Component
import com.vaadin.flow.component.button.Button
import com.vaadin.flow.component.button.ButtonVariant
import com.vaadin.flow.component.html.*
import com.vaadin.flow.component.icon.VaadinIcon
import com.vaadin.flow.component.login.LoginForm
import com.vaadin.flow.component.page.WebStorage
import com.vaadin.flow.router.BeforeEnterEvent
import com.vaadin.flow.router.BeforeEnterObserver
import com.vaadin.flow.router.PageTitle
import com.vaadin.flow.server.auth.AnonymousAllowed
import com.vaadin.flow.spring.security.AuthenticationContext

@PageTitle("Login")
@AnonymousAllowed
// No @Route annotation – route registered dynamically in DevSecurityConfig
class DevLoginView(
    private val authenticationContext: AuthenticationContext
) : Main(), BeforeEnterObserver {

    companion object {
        const val LOGIN_PATH = "dev-login"
        private const val CALLOUT_HIDDEN_KEY = "walking-skeleton-dev-login-callout-hidden"
    }

    private val login: LoginForm = LoginForm().apply {
        action = LOGIN_PATH
        isForgotPasswordButtonVisible = false
    }

    init {
        // Sample user hints
        val exampleUsers = Div(Div("Use the following details to login")).apply {
            addClassNames("dev-users")
            SampleUsers.ALL_USERS.forEach { user ->
                add(createSampleUserCard(user))
            }
        }

        val contentDiv = Div(login, exampleUsers).apply {
            addClassNames("dev-content-div")
        }

        addClassNames("dev-login-view")
        setSizeFull()
        add(contentDiv)

        val devModeMenuDiv = Div("You can also use the Dev Mode Menu here to impersonate any user").apply {
            addClassNames("dev-mode-speech-bubble")
            isVisible = false
            addClickListener {
                WebStorage.setItem(WebStorage.Storage.LOCAL_STORAGE, CALLOUT_HIDDEN_KEY, "1")
                isVisible = false
            }
        }

        WebStorage.getItem(
            WebStorage.Storage.LOCAL_STORAGE,
            CALLOUT_HIDDEN_KEY
        ) { value -> devModeMenuDiv.isVisible = value == null }

        add(devModeMenuDiv)
    }

    private fun createSampleUserCard(user: DevUser): Component {
        val fullName = H3(user.appUser.fullName)

        val credentials = DescriptionList().apply {
            add(
                DescriptionList.Term("Username"),
                DescriptionList.Description(user.username)
            )
            add(
                DescriptionList.Term("Password"),
                DescriptionList.Description(SampleUsers.SAMPLE_PASSWORD)
            )
        }

        val loginButton = Button(VaadinIcon.SIGN_IN.create()) {
            login.element.executeJs(
                """
                document.getElementById("vaadinLoginUsername").value = $0;
                document.getElementById("vaadinLoginPassword").value = $1;
                document.forms[0].submit();
                """.trimIndent(),
                user.username,
                SampleUsers.SAMPLE_PASSWORD
            )
        }.apply {
            addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY)
        }

        return Div().apply {
            addClassNames("dev-user-card")
            add(Div(fullName, credentials))
            add(loginButton)
        }
    }

    override fun beforeEnter(event: BeforeEnterEvent) {
        if (authenticationContext.isAuthenticated) {
            event.forwardTo("")
            return
        }

        if (event.location.queryParameters.parameters.containsKey("error")) {
            login.isError = true
        }
    }
}
