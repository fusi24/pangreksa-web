package com.fusi24.pangreksa.web.view.manager;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.base.util.DatePickerUtil;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.*;
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.CompanyService;
import com.fusi24.pangreksa.web.service.PayrollService;
import com.fusi24.pangreksa.web.service.SalaryBaseLevelService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

@Route("komponen-gaji-list-page-access")
@PageTitle("Komponen Gaji")
@Menu(order = 23, icon = "vaadin:clipboard-check", title = "Komponen Gaji")
@RolesAllowed("KOMP_GAJI")
public class KomponenGajiView extends Main {
    private static final long serialVersionUID = 23L;
    private static final Logger log = LoggerFactory.getLogger(KomponenGajiView.class);

    private final CurrentUser currentUser;
    private final CommonService commonService;
    private final PayrollService payrollService;
    private final CompanyService companyService;
    private final SalaryBaseLevelService salaryBaseLevelService;

    private Authorization auth;
    private FwAppUser currentAppUser;

    public static final String VIEW_NAME = "Komponen Gaji";

    TabSheet tabsheet;
    private VerticalLayout body;

    // Base Level Salary
    Checkbox withInactiveBasicSalary;
    Button sblAddButton;
    Button sblSaveButton;
    Grid<HrSalaryBaseLevel> salaryBaseLevelGrid;

    // Allowance
    Checkbox withInactiveAllowance;
    Button saAddButton;
    Button saSaveButton;
    Grid<HrSalaryAllowance> salaryAllowanceGrid;
    boolean isAllowanceEdit = false;

    // Allowance Package
    Checkbox withInactivePositionAllowance;
    Grid<HrOrgStructure> orgStructureGrid;
    Grid<HrPosition> positionGrid;
    TextField companyTF;
    TextField organizationTF;
    TextField positionTF;
    DatePicker calcullationDateTF;
    TextField totalallowanceTF;
    List<HrSalaryAllowance> availableAllowanceList;
    Grid<HrSalaryPositionAllowance> positionAllowanceGrid;
    boolean isPositionallowanceEdit = false;
    Button paAddButton;
    Button paSaveButton;

    public KomponenGajiView(CurrentUser currentUser,
                            CommonService commonService,
                            PayrollService payrollService,
                            CompanyService companyService,
                            SalaryBaseLevelService salaryBaseLevelService) {

        this.currentUser = currentUser;
        this.commonService = commonService;
        this.payrollService = payrollService;
        this.companyService = companyService;
        this.salaryBaseLevelService = salaryBaseLevelService;

        this.payrollService.setUser(currentUser.require());

        this.auth = commonService.getAuthorization(
                currentUser.require(),
                (String) UI.getCurrent().getSession().getAttribute("responsibility"),
                this.serialVersionUID);

        this.currentAppUser = commonService.getLoginUser(currentUser.require().getUserId().toString());

        log.debug("Page {}, Authorization: {} {} {} {}", VIEW_NAME, auth.canView, auth.canCreate, auth.canEdit, auth.canDelete);

        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar(VIEW_NAME));
        createBody();
        setListener();
        setAuthorization();
    }

    private void setAuthorization(){
        if(!this.auth.canCreate){
            sblAddButton.setEnabled(false);
            sblSaveButton.setEnabled(false);

            saAddButton.setEnabled(false);
            saSaveButton.setEnabled(false);

            paAddButton.setEnabled(false);
            paSaveButton.setEnabled(false);
        }
        if(!this.auth.canEdit){
            sblSaveButton.setEnabled(false);
            saSaveButton.setEnabled(false);
            paSaveButton.setEnabled(false);
        }
        if(!this.auth.canView){
            withInactiveBasicSalary.setEnabled(false);
            withInactiveAllowance.setEnabled(false);
            withInactivePositionAllowance.setEnabled(false);
        }
    }

    private void createBody() {
        this.setHeightFull();

        this.body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);
        body.setHeightFull();

        withInactiveBasicSalary = new Checkbox(true);
        withInactiveBasicSalary.setLabel("With Inactive Level Code");

        withInactiveAllowance = new Checkbox(true);
        withInactiveAllowance.setLabel("With Inactive Allowance");

        withInactivePositionAllowance = new Checkbox(true);
        withInactivePositionAllowance.setLabel("With Inactive Allowance");

        this.tabsheet = new TabSheet();
        tabsheet.setWidthFull();
        tabsheet.setHeightFull();
        tabsheet.add("Gaji Pokok", createSalaryBaseLevelFunction());
        tabsheet.add("Tunjangan", createAlowanceFunction());
        tabsheet.add("Paket Tunjangan", createAllowancePackageFunction());

        body.add(tabsheet);

        populateSalaryBaseLevelGrid(withInactiveBasicSalary.getValue());
        populateAllowanceGrid(withInactiveAllowance.getValue());

        add(body);
    }

    /* =========================
       CREATE COMPONENT
       ========================= */

    private Component createSalaryBaseLevelFunction(){
        sblAddButton = new Button("Add");
        sblSaveButton = new Button("Refresh");

        HorizontalLayout leftLayout = new HorizontalLayout();
        leftLayout.setSpacing(true);
        leftLayout.add(withInactiveBasicSalary);
        leftLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        HorizontalLayout functionMenuLayout = new HorizontalLayout();
        functionMenuLayout.setWidthFull();
        functionMenuLayout.add(leftLayout);
        functionMenuLayout.add(sblSaveButton, sblAddButton);
        functionMenuLayout.setAlignItems(HorizontalLayout.Alignment.BASELINE);
        functionMenuLayout.expand(leftLayout);

        this.salaryBaseLevelGrid = new Grid<>(HrSalaryBaseLevel.class, false);
        salaryBaseLevelGrid.addClassNames("salary-base-level-grid");
        salaryBaseLevelGrid.setWidthFull();
        salaryBaseLevelGrid.setHeight("300px");

        salaryBaseLevelGrid.addColumn(item ->
                item.getCompany() != null ? item.getCompany().getName() : ""
        ).setHeader("Company").setAutoWidth(true);



        salaryBaseLevelGrid.addColumn(HrSalaryBaseLevel::getLevelCode)
                .setHeader("Level Code").setAutoWidth(true);

        salaryBaseLevelGrid.addColumn(item ->
                        item.getBaseSalary() == null ? "" : decimalAmountFormater(item.getBaseSalary()))
                .setHeader("Amount").setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.END);

        salaryBaseLevelGrid.addColumn(item ->
                        item.getStartDate() == null ? "" : item.getStartDate().toString())
                .setHeader("Start Date").setAutoWidth(true);

        salaryBaseLevelGrid.addColumn(item ->
                        item.getEndDate() == null ? "" : item.getEndDate().toString())
                .setHeader("End Date").setAutoWidth(true);

        salaryBaseLevelGrid.addColumn(item ->
                        item.getReason() == null ? "" : item.getReason())
                .setHeader("Reason").setAutoWidth(true);

        // ✅ ACTION COLUMN DI TEMPAT YANG BENAR
        salaryBaseLevelGrid.addColumn(new ComponentRenderer<>(item -> {
            HorizontalLayout actions = new HorizontalLayout();

            Button editBtn = new Button("Edit", ev -> {
                SalaryBaseLevelDialog dialog = new SalaryBaseLevelDialog(
                        salaryBaseLevelService,
                        currentAppUser.getCompany(),
                        currentAppUser,
                        item,
                        () -> populateSalaryBaseLevelGrid(withInactiveBasicSalary.getValue())
                );
                dialog.open();
            });

            Button deleteBtn = new Button("Delete", ev -> {
                if (!auth.canDelete) return;
                if (item.getId() != null) {
                    salaryBaseLevelService.delete(item);
                    populateSalaryBaseLevelGrid(withInactiveBasicSalary.getValue());
                }
            });

            editBtn.setEnabled(auth.canEdit);
            deleteBtn.setEnabled(auth.canDelete);

            actions.add(editBtn, deleteBtn);
            return actions;
        })).setHeader("Action").setFlexGrow(0).setAutoWidth(true);

        VerticalLayout salaryBaseLevelLayout = new VerticalLayout(functionMenuLayout, salaryBaseLevelGrid);
        salaryBaseLevelLayout.setSpacing(false);
        salaryBaseLevelLayout.setPadding(false);
        salaryBaseLevelLayout.setHeightFull();

        return salaryBaseLevelLayout;
    }

    private Component createAlowanceFunction(){
        saAddButton = new Button("Add");
        saSaveButton = new Button("Save");

        HorizontalLayout leftLayout = new HorizontalLayout();
        leftLayout.setSpacing(true);
        leftLayout.add(withInactiveAllowance);
        leftLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        HorizontalLayout functionMenuLayout = new HorizontalLayout();
        functionMenuLayout.setWidthFull();
        functionMenuLayout.add(leftLayout);
        functionMenuLayout.add(saSaveButton, saAddButton);
        functionMenuLayout.setAlignItems(HorizontalLayout.Alignment.BASELINE);
        functionMenuLayout.expand(leftLayout);

        this.salaryAllowanceGrid = new Grid<>(HrSalaryAllowance.class, false);
        salaryAllowanceGrid.addClassNames("salary-base-level-grid");
        salaryAllowanceGrid.setWidthFull();
        salaryAllowanceGrid.setHeight("300px");

        salaryAllowanceGrid.addColumn(new ComponentRenderer<>(item -> {
            TextField nameField = new TextField();
            nameField.setWidthFull();
            nameField.setValue(item.getName() != null ? item.getName() : "");
            nameField.addValueChangeListener(e -> {
                item.setName(e.getValue());
                this.isAllowanceEdit = true;
            });
            return nameField;
        })).setHeader("Allowance Name");

        salaryAllowanceGrid.addColumn(new ComponentRenderer<>(item -> {
            TextField salaryField = new TextField();
            salaryField.setWidthFull();
            salaryField.setValue(item.getAmount() != null ? decimalAmountFormater(item.getAmount()) : "");
            salaryField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
            salaryField.addThemeVariants(TextFieldVariant.LUMO_ALIGN_RIGHT);
            salaryField.setAllowedCharPattern("[0-9.,]");

            salaryField.addValueChangeListener(e -> {
                String value = e.getValue().replace(".", "").replace(",", ".");
                try {
                    BigDecimal parsed = new BigDecimal(value);
                    item.setAmount(parsed);
                    this.isAllowanceEdit = true;
                    salaryField.setValue(decimalAmountFormater(parsed));
                } catch (NumberFormatException ex) {
                    if (item.getAmount() != null) {
                        salaryField.setValue(decimalAmountFormater(item.getAmount()));
                    } else {
                        salaryField.clear();
                    }
                }
            });

            return salaryField;
        })).setHeader("Amount").setAutoWidth(true).setTextAlign(ColumnTextAlign.END);

        salaryAllowanceGrid.addColumn(new ComponentRenderer<>(item -> {
            DatePicker startDate = new DatePicker();
            startDate.setI18n(DatePickerUtil.getIndonesianI18n());
            startDate.setWidthFull();
            startDate.setValue(item.getStartDate());
            startDate.addValueChangeListener(e -> {
                item.setStartDate(e.getValue());
                this.isAllowanceEdit = true;
            });
            return startDate;
        })).setHeader("Start Date").setWidth("130px");

        salaryAllowanceGrid.addColumn(new ComponentRenderer<>(item -> {
            DatePicker endDate = new DatePicker();
            endDate.setI18n(DatePickerUtil.getIndonesianI18n());
            endDate.setWidthFull();
            endDate.setValue(item.getEndDate());
            endDate.addValueChangeListener(e -> {
                item.setEndDate(e.getValue());
                this.isAllowanceEdit = true;
            });
            return endDate;
        })).setHeader("End Date").setWidth("130px");

        salaryAllowanceGrid.addColumn(new ComponentRenderer<>(item -> {
            Button deleteButton = new Button(VaadinIcon.TRASH.create());
            deleteButton.setEnabled(this.auth.canDelete);
            deleteButton.addClickListener(e -> {
                if (!this.auth.canDelete) return;

                Dialog confirm = new Dialog();
                confirm.setHeaderTitle("Delete allowance?");
                Span msg = new Span("Are you sure you want to delete \"" +
                        (item.getName() != null ? item.getName() : "(no name)") + "\"?");
                confirm.add(new VerticalLayout(msg));

                Button cancel = new Button("Cancel", ev -> confirm.close());
                Button yes = new Button("Delete", ev -> {
                    if (item.getId() != null) {
                        payrollService.deleteSalaryAllowance(item);
                    }
                    salaryAllowanceGrid.getListDataView().removeItem(item);
                    Notification.show("Allowance deleted");
                    confirm.close();
                });
                confirm.getFooter().add(cancel, yes);
                confirm.open();
            });
            return deleteButton;
        })).setHeader("Actions").setFlexGrow(0).setAutoWidth(true);

        VerticalLayout salaryAlowanceLayout = new VerticalLayout(functionMenuLayout, salaryAllowanceGrid);
        salaryAlowanceLayout.setSpacing(false);
        salaryAlowanceLayout.setPadding(false);
        salaryAlowanceLayout.setHeightFull();

        return salaryAlowanceLayout;
    }

    private Component createAllowancePackageFunction(){
        HorizontalLayout leftLayout = new HorizontalLayout();
        leftLayout.setSpacing(true);
        leftLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        HorizontalLayout functionMenuLayout = new HorizontalLayout();
        functionMenuLayout.setWidthFull();
        functionMenuLayout.add(leftLayout);
        functionMenuLayout.setAlignItems(HorizontalLayout.Alignment.BASELINE);
        functionMenuLayout.expand(leftLayout);

        String WIDTH_ = "250px";

        orgStructureGrid = new Grid<>(HrOrgStructure.class, false);
        orgStructureGrid.addClassNames("org-structure-grid");
        orgStructureGrid.setWidth(WIDTH_);
        orgStructureGrid.setHeightFull();
        orgStructureGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        orgStructureGrid.addColumn(HrOrgStructure::getName).setHeader("Organization");

        positionGrid = new Grid<>(HrPosition.class, false);
        positionGrid.addClassNames("position-grid");
        positionGrid.setWidth(WIDTH_);
        positionGrid.setHeightFull();
        positionGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        positionGrid.addColumn(HrPosition::getName).setHeader("Position");

        HorizontalLayout horizontalbodyLayout = new HorizontalLayout(
                orgStructureGrid,
                positionGrid,
                createAllowancePackageDetailFunction());
        horizontalbodyLayout.setHeightFull();
        horizontalbodyLayout.setWidthFull();

        VerticalLayout allowancePackageLayout = new VerticalLayout(functionMenuLayout, horizontalbodyLayout);
        allowancePackageLayout.setSpacing(false);
        allowancePackageLayout.setPadding(false);
        allowancePackageLayout.setHeightFull();

        populateOrgStructureGrid();

        return allowancePackageLayout;
    }

    private Component createAllowancePackageDetailFunction() {
        companyTF = new TextField();
        companyTF.setWidthFull();
        companyTF.setReadOnly(true);

        organizationTF = new TextField();
        organizationTF.setWidthFull();
        organizationTF.setReadOnly(true);

        positionTF = new TextField();
        positionTF.setWidthFull();
        positionTF.setReadOnly(true);

        totalallowanceTF = new TextField();
        totalallowanceTF.setWidthFull();
        totalallowanceTF.setReadOnly(true);

        calcullationDateTF = new DatePicker();
        calcullationDateTF.setI18n(DatePickerUtil.getIndonesianI18n());
        calcullationDateTF.setValue(LocalDate.now());
        calcullationDateTF.setTooltipText(
                "is the specific date used as a reference point to perform a calculation. " +
                        "It determines the effective point in time for applying formulas, rules, or computations."
        );

        totalallowanceTF.addThemeVariants(TextFieldVariant.LUMO_ALIGN_RIGHT);
        totalallowanceTF.setAllowedCharPattern("[0-9]");
        totalallowanceTF.setPrefixComponent(new Span("Rp "));
        totalallowanceTF.setValue(decimalAmountFormater(BigDecimal.ZERO));

        paSaveButton = new Button("Save");
        paAddButton = new Button("Add");

        populateAvailableAllowanceList();

        FormLayout formLayout = new FormLayout();
        formLayout.setWidth("500px");
        formLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.ASIDE)
        );

        formLayout.addFormItem(companyTF, "Company");
        formLayout.addFormItem(organizationTF, "Organization");
        formLayout.addFormItem(positionTF, "Position");
        formLayout.addFormItem(calcullationDateTF, "Calculation Date");
        formLayout.addFormItem(totalallowanceTF, "Total Allowance");

        companyTF.setValue(currentAppUser.getCompany().getName());

        HorizontalLayout leftLayout = new HorizontalLayout();
        leftLayout.setSpacing(true);
        leftLayout.add(withInactivePositionAllowance);
        leftLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        HorizontalLayout functionMenuLayout = new HorizontalLayout();
        functionMenuLayout.setWidthFull();
        functionMenuLayout.add(leftLayout);
        functionMenuLayout.add(paSaveButton, paAddButton);
        functionMenuLayout.setAlignItems(HorizontalLayout.Alignment.BASELINE);
        functionMenuLayout.expand(leftLayout);

        positionAllowanceGrid = new Grid<>(HrSalaryPositionAllowance.class, false);
        positionAllowanceGrid.addClassNames("position-allowance-grid");
        positionAllowanceGrid.setWidthFull();
        positionAllowanceGrid.setHeight("200px");
        positionAllowanceGrid.setSelectionMode(Grid.SelectionMode.NONE);

        positionAllowanceGrid.addColumn(pa -> pa.getAllowance().getName()).setHeader("Allowance");

        positionAllowanceGrid.addColumn(new ComponentRenderer<>(item -> {
            TextField amountField = new TextField();
            amountField.setWidthFull();
            amountField.setReadOnly(true);
            amountField.addThemeVariants(TextFieldVariant.LUMO_ALIGN_RIGHT);
            amountField.setAllowedCharPattern("[0-9]");
            amountField.setPrefixComponent(new Span("Rp "));
            if (item.getAllowance() != null && item.getAllowance().getAmount() != null) {
                amountField.setValue(decimalAmountFormater(item.getAllowance().getAmount()));
            }
            return amountField;
        })).setHeader("Amount").setAutoWidth(true).setTextAlign(ColumnTextAlign.END);

        positionAllowanceGrid.addColumn(new ComponentRenderer<>(item -> {
            DatePicker startDate = new DatePicker();
            startDate.setI18n(DatePickerUtil.getIndonesianI18n());
            startDate.setWidthFull();
            startDate.setValue(item.getStartDate());
            startDate.addValueChangeListener(e -> {
                item.setStartDate(e.getValue());
                this.isPositionallowanceEdit = true;
            });
            return startDate;
        })).setHeader("Start Date").setWidth("130px");

        positionAllowanceGrid.addColumn(new ComponentRenderer<>(item -> {
            DatePicker endDate = new DatePicker();
            endDate.setI18n(DatePickerUtil.getIndonesianI18n());
            endDate.setWidthFull();
            endDate.setValue(item.getEndDate());
            endDate.addValueChangeListener(e -> {
                item.setEndDate(e.getValue());
                this.isPositionallowanceEdit = true;
                calculateTotalAllowance();
            });
            return endDate;
        })).setHeader("End Date").setWidth("130px");

        positionAllowanceGrid.addColumn(new ComponentRenderer<>(item -> {
            Button deleteButton = new Button(VaadinIcon.CLOSE.create());
            deleteButton.addClickListener(e -> {
                if (this.auth.canDelete) {
                    positionAllowanceGrid.getListDataView().removeItem(item);
                    calculateTotalAllowance();
                }
            });
            deleteButton.setEnabled(this.auth.canDelete);
            return item.getId() == null ? deleteButton : null;
        })).setHeader("Actions").setFlexGrow(0).setAutoWidth(true);

        VerticalLayout vLayout = new VerticalLayout(formLayout, functionMenuLayout, positionAllowanceGrid);
        vLayout.setWidth("100%");
        vLayout.setHeightFull();

        return vLayout;
    }

    /* =========================
       POPULATE GRID
       ========================= */

    private void populateSalaryBaseLevelGrid(boolean includeInactive){
        if(this.auth.canView) {
            List<HrSalaryBaseLevel> salaryBaseLevelList =
                    payrollService.getAllSalaryBaseLevels(includeInactive);
            salaryBaseLevelGrid.setItems(salaryBaseLevelList);
        }
    }

    private void populateAllowanceGrid(boolean includeInactive){
        if(this.auth.canView) {
            List<HrSalaryAllowance> salaryallowanceList =
                    payrollService.getAllSalaryAllowances(includeInactive);
            salaryAllowanceGrid.setItems(salaryallowanceList);
        }
    }

    private void populateOrgStructureGrid(){
        if(this.auth.canView) {
            List<HrOrgStructure> orgStructureList =
                    companyService.getAllOrgStructuresInCompany(this.currentAppUser.getCompany());
            orgStructureGrid.setItems(orgStructureList);
        }
    }

    private void populatePositionGrid(HrOrgStructure orgStructure){
        if(this.auth.canView) {
            List<HrPosition> positionList =
                    companyService.getAllPositionsInOrganization(this.currentAppUser.getCompany(), orgStructure);
            positionGrid.setItems(positionList);
        }
    }

    private void populateAvailableAllowanceList(){
        this.availableAllowanceList =
                payrollService.getAllSalaryAllowances(withInactivePositionAllowance.getValue());
        log.debug("Refresh Allowance Map, total: {}", availableAllowanceList.size());
    }

    private void populatePositionAllowanceGrid(HrPosition position, HrOrgStructure orgStructure){
        if(position != null && orgStructure != null) {
            List<HrSalaryPositionAllowance> positionAllowanceList =
                    payrollService.getSalaryPositionAllowancesByPosition(position, withInactivePositionAllowance.getValue());
            positionAllowanceGrid.setItems(positionAllowanceList);
            calculateTotalAllowance();
        } else {
            positionAllowanceGrid.setItems();
            totalallowanceTF.setValue(decimalAmountFormater(BigDecimal.ZERO));
        }
    }

    private String decimalAmountFormater(BigDecimal amount){
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.of("id", "ID"));
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        DecimalFormat decimalFormat = new DecimalFormat("#,###.##", symbols);
        return decimalFormat.format(amount);
    }

    private void calculateTotalAllowance(){
        BigDecimal total = BigDecimal.ZERO;

        List<HrSalaryPositionAllowance> positionAllowanceList =
                positionAllowanceGrid.getListDataView().getItems().toList();

        for(HrSalaryPositionAllowance pa : positionAllowanceList){
            if  (
                    (calcullationDateTF.getValue().isAfter(pa.getStartDate())
                            || calcullationDateTF.getValue().isEqual(pa.getStartDate()))
                            &&
                            (pa.getEndDate() == null || calcullationDateTF.getValue().isBefore(pa.getEndDate()))
            ) {
                total = total.add(pa.getAllowance().getAmount());
            }
        }

        totalallowanceTF.setValue(decimalAmountFormater(total));
    }

    /* =========================
       LISTENER
       ========================= */

    private void setListener() {

        // ✅ ADD basic salary -> POPUP
        sblAddButton.addClickListener(e -> {
            SalaryBaseLevelDialog dialog = new SalaryBaseLevelDialog(
                    salaryBaseLevelService,
                    currentAppUser.getCompany(),
                    currentAppUser,
                    null,
                    () -> populateSalaryBaseLevelGrid(withInactiveBasicSalary.getValue())
            );
            dialog.open();
        });

        // ✅ Refresh base salary
        sblSaveButton.addClickListener(e -> {
            populateSalaryBaseLevelGrid(withInactiveBasicSalary.getValue());
            Notification.show("Refreshed");
        });

        withInactiveBasicSalary.addValueChangeListener(e -> {
            populateSalaryBaseLevelGrid(e.getValue());
        });

        // Allowance add
        saAddButton.addClickListener(e -> {
            HrSalaryAllowance newAllowance = new HrSalaryAllowance();
            newAllowance.setName("NEW ALLOWANCE");
            newAllowance.setAmount(BigDecimal.ZERO);
            newAllowance.setStartDate(LocalDate.now());
            salaryAllowanceGrid.getListDataView().addItem(newAllowance);
            this.isAllowanceEdit = true;
        });

        // Allowance save
        saSaveButton.addClickListener(e -> {
            if(this.auth.canEdit && this.isAllowanceEdit) {
                List<HrSalaryAllowance> toSaveList =
                        salaryAllowanceGrid.getListDataView().getItems().toList();

                for (HrSalaryAllowance item : toSaveList) {
                    payrollService.saveSalaryAllowance(item, currentUser.require());
                }
                populateAllowanceGrid(withInactiveAllowance.getValue());
                this.isAllowanceEdit = false;
                Notification.show("Data Allowance Saved Successfully");
            }
        });

        withInactiveAllowance.addValueChangeListener(e -> {
            populateAllowanceGrid(e.getValue());
        });

        orgStructureGrid.addSelectionListener(e -> {
            positionAllowanceGrid.setItems(Collections.emptyList());
            calculateTotalAllowance();

            e.getFirstSelectedItem().ifPresent(item -> {
                positionTF.clear();
                positionGrid.deselectAll();
                populatePositionGrid(item);
                organizationTF.setValue(item.getName());
            });
        });

        positionGrid.addSelectionListener(e -> {
            e.getFirstSelectedItem().ifPresentOrElse(item -> {
                positionTF.setValue(item.getName());
                populatePositionAllowanceGrid(
                        item,
                        orgStructureGrid.getSelectedItems().iterator().next()
                );
            }, () -> {
                positionTF.clear();
                positionAllowanceGrid.setItems(Collections.emptyList());
                calculateTotalAllowance();
            });
        });

        paAddButton.addClickListener(e -> {
            if(this.auth.canCreate) {
                Dialog dialog = new Dialog();
                dialog.setWidth("500px");

                ComboBox<HrSalaryAllowance> allowanceCB = new ComboBox<>("Allowance");
                allowanceCB.setWidthFull();
                allowanceCB.setItems(this.availableAllowanceList);
                allowanceCB.setItemLabelGenerator(HrSalaryAllowance::getName);

                allowanceCB.setRenderer(new ComponentRenderer<>(allowance -> {
                    HorizontalLayout row = new HorizontalLayout();
                    row.setWidthFull();
                    row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

                    Span name = new Span(allowance.getName());
                    NumberFormat formatter = NumberFormat.getNumberInstance(Locale.of("id", "ID"));
                    Span amount = new Span("Rp " + formatter.format(allowance.getAmount()));
                    amount.getStyle().set("color", "gray");

                    row.add(name, amount);
                    return row;
                }));

                Button cancelButton = new Button("Cancel", event -> dialog.close());
                Button addButton = new Button("Add");

                if(!this.auth.canEdit){
                    addButton.setEnabled(false);
                }

                addButton.addClickListener(event -> {
                    HrSalaryAllowance selectedAllowance = allowanceCB.getValue();
                    if (selectedAllowance != null && !positionTF.getValue().equals("") &&
                            positionGrid.getSelectedItems().size() >= 1 &&
                            orgStructureGrid.getSelectedItems().size() >= 1) {

                        HrPosition selectedPosition = positionGrid.getSelectedItems().iterator().next();
                        HrOrgStructure selectedOrg = orgStructureGrid.getSelectedItems().iterator().next();

                        HrSalaryPositionAllowance newPA = new HrSalaryPositionAllowance();
                        newPA.setAllowance(selectedAllowance);
                        newPA.setPosition(selectedPosition);
                        newPA.setOrgStructure(selectedOrg);
                        newPA.setCompany(this.currentAppUser.getCompany());
                        newPA.setStartDate(LocalDate.now());

                        positionAllowanceGrid.getListDataView().addItem(newPA);
                        isPositionallowanceEdit = true;
                    } else {
                        Notification.show("Please select an allowance and position");
                    }

                    calculateTotalAllowance();
                    dialog.close();
                });

                HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, addButton);
                buttonLayout.setWidthFull();
                buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

                dialog.add(allowanceCB, buttonLayout);
                dialog.open();
            }
        });

        paSaveButton.addClickListener(e -> {
            if(this.auth.canEdit && this.isPositionallowanceEdit) {
                List<HrSalaryPositionAllowance> toSaveList =
                        positionAllowanceGrid.getListDataView().getItems().toList();

                for (HrSalaryPositionAllowance item : toSaveList) {
                    payrollService.saveSalaryPositionAllowance(item, currentUser.require());
                }

                populatePositionAllowanceGrid(
                        positionGrid.getSelectedItems().iterator().next(),
                        orgStructureGrid.getSelectedItems().iterator().next()
                );

                this.isPositionallowanceEdit = false;
                Notification.show("Data Position Allowance Saved Successfully");
            }
        });

        tabsheet.addSelectedChangeListener(event -> {
            Tab selectedTab = event.getSelectedTab();
            log.debug("Select tab {} on tabsheet", selectedTab.getLabel());
            if (selectedTab.getLabel().equals("Allowance Packages")) {
                populateAvailableAllowanceList();
            }
        });

        withInactivePositionAllowance.addValueChangeListener(e -> {
            if(positionGrid.getSelectedItems().iterator().hasNext()
                    && orgStructureGrid.getSelectedItems().iterator().hasNext()) {
                populatePositionAllowanceGrid(
                        positionGrid.getSelectedItems().iterator().next(),
                        orgStructureGrid.getSelectedItems().iterator().next()
                );
            }
        });

        calcullationDateTF.addValueChangeListener(e -> calculateTotalAllowance());
    }
}
