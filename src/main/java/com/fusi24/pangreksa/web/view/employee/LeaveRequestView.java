package com.fusi24.pangreksa.web.view.employee;

import com.fusi24.pangreksa.web.model.entity.HrLeaveAbsenceTypes;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.html.Span;
import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.HrLeaveApplication;
import com.fusi24.pangreksa.web.model.entity.HrLeaveBalance;
import com.fusi24.pangreksa.web.model.entity.HrPerson;
import com.fusi24.pangreksa.web.model.enumerate.LeaveStatusEnum;
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.LeaveService;
import com.fusi24.pangreksa.web.service.PersonService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Route("leave-request-page-access")
@PageTitle("Leave Request")
@Menu(order = 37, icon = "vaadin:calendar-user", title = "Leave Request")
@RolesAllowed("LEAVE_REQ")
//@PermitAll // When security is enabled, allow all authenticated users
public class LeaveRequestView extends Main {
    private static final long serialVersionUID = 37L;
    private static final Logger log = LoggerFactory.getLogger(LeaveRequestView.class);
    private final CurrentUser currentUser;
    private final CommonService commonService;
    private final PersonService personService;
    private final LeaveService leaveService;
    private Authorization auth;

    public static final String VIEW_NAME = "Leave Request";

    private HorizontalLayout toolbarLayoutMaster;

    private VerticalLayout body;

    Button requestButton;
    Grid<HrLeaveApplication> leaveAppGrid;

    List<HrLeaveAbsenceTypes> leaveAbsenceTypesList;

    public LeaveRequestView(CurrentUser currentUser, CommonService commonService, PersonService personService, LeaveService leaveService) {
        this.currentUser = currentUser;
        this.commonService = commonService;
        this.personService = personService;
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

        this.leaveAbsenceTypesList = leaveService.getLeaveAbsenceTypesList();

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

        HorizontalLayout horizontalLayoutDashboard = new HorizontalLayout();
        horizontalLayoutDashboard.setWidth("150px");
        horizontalLayoutDashboard.setWidthFull();

        List<HrLeaveBalance> leaveBalances = leaveService.getAllLeaveBalance(currentUser.require(), LocalDate.now().getYear());

        for (HrLeaveBalance leaveBalance : leaveBalances) {
            if (leaveBalance.getUsedDays() > 0)
                horizontalLayoutDashboard.add(createDashboardCard(leaveBalance.getLeaveAbsenceType().getLabel(), String.valueOf(leaveBalance.getUsedDays())));
        }

        // Inisiasi toolbar Master
        toolbarLayoutMaster = new HorizontalLayout();
        toolbarLayoutMaster.setWidthFull();
        toolbarLayoutMaster.setAlignItems(FlexComponent.Alignment.END);

        requestButton = new Button("Request");
        requestButton.addClickListener( e-> {
            openRequestDialog();
        });

        toolbarLayoutMaster.add(requestButton);
//        toolbarLayoutMaster.add(createContent());
        toolbarLayoutMaster.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        body.add(horizontalLayoutDashboard, toolbarLayoutMaster, gridLeaveApplication());


        populateGrid();

        add(body);
    }

    private Grid gridLeaveApplication(){
        leaveAppGrid = new Grid<>(HrLeaveApplication.class, false);
        //add column
        leaveAppGrid.addColumn(HrLeaveApplication::getSubmittedAt).setHeader("Submitted").setSortable(true);
        leaveAppGrid.addColumn( l -> l.getLeaveAbsenceType().getLabel()).setHeader("Type").setSortable(true);
        leaveAppGrid.addColumn(HrLeaveApplication::getStartDate).setHeader("Start Date").setSortable(true);
        leaveAppGrid.addColumn(HrLeaveApplication::getTotalDays).setHeader("Total Days").setSortable(true);
        leaveAppGrid.addColumn(l -> l.getSubmittedTo().getFirstName() + " " +l.getSubmittedTo().getLastName()).setHeader("Approver").setSortable(true);
        leaveAppGrid.addColumn(HrLeaveApplication::getStatus).setHeader("Status").setSortable(true);

        return leaveAppGrid;
    }

    private void populateGrid(){
        leaveAppGrid.setItems(Collections.emptyList());
        leaveAppGrid.setItems(
                leaveService.getLeaveApplicationsByEmployee(currentUser.require())
        );
    }

    private void openRequestDialog() {
        requestButton.setEnabled(false);

        Dialog dialog = new Dialog("Leave Form");
        dialog.setModal(false);
        dialog.setDraggable(true);
        dialog.setWidth("600px");

        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("600px", 2)
        );

        ComboBox<HrLeaveAbsenceTypes> leaveAbsenceTypeDropdown = new ComboBox<>("Leave Type");
        leaveAbsenceTypeDropdown.setItems(leaveAbsenceTypesList);
        leaveAbsenceTypeDropdown.setItemLabelGenerator(HrLeaveAbsenceTypes::getLabel);

        DatePicker startDate = new DatePicker("Start Date");
        DatePicker endDate = new DatePicker("End Date");

        TextArea reason = new TextArea("Reason");
        reason.setWidthFull();
        reason.setHeight("15em");

        ComboBox<HrPerson> submittedToCombo = new ComboBox<>();
        submittedToCombo = new ComboBox<>("Person");
        submittedToCombo.addClassName("no-dropdown-icon");
        submittedToCombo.setItemLabelGenerator(p -> p.getFirstName() + " " + (p.getLastName() != null ? p.getLastName() : ""));
        submittedToCombo.setPlaceholder("Type to search");
        submittedToCombo.setClearButtonVisible(true);

        // set value as getManager(), but first set items
        HrPerson manager = getManager();

        // set Items
        submittedToCombo.setItems(query -> {
            String filter = query.getFilter().orElse("");
            int offset = query.getOffset();
            int limit = query.getLimit(); // not used in this example, but can be used for pagination
            log.debug("Searching persons with filter: {}", filter);

            List<HrPerson> managers = personService.findPersonByKeyword(filter);
            if (manager != null)// Add to stream
                managers.add(manager);

            return managers.stream();
        } );

        // get manager as list
        if (manager != null) {
            submittedToCombo.setValue(manager);
        }

        Button generateReasonButton = new Button("Help with reason..");
        Button cancelButton = new Button("Cancel");
        Button submitButton = new Button("Submit");

        //create horizontal layout for buttons, align and justify right
        HorizontalLayout buttonLayout = new HorizontalLayout(generateReasonButton, cancelButton, submitButton);
        buttonLayout.setWidthFull();
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setAlignItems(FlexComponent.Alignment.END);

        formLayout.add(
                leaveAbsenceTypeDropdown, startDate, endDate,
                submittedToCombo, reason
                );

        final String[] reasonTemplate = {"""
                Dengan hormat, 

                Saya ijin untuk mengajukan [TYPE] pada tanggal [FROM_DATE] sampai dengan [TO_DATE]. Adapun pengajuan ini untuk keperluan pribadi [ALASAN].

                Mohon dapat dipertimbangkan, dan di approve. Bilamana ada yang kurang jelas mohon hubungi Saya.

                Terima kasih.
                """};

        generateReasonButton.addClickListener(e -> {
            reason.setValue(new String());
            // Replace placeholders with actual values: TYPE, FROM_DATE, TO_DATE, ALASAN, PERSON
            reasonTemplate[0] = reasonTemplate[0].replace("[TYPE]", leaveAbsenceTypeDropdown.getValue() != null ? leaveAbsenceTypeDropdown.getValue().getLabel() : "[TYPE]");
            reasonTemplate[0] = reasonTemplate[0].replace("[FROM_DATE]", startDate.getValue() != null ? startDate.getValue().toString() : "[FROM_DATE]");
            reasonTemplate[0] = reasonTemplate[0].replace("[TO_DATE]", endDate.getValue() != null ? endDate.getValue().toString() : "[TO_DATE]");
            reason.setValue(reasonTemplate[0]);
        });

        ComboBox<HrPerson> finalSubmittedToCombo = submittedToCombo;
        submitButton.addClickListener(e-> {
            if (this.auth.canCreate){
                HrLeaveApplication request = HrLeaveApplication.builder()
//                        .leaveAbsenceTypeDropdown(leaveAbsenceTypeDropdown.getValue())
                        .leaveAbsenceType(leaveAbsenceTypeDropdown.getValue())
                        .startDate(startDate.getValue())
                        .endDate(endDate.getValue())
                        .reason(reason.getValue())
                        .status(LeaveStatusEnum.SUBMITTED)
                        .submittedTo(finalSubmittedToCombo.getValue())
                        .build();

                log.debug("Submitting leave request: {} {} {}", currentUser.require().getUserId(), request.getLeaveAbsenceType().getLabel(), request.getStatus().toString());

                request = leaveService.saveApplication(request, currentUser.require());
                if (request.getId() != null) {
                    Notification.show("Leave request submitted successfully!");
                    populateGrid();
                }

                dialog.close();
                requestButton.setEnabled(true);
            }
        });

        leaveAbsenceTypeDropdown.addValueChangeListener(e-> {
            //getLeaveBalance
            if (leaveAbsenceTypeDropdown.getValue() != null) {
                // Get the leave balance for the current user and selected leave type
                AppUserInfo appUser = currentUser.require();
                HrLeaveBalance leaveBalance = leaveService.getLeaveBalance(appUser, LocalDate.now().getYear(), leaveAbsenceTypeDropdown.getValue());

                if(e.getValue().getId() == 1)
                    leaveAbsenceTypeDropdown.setHelperText("You have " + (leaveBalance != null ? leaveBalance.getRemainingDays() : 0) + " days left for " + leaveAbsenceTypeDropdown.getValue().getLabel());
                else leaveAbsenceTypeDropdown.setHelperText(null);

                if (leaveBalance.getRemainingDays() <= 0) {
                    submitButton.setEnabled(false);
                } else {
                    submitButton.setEnabled(true);
                }
            }
        });

        cancelButton.addClickListener(e-> {
            dialog.close();
            requestButton.setEnabled(true);
        });

        dialog.add(formLayout,buttonLayout); // Placeholder for actual content

        dialog.open();
    }

    public void submitLeaveRequest() {
        // Logic to submit the leave request
        // This could involve validating the input, saving to the database, etc.



        Notification.show("Leave request submitted successfully!");
    }

    private Card createDashboardCard(String stringTitle, String stringValue) {
        // Format stringValue
        String formattedTitle = Arrays.stream(stringTitle.split("_"))
                .map(s -> s.isEmpty() ? s :
                        Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));

        Card card = new Card();
//        card.addThemeVariants(CardVariant.LUMO_ELEVATED);
        card.setMinWidth("150px");
        card.setHeight("75px");
        // Title
        Span cardTitle = new Span(formattedTitle);
        cardTitle.getStyle().set("color", "var(--lumo-secondary-text-color)");
        cardTitle.getStyle().set("text-align", "center");
        cardTitle.getStyle().set("display", "block");
        cardTitle.getStyle().set("margin", "auto");
        card.setTitle(cardTitle);
        // Card Value
        Span cardValue = new Span(stringValue);
        cardValue.getStyle().set("text-align", "center");
        cardValue.getStyle().set("display", "block");
        cardValue.getStyle().set("margin", "auto");
        // Layout inside card
        VerticalLayout cardContent = new VerticalLayout(cardValue);
        cardContent.setAlignItems(FlexComponent.Alignment.CENTER);
        cardContent.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        cardContent.setSizeFull();
        cardContent.setPadding(false);
        cardContent.setSpacing(false);

        card.add(cardContent);
        return card;
    }

    private HrPerson getManager(){
        var user = currentUser.require();
        return personService.getManager(user);
    }

    private void setListener() {
        // Add listeners for UI components here
        // For example, you can add a click listener to a button
        // saveButton.addClickListener(event -> savePage());
    }
}
