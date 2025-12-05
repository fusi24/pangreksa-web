// src/main/java/com/fusi24/pangreksa/web/component/attendance/CheckInOutDialog.java
package com.fusi24.pangreksa.web.view.common;

import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.entity.HrAttendance;
import com.fusi24.pangreksa.web.service.AttendanceService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import org.apache.commons.lang3.StringUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class CheckInOutDialog extends Dialog {

    private final AttendanceService attendanceService;
    private final HrAttendance attendance;
    private final CurrentUser currentUser;
    private final Runnable onActionComplete;

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

    private void configureContent() {
        String fullName = attendance.getPerson().getFirstName() + " " + attendance.getPerson().getLastName();
        add(new H3(fullName));
        add(new Span("Tanggal: " + attendance.getAttendanceDate().format(DateTimeFormatter.ofPattern("dd MMM yyyy"))));

        boolean hasCheckIn = attendance.getCheckIn() != null;
        boolean hasCheckOut = attendance.getCheckOut() != null;

        if (!hasCheckIn) {
            actionButton.setText("Clock-In");
            actionButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            actionButton.addClickListener(e -> handleCheckIn());
        } else if (!hasCheckOut) {
            actionButton.setText("Clock-Out");
            actionButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
            actionButton.addClickListener(e -> handleCheckOut());
        } else {
            actionButton.setText("Sudah Clock-Out");
            actionButton.setEnabled(false);
        }

        notesField.setMaxLength(255);
        notesField.setValue(StringUtils.defaultIfBlank(attendance.getNotes(), StringUtils.EMPTY));
        notesField.setWidthFull();

        VerticalLayout content = new VerticalLayout(actionButton, notesField);
        content.setSpacing(true);
        content.setPadding(true);
        add(content);
    }

    private void handleCheckIn() {
        try {
            attendance.setCheckIn(LocalDateTime.now());
            attendance.setNotes(notesField.getValue());
            attendanceService.saveAttendance(attendance, currentUser.require());
            Notification.show("Clock-In berhasil", 3000, Notification.Position.MIDDLE);
            closeAndNotify();
        } catch (Exception ex) {
            Notification.show("Gagal Clock-In: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void handleCheckOut() {
        try {
            attendance.setCheckOut(LocalDateTime.now());
            attendance.setNotes(notesField.getValue());
            attendanceService.saveAttendance(attendance, currentUser.require());
            Notification.show("Clock-Out berhasil", 3000, Notification.Position.MIDDLE);
            closeAndNotify();
        } catch (Exception ex) {
            Notification.show("Gagal Clock-Out: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void closeAndNotify() {
        close();
        if (onActionComplete != null) {
            onActionComplete.run();
        }
    }
}