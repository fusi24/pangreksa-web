package com.fusi24.pangreksa.base.ui.view;

import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.view.admin.CampaignListView;
import com.fusi24.pangreksa.web.view.admin.MasterCompanyView;
import com.fusi24.pangreksa.web.view.admin.SettingsView;
import com.fusi24.pangreksa.web.view.admin.UserManagementView;
import com.fusi24.pangreksa.web.view.dashboard.AdminDashboardView;
import com.fusi24.pangreksa.web.view.dashboard.DashboardView;
import com.fusi24.pangreksa.web.view.employee.*;
import com.fusi24.pangreksa.web.view.manager.CompanyCalendarView;
import com.pangreksa.service.shared.security.AppUserInfo;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouteConfiguration;
import com.vaadin.flow.server.auth.AccessAnnotationChecker;
import jakarta.annotation.security.PermitAll;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Home page shown at the root ('/') after login and responsibility selection.
 */
@Route
@PermitAll
@StyleSheet("main-view.css")
public final class MainView extends VerticalLayout {

    // ── Card definitions ────────────────────────────────────────────────────────

    private record QuickCardDef(
            Class<? extends Component> viewClass,
            VaadinIcon icon, String iconColor, String iconBg,
            String title, String description
    ) {}

    private static final List<QuickCardDef> ALL_CARDS = List.of(
            // Admin cards
            new QuickCardDef(AdminDashboardView.class,
                    VaadinIcon.DASHBOARD, "#1e40af", "#dbeafe",
                    "Dashboard Admin", "Ringkasan data karyawan, kehadiran, dan cuti."),
            new QuickCardDef(UserManagementView.class,
                    VaadinIcon.USERS, "#7c3aed", "#ede9fe",
                    "Manajemen User", "Kelola akun pengguna dan hak akses."),
            new QuickCardDef(MasterCompanyView.class,
                    VaadinIcon.OFFICE, "#0d9488", "#ccfbf1",
                    "Master Perusahaan", "Data perusahaan, departemen, dan lokasi."),
            new QuickCardDef(CompanyCalendarView.class,
                    VaadinIcon.CALENDAR, "#ea580c", "#fff7ed",
                    "Kalender Perusahaan", "Atur hari libur dan jadwal kerja."),
            new QuickCardDef(ProfilDataKaryawanView.class,
                    VaadinIcon.CLIPBOARD_TEXT, "#be185d", "#fce7f3",
                    "Data Karyawan", "Lihat dan kelola data seluruh karyawan."),
            new QuickCardDef(SettingsView.class,
                    VaadinIcon.COG, "#475569", "#f1f5f9",
                    "Pengaturan", "Konfigurasi sistem aplikasi."),

            // Employee cards
            new QuickCardDef(DashboardView.class,
                    VaadinIcon.DASHBOARD, "#1e40af", "#dbeafe",
                    "Dashboard", "Lihat ringkasan kehadiran dan sisa cuti."),
            new QuickCardDef(AttendanceView.class,
                    VaadinIcon.CLOCK, "#0d9488", "#ccfbf1",
                    "Kehadiran", "Check-in, check-out, dan riwayat kehadiran."),
            new QuickCardDef(LeaveRequestView.class,
                    VaadinIcon.FLIGHT_TAKEOFF, "#7c3aed", "#ede9fe",
                    "Pengajuan Cuti", "Ajukan dan pantau status cuti Anda."),
            new QuickCardDef(MyProfileView.class,
                    VaadinIcon.USER, "#ea580c", "#fff7ed",
                    "Profil Saya", "Lihat dan perbarui data pribadi."),
            new QuickCardDef(LeaveApprovalView.class,
                    VaadinIcon.CHECK_SQUARE, "#be185d", "#fce7f3",
                    "Persetujuan Cuti", "Tinjau pengajuan cuti tim Anda."),
            new QuickCardDef(CampaignListView.class,
                    VaadinIcon.NEWSPAPER, "#475569", "#f1f5f9",
                    "Pengumuman", "Baca pengumuman dan berita perusahaan.")
    );

    // ── Constructor ─────────────────────────────────────────────────────────────

    MainView(CurrentUser currentUser, AccessAnnotationChecker accessChecker) {
        setSizeFull();
        setPadding(false);
        setSpacing(false);

        AppUserInfo user = currentUser.require();

        var content = new VerticalLayout();
        content.setWidthFull();
        content.setPadding(true);
        content.setSpacing(false);

        content.add(createHeroBanner(user));

        var sectionTitle = new H2("Akses Cepat");
        sectionTitle.addClassName("main-section-title");
        content.add(sectionTitle);

        content.add(createQuickAccessGrid(accessChecker));

        var scroller = new Scroller(content);
        scroller.setSizeFull();
        add(scroller);
    }

    /**
     * Navigates to the main view.
     */
    public static void showMainView() {
        UI.getCurrent().navigate(MainView.class);
    }

    // ── Hero banner ─────────────────────────────────────────────────────────────

    private Component createHeroBanner(AppUserInfo user) {
        // Greeting text
        String firstName = user.getFullName() != null
                ? user.getFullName().split(" ")[0]
                : "User";

        var greeting = new H2("Selamat Datang, " + firstName + "!");
        greeting.addClassName("main-greeting");

        String today = LocalDate.now().format(
                DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", new Locale("id", "ID")));
        var date = new Paragraph(today);
        date.addClassName("main-date");

        var subtitle = new Paragraph(
                "Kelola kehadiran, pengajuan cuti, dan informasi karyawan Anda dari satu tempat.");
        subtitle.addClassName("main-subtitle");

        var textBlock = new Div(greeting, date, subtitle);
        textBlock.addClassName("main-hero-text");

        // Hero image
        var heroImage = new Image("images/main-hero.jpg", "Welcome");
        heroImage.addClassName("main-hero-image");

        var hero = new Div(textBlock, heroImage);
        hero.addClassName("main-hero");

        return hero;
    }

    // ── Quick-access grid ───────────────────────────────────────────────────────

    private Component createQuickAccessGrid(AccessAnnotationChecker accessChecker) {
        var grid = new Div();
        grid.addClassName("dashboard-grid");

        ALL_CARDS.stream()
                .filter(card -> accessChecker.hasAccess(card.viewClass()))
                .map(this::createQuickCard)
                .forEach(grid::add);

        return grid;
    }

    private Component createQuickCard(QuickCardDef def) {
        // Icon circle
        Icon ic = def.icon().create();
        ic.setColor(def.iconColor());

        var iconContainer = new Div(ic);
        iconContainer.addClassName("quick-card-icon");
        iconContainer.getStyle()
                .set("background-color", def.iconBg())
                .set("color", def.iconColor());

        // Text
        var cardTitle = new H3(def.title());
        cardTitle.addClassName("quick-card-title");

        var cardDesc = new Paragraph(def.description());
        cardDesc.addClassName("quick-card-desc");

        // Card
        var card = new Div(iconContainer, cardTitle, cardDesc);
        card.addClassNames("quick-card", "col-span-2");

        String route = RouteConfiguration.forSessionScope().getUrl(def.viewClass());
        card.addClickListener(e -> UI.getCurrent().navigate(route));

        return card;
    }
}
