package com.fusi24.pangreksa.web.view.manager;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.HrPayroll;
import com.fusi24.pangreksa.web.model.entity.HrPayrollCalculation;
import com.fusi24.pangreksa.web.model.entity.HrPerson;
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.PayrollService;
import com.fusi24.pangreksa.web.service.SystemService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;


@Route("payroll-list-page-access")
@PageTitle("Payroll")
@Menu(order = 27, icon = "vaadin:clipboard-check", title = "Payroll")
@RolesAllowed("PAYROLL")
public class PayrollView extends Main {
    public static final String VIEW_NAME = "Payroll";
    private static final long serialVersionUID = 19092L;
    private static final Logger log = LoggerFactory.getLogger(PayrollView.class);
    private final CurrentUser currentUser;
    private final CommonService commonService;
    private final SystemService systemService;
    private final PayrollService payrollService;

    private Authorization auth;

    private Grid<HrPayroll> grid = new Grid<>(HrPayroll.class, false);
    private TextField searchField = new TextField();
    private Dialog detailDialog = new Dialog();
    private Dialog addEditDialog = new Dialog();

    private ComboBox<Integer> yearFilter = new ComboBox<>();
    private ComboBox<Integer> monthFilter = new ComboBox<>();

    MutableObject<HrPayroll> mObject = new MutableObject<>();

    public PayrollView(CurrentUser currentUser, CommonService commonService, SystemService systemService, PayrollService payrollService) {
        this.payrollService = payrollService;
        this.currentUser = currentUser;
        this.commonService = commonService;
        this.systemService = systemService;

        this.payrollService.setUser(currentUser.require());

        this.auth = commonService.getAuthorization(
                currentUser.require(),
                (String) UI.getCurrent().getSession().getAttribute("responsibility"),
                this.serialVersionUID);

        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar(VIEW_NAME));

        log.debug("Page {}, Authorization: {} {} {} {}", VIEW_NAME, auth.canView, auth.canCreate, auth.canEdit, auth.canDelete);
        initializeView();
    }

    private void initializeView() {
        this.setHeightFull();
        // Configure Grid
        grid.addColumn(HrPayroll::getId).setHeader("ID").setWidth("80px");
        grid.addColumn(payroll -> payroll.getPerson().getFirstName() + " " + payroll.getPerson().getLastName())
                .setHeader("Employee").setAutoWidth(true);
        grid.addColumn(HrPayroll::getPayrollMonth).setHeader("Payroll Month").setWidth("150px");
        grid.addColumn(HrPayroll::getVariableAllowances).setHeader("Var. Allowances").setWidth("150px");
        grid.addColumn(HrPayroll::getOvertimeAmount).setHeader("Overtime").setWidth("120px");
        grid.addColumn(HrPayroll::getAnnualBonus).setHeader("Bonus").setWidth("120px");
        grid.addColumn(HrPayroll::getOtherDeductions).setHeader("Other Deduct.").setWidth("140px");
//        grid.addColumn(HrPayroll::getNetTakeHomePay).setHeader("Net THP").setWidth("120px"); // Requires join to HrPayrollCalculation

        // Add Action Buttons Column
        grid.addComponentColumn(payroll -> {
            HorizontalLayout actions = new HorizontalLayout();

            Button detailBtn = new Button("Detail", e -> openDetailDialog(payroll));
            detailBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
            detailBtn.setAriaLabel("View Detail");

            Button recalculateBtn = new Button("Recalculate", e -> {
                try {
                    payrollService.calculatePayroll(payroll); // triggers recalculation
                    Notification.show("Recalculated successfully for " + payroll.getPerson().getFirstName(), 3000, Notification.Position.MIDDLE);
                    applyFilters(); // optional: refresh to show updated Net THP if displayed
                } catch (Exception ex) {
                    Notification.show("Recalculation failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                    ex.printStackTrace();
                }
            });
            recalculateBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            recalculateBtn.setAriaLabel("Recalculate Payroll");

            actions.add(detailBtn, recalculateBtn);
            actions.setSpacing(true);
            return actions;
        }).setHeader("Actions").setWidth("200px");

        // Search Field
        searchField.setPlaceholder("Search");
        searchField.setWidth("50%");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
//        searchField.addValueChangeListener(e -> refreshGrid(e.getValue()));

        // Add Button
        Button addButton = new Button("Add Payroll", e -> openAddEditDialog(null));
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // === Year & Month Filters ===
        yearFilter.setPlaceholder("Year");
        yearFilter.setItems(getRecentYears(5));
        yearFilter.setValue(LocalDate.now().getYear());
        yearFilter.setClearButtonVisible(true);
        yearFilter.setWidth("120px");
//        yearFilter.addValueChangeListener(e -> applyFilters());

        monthFilter.setPlaceholder("Month");
        monthFilter.setItems(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        monthFilter.setItemLabelGenerator(this::getMonthName);
        monthFilter.setClearButtonVisible(true);
        monthFilter.setWidth("140px");
//        monthFilter.addValueChangeListener(e -> applyFilters());

        HorizontalLayout filterBar = new HorizontalLayout(yearFilter, monthFilter);
        filterBar.setSpacing(true);

        // Search Button
        Button searchButton = new Button(new Icon(VaadinIcon.SEARCH), e -> applyFilters());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Reset Button
        Button resetButton = new Button(new Icon(VaadinIcon.RECYCLE), e -> {
            reselFilter();
            applyFilters();
        });
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Toolbar
        HorizontalLayout toolbar = new HorizontalLayout(filterBar, searchField, searchButton, resetButton, addButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbar.expand(addButton);

        // Layout
        VerticalLayout layout = new VerticalLayout(toolbar, grid);
        layout.setHeightFull();
        layout.setPadding(false);
        layout.setSpacing(false);
        grid.setSizeFull();

        add(layout);

        // Initial load
        applyFilters();
    }

    private void reselFilter(){
        yearFilter.clear();
        monthFilter.clear();
        searchField.clear();
    }

    private void applyFilters() {
        Integer selectedYear = yearFilter.getValue();
        Integer selectedMonth = monthFilter.getValue();
        String searchTerm = searchField.getValue();

        if (selectedYear != null && selectedMonth != null) {
            // Filter by specific year-month
            LocalDate filterDate = LocalDate.of(selectedYear, selectedMonth, 1);
            grid.setItems(payrollService.getPayrollPage(PageRequest.of(0, 50), selectedYear, filterDate, searchTerm).getContent());
        } else if (selectedYear != null) {
            // Filter by year only
            grid.setItems(payrollService.getPayrollPage(PageRequest.of(0, 50), selectedYear, null, searchTerm).getContent());
        } else {
            // Default: no year/month filter, just search
            grid.setItems(payrollService.getPayrollPage(PageRequest.of(0, 50), null, null, searchTerm).getContent());
        }
    }

    private void openDetailDialog(HrPayroll payroll) {
        detailDialog.removeAll();

        HrPayrollCalculation calc = payrollService.getCalculationByPayrollId(payroll.getId());

        VerticalLayout content = new VerticalLayout();
        content.setPadding(true);
        content.setSpacing(true);

        content.add(new H3("Payroll Detail"));
        content.add(new Paragraph("Employee: " + payroll.getPerson().getFirstName() + " " + payroll.getPerson().getLastName()));
        content.add(new Paragraph("Month: " + payroll.getPayrollMonth()));
        content.add(new Hr());

        if (calc != null) {
            content.add(new H4("Calculated Breakdown"));
            content.add(createDetailRow("Gross Salary", calc.getGrossSalary()));
            content.add(createDetailRow("BPJS Health", calc.getBpjsHealthDeduction()));
            content.add(createDetailRow("BPJS JHT", calc.getBpjsJhtDeduction()));
            content.add(createDetailRow("BPJS JP", calc.getBpjsJpDeduction()));
            content.add(createDetailRow("PPh 21", calc.getPph21Amount()));
            content.add(createDetailRow("Other Deductions", payroll.getOtherDeductions()));
            content.add(createDetailRow("Previous THP Paid", payroll.getPreviousThpPaid()));
            content.add(new Hr());
            content.add(createDetailRow("NET TAKE HOME PAY", calc.getNetTakeHomePay(), true));
        } else {
            content.add(new Span("No calculation data available yet."));
        }

        Button closeButton = new Button("Close", e -> detailDialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        VerticalLayout dialogLayout = new VerticalLayout(content, closeButton);
        dialogLayout.setPadding(true);

        detailDialog.add(dialogLayout);
        detailDialog.setWidth("600px");
        detailDialog.open();
    }

    private HorizontalLayout createDetailRow(String label, Number value) {
        return createDetailRow(label, value, false);
    }

    private HorizontalLayout createDetailRow(String label, Number value, boolean bold) {
        Span lbl = new Span(label + ":");
        Span val = new Span("Rp " + (value != null ? String.format("%,.2f", value) : "0.00"));

        if (bold) {
            val.getStyle().set("font-weight", "bold");
            val.getStyle().set("font-size", "1.2em");
        }

        HorizontalLayout row = new HorizontalLayout(lbl, val);
        row.setWidthFull();
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        return row;
    }

    private void openAddEditDialog(HrPayroll payroll) {
        addEditDialog.removeAll();
        mObject.setValue(payroll);

        // Fetch employees for dropdown
        List<HrPerson> employees = payrollService.getActiveEmployees();

        PayrollForm form = new PayrollForm(payrollService, employees, currentUser, () -> {
            addEditDialog.close();
            applyFilters();
        });

        if (payroll != null) {
            form.setPayroll(payroll);
        }

        // Override cancel behavior
        form.getCancelButton().getElement().addEventListener("click", e -> addEditDialog.close());

        addEditDialog.add(form);
        addEditDialog.setWidth("80%");
        addEditDialog.setHeight("60%");
        addEditDialog.open();
    }

    private List<Integer> getRecentYears(int pastYears) {
        int currentYear = LocalDate.now().getYear();
        List<Integer> years = new ArrayList<>();
        for (int i = pastYears; i >= 0; i--) {
            years.add(currentYear - i);
        }
        return years;
    }

    private String getMonthName(Integer month) {
        if (month == null) return "";
        return Month.of(month).getDisplayName(TextStyle.FULL, Locale.ENGLISH);
    }

    public static class PayrollForm extends FormLayout {

        private final PayrollService payrollService;
        private final Runnable onSaveSuccess;

        private final ComboBox<HrPerson> personField = new ComboBox<>("Employee");
        private final DatePicker payrollMonthField = new DatePicker("Payroll Month");
        private final BigDecimalField variableAllowancesField = new BigDecimalField("Variable Allowances");
        private final BigDecimalField overtimeHoursField = new BigDecimalField("Overtime Hours");
        private final BigDecimalField overtimeAmountField = new BigDecimalField("Overtime Amount");
        private final BigDecimalField annualBonusField = new BigDecimalField("Annual Bonus");
        private final BigDecimalField otherDeductionsField = new BigDecimalField("Other Deductions");
        private final BigDecimalField previousThpPaidField = new BigDecimalField("Previous THP Paid");
        private final IntegerField attendanceDaysField = new IntegerField("Attendance Days");
        private final Button saveButton = new Button("Save");
        private final Button cancelButton = new Button("Cancel");

        private final Binder<HrPayroll> binder = new BeanValidationBinder<>(HrPayroll.class);
        private final CurrentUser currentUser;
        private List<HrPerson> employees;

        public PayrollForm(PayrollService payrollService, List<HrPerson> employees, CurrentUser currentUser, Runnable onSaveSuccess) {
            this.payrollService = payrollService;
            this.onSaveSuccess = onSaveSuccess;
            this.employees = employees;
            this.currentUser = currentUser;

            configureFields();
            configureBinder();
            configureLayout();
            configureActions();
        }

        private void configureFields() {
            personField.setItems(employees);
            personField.setItemLabelGenerator(p -> p.getFirstName() + " " + p.getLastName());
            personField.setRequired(true);
            personField.setClearButtonVisible(true);
            personField.setWidthFull();

            payrollMonthField.setRequired(true);
            payrollMonthField.setPlaceholder("Select month");
            payrollMonthField.setWidthFull();

            variableAllowancesField.setPrefixComponent(new Div("Rp"));
            variableAllowancesField.setWidthFull();
//            variableAllowancesField.setMin(BigDecimal.ZERO);
//            variableAllowancesField.setStep(BigDecimal.valueOf(1000));

//            overtimeHoursField.setMin(BigDecimal.ZERO);
//            overtimeHoursField.setMax(BigDecimal.valueOf(200));
//            overtimeHoursField.setStep(BigDecimal.ONE);

            overtimeAmountField.setPrefixComponent(new Div("Rp"));
            overtimeAmountField.setWidthFull();
//            overtimeAmountField.setMin(BigDecimal.ZERO);

            annualBonusField.setPrefixComponent(new Div("Rp"));
            annualBonusField.setWidthFull();
//            annualBonusField.setMin(BigDecimal.ZERO);

            otherDeductionsField.setPrefixComponent(new Div("Rp"));
            otherDeductionsField.setWidthFull();
//            otherDeductionsField.setMin(BigDecimal.ZERO);

            previousThpPaidField.setPrefixComponent(new Div("Rp"));
            previousThpPaidField.setWidthFull();
//            previousThpPaidField.setMin(BigDecimal.ZERO);

            attendanceDaysField.setMin(0);
            attendanceDaysField.setMax(31);
            attendanceDaysField.setWidthFull();
        }

        private void configureBinder() {
            binder.forField(personField).asRequired("Employee is required").bind(HrPayroll::getPerson, HrPayroll::setPerson);
            binder.forField(payrollMonthField).asRequired("Payroll month is required").bind(HrPayroll::getPayrollMonth, HrPayroll::setPayrollMonth);
            binder.bind(variableAllowancesField, HrPayroll::getVariableAllowances, HrPayroll::setVariableAllowances);
            binder.bind(overtimeHoursField, HrPayroll::getOvertimeHours, HrPayroll::setOvertimeHours);
            binder.bind(overtimeAmountField, HrPayroll::getOvertimeAmount, HrPayroll::setOvertimeAmount);
            binder.bind(annualBonusField, HrPayroll::getAnnualBonus, HrPayroll::setAnnualBonus);
            binder.bind(otherDeductionsField, HrPayroll::getOtherDeductions, HrPayroll::setOtherDeductions);
            binder.bind(previousThpPaidField, HrPayroll::getPreviousThpPaid, HrPayroll::setPreviousThpPaid);
            binder.bind(attendanceDaysField, HrPayroll::getAttendanceDays, HrPayroll::setAttendanceDays);

            binder.readBean(null); // clear
        }

        private void configureLayout() {
            addFormItem(personField, "Employee");
            addFormItem(payrollMonthField, "Payroll Month");
            addFormItem(variableAllowancesField, "Variable Allowances");
            addFormItem(overtimeHoursField, "Overtime Hours");
            addFormItem(overtimeAmountField, "Overtime Amount");
            addFormItem(annualBonusField, "Annual Bonus");
            addFormItem(otherDeductionsField, "Other Deductions");
            addFormItem(previousThpPaidField, "Previous THP Paid");
            addFormItem(attendanceDaysField, "Attendance Days");

            HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);
            add(buttons);

//            setResponsiveSteps(
//                    new ResponsiveStep("0", 1),
//                    new ResponsiveStep("500px", 2)
//            );

//            setMaxWidth("800px");
        }

        private void configureActions() {
            saveButton.addClickListener(e -> {
                if (binder.writeBeanIfValid(getPayroll())) {
                    try {
                        payrollService.savePayroll(getPayroll(), this.currentUser.require()); // See STEP 2 below
                        Notification.show("Payroll saved successfully", 3000, Notification.Position.MIDDLE);
                        onSaveSuccess.run();
                    } catch (Exception ex) {
                        Notification.show("Error saving payroll: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                        ex.printStackTrace();
                    }
                }
            });

            cancelButton.addClickListener(e -> onCancel());
        }

        public void setPayroll(HrPayroll payroll) {
            binder.readBean(payroll);
        }

        public HrPayroll getPayroll() {
            HrPayroll payroll = binder.getBean();
            if (payroll == null) {
                payroll = new HrPayroll();
                binder.setBean(payroll);
            }
            return payroll;
        }

        public void onCancel() {
            // Close dialog from parent
        }

        public Component getSaveButton() {
            return saveButton;
        }

        public Component getCancelButton() {
            return cancelButton;
        }
    }
}
