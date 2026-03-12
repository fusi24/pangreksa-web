// src/main/java/com/fusi24/pangreksa/web/component/attendance/CheckInOutDialog.java
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
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
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

    private final ComboBox<HrCompanyBranch> branchField = new ComboBox<>("Branch");
    private final TextArea notesField = new TextArea("Catatan");
    private final Button actionButton = new Button();

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
        setHeaderTitle("Absensi Hari Ini");

        configureContent();
    }

    private void handleCheckIn() {
        try {
    // validasi branch wajib
    HrCompanyBranch selected = branchField.getValue();
    if (selected == null) {
        AppNotification.error("Branch wajib dipilih sebelum Check-in");
        return;
    }

    attendance.setCheckIn(
        ZonedDateTime.now(ZoneId.of("Asia/Jakarta")).toLocalDateTime()
    );
    attendance.setNotes(notesField.getValue());

    // snapshot branch -> attendance
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

    private void configureContent() {

        String fullName = attendance.getPerson().getFirstName() + " " + attendance.getPerson().getLastName();
        add(new H3(fullName));
        add(new Span("Tanggal: " + attendance.getAttendanceDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))));

        boolean hasCheckIn = attendance.getCheckIn() != null;
        boolean hasCheckOut = attendance.getCheckOut() != null;

        // ===== Branch Field =====
        branchField.setWidthFull();
        branchField.setPlaceholder("Pilih branch");
        branchField.setClearButtonVisible(true);

        branchField.setItemLabelGenerator(b -> {
            if (b == null) return "";
            String code = b.getBranchCode() == null ? "" : b.getBranchCode().trim();
            String name = b.getBranchName() == null ? "" : b.getBranchName().trim();
            String label = (code + " - " + name).trim();
            return label.isBlank() ? "(No Name)" : label;
        });

        try {
            branchField.setItems(attendanceService.getBranchesForCurrentUserCompany());
        } catch (Exception ex) {
            AppNotification.error("Gagal load branch: " + ex.getMessage());
        }

        // ===== Branch Info =====
        Span branchInfo = null;
        if (StringUtils.isNotBlank(attendance.getBranchCode()) || StringUtils.isNotBlank(attendance.getBranchName())) {
            String info = "Branch: "
                    + StringUtils.defaultString(attendance.getBranchCode(), "-")
                    + " - "
                    + StringUtils.defaultString(attendance.getBranchName(), "-");
            branchInfo = new Span(info);
        }

        // ===== Close Button =====
        Button closeButton = new Button("Tutup");
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        closeButton.addClickListener(e -> close());

        // ===== Action Button =====
        if (!hasCheckIn) {

            actionButton.setText("Check-In");
            actionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            actionButton.addClickListener(e -> handleCheckIn());

            branchField.setVisible(true);
            if (branchInfo != null) {
                branchInfo.setVisible(false);
            }

        } else if (!hasCheckOut) {

            actionButton.setText("Check-Out");
            actionButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            actionButton.addClickListener(e -> handleCheckOut());

            branchField.setVisible(false);

        } else {

            actionButton.setText("Sudah Check-Out");
            actionButton.setEnabled(false);
            branchField.setVisible(false);
        }

        // ===== Notes =====
        notesField.setMaxLength(255);
        notesField.setValue(StringUtils.defaultIfBlank(attendance.getNotes(), StringUtils.EMPTY));
        notesField.setWidthFull();

        // ===== Layout =====
        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);

        if (branchInfo != null) content.add(branchInfo);

        content.add(branchField, actionButton, notesField, closeButton);

        add(content);
    }

    private void handleCheckOut() {
      try {
    attendance.setCheckOut(
        ZonedDateTime.now(ZoneId.of("Asia/Jakarta")).toLocalDateTime()
    );
    attendance.setNotes(notesField.getValue());

    // Branch tidak diubah saat Check-out
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