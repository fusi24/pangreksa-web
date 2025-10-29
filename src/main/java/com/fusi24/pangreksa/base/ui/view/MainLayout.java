package com.fusi24.pangreksa.base.ui.view;

import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Responsibility;
import com.fusi24.pangreksa.web.service.AppUserAuthService;
import com.fusi24.pangreksa.web.service.SystemService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.spring.security.AuthenticationContext;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.vaadin.flow.theme.lumo.LumoUtility.*;

@Layout
@PermitAll // When security is enabled, allow all authenticated users
public final class MainLayout extends AppLayout {
    private static final Logger log = LoggerFactory.getLogger(MainLayout.class);
    private final AppUserAuthService appUserAuthService;
    private final CurrentUser currentUser;
    private final AuthenticationContext authenticationContext;
    private final SystemService systemService;
    private ComboBox<Responsibility> responsibilityDropdown;

    VerticalLayout navLayout;
    List<Responsibility> responsibilityList;
    AppUserInfo user;

    MainLayout(CurrentUser currentUser, AuthenticationContext authenticationContext, AppUserAuthService appUserAuthService, SystemService systemService) {
        this.currentUser = currentUser;
        this.authenticationContext = authenticationContext;
        this.appUserAuthService = appUserAuthService;
        this.systemService = systemService;

        this.user = currentUser.require();
        responsibilityList = appUserAuthService.getAllResponsibilitiesFromUsername(user.getUserId().toString());

        navLayout = new VerticalLayout();
        navLayout.setSizeUndefined();
        navLayout.setMargin(false);
        navLayout.addClassNames(Padding.Top.NONE, Padding.Right.MEDIUM, Padding.Bottom.MEDIUM, Padding.Left.MEDIUM);

        setPrimarySection(Section.DRAWER);
        addToDrawer(createHeader(), responsibilitySwitcher(), new Scroller(navLayout), createUserMenu());
    }

    private Div createHeader() {
        // Try to fetch logo URL from system
        String logoUrl = systemService.getStringAppLogo();
        Component appLogo;
        if (logoUrl != null && (
                logoUrl.toLowerCase().endsWith(".png") ||
                logoUrl.toLowerCase().endsWith(".jpg") ||
                logoUrl.toLowerCase().endsWith(".jpeg"))) {
            Image img = new Image(logoUrl, "App Logo");
            img.setHeight("40px");
            img.setWidth("40px");
            appLogo = img;
        } else {
            appLogo = VaadinIcon.CUBES.create();
            appLogo.addClassNames(TextColor.PRIMARY, IconSize.LARGE);
        }

        String name = systemService.getStringAppName();
        var appName = new Span( !name.equals("") ? name : "Pangreksa");
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
        responsibilityDropdown.getStyle().setWidth("250px");
        responsibilityDropdown.setPlaceholder("Select responsibility..");

        String resp = (String) UI.getCurrent().getSession().getAttribute("responsibility");
        if (resp != null) {
            Responsibility rData = Objects.requireNonNull(responsibilityList.stream()
                    .filter(r -> r.getResponsibility().equals(resp))
                    .findFirst()
                    .orElse(null));
            responsibilityDropdown.setValue(rData);

            this.navLayout.removeAll();
            navLayout.add(populateNavigation(rData));
        }

        Div div = new Div(responsibilityDropdown);
        div.addClassNames(Display.FLEX, Padding.MEDIUM, Gap.MEDIUM, AlignItems.CENTER, Padding.Top.NONE, Padding.Right.MEDIUM, Padding.Bottom.MEDIUM, Padding.Left.MEDIUM);

        responsibilityDropdown.addValueChangeListener( e -> {
            UI.getCurrent().getSession().setAttribute("responsibility", e.getValue().getResponsibility());
            log.debug("Responsibility changed to: {}", e.getValue().getResponsibility());
            this.navLayout.removeAll();
            navLayout.add(populateNavigation(e.getValue()));
        });

        return div;
    }

    private SideNav populateNavigation(Responsibility responsibility){
        var nav = new SideNav();

        // If groupMenuEntries is null or empty, fall back to flat menuEntries (optional)
        if (responsibility.getGroupMenuEntries() != null && !responsibility.getGroupMenuEntries().isEmpty()) {
            // Sort group names (optional: you could use a LinkedHashMap to preserve order)
            responsibility.getGroupMenuEntries().entrySet().stream()
                    .forEach(entry -> {
                        String groupName = entry.getKey();
                        List<MenuEntry> items = entry.getValue();

                        // Create a top-level group item (non-clickable header)
                        SideNavItem groupItem = new SideNavItem(groupName);

                        // Optionally add an icon (you'd need to map group name to icon separately if desired)
                        // groupItem.setPrefixComponent(new Icon("..."));

                        // Sort child items by order and add as nested items
                        items.stream()
                                .sorted(Comparator.comparingDouble(MenuEntry::order))
                                .forEach(menuEntry -> {
                                    groupItem.addItem(createSideNavItem(menuEntry));
                                });

                        groupItem.setExpanded(true);
                        Tooltip.forComponent(groupItem).setText(groupName);
                        nav.addItem(groupItem);
                    });
        } else if (responsibility.getMenuEntries() != null) {
            // Fallback: flat list (original behavior)
            responsibility.getMenuEntries().stream()
                    .sorted(Comparator.comparingDouble(MenuEntry::order))
                    .forEach(menuEntry -> {
                        nav.addItem(createSideNavItem(menuEntry));
                    });
        }

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

        Tooltip.forComponent(item).setText(menuEntry.title());

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
