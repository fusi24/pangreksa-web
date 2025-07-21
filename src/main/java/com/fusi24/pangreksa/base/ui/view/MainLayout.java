package com.fusi24.pangreksa.base.ui.view;

import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Responsibility;
import com.fusi24.pangreksa.web.service.AppUserAuthService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

import static com.vaadin.flow.theme.lumo.LumoUtility.*;

@Layout
@PermitAll // When security is enabled, allow all authenticated users
@Slf4j
public final class MainLayout extends AppLayout {
    private final AppUserAuthService appUserAuthService;
    private final CurrentUser currentUser;
    private final AuthenticationContext authenticationContext;

    MainLayout(CurrentUser currentUser, AuthenticationContext authenticationContext, AppUserAuthService appUserAuthService) {
        this.currentUser = currentUser;
        this.authenticationContext = authenticationContext;
        this.appUserAuthService = appUserAuthService;
        setPrimarySection(Section.DRAWER);
        addToDrawer(createHeader(), new Scroller(navWrapper()), createUserMenu());
    }

    private Div createHeader() {
        // TODO Replace with real application logo and name
        var appLogo = VaadinIcon.CUBES.create();
        appLogo.addClassNames(TextColor.PRIMARY, IconSize.LARGE);

        var appName = new Span("Pangreksa");
        appName.addClassNames(FontWeight.SEMIBOLD, FontSize.LARGE);

        var header = new Div(appLogo, appName);
        header.addClassNames(Display.FLEX, Padding.MEDIUM, Gap.MEDIUM, AlignItems.CENTER);
        return header;
    }

    private VerticalLayout navWrapper(){
        var layout = new VerticalLayout();
        var user = currentUser.require();
        List<Responsibility> responsibilityList = appUserAuthService.getAllResponsibilitiesFromUsername(user.getUserId().toString());
        log.info("Create navWrapper using username: {} with Responsibility: {}", user.getUserId(), responsibilityList.size());

        responsibilityList.forEach( r -> {
            log.info( "Create SideNav for responsibility: {}, with {} entries", r.getResponsibility(), r.getMenuEntries().size());
            SideNav nav = createSideNav(r.getResponsibility(), r.getMenuEntries());
            layout.add(nav);
        });

        layout.setSizeUndefined();
        layout.setMargin(false);
        return layout;
    }

    private SideNav createSideNav(String responsibility, List<MenuEntry> menuEntries) {
        var nav = new SideNav();
        nav.addClassNames(Margin.Horizontal.MEDIUM);

        nav.setLabel(responsibility);
        nav.setCollapsible(true);
        nav.setExpanded(false);
        menuEntries.forEach( menuEntry -> {
            nav.addItem(createSideNavItem(menuEntry));
        });

        nav.setWidthFull();

        return nav;
    }

    private SideNavItem createSideNavItem(MenuEntry menuEntry) {
        if (menuEntry.icon() != null) {
            return new SideNavItem(menuEntry.title(), menuEntry.path(), new Icon(menuEntry.icon()));
        } else {
            return new SideNavItem(menuEntry.title(), menuEntry.path());
        }
    }

    private Component createUserMenu() {
        var user = currentUser.require();

        var avatar = new Avatar(user.getFullName(), user.getPictureUrl());
        avatar.addThemeVariants(AvatarVariant.LUMO_XSMALL);
        avatar.addClassNames(Margin.Right.SMALL);
        avatar.setColorIndex(5);

        var userMenu = new MenuBar();
        userMenu.addThemeVariants(MenuBarVariant.LUMO_TERTIARY_INLINE);
        userMenu.addClassNames(Margin.MEDIUM);

        var userMenuItem = userMenu.addItem(avatar);
        userMenuItem.add(user.getFullName());
        if (user.getProfileUrl() != null) {
            userMenuItem.getSubMenu().addItem("View Profile",
                    event -> UI.getCurrent().getPage().open(user.getProfileUrl()));
        }
        // TODO Add additional items to the user menu if needed
        userMenuItem.getSubMenu().addItem("Logout", event -> authenticationContext.logout());

        return userMenu;
    }

}
