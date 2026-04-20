package com.fusi24.pangreksa.web.view.common;

import com.fusi24.pangreksa.base.ui.notification.AppNotification;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.entity.HrAttendance;
import com.fusi24.pangreksa.web.model.entity.HrCompanyBranch;
import com.fusi24.pangreksa.web.service.AttendanceService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public class CheckInOutDialog extends Dialog {

    private final AttendanceService attendanceService;
    private final HrAttendance attendance;
    private final CurrentUser currentUser;
    private final Runnable onActionComplete;

    private final ComboBox<HrCompanyBranch> branchField = new ComboBox<>("Lokasi Kantor / Branch");
    private final TextArea notesField = new TextArea("Catatan");
    private final Button actionButton = new Button();
    private final Button closeButton = new Button("Batal");

    public CheckInOutDialog(
            AttendanceService attendanceService,
            HrAttendance attendance,
            CurrentUser currentUser,
            Runnable onActionComplete) {

        this.attendanceService = attendanceService;
        this.attendance = attendance;
        this.currentUser = currentUser;
        this.onActionComplete = onActionComplete;

        setModal(true);
        setCloseOnEsc(false);
        setCloseOnOutsideClick(false);

        // Pengaturan Lebar Dialog agar lebih proporsional
        setWidth("450px");
        setHeaderTitle(attendance.getCheckIn() == null ? "Konfirmasi Check-In" : "Konfirmasi Check-Out");

        configureContent();
    }

    private void configureContent() {
        VerticalLayout mainLayout = new VerticalLayout();
        mainLayout.setPadding(true);
        mainLayout.setSpacing(true);
        mainLayout.setAlignItems(FlexComponent.Alignment.STRETCH);

        // 1. Seksi Informasi (Header Visual)
        VerticalLayout infoBox = new VerticalLayout();
        infoBox.getStyle().set("background-color", "var(--lumo-contrast-5pct)");
        infoBox.getStyle().set("border-radius", "var(--lumo-border-radius-m)");
        infoBox.setPadding(true);
        infoBox.setSpacing(false);

        String fullName = attendance.getPerson().getFirstName() + " " + attendance.getPerson().getLastName();
        Span nameSpan = new Span(fullName);
        nameSpan.getStyle().set("font-weight", "bold");
        nameSpan.getStyle().set("font-size", "var(--lumo-font-size-l)");

        String dateStr = attendance.getAttendanceDate().format(DateTimeFormatter.ofPattern("EEEE, dd MMM yyyy"));
        Span dateSpan = new Span(dateStr);
        dateSpan.getStyle().set("color", "var(--lumo-secondary-text-color)");
        dateSpan.getStyle().set("font-size", "var(--lumo-font-size-s)");

        infoBox.add(nameSpan, dateSpan);

        // Menampilkan Info Branch jika sudah Check-In
        if (StringUtils.isNotBlank(attendance.getBranchName())) {
            Span branchInfo = new Span("Lokasi: " + attendance.getBranchCode() + " - " + attendance.getBranchName());
            branchInfo.getStyle().set("margin-top", "8px");
            branchInfo.getStyle().set("font-size", "var(--lumo-font-size-s)");
            infoBox.add(branchInfo);
        }

        // 2. Konfigurasi Field Input
        branchField.setWidthFull();
        branchField.setPlaceholder("Pilih branch...");
        branchField.setItemLabelGenerator(b -> {
            if (b == null) return "";
            String code = b.getBranchCode() == null ? "" : b.getBranchCode().trim();
            String name = b.getBranchName() == null ? "" : b.getBranchName().trim();
            return (code + " - " + name).trim();
        });

        notesField.setWidthFull();
        notesField.setPlaceholder("Tambahkan catatan jika diperlukan...");
        notesField.setHeight("100px");
        notesField.setValue(StringUtils.defaultIfBlank(attendance.getNotes(), ""));

        // 3. Konfigurasi Tombol Aksi (Logic Tetap Sama)
        boolean hasCheckIn = attendance.getCheckIn() != null;
        boolean hasCheckOut = attendance.getCheckOut() != null;

        actionButton.setWidthFull();
        actionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        if (!hasCheckIn) {
            actionButton.setText("Check-In Sekarang");
            actionButton.setIcon(VaadinIcon.SIGN_IN.create());
            actionButton.addClickListener(e -> handleCheckIn());

            try {
                branchField.setItems(attendanceService.getBranchesForCurrentUserCompany());
            } catch (Exception ex) {
                AppNotification.error("Gagal load branch: " + ex.getMessage());
            }
        } else if (!hasCheckOut) {
            actionButton.setText("Check-Out Sekarang");
            actionButton.setIcon(VaadinIcon.SIGN_OUT.create());
            actionButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            actionButton.addClickListener(e -> handleCheckOut());
            branchField.setVisible(false);
        } else {
            actionButton.setText("Sudah Check-Out");
            actionButton.setEnabled(false);
            branchField.setVisible(false);
        }

        closeButton.setWidthFull();
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeButton.addClickListener(e -> close());

        mainLayout.add(infoBox, branchField, notesField, actionButton, closeButton);
        add(mainLayout);
    }

    private void handleCheckIn() {
        try {
            HrCompanyBranch selected = branchField.getValue();
            if (selected == null) {
                AppNotification.error("Branch wajib dipilih sebelum Check-in");
                return;
            }

            attendance.setCheckIn(ZonedDateTime.now(ZoneId.of("Asia/Jakarta")).toLocalDateTime());
            attendance.setNotes(notesField.getValue());

            // Menggunakan method yang benar sesuai entity HrAttendance Anda
            attendance.setBranchCode(selected.getBranchCode());
            attendance.setBranchName(selected.getBranchName());
            attendance.setBranchAddress(selected.getBranchAddress());

            attendanceService.saveAttendance(attendance, currentUser.require());
            AppNotification.success("Check-in berhasil");
            closeAndNotify();
        } catch (Exception ex) {
            AppNotification.error("Gagal Check-in: " + ex.getMessage());
        }
    }

    private void handleCheckOut() {
        try {
            attendance.setCheckOut(ZonedDateTime.now(ZoneId.of("Asia/Jakarta")).toLocalDateTime());
            attendance.setNotes(notesField.getValue());
            attendanceService.saveAttendance(attendance, currentUser.require());
            AppNotification.success("Check-out berhasil");
            closeAndNotify();
        } catch (Exception ex) {
            AppNotification.error("Gagal Check-out: " + ex.getMessage());
        }
    }

    private void closeAndNotify() {
        close();
        if (onActionComplete != null) {
            onActionComplete.run();
        }
    }
}