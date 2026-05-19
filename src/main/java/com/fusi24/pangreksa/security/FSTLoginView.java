package com.fusi24.pangreksa.security;

import com.pangreksa.service.service.SystemService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.DescriptionList;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.page.ColorScheme;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import lombok.extern.slf4j.Slf4j;

import jakarta.servlet.ServletContext;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Login view for development — split-screen layout with a random banner image
 * on the left (2/3) and the login form on the right (1/3).
 */
@PageTitle("Login")
@AnonymousAllowed
@Slf4j
@StyleSheet("login.css")
// No @Route annotation — the route is registered dynamically by DevSecurityConfig.
class FSTLoginView extends Main implements BeforeEnterObserver {

    static final String LOGIN_PATH = "login";
    private static final String BANNER_DIR = "/images/login/";
    private static final String BANNER_PREFIX = "login-banner-";
    private static final String FALLBACK_BANNER = "images/login/login-banner-01.jpg";

    private final com.vaadin.flow.spring.security.AuthenticationContext authenticationContext;
    private final LoginForm login;

    FSTLoginView(com.vaadin.flow.spring.security.AuthenticationContext authenticationContext,
                 SystemService systemService,
                 ServletContext servletContext) {
        this.authenticationContext = authenticationContext;

        setSizeFull();
        addClassNames("dev-login-view");
        UI.getCurrent().getPage().setColorScheme(ColorScheme.Value.LIGHT);

        // ── Left: banner (2/3) ─────────────────────────────────────────
        String bannerUrl = pickRandomBanner(servletContext);

        // Brand overlay: logo + app name + tagline
        Image appLogo = new Image("images/pangreksa-logo-white.png", "Pangreksa Logo");
        appLogo.addClassName("login-banner-logo");

        String name = systemService.getStringAppName();
        var appName = new Span(!name.isBlank() ? name : "Pangreksa");
        appName.addClassName("login-banner-app-name");

        var tagline = new Paragraph(
                "Manage your organization's most valuable asset - your people.");
        tagline.addClassName("login-banner-tagline");

        var brandOverlay = new Div(appLogo, appName, tagline);
        brandOverlay.addClassName("login-banner-overlay");

        var bannerPanel = new Div(brandOverlay);
        bannerPanel.addClassName("login-banner");
        bannerPanel.getStyle().set("background-image", "url('" + bannerUrl + "')");

        // ── Right: login form (1/3) ────────────────────────────────────
        var welcomeTitle = new H2("Welcome back");
        welcomeTitle.addClassName("login-welcome-title");

        var welcomeSubtitle = new Paragraph("Sign in to continue to your application.");
        welcomeSubtitle.addClassName("login-welcome-subtitle");

        login = new LoginForm();
        login.setAction(LOGIN_PATH);
        login.setForgotPasswordButtonVisible(false);

        var exampleUsers = new Div(new Div("Dev credentials"));
        exampleUsers.add(createSampleUserCard("Maman Surahman", "maman", "123"));
        exampleUsers.add(createSampleUserCard("Okan Ayadin", "okanay", "singgih"));
        exampleUsers.addClassNames("dev-users");

        var formPanel = new Div(welcomeTitle, welcomeSubtitle, login/*, exampleUsers*/);
        formPanel.addClassName("login-form-panel");

        add(bannerPanel, formPanel);
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
            event.forwardTo("");
            return;
        }

        if (event.getLocation().getQueryParameters().getParameters().containsKey("error")) {
            login.setError(true);
        }
    }

    /**
     * Scans the {@code /images/login/} resource directory for files whose name
     * starts with {@code login-banner-} and picks one at random.
     */
    private static String pickRandomBanner(ServletContext servletContext) {
        try {
            var paths = servletContext.getResourcePaths(BANNER_DIR);
            if (paths != null) {
                List<String> banners = paths.stream()
                        .map(p -> p.startsWith("/") ? p.substring(1) : p)  // strip leading /
                        .filter(p -> {
                            String fileName = p.substring(p.lastIndexOf('/') + 1);
                            return fileName.startsWith(BANNER_PREFIX);
                        })
                        .toList();
                if (!banners.isEmpty()) {
                    return banners.get(ThreadLocalRandom.current().nextInt(banners.size()));
                }
            }
        } catch (Exception e) {
            log.warn("Could not list login banners from {}: {}", BANNER_DIR, e.getMessage());
        }
        return FALLBACK_BANNER;
    }
}
