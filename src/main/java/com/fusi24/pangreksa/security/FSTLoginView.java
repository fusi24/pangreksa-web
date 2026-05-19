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

import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.util.Arrays;
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
                 ResourcePatternResolver resourceResolver) {
        this.authenticationContext = authenticationContext;

        setSizeFull();
        addClassNames("dev-login-view");
        UI.getCurrent().getPage().setColorScheme(ColorScheme.Value.LIGHT);

        // ── Left: banner (2/3) ─────────────────────────────────────────
        String bannerUrl = pickRandomBanner(resourceResolver);

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
     * Scans the classpath for banner images whose filename starts with
     * {@code login-banner-}. Works in both exploded (dev) and packaged JAR
     * (production) mode — unlike {@code ServletContext.getResourcePaths()},
     * Spring's {@link ResourcePatternResolver} can list entries inside JARs.
     */
    private static String pickRandomBanner(ResourcePatternResolver resolver) {
        try {
            Resource[] resources = resolver.getResources(
                    "classpath:/META-INF/resources/images/login/" + BANNER_PREFIX + "*");
            List<String> banners = Arrays.stream(resources)
                    .map(Resource::getFilename)
                    .filter(name -> name != null && name.startsWith(BANNER_PREFIX))
                    .map(name -> "images/login/" + name)
                    .toList();
            if (!banners.isEmpty()) {
                return banners.get(ThreadLocalRandom.current().nextInt(banners.size()));
            }
        } catch (IOException e) {
            log.warn("Could not list login banners: {}", e.getMessage());
        }
        return FALLBACK_BANNER;
    }
}
