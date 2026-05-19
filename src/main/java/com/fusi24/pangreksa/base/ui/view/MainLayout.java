package com.fusi24.pangreksa.base.ui.view;

import com.fusi24.pangreksa.web.view.common.CheckInOutDialog;
import com.pangreksa.service.model.entity.HrNotification;
import com.pangreksa.service.service.NotificationService;
import com.pangreksa.service.shared.security.AppUserInfo;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Responsibility;
import com.pangreksa.service.model.entity.HrAttendance;
import com.fusi24.pangreksa.web.service.AppUserAuthService;
import com.pangreksa.service.service.AttendanceService;
import com.pangreksa.service.service.SystemService;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.ContextMenu;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Hr;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.shared.Tooltip;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.sidenav.SideNavItem;
import com.vaadin.flow.router.Layout;
import com.vaadin.flow.server.menu.MenuEntry;
import com.vaadin.flow.spring.security.AuthenticationContext;
import jakarta.annotation.security.PermitAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

import static com.fusi24.pangreksa.base.ui.TailwindUtility.*;

@Layout
@PermitAll // When security is enabled, allow all authenticated users
public final class MainLayout extends AppLayout {
    private static final Logger log = LoggerFactory.getLogger(MainLayout.class);
    private final AppUserAuthService appUserAuthService;
    private final AttendanceService attendanceService;
    private final CurrentUser currentUser;
    private final AuthenticationContext authenticationContext;
    private final SystemService systemService;
    private ComboBox<Responsibility> responsibilityDropdown;
    private Span notificationBadge;
    private TextField searchField;
    private Responsibility currentResponsibility;
    private final NotificationService notificationService;

    VerticalLayout navLayout;
    List<Responsibility> responsibilityList;
    AppUserInfo user;

    MainLayout(CurrentUser currentUser,
               AuthenticationContext authenticationContext,
               AppUserAuthService appUserAuthService,
               SystemService systemService,
               AttendanceService attendanceService,
               NotificationService notificationService) {
        this.currentUser = currentUser;
        this.authenticationContext = authenticationContext;
        this.appUserAuthService = appUserAuthService;
        this.systemService = systemService;
        this.attendanceService = attendanceService;
        this.notificationService = notificationService;

        this.user = currentUser.require();
        responsibilityList = appUserAuthService.getAllResponsibilitiesFromUsername(user.getUserId().toString());
        attendanceService.setUser(this.user);

        navLayout = new VerticalLayout();
        navLayout.setWidthFull();
        navLayout.setMargin(false);
        navLayout.setPadding(false);

        setPrimarySection(Section.DRAWER);
        setDrawerOpened(true);

        // Top bar: DrawerToggle on the left, notification bell on the right
        addToNavbar(createNavbar());

        // Drawer: brand header → search → nav scroller → user panel (with responsibility)
        addToDrawer(createHeader(), createSearchField(), new Scroller(navLayout), createUserPanel());
    }

    // ── Navbar ───────────────────────────────────────────────────────────────────

    private Component createNavbar() {
        var toggle = new DrawerToggle();

        // Notification bell button with badge
        int unread = notificationService.countUnread(user.getUserId().toString());
        var bellButton = new Button(VaadinIcon.BELL.create(), e -> openNotificationPanel());
        bellButton.addThemeVariants(ButtonVariant.TERTIARY);

        notificationBadge = new Span(String.valueOf(unread));
        notificationBadge.getStyle()
                .set("background", "red")
                .set("color", "white")
                .set("border-radius", "50%")
                .set("padding", "2px 6px")
                .set("font-size", "10px")
                .set("position", "absolute")
                .set("top", "0")
                .set("right", "0");
        notificationBadge.setVisible(unread > 0);

        var bellWrapper = new Div(bellButton, notificationBadge);
        bellWrapper.getStyle().set("position", "relative").set("display", "inline-block");

        var spacer = new Span();
        spacer.addClassName(Flex.GROW);

        var navbar = new Div(toggle, spacer, bellWrapper);
        navbar.addClassNames(Display.FLEX, AlignItems.CENTER, Width.FULL);
        return navbar;
    }

    // ── Drawer: brand header ──────────────────────────────────────────────────────

    private Component createHeader() {
        Image appLogo = new Image("images/pangreksa-logo-white.png", "Pangreksa Logo");
        appLogo.setHeight("35px");

        String name = systemService.getStringAppName();
        var appName = new Span(!name.isBlank() ? name : "Pangreksa");
        appName.addClassNames(FontWeight.SEMIBOLD);
        appName.getStyle().set("font-size", "0.95rem");

        var header = new Div(appLogo, appName);
        header.setHeight("50px");
        header.addClassNames(Display.FLEX, AlignItems.CENTER, Gap.MEDIUM,
                Padding.Left.MEDIUM, Padding.Right.MEDIUM);
        return header;
    }

    // ── Drawer: search field ─────────────────────────────────────────────────────

    private Component createSearchField() {
        searchField = new TextField();
        searchField.setPlaceholder("Search menu...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setClearButtonVisible(true);
        searchField.setWidthFull();
        searchField.addThemeVariants(TextFieldVariant.LUMO_SMALL);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);
        searchField.addValueChangeListener(e -> refreshNavigation());

        var wrapper = new Div(searchField);
        wrapper.addClassNames(Padding.MEDIUM);
        return wrapper;
    }

    private void refreshNavigation() {
        if (currentResponsibility == null) return;
        navLayout.removeAll();
        navLayout.add(populateNavigation(currentResponsibility));
    }

    // ── Drawer: user panel (footer) ───────────────────────────────────────────────

    private Component createUserPanel() {
        // Avatar + name + three-dot menu row
        var avatar = new Avatar(user.getFullName(), user.getPictureUrl());
        avatar.addThemeVariants(AvatarVariant.LUMO_XSMALL);

        var nameSpan = new Span(user.getFullName());
        nameSpan.addClassNames(FontWeight.MEDIUM);
        nameSpan.getStyle().set("font-size", "var(--lumo-font-size-s)");

        // Three-dot menu with logout
        var contextButton = new Button(VaadinIcon.ELLIPSIS_DOTS_V.create());
        contextButton.addThemeVariants(ButtonVariant.TERTIARY);
        var contextMenu = new ContextMenu();
        contextMenu.setOpenOnClick(true);
        contextMenu.setTarget(contextButton);
        contextMenu.addItem("Logout", e -> authenticationContext.logout());

        var gapSpan = new Span();
        gapSpan.addClassNames(Flex.GROW);
        var userInfo = new Div(avatar, nameSpan, gapSpan, contextButton);
        userInfo.addClassNames(Display.FLEX, AlignItems.CENTER, Gap.MEDIUM, Width.FULL);

        // Responsibility selector — no label, placeholder only, defaults to first role
        responsibilityDropdown = new ComboBox<>();
        responsibilityDropdown.setItems(responsibilityList);
        responsibilityDropdown.setItemLabelGenerator(Responsibility::getResponsibility);
        responsibilityDropdown.setWidthFull();
        responsibilityDropdown.setPlaceholder("Select responsibility...");

        // Restore from session, or default to first role
        String savedResp = (String) UI.getCurrent().getSession().getAttribute("responsibility");
        Responsibility initial = null;
        if (savedResp != null) {
            initial = responsibilityList.stream()
                    .filter(r -> r.getResponsibility().equals(savedResp))
                    .findFirst()
                    .orElse(null);
        }
        if (initial == null && !responsibilityList.isEmpty()) {
            initial = responsibilityList.getFirst();
        }
        if (initial != null) {
            responsibilityDropdown.setValue(initial);
            currentResponsibility = initial;
            UI.getCurrent().getSession().setAttribute("responsibility", initial.getResponsibility());
            navLayout.removeAll();
            navLayout.add(populateNavigation(initial));
        }

        responsibilityDropdown.addValueChangeListener(e -> {
            if (e.getValue() == null) return;
            currentResponsibility = e.getValue();
            if (searchField != null) searchField.clear();
            UI.getCurrent().getSession().setAttribute("responsibility", e.getValue().getResponsibility());
            log.debug("Responsibility changed to: {}", e.getValue().getResponsibility());
            navLayout.removeAll();
            navLayout.add(populateNavigation(e.getValue()));
        });

        // Assemble panel
        var panel = new Div();
        panel.addClassNames(Display.FLEX, FlexDirection.COLUMN, Border.TOP, Gap.MEDIUM, Padding.Vertical.MEDIUM, Margin.Horizontal.MEDIUM);
        panel.getStyle().set("border-color", "var(--aura-accent-border-color)");
        panel.add(userInfo, responsibilityDropdown);
        return new Div(panel);
    }

    // ── Navigation ───────────────────────────────────────────────────────────────

    private Component populateNavigation(Responsibility responsibility) {
        var container = new VerticalLayout();
        container.setSpacing(false);
        container.setPadding(false);
        container.setMargin(false);
        container.setWidthFull();

        String filter = (searchField != null && searchField.getValue() != null)
                ? searchField.getValue().trim().toLowerCase() : "";

        String role = responsibility.getResponsibility();

        // Dashboard — standalone item, filtered as a leaf
        if (role != null) {
            String dashLabel = "Dashboard";
            if (filter.isEmpty() || dashLabel.toLowerCase().contains(filter)) {
                var dashNav = new SideNav();
                dashNav.setWidthFull();
                if (role.equalsIgnoreCase("Administrator") || role.equalsIgnoreCase("HR Manager")) {
                    dashNav.addItem(new SideNavItem(dashLabel, "admin-dashboard", VaadinIcon.DASHBOARD.create()));
                } else if (role.equalsIgnoreCase("Karyawan")) {
                    dashNav.addItem(new SideNavItem(dashLabel, "dashboard", VaadinIcon.DASHBOARD.create()));
                }
                container.add(dashNav);
            }
        }

        if (responsibility.getGroupMenuEntries() != null && !responsibility.getGroupMenuEntries().isEmpty()) {
            responsibility.getGroupMenuEntries().forEach((groupName, items) -> {
                if (items == null || items.isEmpty()) {
                    return;
                }

                // Filter leaf items by name
                var filtered = items.stream()
                        .filter(me -> filter.isEmpty() || me.title().toLowerCase().contains(filter))
                        .sorted(Comparator.comparingDouble(MenuEntry::order))
                        .toList();

                // Hide group if no children match
                if (filtered.isEmpty()) {
                    return;
                }

                // Group with children — render as label + child items
                var groupNav = new SideNav();
                groupNav.setLabel(groupName);
                groupNav.setCollapsible(true);
                groupNav.setWidthFull();
                filtered.forEach(menuEntry -> groupNav.addItem(createSideNavItem(menuEntry)));
                container.add(groupNav);
            });
        } else if (responsibility.getMenuEntries() != null) {
            var nav = new SideNav();
            nav.setWidthFull();
            responsibility.getMenuEntries().stream()
                    .filter(me -> filter.isEmpty() || me.title().toLowerCase().contains(filter))
                    .sorted(Comparator.comparingDouble(MenuEntry::order))
                    .forEach(menuEntry -> nav.addItem(createSideNavItem(menuEntry)));
            container.add(nav);
        }

        if (responsibility.getResponsibility().equals("Karyawan")) {
            HrAttendance currentRecord = attendanceService.getOrCreateTodayAttendance(attendanceService.getCurrentUser());
            if (currentRecord != null
                    && (attendanceService.shouldShowCheckInPopup() || attendanceService.shouldShowCheckOutPopup())) {
                CheckInOutDialog dialog = new CheckInOutDialog(
                        attendanceService, currentRecord, currentUser, () -> {});
                dialog.open();
            }
        }

        return container;
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

    // ── Notification panel ────────────────────────────────────────────────────────

    private void openNotificationPanel() {
        var currentUserInfo = currentUser.require();

        var panel = new Div();
        panel.setWidth("320px");
        panel.getStyle()
                .set("position", "fixed")
                .set("top", "70px")
                .set("right", "20px")
                .set("background", "white")
                .set("box-shadow", "0 2px 10px rgba(0,0,0,0.2)")
                .set("padding", "10px")
                .set("z-index", "999");

        var close = VaadinIcon.CLOSE.create();
        close.addClickListener(e -> panel.removeFromParent());
        panel.add(close);

        List<HrNotification> notifications =
                notificationService.getNotifications(currentUserInfo.getUserId().toString());
        for (HrNotification n : notifications) {
            var item = new Div();
            var title = new Span(n.getTitle());
            if (!n.getIsRead()) {
                title.getStyle().set("font-weight", "bold").set("color", "red");
            }
            var time = new Span(formatTime(n.getCreatedAt()));
            time.getStyle().set("font-size", "10px");
            item.add(title, new Div(time));
            panel.add(item);
        }

        UI.getCurrent().add(panel);
    }

    private String formatTime(LocalDateTime time) {
        var duration = java.time.Duration.between(time, LocalDateTime.now());
        if (duration.toHours() < 24) {
            return duration.toHours() + " jam lalu";
        } else if (duration.toDays() < 30) {
            return duration.toDays() + " hari lalu";
        } else {
            return duration.toDays() / 30 + " bulan lalu";
        }
    }
}
