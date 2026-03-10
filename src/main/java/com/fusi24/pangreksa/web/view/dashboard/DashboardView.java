package com.fusi24.pangreksa.web.view.dashboard;

import com.fusi24.pangreksa.base.ui.view.MainLayout;
import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.entity.HrAttendance;
import com.fusi24.pangreksa.web.model.entity.HrLeaveBalance;
import com.fusi24.pangreksa.web.model.entity.HrPerson;
import com.fusi24.pangreksa.web.service.AttendanceService;
import com.fusi24.pangreksa.web.service.LeaveService;
import com.fusi24.pangreksa.web.service.PersonService;

import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route(value = "dashboard", layout = MainLayout.class)
@PageTitle("Dashboard")
@PermitAll
public class DashboardView extends VerticalLayout {

    private final CurrentUser currentUser;
    private final AttendanceService attendanceService;
    private final LeaveService leaveService;
    private final PersonService personService;

    public DashboardView(
            CurrentUser currentUser,
            AttendanceService attendanceService,
            LeaveService leaveService,
            PersonService personService
    ) {

        this.currentUser = currentUser;
        this.attendanceService = attendanceService;
        this.leaveService = leaveService;
        this.personService = personService;

        setWidthFull();
        setPadding(true);

        AppUserInfo user = currentUser.require();
        attendanceService.setUser(user);

        H2 title = new H2("Dashboard Karyawan");
        title.getStyle().set("margin-top", "0").set("color", "#111827");

        Div grid = new Div();
        grid.addClassName("dashboard-grid");

        // Baris 1: Sisa Cuti, Kehadiran, Notifikasi (Masing-masing ambil 2 slot dari total 6)
        grid.add(
                createLeaveBalanceCard(user),
                createAttendanceCard(user),
                createNotificationCard()
        );

        // Baris 2: Direktori Karyawan, Ulang Tahun (Masing-masing ambil 3 slot dari total 6)
        grid.add(
                createCoworkerCard(),
                createBirthdayCard()
        );

        add(title, grid);
    }

    /* =====================================================
       1. LEAVE BALANCE
    ===================================================== */
    private Div createLeaveBalanceCard(AppUserInfo user){

        int year = LocalDate.now().getYear();
        List<HrLeaveBalance> balances = leaveService.getAllLeaveBalance(user, year);

        // Hitung total nilai cuti (Annual Leave) saja dari database
        int annualLeaveData = balances.stream()
                .filter(b -> b.getLeaveAbsenceType() != null)
                .filter(b -> "ANNUAL".equalsIgnoreCase(
                        b.getLeaveAbsenceType().getLeaveType()
                ))
                .mapToInt(HrLeaveBalance::getRemainingDays)
                .sum();

        // Menerapkan logika: 20 -> 10 hari, 19 -> 9.5 hari
        double baseLeaveDays = annualLeaveData / 2.0;

        // Ambil employee login dari attendance
        HrAttendance attendance = attendanceService.getOrCreateTodayAttendance(
                attendanceService.getCurrentUser()
        );

        double penalty = 0.0;

        // SAYA COMMENT SEMENTARA LOGIKA PENALTINYA AGAR TIDAK OTOMATIS MEMOTONG 0.5
    /*
    if(attendance != null && attendance.getPerson() != null){
        HrPerson me = attendance.getPerson();

        // Ini logika yang salah sebelumnya, karena selalu bernilai 1
        long missedCheckout = personService.findAllPerson()
                .stream()
                .filter(p -> p.getId().equals(me.getId()))
                .count();

        // penalti 0.5 hari
        penalty = missedCheckout * 0.5;
    }
    */

        // Kalkulasi sisa cuti akhir setelah dipotong penalti
        double remainingLeave = baseLeaveDays - penalty;

        if(remainingLeave < 0){
            remainingLeave = 0.0;
        }

        // FORMAT TAMPILAN:
        String displayLeaveText;
        if (remainingLeave % 1 == 0) {
            displayLeaveText = String.format("%.0f", remainingLeave); // Menghilangkan .0 jika bulat
        } else {
            displayLeaveText = String.valueOf(remainingLeave); // Tetap tampilkan .5 jika desimal
        }

        Div card = new Div();
        card.addClassNames("dashboard-card", "col-span-2");

        Div title = new Div(VaadinIcon.FLIGHT_TAKEOFF.create(), new Span("Sisa Cuti Saya"));
        title.addClassName("dashboard-title");

        // Lingkaran Cuti
        Div circleInner = new Div();
        circleInner.setText(displayLeaveText);
        circleInner.addClassName("leave-ring-inner");

        Div circleOuter = new Div(circleInner);
        circleOuter.addClassName("leave-ring");

        Span label = new Span("Sisa Hari");
        label.getStyle().set("color", "#64748b").set("font-weight", "500");

//        Button apply = new Button("Apply", VaadinIcon.PLUS.create());
//        apply.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
//
//        HorizontalLayout bottom = new HorizontalLayout(label, apply);
//        bottom.setWidthFull();
//        bottom.setJustifyContentMode(JustifyContentMode.BETWEEN);
//        bottom.setAlignItems(Alignment.CENTER);
//        bottom.getStyle().set("margin-top", "auto");

        card.add(title, circleOuter);

        return card;
    }

    /* =====================================================
       2. ATTENDANCE TODAY
    ===================================================== */
    private Div createAttendanceCard(AppUserInfo user){
        HrAttendance attendance = attendanceService.getOrCreateTodayAttendance(attendanceService.getCurrentUser());

        String checkIn = "--:--";
        String checkOut = "--:--";
        String statusText = "Pending";
        String statusColor = "#64748b";
        DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

        if(attendance != null){
            if(attendance.getCheckIn() != null){
                checkIn = attendance.getCheckIn().toLocalTime().format(timeFormatter);
                statusText = "Working";
                statusColor = "#0ea5e9"; // Biru
            }
            if(attendance.getCheckOut() != null){
                checkOut = attendance.getCheckOut().toLocalTime().format(timeFormatter);
                statusText = "Completed";
                statusColor = "#16a34a"; // Hijau
            }
        }

        Div card = new Div();
        card.addClassNames("dashboard-card", "col-span-2");

        Div title = new Div(VaadinIcon.CLOCK.create(), new Span("Kehadiran Hari Ini"));
        title.addClassName("dashboard-title");

        VerticalLayout list = new VerticalLayout(
                createAttendanceRow("Check In", checkIn, checkIn.equals("--:--") ? "#64748b" : "#16a34a"),
                createAttendanceRow("Check Out", checkOut, "#64748b"),
                createAttendanceRow("Status", statusText, statusColor)
        );
        list.setPadding(false);
        list.setSpacing(false);

        card.add(title, list);
        return card;
    }

    private Div createAttendanceRow(String label, String value, String color){
        Div row = new Div();
        row.addClassName("attendance-row");

        Span lbl = new Span(label);
        lbl.getStyle().set("color", "#475569").set("font-weight", "500");

        Span val = new Span(value);
        val.getStyle().set("font-weight", "700").set("color", color);

        row.add(lbl, val);
        return row;
    }

    /* =====================================================
       3. NOTIFICATION
    ===================================================== */
    private Div createNotificationCard(){
        Div card = new Div();
        card.addClassNames("dashboard-card", "col-span-2");

        Div title = new Div(VaadinIcon.ALARM.create(), new Span("Pengumuman"));
        title.addClassName("dashboard-title");

        Div note = new Div(new Span("Remember to complete your attendance today."));
        note.addClassName("notification-box");

        card.add(title, note);
        return card;
    }

    /* =====================================================
           COWORKERS (REKAN KERJA SATU STRUKTUR ORGANISASI)
        ===================================================== */
    private Div createCoworkerCard(){

        Div card = new Div();
        card.addClassNames("dashboard-card", "col-span-3");

        Div title = new Div(VaadinIcon.USERS.create(), new Span("Rekan Kerja"));
        title.addClassName("dashboard-title");

        VerticalLayout list = new VerticalLayout();
        list.setPadding(false);
        list.setSpacing(false);

        // ... (kode awal tetap sama)
        HrAttendance attendance = attendanceService.getOrCreateTodayAttendance(
                attendanceService.getCurrentUser()
        );

        if(attendance == null || attendance.getPerson() == null){
            card.add(title, new Span("Data karyawan tidak ditemukan"));
            return card;
        }

        // PERBAIKAN: Tarik ulang data 'me' secara utuh (lengkap dengan posisi & struktur org)
        HrPerson me = personService.getPersonWithDetails(attendance.getPerson().getId());

        // Validasi Struktur Organisasi
        if(me == null || me.getPersonPosition() == null ||
                me.getPersonPosition().getPosition() == null ||
                me.getPersonPosition().getPosition().getOrgStructure() == null){
            card.add(title, new Span("Struktur organisasi tidak ditemukan"));
            return card;
        }

        Long myOrgStructureId = me.getPersonPosition().getPosition().getOrgStructure().getId();
        String myOrgName = me.getPersonPosition().getPosition().getOrgStructure().getName();

        // Panggil method service yang sudah kita buat sebelumnya
        List<HrPerson> coworkers = personService.getCoworkers(myOrgStructureId, me.getId());

        // ... (loop forEach rekan kerja tetap sama)

        // 4. Render ke UI
        for (HrPerson p : coworkers) {
            String name = p.getFirstName() + " " + p.getLastName();
            String jabatan = "Tim " + (myOrgName != null ? myOrgName : "Kerja");

            if (p.getPersonPosition() != null &&
                    p.getPersonPosition().getPosition() != null &&
                    p.getPersonPosition().getPosition().getName() != null) {
                jabatan = p.getPersonPosition().getPosition().getName();
            }

            list.add(createPersonItem(name, jabatan));
        }

        if (list.getComponentCount() == 0) {
            Span emptyMsg = new Span("Belum ada rekan kerja di struktur organisasi ini.");
            emptyMsg.getStyle().set("color", "#64748b").set("font-size", "0.9rem").set("font-style", "italic");
            list.add(emptyMsg);
        }

        card.add(title, list);
        return card;
    }

    /* =====================================================
       UI HELPER: COWORKER ITEM
    ===================================================== */
    private HorizontalLayout createPersonItem(String name, String role){
        Avatar avatar = new Avatar(name);

        Span nameSpan = new Span(name);
        nameSpan.getStyle().set("font-weight", "700").set("color", "#1e293b");

        Span roleSpan = new Span(role);
        roleSpan.getStyle().set("font-size", "0.85rem").set("color", "#64748b");

        VerticalLayout info = new VerticalLayout(nameSpan, roleSpan);
        info.setSpacing(false);
        info.setPadding(false);

        HorizontalLayout row = new HorizontalLayout(avatar, info);
        row.setWidthFull();
        row.setAlignItems(Alignment.CENTER);
        row.getStyle().set("padding", "10px 0").set("border-bottom", "1px solid #f1f5f9");

        return row;
    }

    private HorizontalLayout createPersonItem(String name){
        Avatar avatar = new Avatar(name);

        Span text = new Span(name);
        text.getStyle().set("font-weight", "700").set("color", "#1e293b");

        HorizontalLayout row = new HorizontalLayout(avatar, text);
        row.setWidthFull();
        row.setAlignItems(Alignment.CENTER);
        row.getStyle().set("padding", "8px 0").set("border-bottom", "1px solid #f1f5f9");

        return row;
    }

    /* =====================================================
       5. BIRTHDAY
    ===================================================== */
    private Div createBirthdayCard(){
        Div card = new Div();
        card.addClassNames("dashboard-card", "col-span-3");

        Div title = new Div(VaadinIcon.GIFT.create(), new Span("Ulang Tahun Bulan Ini"));
        title.addClassName("dashboard-title");

        VerticalLayout list = new VerticalLayout();
        list.setPadding(false);
        list.setSpacing(false);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM");

        int currentMonth = LocalDate.now().getMonthValue();

        personService.findAllPerson()
                .stream()
                .filter(p -> p.getDob() != null)
                .filter(p -> p.getDob().getMonthValue() == currentMonth)
                .limit(5)
                .forEach(p -> {
                    String name = p.getFirstName() + " " + p.getLastName();
                    String dob = p.getDob().format(formatter);
                    list.add(createBirthdayItem(name, dob));
                });

        card.add(title, list);
        return card;
    }

    private HorizontalLayout createBirthdayItem(String name, String dob){
        Avatar avatar = new Avatar(name);

        Span nameSpan = new Span(name);
        nameSpan.getStyle().set("font-weight", "600").set("color", "#1e293b");

        Span dateSpan = new Span(dob);
        dateSpan.getStyle().set("color", "#ec4899").set("font-weight", "700").set("font-size", "0.9rem");

        HorizontalLayout row = new HorizontalLayout(avatar, nameSpan, dateSpan);
        row.setWidthFull();
        row.setAlignItems(Alignment.CENTER);
        row.setFlexGrow(1, nameSpan); // Memastikan nama mengisi ruang tengah, tanggal terdorong ke kanan
        row.getStyle().set("padding", "10px 0").set("border-bottom", "1px solid #f1f5f9");

        return row;
    }
}