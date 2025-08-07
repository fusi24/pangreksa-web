package com.fusi24.pangreksa.web.view.employee;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.HrLeaveApplication;
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.LeaveService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

@Route("leave-approval-page-access")
@PageTitle("Leave Approval")
@Menu(order = 38, icon = "vaadin:calendar-user", title = "Leave Approval")
@RolesAllowed("LEAVE_APPR")
//@PermitAll // When security is enabled, allow all authenticated users
public class LeaveApprovalView extends Main {
    private static final long serialVersionUID = 38L;
    private static final Logger log = LoggerFactory.getLogger(LeaveApprovalView.class);
    private final CurrentUser currentUser;
    private final CommonService commonService;
    private final LeaveService leaveService;
    private Authorization auth;

    public static final String VIEW_NAME = "Leave Approval";

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

        body.add(gridLeaveApplication());
        populateGrid();

        add(body);
    }

    private Grid gridLeaveApplication(){
        leaveAppGrid = new Grid<>(HrLeaveApplication.class, false);
        //add column
        leaveAppGrid.addColumn(HrLeaveApplication::getSubmittedAt).setHeader("Submitted").setSortable(true);
        leaveAppGrid.addColumn(l -> l.getEmployee().getFirstName() + " " +l.getEmployee().getLastName()).setHeader("Requestor").setSortable(true);
        leaveAppGrid.addColumn(HrLeaveApplication::getLeaveType).setHeader("Type").setSortable(true);
        leaveAppGrid.addColumn(HrLeaveApplication::getStartDate).setHeader("Start Date").setSortable(true);
        leaveAppGrid.addColumn(HrLeaveApplication::getTotalDays).setHeader("Total Days").setSortable(true);

        return leaveAppGrid;
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

