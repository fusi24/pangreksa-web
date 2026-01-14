package com.fusi24.pangreksa.web.view.admin;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrLeaveGenerationLog;
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.CompanyService;
import com.fusi24.pangreksa.web.service.LeaveService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.util.Collections;

@Route("leave-balance-page-access")
@PageTitle("Data Saldo Cuti.")
@Menu(order = 36, icon = "vaadin:calendar-user", title = "Data Saldo Cuti.")
@RolesAllowed("LEAVE_BAL")
//@PermitAll // When security is enabled, allow all authenticated users
public class LeaveBalanceDataView extends Main {
    private static final long serialVersionUID = 36L;
    private static final Logger log = LoggerFactory.getLogger(LeaveBalanceDataView.class);
    private final CurrentUser currentUser;
    private final CommonService commonService;
    private final CompanyService companyService;
    private final LeaveService leaveService;
    private Authorization auth;

    public static final String VIEW_NAME = "Data Saldo Cuti.";

    private VerticalLayout body;

    private ComboBox<HrCompany> companyDropdown;
    private ComboBox<Integer> yearDropdown;

    private Button checkButton;

    private Grid<HrLeaveGenerationLog> leaveGenerationLogGrid;


    public LeaveBalanceDataView(CurrentUser currentUser, CommonService commonService, CompanyService companyService, LeaveService leaveService) {
        this.currentUser = currentUser;
        this.commonService = commonService;
        this.companyService = companyService;
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
        companyDropdown = new ComboBox<>("Perusahaan");
        companyDropdown.setItems(companyService.getallCompanies());
        companyDropdown.setItemLabelGenerator(HrCompany::getName);
        companyDropdown.getStyle().setWidth("350px");

        yearDropdown = new ComboBox<>("Tahun");
        // put 5 years back and next 5 years
        int currentYear = LocalDate.now().getYear();
        yearDropdown.setItems(currentYear - 5, currentYear - 4, currentYear - 3, currentYear - 2, currentYear - 1,
                currentYear, currentYear + 1, currentYear + 2, currentYear + 3, currentYear + 4, currentYear + 5);
        yearDropdown.setItemLabelGenerator(String::valueOf);
        yearDropdown.setValue(currentYear);

        checkButton = new Button("Cek Saldo Cuti");

        HorizontalLayout headerFunction = new HorizontalLayout(companyDropdown, yearDropdown, checkButton);
        headerFunction.setWidthFull();
        headerFunction.setJustifyContentMode(FlexComponent.JustifyContentMode.START);
        headerFunction.setAlignItems(FlexComponent.Alignment.BASELINE);

        Grid<HrLeaveGenerationLog> grid = createLeaveGenerationLogGrid();
        grid.setSizeFull();

        body.add(headerFunction, grid);
        body.setFlexGrow(1, grid);

        add(body);
    }

    private Grid<HrLeaveGenerationLog> createLeaveGenerationLogGrid() {
        leaveGenerationLogGrid = new Grid<>(HrLeaveGenerationLog.class, false);

        leaveGenerationLogGrid.addColumn(log -> log.getCompany().getName()).setHeader("Perusahaan");
        leaveGenerationLogGrid.addColumn(HrLeaveGenerationLog::getYear).setHeader("Tahun");
        leaveGenerationLogGrid.addColumn(HrLeaveGenerationLog::getDataGenerated).setHeader("Total Data");
        leaveGenerationLogGrid.addColumn(log -> log.getCreatedBy().getUsername()).setHeader("Dibuat Oleh");
        leaveGenerationLogGrid.addColumn(HrLeaveGenerationLog::getCreatedAt).setHeader("Dibuat Pada");

        leaveGenerationLogGrid.setSizeFull(); // ⬅️ penting
        leaveGenerationLogGrid.setItems(Collections.emptyList());

        return leaveGenerationLogGrid;
    }


    private void setListener() {
        checkButton.addClickListener( e -> {
            if (this.auth.canView) {
                HrCompany company = this.companyDropdown.getValue();
                int year = this.yearDropdown.getValue();
                Long leaveServerCount = leaveService.countLeaveBalanceRowPerCompany(company, year);
                Long personCount = leaveService.countPersonPerCompany(company, year);
                int leaveTypeCount = leaveService.getLeaveAbsenceTypesList().size();

                log.debug("Data Saldo Cuti. for Company: {}, Year: {}, Leave Count: {}, Person Count: {}",
                        company.getName(), year, leaveServerCount, personCount);

                Dialog dialog = new Dialog();
                dialog.setWidth("500px");

                VerticalLayout dialogContent = new VerticalLayout();
                HorizontalLayout yearCountNF = new HorizontalLayout();
                yearCountNF.setWidthFull();
                yearCountNF.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
                yearCountNF.add(new Span("Year"), new Span(String.valueOf(year)));
                // Create a row using HorizontalLayout. Add two Text with adjacent on left and other right.
                HorizontalLayout leaveServerCountNF = new HorizontalLayout();
                leaveServerCountNF.setWidthFull();
                leaveServerCountNF.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
                leaveServerCountNF.add(new Span("Current row on database"), new Span(String.valueOf(leaveServerCount)));
                HorizontalLayout personCountNF = new HorizontalLayout();
                personCountNF.setWidthFull();
                personCountNF.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
                personCountNF.add(new Span("Total Employees on current year"), new Span(String.valueOf(personCount)));
                HorizontalLayout leaveTypeCountNF = new HorizontalLayout();
                leaveTypeCountNF.setWidthFull();
                leaveTypeCountNF.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
                Span leaveTypeLabel = new Span("Tipe of Leave");
                leaveTypeCountNF.add(leaveTypeLabel, new Span(String.valueOf(leaveTypeCount)));

                int totalRowGenerated = Math.toIntExact((int) (personCount * leaveTypeCount) - leaveServerCount);

                Span totalRowSpan = new Span(String.valueOf(totalRowGenerated));
                totalRowSpan.getStyle().set("font-weight", "bold");

                String message = totalRowGenerated > 0
                        ? "Need to generate "
                        : "Have enough rows in the database.";

                HorizontalLayout buttonLayout = new HorizontalLayout();
                Button cancelButton = new Button("Cancel", event -> dialog.close());
                Button saveButton = new Button("Simpan");

                if(!this.auth.canEdit){
                    saveButton.setEnabled(false);
                }

                Span messageSpan;
                if (totalRowGenerated > 0) {
                    messageSpan = new Span();
                    messageSpan.add(new Span("System will generate "), totalRowSpan, new Span(" rows."));
                } else {
                    messageSpan = new Span("Have enough rows in the database.");
                    saveButton.setEnabled(false);
                }

                saveButton.addClickListener(event -> {
                    // Do Whatever you want to do

                    log.debug("Save button clicked for Company: {}, Year: {}", company.getName(), year);
                    leaveService.generateLeaveBalanceData(company, year, currentUser.require());

                    dialog.close();
                });

                buttonLayout.add(cancelButton, saveButton);
                // button layout full width and justify content to end
                buttonLayout.setWidthFull();
                buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

                dialogContent.add(new Span(company.getName()),
                        yearCountNF,
                        leaveServerCountNF,
                        personCountNF,
                        leaveTypeCountNF,
                        messageSpan,
                        buttonLayout);
                dialog.add(dialogContent);
                dialog.open();
            }
        });

        companyDropdown.addValueChangeListener( e-> {
            if (this.auth.canView) {
                leaveGenerationLogGrid.setItems(Collections.emptyList());
                leaveGenerationLogGrid.setItems(leaveService.getLeaveGenerationLogs(e.getValue()));
            }
        });
    }
}
