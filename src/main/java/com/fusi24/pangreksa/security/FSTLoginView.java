package com.fusi24.pangreksa.security;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.DescriptionList;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.page.WebStorage;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.spring.security.AuthenticationContext;
import lombok.extern.slf4j.Slf4j;

/**
 * Login view for development.
 */
@PageTitle("Login")
@AnonymousAllowed
@Slf4j
// No @Route annotation - the route is registered dynamically by DevSecurityConfig.
class FSTLoginView extends Main implements BeforeEnterObserver{

    static final String LOGIN_PATH = "login";
    private static final String CALLOUT_HIDDEN_KEY = "walking-skeleton-fst-login-callout-hidden";

    private final AuthenticationContext authenticationContext;
    private final LoginForm login;

    FSTLoginView(AuthenticationContext authenticationContext) {
        this.authenticationContext = authenticationContext;

        // Create the components
        login = new LoginForm();
        login.setAction(LOGIN_PATH);
        login.setForgotPasswordButtonVisible(false);

        // Configure the view
        setSizeFull();
        addClassNames("dev-login-view");

        var exampleUsers = new Div(new Div("Use the following details to login"));
        exampleUsers.add(createSampleUserCard("Maman Surahman", "maman","123"));
        exampleUsers.add(createSampleUserCard("Okan Ayadin", "okanay","singgih"));
        exampleUsers.addClassNames("dev-users");

        var contentDiv = new Div(login, exampleUsers);
        contentDiv.addClassNames("dev-content-div");
        add(contentDiv);

        exampleUsers.setVisible(false);


    }

    private Component createSampleUserCard(String nickname, String username, String password) {
        var card = new Div();
        card.addClassNames("dev-user-card");

        var fullName = new H3(nickname);

        var credentials = new DescriptionList();
        credentials.add(new DescriptionList.Term("Username"),
                new DescriptionList.Description(username));
        credentials.add(new DescriptionList.Term("Password"),
                new DescriptionList.Description(password));

        // Make it easier to log in while still going through the normal authentication process.
        var loginButton = new Button(VaadinIcon.SIGN_IN.create(), event -> {
            login.getElement().executeJs("""
                    document.getElementById("vaadinLoginUsername").value = $0;
                    document.getElementById("vaadinLoginPassword").value = $1;
                    document.forms[0].submit();
                    """, username, password);
        });
        loginButton.addThemeVariants(ButtonVariant.LUMO_ICON, ButtonVariant.LUMO_TERTIARY);

        card.add(new Div(fullName, credentials));
        card.add(loginButton);

        return card;
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (authenticationContext.isAuthenticated()) {
            // Redirect to the main view if the user is already logged in. This makes impersonation easier to work with.
            event.forwardTo("");
            return;
        }

        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            login.setError(true);
        }
    }
}
