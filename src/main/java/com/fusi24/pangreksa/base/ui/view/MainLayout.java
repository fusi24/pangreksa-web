package com.fusi24.pangreksa.base.ui.view;

import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Responsibility;
import com.fusi24.pangreksa.web.model.entity.FwPages;
import com.fusi24.pangreksa.web.service.AppUserAuthService;
import com.fusi24.pangreksa.web.view.admin.ResponsibilitiesView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.combobox.ComboBox;
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
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.vaadin.flow.theme.lumo.LumoUtility.*;

@Layout
@PermitAll // When security is enabled, allow all authenticated users
public final class MainLayout extends AppLayout {
    private static final Logger log = LoggerFactory.getLogger(MainLayout.class);
    private final AppUserAuthService appUserAuthService;
    private final CurrentUser currentUser;
    private final AuthenticationContext authenticationContext;
    private ComboBox<Responsibility> responsibilityDropdown;

    VerticalLayout navLayout;
    List<Responsibility> responsibilityList;
    AppUserInfo user;

    MainLayout(CurrentUser currentUser, AuthenticationContext authenticationContext, AppUserAuthService appUserAuthService) {
        this.currentUser = currentUser;
        this.authenticationContext = authenticationContext;
        this.appUserAuthService = appUserAuthService;

        this.user = currentUser.require();
        responsibilityList = appUserAuthService.getAllResponsibilitiesFromUsername(user.getUserId().toString());

        navLayout = new VerticalLayout();
        navLayout.setSizeUndefined();
        navLayout.setMargin(false);

        setPrimarySection(Section.DRAWER);
        addToDrawer(createHeader(), responsibilitySwitcher(), new Scroller(navLayout), createUserMenu());
    }

    private Div createHeader() {
        // TODO Replace with real application logo and name
        var appLogo = VaadinIcon.CUBES.create();
        appLogo.addClassNames(TextColor.PRIMARY, IconSize.LARGE);

        var appName = new Span("Pangreksa");
        appName.addClassNames(FontWeight.SEMIBOLD, FontSize.LARGE);

        var header = new Div(appLogo, appName);
        header.addClassNames(Display.FLEX, Padding.MEDIUM, Gap.MEDIUM, AlignItems.CENTER, LumoUtility.Padding.Top.MEDIUM,
                LumoUtility.Padding.Right.MEDIUM, LumoUtility.Padding.Bottom.NONE, LumoUtility.Padding.Left.MEDIUM);
        return header;
    }

    private Div responsibilitySwitcher(){
        // Create a dropdown side responsibilities
        responsibilityDropdown = new ComboBox<>("Responsibility");
        responsibilityDropdown.setItems(responsibilityList);
        responsibilityDropdown.setItemLabelGenerator(Responsibility::getResponsibility);
        responsibilityDropdown.getStyle().setWidth("220px");
        responsibilityDropdown.setPlaceholder("Select responsibility..");

        Div div = new Div(responsibilityDropdown);
        div.addClassNames(Display.FLEX, Padding.MEDIUM, Gap.MEDIUM, AlignItems.CENTER, LumoUtility.Padding.Top.NONE, LumoUtility.Padding.Right.MEDIUM, LumoUtility.Padding.Bottom.MEDIUM, LumoUtility.Padding.Left.MEDIUM);

        responsibilityDropdown.addValueChangeListener( e -> {
            UI.getCurrent().getSession().setAttribute("responsibility", e.getValue().getResponsibility());
            this.navLayout.removeAll();
            navLayout.add(populateNavigation(e.getValue()));
            navLayout.addClassNames(LumoUtility.Padding.Top.NONE, LumoUtility.Padding.Right.MEDIUM, LumoUtility.Padding.Bottom.MEDIUM, LumoUtility.Padding.Left.MEDIUM);
        });

        return div;
    }

    private SideNav populateNavigation(Responsibility responsibility){
        var nav = new SideNav();

        responsibility.getMenuEntries().forEach( menuEntry -> {
            nav.addItem(createSideNavItem(menuEntry));
        });

        nav.setWidthFull();

        return nav;
    }

    private SideNavItem createSideNavItem(MenuEntry menuEntry) {
        SideNavItem item;
        if (menuEntry.icon() != null) {
            item = new SideNavItem(menuEntry.title(), menuEntry.path(), new Icon(menuEntry.icon()));
        } else {
            item = new SideNavItem(menuEntry.title(), menuEntry.path());
        }

        return item;
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
                    event -> {
                        UI.getCurrent().getPage().open(user.getProfileUrl());
                        log.info("User {} opened profile URL: {}", user.getUserId(), user.getProfileUrl());
                    });
        }
        // TODO Add additional items to the user menu if needed
        userMenuItem.getSubMenu().addItem("Logout", event -> authenticationContext.logout());

        return userMenu;
    }

}
