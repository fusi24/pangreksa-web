package com.fusi24.pangreksa.web.view.dashboard;

import com.fusi24.pangreksa.base.ui.view.MainLayout;
import com.fusi24.pangreksa.web.repo.HrAttendanceRepository;
import com.fusi24.pangreksa.web.repo.HrLeaveApplicationRepository;
import com.fusi24.pangreksa.web.repo.HrPayrollRepository;
import com.fusi24.pangreksa.web.service.PersonService;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

import jakarta.annotation.security.PermitAll;

import java.time.LocalDate;

@Route(value = "admin-dashboard", layout = MainLayout.class)
@PageTitle("Admin Dashboard")
@PermitAll
public class AdminDashboardView extends VerticalLayout {

    private final PersonService personService;
    private final HrAttendanceRepository attendanceRepository;
    private final HrLeaveApplicationRepository leaveRepository;
    private final HrPayrollRepository payrollRepository;

    public AdminDashboardView(
            PersonService personService,
            HrAttendanceRepository attendanceRepository,
            HrLeaveApplicationRepository leaveRepository,
            HrPayrollRepository payrollRepository
    ) {

        this.personService = personService;
        this.attendanceRepository = attendanceRepository;
        this.leaveRepository = leaveRepository;
        this.payrollRepository = payrollRepository;

        setWidthFull();
        setPadding(true);

        H2 title = new H2("HR Dashboard");
        title.getStyle().set("margin-top", "0").set("color", "#111827");

        Div grid = new Div();
        grid.addClassName("dashboard-grid");

        // Baris Atas (3 Kolom)
        grid.add(
                createTotalEmployeeCard(),
                createAttendanceCard(),
                createLeaveRequestCard()
        );

        // Baris Bawah (2 Kolom)
        grid.add(
                createPayrollCard(),
                createActivityCard()
        );

        add(title, grid);
    }

    /* =========================================================
       1. TOTAL EMPLOYEE
    ========================================================= */
    private Div createTotalEmployeeCard() {
        int totalEmployee = personService.findAllPerson().size();

        Div card = new Div();
        card.addClassNames("dashboard-card", "col-span-2");

        Div title = new Div(VaadinIcon.USERS.create(), new Span("Total Employees"));
        title.addClassName("dashboard-title");

        Span number = new Span(String.valueOf(totalEmployee));
        number.getStyle().set("font-size", "42px").set("font-weight", "800");

        Span growth = new Span("Active Staff");
        growth.getStyle().set("color", "#64748b").set("font-size", "0.9rem");

        VerticalLayout leftSide = new VerticalLayout(number, growth);
        leftSide.setPadding(false);
        leftSide.setSpacing(false);

        // Background Icon agar estetis
        com.vaadin.flow.component.icon.Icon sparkline = VaadinIcon.CHART_LINE.create();
        sparkline.setSize("60px");
        sparkline.setColor("#1d71b8");
        sparkline.getStyle().set("opacity", "0.2");

        HorizontalLayout layout = new HorizontalLayout(leftSide, sparkline);
        layout.setWidthFull();
        layout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        layout.setAlignItems(Alignment.CENTER);

        card.add(title, layout);
        return card;
    }

    /* =========================================================
       2. ATTENDANCE TODAY
    ========================================================= */
    private Div createAttendanceCard() {
        LocalDate today = LocalDate.now();
        int totalEmployee = personService.findAllPerson().size();
        int present = attendanceRepository.findByAttendanceDate(today).size();
        int percent = totalEmployee == 0 ? 0 : (present * 100 / totalEmployee);

        Div card = new Div();
        card.addClassNames("dashboard-card", "col-span-2");

        Div title = new Div(VaadinIcon.CLOCK.create(), new Span("Attendance Today"));
        title.addClassName("dashboard-title");

        // Teks di dalam lingkaran
        Div progressInner = new Div();
        progressInner.setText(percent + "%");
        progressInner.addClassName("progress-ring-inner");

        // Lingkaran Progress (Dinamis mengisi sesuai persentase)
        Div progressRing = new Div(progressInner);
        progressRing.addClassName("progress-ring");
        progressRing.getStyle().set("background",
                "conic-gradient(#1f73b7 0% " + percent + "%, #e2e8f0 " + percent + "% 100%)");

        // Teks rasio di sebelah kanan lingkaran
        Span count = new Span(present + " / " + totalEmployee);
        count.getStyle().set("font-size", "1.2rem").set("font-weight", "700");
        Span presentText = new Span("employees present");
        presentText.getStyle().set("color", "#64748b").set("font-size", "0.9rem");

        VerticalLayout textLayout = new VerticalLayout(count, presentText);
        textLayout.setPadding(false);
        textLayout.setSpacing(false);

        HorizontalLayout layout = new HorizontalLayout(progressRing, textLayout);
        layout.setAlignItems(Alignment.CENTER);
        layout.setSpacing(true);

        card.add(title, layout);
        return card;
    }

    /* =========================================================
       3. LEAVE REQUEST
    ========================================================= */
    private Div createLeaveRequestCard() {
        int leaveCount = leaveRepository.findApprovedLeaves().size();

        Div card = new Div();
        card.addClassNames("dashboard-card", "col-span-2");

        Div title = new Div(VaadinIcon.CALENDAR.create(), new Span("Leave Requests"));
        title.addClassName("dashboard-title");

        Span number = new Span(String.valueOf(leaveCount));
        number.getStyle().set("font-size", "42px").set("font-weight", "800").set("color", "#111827");

        Span subtitle = new Span("Approved leaves");
        subtitle.getStyle().set("color", "#64748b").set("font-size", "0.9rem");

        card.add(title, number, subtitle);
        return card;
    }

    /* =========================================================
       4. PAYROLL SUMMARY
    ========================================================= */
    private Div createPayrollCard() {
        long payrollCount = payrollRepository.count();

        Div card = new Div();
        card.addClassNames("dashboard-card", "col-span-3");

        Div title = new Div(VaadinIcon.MONEY.create(), new Span("Payroll Records"));
        title.addClassName("dashboard-title");

        Span number = new Span(String.valueOf(payrollCount));
        number.getStyle().set("font-size", "42px").set("font-weight", "800").set("color", "#111827");

        Span subtitle = new Span("Total payroll generated");
        subtitle.getStyle().set("color", "#64748b").set("font-size", "0.9rem");

        card.add(title, number, subtitle);
        return card;
    }

    /* =========================================================
       5. RECENT ACTIVITY
    ========================================================= */
    private Div createActivityCard() {
        Div card = new Div();
        card.addClassNames("dashboard-card", "col-span-3");

        Div title = new Div(VaadinIcon.LIST.create(), new Span("Recent Leave Activity"));
        title.addClassName("dashboard-title");

        VerticalLayout list = new VerticalLayout();
        list.setPadding(false);
        list.setSpacing(true); // Jarak antar item aktivitas

        leaveRepository.findApprovedLeaves()
                .stream()
                .limit(5)
                .forEach(leave -> {
                    String name = leave.getEmployee().getFirstName() + " " + leave.getEmployee().getLastName();
                    list.add(createActivityItem(VaadinIcon.CHECK_CIRCLE, name, "approved leave"));
                });

        card.add(title, list);
        return card;
    }

    /* =========================================================
       ACTIVITY ITEM UI HELPER
    ========================================================= */
    private HorizontalLayout createActivityItem(VaadinIcon icon, String name, String action) {
        // Icon background
        Div iconBg = new Div(icon.create());
        iconBg.addClassName("activity-icon-bg");
        iconBg.getChildren().findFirst().ifPresent(i -> ((com.vaadin.flow.component.icon.Icon) i).setSize("16px"));

        // Buat warna hijau khusus untuk approval
        iconBg.getStyle().set("color", "#16a34a").set("background-color", "#dcfce7");

        // Nama dan Aksi
        Span nameSpan = new Span(name + " ");
        nameSpan.getStyle().set("font-weight", "700").set("color", "#111827");
        Span actionSpan = new Span(action);
        actionSpan.getStyle().set("color", "#475569");

        Div mainText = new Div(nameSpan, actionSpan);
        mainText.getStyle().set("font-size", "0.95rem");

        // Avatar di kanan
        Avatar avatar = new Avatar(name);
        avatar.getStyle().set("margin-left", "auto"); // Mendorong avatar ke ujung kanan

        HorizontalLayout row = new HorizontalLayout(iconBg, mainText, avatar);
        row.setWidthFull();
        row.setAlignItems(Alignment.CENTER);
        row.getStyle().set("margin-bottom", "12px");

        return row;
    }
}