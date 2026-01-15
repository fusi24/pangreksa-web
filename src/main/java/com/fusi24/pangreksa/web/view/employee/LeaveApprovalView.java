package com.fusi24.pangreksa.web.view.employee;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.base.util.DatePickerUtil;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.HrLeaveAbsenceTypes;
import com.fusi24.pangreksa.web.model.entity.HrLeaveApplication;
import com.fusi24.pangreksa.web.model.enumerate.LeaveStatusEnum;
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.LeaveService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

@Route("leave-approval-page-access")
@PageTitle("Pengajuan Cuti")
@Menu(order = 38, icon = "vaadin:calendar-user", title = "Pengajuan Cuti")
@RolesAllowed("LEAVE_APPR")
//@PermitAll // When security is enabled, allow all authenticated users
public class LeaveApprovalView extends Main {
    private static final long serialVersionUID = 38L;
    private static final Logger log = LoggerFactory.getLogger(LeaveApprovalView.class);
    private final CurrentUser currentUser;
    private final CommonService commonService;
    private final LeaveService leaveService;
    private Authorization auth;

    public static final String VIEW_NAME = "Pengajuan Cuti";

    private VerticalLayout body;

    Grid<HrLeaveApplication> leaveAppGrid;

    public LeaveApprovalView(CurrentUser currentUser, CommonService commonService, LeaveService leaveService) {
        this.currentUser = currentUser;
        this.commonService = commonService;
        this.leaveService = leaveService;

        this.auth = commonService.getAuthorization(
                currentUser.require(),
                (String) UI.getCurrent().getSession().getAttribute("responsibility"),
                this.serialVersionUID);

        log.debug("Page {}, Authorization: {} {} {} {}", VIEW_NAME, auth.canView, auth.canCreate, auth.canEdit, auth.canDelete);

        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
        setSizeFull();
        add(new ViewToolbar(VIEW_NAME));
        createBody();

        setListener();
        setAuthorization();
    }

    private void setAuthorization(){
        if(!this.auth.canView){
            // User does not have permission to view this page
        }
    }

    private void createBody() {
        this.body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);
        body.setSizeFull();
        Grid<HrLeaveApplication> grid = gridLeaveApplication();
        grid.setSizeFull();

        body.add(grid);
        body.setFlexGrow(1, grid);
        populateGrid();

        add(body);
    }

    private Grid gridLeaveApplication(){
        leaveAppGrid = new Grid<>(HrLeaveApplication.class, false);
        leaveAppGrid.setSizeFull();
        //add column
        leaveAppGrid.addColumn(HrLeaveApplication::getSubmittedAt).setHeader("Tanggal Pengajuan").setSortable(true);
        leaveAppGrid.addColumn(l -> l.getEmployee().getFirstName() + " " +l.getEmployee().getLastName()).setHeader("Mengajukan").setSortable(true);
        leaveAppGrid.addColumn(l -> l.getLeaveAbsenceType().getLabel()).setHeader("Tipe").setSortable(true);
        leaveAppGrid.addColumn(HrLeaveApplication::getStartDate).setHeader("Tanggal Mulai").setSortable(true);
        leaveAppGrid.addColumn(HrLeaveApplication::getTotalDays).setHeader("Jumlah Hari").setSortable(true);
        leaveAppGrid.addColumn(HrLeaveApplication::getStatus).setHeader("Status").setSortable(true);

        // Action column with delete button (icon only, no title)
        leaveAppGrid.addColumn(new ComponentRenderer<>(leaveApplication -> {
            // View & Edit button
            Button viewRequestButton = new Button();
            viewRequestButton.setIcon(VaadinIcon.NEWSPAPER.create());
            viewRequestButton.getElement().setAttribute("title", "View Request");

            if(!this.auth.canEdit){
                viewRequestButton.setEnabled(false);
            }

            viewRequestButton.addClickListener(e -> {

                openRequestDialog(leaveApplication);

            });

            if(!this.auth.canDelete){
                viewRequestButton.setEnabled(false);
            }

            HorizontalLayout actionLayout = new HorizontalLayout(viewRequestButton);
            return actionLayout;

        })).setHeader("").setFlexGrow(1).setAutoWidth(false);

        return leaveAppGrid;
    }

    private void openRequestDialog(HrLeaveApplication leaveApp) {
        Dialog dialog = new Dialog("Leave Form");
        dialog.setWidth("600px");

        Span nameSpan = new Span("");

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );

        ComboBox<HrLeaveAbsenceTypes> leaveType = new ComboBox<>("Tipe Cuti");
        leaveType.setItemLabelGenerator(HrLeaveAbsenceTypes::getLabel);

        DatePicker startDate = new DatePicker("Tanggal Mulai");
        DatePicker endDate = new DatePicker("Tanggal Selesai");

        startDate.setI18n(DatePickerUtil.getIndonesianI18n());
        endDate.setI18n(DatePickerUtil.getIndonesianI18n());

        TextArea reason = new TextArea("Keterangan");
        reason.setWidthFull();
        reason.setHeight("15em");

        Button cancelButton = new Button("Batal");
        Button approveButton = new Button("Approve");
        Button rejectButton = new Button("Reject");

        //create horizontal layout for buttons, align and justify right
        HorizontalLayout buttonLayout = new HorizontalLayout(rejectButton, approveButton, cancelButton);
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setAlignItems(FlexComponent.Alignment.END);

        formLayout.add(
                leaveType, startDate, endDate, reason
        );

        if(leaveApp.getId() != null) {
            nameSpan = new Span(leaveApp.getEmployee().getFirstName() + " " + leaveApp.getEmployee().getLastName());
            leaveType.setItems(leaveApp.getLeaveAbsenceType());
            leaveType.setValue(leaveApp.getLeaveAbsenceType());
            startDate.setValue(leaveApp.getStartDate());
            endDate.setValue(leaveApp.getEndDate());
            reason.setValue(leaveApp.getReason());
            leaveType.setReadOnly(true);
            startDate.setReadOnly(true);
            endDate.setReadOnly(true);
            reason.setReadOnly(true);
        }

        cancelButton.addClickListener(e -> {
            dialog.close();
        });

        rejectButton.addClickListener(e-> {
            if (this.auth.canEdit){
                leaveApp.setStatus(LeaveStatusEnum.REJECTED);

                saveLeaveApplication(leaveApp);

                dialog.close();
            }
        });

        approveButton.addClickListener(e-> {
            if (this.auth.canEdit){
                leaveApp.setStatus(LeaveStatusEnum.APPROVED);

                saveLeaveApplication(leaveApp);

                dialog.close();
            }
        });

        dialog.add(nameSpan, formLayout,buttonLayout); // Placeholder for actual content

        dialog.open();
    }

    private void saveLeaveApplication(HrLeaveApplication request){
        log.debug("Submitting Pengajuan Cuti: {} {} {}", currentUser.require().getUserId(), request.getLeaveAbsenceType().getLabel(), request.getStatus().toString());

        request = leaveService.saveApplication(request, currentUser.require());
        leaveService.updateLeaveBalance(request, currentUser.require());
        populateGrid();


    }

    private void populateGrid(){
        leaveAppGrid.setItems(Collections.emptyList());
        leaveAppGrid.setItems(
                leaveService.getLeaveApplicationForApproval(currentUser.require())
        );
    }

    private void setListener() {
        // Add listeners for UI components here
        // For example, you can add a click listener to a button
        // saveButton.addClickListener(event -> savePage());
    }
}

