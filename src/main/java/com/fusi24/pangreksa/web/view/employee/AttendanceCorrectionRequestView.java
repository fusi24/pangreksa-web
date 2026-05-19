package com.fusi24.pangreksa.web.view.employee;

import com.fusi24.pangreksa.base.ui.ThemeUtility;
import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.base.ui.notification.AppNotification;
import com.fusi24.pangreksa.security.CurrentUser;
import com.pangreksa.service.model.entity.FwAppUser;
import com.pangreksa.service.model.entity.HrAttendance;
import com.pangreksa.service.model.entity.HrAttendanceCorrection;
import com.pangreksa.service.model.entity.HrPerson;
import com.pangreksa.service.model.enumerate.AttendanceCorrectionStatusEnum;
import com.pangreksa.service.service.AttendanceCorrectionService;
import com.pangreksa.service.service.AttendanceService;
import com.pangreksa.service.service.PersonService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route("attendance-correction-request")
@PageTitle("Pengajuan Koreksi Absensi")
@Menu(order = 26, icon = "vaadin:edit", title = "Pengajuan Koreksi Absensi")
@RolesAllowed("ATTEND_REQ")
public class AttendanceCorrectionRequestView extends Main {

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final CurrentUser currentUser;
    private final AttendanceService attendanceService;
    private final AttendanceCorrectionService correctionService;
    private final PersonService personService;

    private final Grid<HrAttendanceCorrection> grid = new Grid<>(HrAttendanceCorrection.class, false);

    public AttendanceCorrectionRequestView(CurrentUser currentUser,
                                           AttendanceService attendanceService,
                                           AttendanceCorrectionService correctionService,
                                           PersonService personService) {
        this.currentUser = currentUser;
        this.attendanceService = attendanceService;
        this.correctionService = correctionService;
        this.personService = personService;

        this.attendanceService.setUser(currentUser.require());

        addClassNames(
                ThemeUtility.BoxSizing.BORDER,
                ThemeUtility.Display.FLEX,
                ThemeUtility.FlexDirection.COLUMN,
                ThemeUtility.Padding.MEDIUM,
                ThemeUtility.Gap.SMALL
        );

        add(new ViewToolbar("Pengajuan Koreksi Absensi"));
        buildContent();
        refreshGrid();
    }

    private void buildContent() {
        Button createButton = new Button("Ajukan Koreksi", e -> openRequestDialog());
        createButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        grid.addColumn(HrAttendanceCorrection::getSubmittedAt).setHeader("Tanggal Pengajuan").setAutoWidth(true);
        grid.addColumn(c -> c.getRequestedAttendanceDate() != null ? c.getRequestedAttendanceDate() : "-")
                .setHeader("Tanggal Absensi").setAutoWidth(true);
        grid.addColumn(c -> formatDateTime(c.getOriginalCheckIn())).setHeader("Check-In Lama").setAutoWidth(true);
        grid.addColumn(c -> formatDateTime(c.getRequestedCheckIn())).setHeader("Check-In Baru").setAutoWidth(true);
        grid.addColumn(c -> formatDateTime(c.getOriginalCheckOut())).setHeader("Check-Out Lama").setAutoWidth(true);
        grid.addColumn(c -> formatDateTime(c.getRequestedCheckOut())).setHeader("Check-Out Baru").setAutoWidth(true);
        grid.addColumn(c -> c.getStatus() == null ? "-" : c.getStatus().name()).setHeader("Status").setAutoWidth(true);
        grid.addColumn(c -> c.getReason() == null ? "-" : c.getReason()).setHeader("Alasan").setFlexGrow(1);

        grid.setSizeFull();

        VerticalLayout layout = new VerticalLayout(createButton, grid);
        layout.setPadding(false);
        layout.setSpacing(true);
        layout.setSizeFull();
        layout.setFlexGrow(1, grid);
        add(layout);
    }

    private void refreshGrid() {

        FwAppUser user =
                attendanceService.getCurrentUser();

        if (user == null || user.getPerson() == null) {

            grid.setItems(List.of());

            return;
        }

        grid.setItems(
                correctionService.getSubmissionHistory(
                        user.getPerson(),
                        6
                )
        );

        System.out.println(
                "CURRENT PERSON ID = "
                        + user.getPerson().getId()
        );
    }

    private void openRequestDialog() {

        FwAppUser user =
                attendanceService.getCurrentUser();

        HrPerson defaultApprover = null;

        try {

            defaultApprover =
                    personService.getManager(
                            currentUser.require()
                    );

        } catch (Exception ignored) {

        }

        Dialog dialog = new Dialog();

        dialog.setHeaderTitle(
                "Form Pengajuan Koreksi Absensi"
        );

        dialog.setWidth("720px");

        // =====================================================
        // ATTENDANCE REFERENCE
        // =====================================================

        ComboBox<HrAttendance> attendanceRef =
                new ComboBox<>("Referensi Data Absensi");

        List<HrAttendance> histories =
                attendanceService.getAttendanceList(
                        LocalDate.now().minusMonths(2),
                        LocalDate.now(),
                        "",
                        null,
                        null,
                        user.getPerson()
                );

        attendanceRef.setItems(histories);

        attendanceRef.setItemLabelGenerator(att ->
                att.getAttendanceDate()
                        + " | IN: "
                        + formatDateTime(att.getCheckIn())
                        + " | OUT: "
                        + formatDateTime(att.getCheckOut())
        );

        attendanceRef.setWidthFull();

        // =====================================================
        // REQUESTED DATE
        // =====================================================

        DatePicker requestedDate =
                new DatePicker(
                        "Tanggal Absensi Yang Dibenarkan"
                );

        DateTimePicker requestedIn =
                new DateTimePicker(
                        "Check-In Baru"
                );

        DateTimePicker requestedOut =
                new DateTimePicker(
                        "Check-Out Baru"
                );

        // =====================================================
        // APPROVER
        // =====================================================

        ComboBox<HrPerson> submittedToCombo =
                new ComboBox<>("Penyetuju / Atasan");

        submittedToCombo.addClassName(
                "no-dropdown-icon"
        );

        submittedToCombo.setItemLabelGenerator(p ->
                p.getFirstName()
                        + " "
                        + (
                        p.getLastName() != null
                                ? p.getLastName()
                                : ""
                )
        );

        submittedToCombo.setPlaceholder(
                "Ketik untuk mencari penyetuju"
        );

        submittedToCombo.setClearButtonVisible(true);

        submittedToCombo.setWidthFull();

        submittedToCombo.setItems(
                personService.findAllPerson()
        );

        if (defaultApprover != null) {

            submittedToCombo.setValue(
                    defaultApprover
            );
        }

        // =====================================================
        // REASON
        // =====================================================

        TextArea reason =
                new TextArea(
                        "Alasan Koreksi"
                );

        reason.setWidthFull();

        // =====================================================
        // AUTO FILL
        // =====================================================

        attendanceRef.addValueChangeListener(event -> {

            HrAttendance selected =
                    event.getValue();

            if (selected == null) {
                return;
            }

            requestedDate.setValue(
                    selected.getAttendanceDate()
            );

            requestedIn.setValue(
                    selected.getCheckIn()
            );

            requestedOut.setValue(
                    selected.getCheckOut()
            );
        });

        // =====================================================
        // SUBMIT BUTTON
        // =====================================================

        Button submit =
                new Button("Submit");

        submit.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY
        );

        submit.addClickListener(e -> {

            if (submittedToCombo.getValue() == null) {

                AppNotification.error(
                        "Approver wajib dipilih."
                );

                return;
            }

            if (
                    requestedIn.getValue() == null
                            &&
                            requestedOut.getValue() == null
            ) {

                AppNotification.error(
                        "Minimal isi salah satu: check-in atau check-out."
                );

                return;
            }

            try {

                HrAttendanceCorrection request =
                        HrAttendanceCorrection.builder()
                                .attendance(
                                        attendanceRef.getValue()
                                )
                                .requestedAttendanceDate(
                                        requestedDate.getValue()
                                )
                                .requestedCheckIn(
                                        requestedIn.getValue()
                                )
                                .requestedCheckOut(
                                        requestedOut.getValue()
                                )
                                .submittedTo(
                                        submittedToCombo.getValue()
                                )
                                .reason(
                                        reason.getValue()
                                )
                                .status(
                                        AttendanceCorrectionStatusEnum.SUBMITTED
                                )
                                .build();

                correctionService.submitCorrection(
                        request,
                        user
                );

                AppNotification.success(
                        "Pengajuan koreksi absensi berhasil dikirim."
                );

                dialog.close();

                refreshGrid();

            } catch (Exception ex) {

                AppNotification.error(
                        "Gagal submit pengajuan: "
                                + ex.getMessage()
                );
            }
        });

        // =====================================================
        // CANCEL
        // =====================================================

        Button cancel =
                new Button(
                        "Batal",
                        e -> dialog.close()
                );

        HorizontalLayout actions =
                new HorizontalLayout(
                        cancel,
                        submit
                );

        // =====================================================
        // FORM
        // =====================================================

        FormLayout formLayout =
                new FormLayout();

        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("700px", 2)
        );

        formLayout.add(
                attendanceRef,
                submittedToCombo,
                requestedDate,
                reason,
                requestedIn,
                requestedOut
        );

        formLayout.setColspan(attendanceRef, 2);
        formLayout.setColspan(submittedToCombo, 2);
        formLayout.setColspan(reason, 2);

        dialog.add(
                formLayout,
                actions
        );

        dialog.open();
    }

    private String formatDateTime(java.time.LocalDateTime value) {
        return value == null ? "-" : value.format(DT_FORMAT);
    }
}
