package com.fusi24.pangreksa.web.view.employee;

import com.fusi24.pangreksa.base.ui.ThemeUtility;
import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.base.ui.notification.AppNotification;
import com.fusi24.pangreksa.security.CurrentUser;
import com.pangreksa.service.model.entity.FwAppUser;
import com.pangreksa.service.model.entity.HrAttendanceCorrection;
import com.pangreksa.service.model.entity.HrPerson;
import com.pangreksa.service.model.enumerate.AttendanceCorrectionStatusEnum;
import com.pangreksa.service.service.AttendanceCorrectionService;
import com.pangreksa.service.service.AttendanceService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
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

import java.time.format.DateTimeFormatter;
import java.util.List;

@Route("attendance-correction-approval")
@PageTitle("Approval Koreksi Absensi")
@Menu(order = 27, icon = "vaadin:clipboard-check", title = "Approval Koreksi Absensi")
@RolesAllowed("ATTEND_APPR")
public class AttendanceCorrectionApprovalView extends Main {

    private static final DateTimeFormatter DT_FORMAT = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm");

    private final AttendanceService attendanceService;
    private final AttendanceCorrectionService correctionService;
    private final Grid<HrAttendanceCorrection> grid = new Grid<>(HrAttendanceCorrection.class, false);

    public AttendanceCorrectionApprovalView(CurrentUser currentUser,
                                            AttendanceService attendanceService,
                                            AttendanceCorrectionService correctionService) {
        this.attendanceService = attendanceService;
        this.correctionService = correctionService;

        this.attendanceService.setUser(currentUser.require());

        addClassNames(
                ThemeUtility.BoxSizing.BORDER,
                ThemeUtility.Display.FLEX,
                ThemeUtility.FlexDirection.COLUMN,
                ThemeUtility.Padding.MEDIUM,
                ThemeUtility.Gap.SMALL
        );

        add(new ViewToolbar("Approval Koreksi Absensi"));
        buildContent();
        refreshGrid();
    }

    private void buildContent() {
        grid.addColumn(HrAttendanceCorrection::getSubmittedAt).setHeader("Tanggal Pengajuan").setAutoWidth(true);
        grid.addColumn(c -> c.getEmployee().getFirstName() + " " + c.getEmployee().getLastName())
                .setHeader("Karyawan").setAutoWidth(true);
        grid.addColumn(HrAttendanceCorrection::getRequestedAttendanceDate).setHeader("Tanggal Absensi").setAutoWidth(true);
        grid.addColumn(c -> formatDateTime(c.getOriginalCheckIn())).setHeader("Check-In Lama").setAutoWidth(true);
        grid.addColumn(c -> formatDateTime(c.getRequestedCheckIn())).setHeader("Check-In Baru").setAutoWidth(true);
        grid.addColumn(c -> formatDateTime(c.getOriginalCheckOut())).setHeader("Check-Out Lama").setAutoWidth(true);
        grid.addColumn(c -> formatDateTime(c.getRequestedCheckOut())).setHeader("Check-Out Baru").setAutoWidth(true);
        grid.addColumn(c -> c.getReason() == null ? "-" : c.getReason()).setHeader("Alasan").setFlexGrow(1);
        grid.addComponentColumn(this::buildActionColumn).setHeader("Aksi").setAutoWidth(true);
        grid.setSizeFull();

        VerticalLayout layout = new VerticalLayout(grid);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setSizeFull();
        layout.setFlexGrow(1, grid);
        add(layout);
    }

    private HorizontalLayout buildActionColumn(HrAttendanceCorrection correction) {
        Button process = new Button("Proses");
        process.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL);
        process.addClickListener(e -> openApprovalDialog(correction));
        return new HorizontalLayout(process);
    }

    private void openApprovalDialog(HrAttendanceCorrection correction) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Approval Koreksi Absensi");
        dialog.setWidth("700px");

        TextArea oldIn = ro("Check-In Lama", formatDateTime(correction.getOriginalCheckIn()));
        TextArea newIn = ro("Check-In Baru", formatDateTime(correction.getRequestedCheckIn()));
        TextArea oldOut = ro("Check-Out Lama", formatDateTime(correction.getOriginalCheckOut()));
        TextArea newOut = ro("Check-Out Baru", formatDateTime(correction.getRequestedCheckOut()));
        TextArea reason = ro("Alasan Karyawan", correction.getReason());
        TextArea rejectionReason = new TextArea("Alasan Penolakan (opsional jika reject)");
        rejectionReason.setWidthFull();

        FormLayout formLayout = new FormLayout(oldIn, newIn, oldOut, newOut, reason, rejectionReason);
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("700px", 2));
        formLayout.setColspan(reason, 2);
        formLayout.setColspan(rejectionReason, 2);

        Button approve = new Button("Approve");
        approve.addThemeVariants(ButtonVariant.LUMO_SUCCESS);
        approve.addClickListener(e -> processApprove(correction, dialog));

        Button reject = new Button("Reject");
        reject.addThemeVariants(ButtonVariant.LUMO_ERROR);
        reject.addClickListener(e -> processReject(correction, rejectionReason.getValue(), dialog));

        Button close = new Button("Tutup", e -> dialog.close());

        dialog.add(formLayout, new HorizontalLayout(close, reject, approve));
        dialog.open();
    }

    private void processApprove(HrAttendanceCorrection correction, Dialog dialog) {
        try {
            FwAppUser approver = attendanceService.getCurrentUser();
            correctionService.approveCorrection(correction, approver);
            AppNotification.success("Koreksi absensi disetujui.");
            dialog.close();
            refreshGrid();
        } catch (Exception ex) {
            AppNotification.error("Gagal approve: " + ex.getMessage());
        }
    }

    private void processReject(HrAttendanceCorrection correction, String rejectReason, Dialog dialog) {
        try {
            FwAppUser approver = attendanceService.getCurrentUser();
            correctionService.rejectCorrection(correction, approver, rejectReason);
            AppNotification.success("Koreksi absensi ditolak.");
            dialog.close();
            refreshGrid();
        } catch (Exception ex) {
            AppNotification.error("Gagal reject: " + ex.getMessage());
        }
    }

    private void refreshGrid() {
        HrPerson approver = attendanceService.getCurrentUser().getPerson();
        List<HrAttendanceCorrection> pending = correctionService.getPendingApprovals(approver, 6);
        grid.setItems(pending.stream()
                .filter(c -> c.getStatus() == AttendanceCorrectionStatusEnum.SUBMITTED)
                .toList());
    }

    private TextArea ro(String label, String value) {
        TextArea area = new TextArea(label);
        area.setReadOnly(true);
        area.setWidthFull();
        area.setValue(value == null ? "-" : value);
        return area;
    }

    private String formatDateTime(java.time.LocalDateTime value) {
        return value == null ? "-" : value.format(DT_FORMAT);
    }
}
