package com.fusi24.pangreksa.web.view.manager;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.base.util.FormattingUtils;
import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.HrPayroll;
import com.fusi24.pangreksa.web.model.entity.HrPayrollCalculation;
import com.fusi24.pangreksa.web.model.entity.HrSalaryAllowance;
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.PayrollService;
import com.fusi24.pangreksa.web.service.SystemService;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.provider.QuerySortOrder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.lang3.mutable.MutableObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;


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

    private final Button deleteButton = new Button("Delete");
    private Grid<HrPayroll> grid = new Grid<>(HrPayroll.class, false);
    private TextField searchField = new TextField();

    private ComboBox<Integer> yearFilter = new ComboBox<>();
    private ComboBox<Integer> monthFilter = new ComboBox<>();

    private Dialog addPayrollDialog = new Dialog();

    MutableObject<HrPayroll> mObject = new MutableObject<>();

    // Untuk numbering "No" yang stabil lintas pagination
    private final Map<Long, Integer> rowNoByPayrollId = new ConcurrentHashMap<>();

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

        // =========================
        // Grid Columns (NEW LOGIC)
        // =========================

        // 1) No (Looping logic) - stable with pagination
        grid.addComponentColumn(payroll -> {
            Long id = payroll.getId();
            Integer no = id == null ? null : rowNoByPayrollId.get(id);
            return new Span(no == null ? "-" : String.valueOf(no));
        }).setHeader("No").setWidth("70px").setFlexGrow(0);

        // 2) Employee (dari hr_payroll.first_name + last_name)
        grid.addColumn(payroll -> {
            String fn = payroll.getFirstName() == null ? "" : payroll.getFirstName();
            String ln = payroll.getLastName() == null ? "" : payroll.getLastName();
            String full = (fn + " " + ln).trim();
            return full.isBlank() ? "-" : full;
        }).setHeader("Employee").setAutoWidth(true).setFlexGrow(1);

        // 3) Payroll Month (dari hr_payroll.payroll_date)
        grid.addColumn(payroll -> formatPayrollMonth(payroll.getPayrollDate()))
                .setHeader("Payroll Month")
                .setWidth("160px")
                .setFlexGrow(0);

        // 3.1) Gross Salary
        grid.addColumn(payroll -> {
            HrPayrollCalculation calc = payroll.getCalculation();
            BigDecimal val = calc == null ? BigDecimal.ZERO : nvl(calc.getGrossSalary());
            return FormattingUtils.formatPayrollAmount(val);
        }).setHeader("Gross Salary").setWidth("160px").setFlexGrow(0);

        // 4) Total Allowance (hr_payroll_calculations.total_allowances)
        grid.addColumn(payroll -> {
            HrPayrollCalculation calc = payroll.getCalculation();
            BigDecimal val = calc == null ? BigDecimal.ZERO : nvl(calc.getTotalAllowances());
            return FormattingUtils.formatPayrollAmount(val);
        }).setHeader("Total Allowance").setWidth("160px").setFlexGrow(0);

        // 5) Total Overtime (hr_payroll_calculations.total_overtimes)
        grid.addColumn(payroll -> {
            HrPayrollCalculation calc = payroll.getCalculation();
            BigDecimal val = calc == null ? BigDecimal.ZERO : nvl(calc.getTotalOvertimes());
            return FormattingUtils.formatPayrollAmount(val);
        }).setHeader("Total Overtime").setWidth("160px").setFlexGrow(0);

        // 6) Total Bonus (hr_payroll_calculations.total_bonus / 12)
        grid.addColumn(payroll -> {
            HrPayrollCalculation calc = payroll.getCalculation();
            BigDecimal annual = calc == null ? BigDecimal.ZERO : nvl(calc.getTotalBonus());
            BigDecimal perMonth = annual.divide(BigDecimal.valueOf(12), 2, RoundingMode.HALF_UP);
            return FormattingUtils.formatPayrollAmount(perMonth);
        }).setHeader("Total Bonus").setWidth("150px").setFlexGrow(0);

        // 7) Other Deduct. (hr_payroll_calculations.total_other_deductions)
        grid.addColumn(payroll -> {
            HrPayrollCalculation calc = payroll.getCalculation();
            BigDecimal val = calc == null ? BigDecimal.ZERO : nvl(calc.getTotalOtherDeductions());
            return FormattingUtils.formatPayrollAmount(val);
        }).setHeader("Other Deduct.").setWidth("160px").setFlexGrow(0);

        // 8) Net THP (hr_payroll_calculations.net_take_home_pay)
        grid.addColumn(payroll -> {
            HrPayrollCalculation calc = payroll.getCalculation();
            BigDecimal val = calc == null ? BigDecimal.ZERO : nvl(calc.getNetTakeHomePay());
            return FormattingUtils.formatPayrollAmount(val);
        }).setHeader("Net THP").setWidth("140px").setFlexGrow(0);

        // 9) Actions (ONLY: Recalculate + Add Komponen)
        grid.addComponentColumn(payroll -> {
            HorizontalLayout actions = new HorizontalLayout();

            Button recalculateBtn = new Button("Recalculate", e -> openRecalculateDialog(payroll));
            recalculateBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

            actions.add(recalculateBtn);
            return actions;
        }).setHeader("Actions").setWidth("160px").setFlexGrow(0);

        // =========================
        // Search / Filter toolbar
        // =========================

        // Search Field (employee)
        searchField.setPlaceholder("Search employee");
        searchField.setWidth("40%");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.EAGER);

        // Add Payroll Button
        Button addButton = new Button("Add Payroll", e -> openAddPayrollDialog());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Year Filter
        yearFilter.setPlaceholder("Year");
        yearFilter.setItems(getRecentYears(5));
        yearFilter.setValue(LocalDate.now().getYear());
        yearFilter.setClearButtonVisible(true);
        yearFilter.setWidth("120px");

        // Month Filter
        monthFilter.setPlaceholder("Month");
        monthFilter.setItems(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        monthFilter.setItemLabelGenerator(this::getMonthName);
        monthFilter.setClearButtonVisible(true);
        monthFilter.setWidth("150px");

        HorizontalLayout filterBar = new HorizontalLayout(yearFilter, monthFilter);
        filterBar.setSpacing(true);

        // Search Button
        Button searchButton = new Button(new Icon(VaadinIcon.SEARCH), e -> applyFilters());
        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        // Reset Button
        Button resetButton = new Button(new Icon(VaadinIcon.RECYCLE), e -> {
            reselFilter();
            applyFilters();
        });
        resetButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        // Toolbar layout: kiri filter+search+reset, kanan Add Payroll
        HorizontalLayout toolbarLeft = new HorizontalLayout(filterBar, searchField, searchButton, resetButton);
        toolbarLeft.setAlignItems(FlexComponent.Alignment.END);
        toolbarLeft.setSpacing(true);

        HorizontalLayout toolbarRight = new HorizontalLayout(deleteButton, addButton);
        toolbarRight.setAlignItems(FlexComponent.Alignment.END);
        toolbarRight.setSpacing(true);

        HorizontalLayout toolbar = new HorizontalLayout(toolbarLeft, toolbarRight);
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.END);
        toolbar.expand(toolbarLeft);

        // Layout
        VerticalLayout layout = new VerticalLayout(toolbar, grid);
        layout.setHeightFull();
        layout.setPadding(false);
        layout.setSpacing(false);
        grid.setSizeFull();

        grid.setSelectionMode(Grid.SelectionMode.MULTI);

        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.setVisible(false);

        grid.addSelectionListener(e -> {
            deleteButton.setVisible(!grid.getSelectedItems().isEmpty());
        });

        deleteButton.addClickListener(e -> {
            List<Long> ids = grid.getSelectedItems().stream()
                    .map(HrPayroll::getId)
                    .filter(Objects::nonNull)
                    .toList();

            if (ids.isEmpty()) {
                Notification.show("No data selected", 2500, Notification.Position.MIDDLE);
                return;
            }

            runWithPreloading("Deleting payroll...", () -> {
                payrollService.deletePayrolls(ids);
            }, () -> {
                grid.deselectAll();
                applyFilters();
                deleteButton.setVisible(false);
                Notification.show("Deleted " + ids.size() + " payroll(s)", 3000, Notification.Position.MIDDLE);
            });
        });

        add(layout);

        // Initial load
        applyFilters();
    }

    private void reselFilter(){
        yearFilter.clear();
        monthFilter.clear();
        searchField.clear();
    }

    private Dialog recalculateDialog = new Dialog();

    private void openRecalculateDialog(HrPayroll payroll) {
        recalculateDialog.removeAll();

        RecalculateForm form = new RecalculateForm(
                payrollService,
                currentUser,
                this::runWithPreloading,     // inject loader
                payroll,
                () -> {
                    recalculateDialog.close();
                    applyFilters();
                }
        );

        recalculateDialog.add(form);
        recalculateDialog.setWidth("900px");
        recalculateDialog.open();
    }

    public static class RecalculateForm extends FormLayout {
        private final PayrollService payrollService;
        private final CurrentUser currentUser;
        private final HrPayroll payroll;
        private final Runnable onSaveSuccess;

        // reuse fields like AddPayrollForm
        private final ComboBox<Integer> yearField = new ComboBox<>();
        private final ComboBox<Integer> monthField = new ComboBox<>();
        private final IntegerField attendanceDaysField = new IntegerField();
        private final IntegerField overtimeMinutesField = new IntegerField();

        private final RadioButtonGroup<String> overtimePaymentType = new RadioButtonGroup<>();
        private final BigDecimalField overtimeStaticNominal = new BigDecimalField();
        private final IntegerField overtimePercent = new IntegerField();

        private final RadioButtonGroup<String> allowanceMode = new RadioButtonGroup<>();
        private final MultiSelectComboBox<HrSalaryAllowance> allowanceMultiSelect = new MultiSelectComboBox<>();

        // Add Component fields
        private final BigDecimalField totalBonusField = new BigDecimalField();
        private final BigDecimalField totalOtherDedField = new BigDecimalField();
        private final BigDecimalField totalTaxableField = new BigDecimalField();

        private final Button saveButton = new Button("Recalculate");
        private final Button cancelButton = new Button("Cancel");

        private final Binder<PayrollService.AddPayrollRequest> binder =
                new BeanValidationBinder<>(PayrollService.AddPayrollRequest.class);

        private final UiLoader loader;

        public RecalculateForm(PayrollService payrollService, CurrentUser currentUser, UiLoader loader,
                               HrPayroll payroll, Runnable onSaveSuccess) {
            this.payrollService = payrollService;
            this.currentUser = currentUser;
            this.loader = loader;
            this.payroll = payroll;
            this.onSaveSuccess = onSaveSuccess;

            configureFields();
            configureBinder();
            configureLayout();
            configureActions();
        }

        private void configureFields() {
            LocalDate d = payroll.getPayrollDate();

            yearField.setItems(d.getYear());
            yearField.setValue(d.getYear());
            yearField.setEnabled(false);
            yearField.setWidthFull();

            monthField.setItems(d.getMonthValue());
            monthField.setValue(d.getMonthValue());
            monthField.setEnabled(false);
            monthField.setWidthFull();

            attendanceDaysField.setMin(0);
            attendanceDaysField.setMax(31);
            attendanceDaysField.setWidthFull();

            overtimeMinutesField.setMin(0);
            overtimeMinutesField.setMax(60);
            overtimeMinutesField.setWidthFull();

            overtimePaymentType.setItems("STATIC", "PERCENTAGE");
            overtimePaymentType.setValue("STATIC");
            overtimePaymentType.setWidthFull();

            overtimeStaticNominal.setWidthFull();
            overtimePercent.setMin(0);
            overtimePercent.setMax(100);
            overtimePercent.setWidthFull();

            overtimeStaticNominal.setVisible(true);
            overtimePercent.setVisible(false);
            overtimePaymentType.addValueChangeListener(e -> {
                boolean isStatic = "STATIC".equals(e.getValue());
                overtimeStaticNominal.setVisible(isStatic);
                overtimePercent.setVisible(!isStatic);
            });

            allowanceMode.setItems("NO ALLOWANCE", "SELECT ALLOWANCE", "BENEFITS PACKAGE");
            allowanceMode.setValue("NO ALLOWANCE");
            allowanceMode.setWidthFull();

            allowanceMultiSelect.setVisible(false);
            allowanceMultiSelect.setWidthFull();

            allowanceMode.addValueChangeListener(e -> {
                allowanceMultiSelect.setVisible("SELECT ALLOWANCE".equals(e.getValue()));
            });

            // Add component fields
            totalBonusField.setPlaceholder("Rp 0");
            totalOtherDedField.setPlaceholder("Rp 0");
            totalTaxableField.setPlaceholder("Rp 0");

            saveButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        }

        private void configureBinder() {
            PayrollService.AddPayrollRequest bean = new PayrollService.AddPayrollRequest();
            bean.setYear(yearField.getValue());
            bean.setMonth(monthField.getValue());
            bean.setParamAttendanceDays(payroll.getParamAttendanceDays() == null ? 0 : payroll.getParamAttendanceDays());
            bean.setOvertimeMinutes(payroll.getOvertimeHours() == null ? 0 : payroll.getOvertimeHours().intValue());
            bean.setOvertimePaymentType(payroll.getOvertimeType() == null ? "STATIC" : payroll.getOvertimeType());
            bean.setAllowanceMode(payroll.getAllowancesType() == null ? "NO ALLOWANCE" : payroll.getAllowancesType());

            binder.setBean(bean);

            binder.forField(attendanceDaysField)
                    .bind(PayrollService.AddPayrollRequest::getParamAttendanceDays, PayrollService.AddPayrollRequest::setParamAttendanceDays);

            binder.forField(overtimeMinutesField)
                    .bind(PayrollService.AddPayrollRequest::getOvertimeMinutes, PayrollService.AddPayrollRequest::setOvertimeMinutes);

            binder.forField(overtimePaymentType)
                    .bind(PayrollService.AddPayrollRequest::getOvertimePaymentType, PayrollService.AddPayrollRequest::setOvertimePaymentType);

            binder.forField(overtimeStaticNominal)
                    .bind(PayrollService.AddPayrollRequest::getOvertimeStaticNominal, PayrollService.AddPayrollRequest::setOvertimeStaticNominal);

            binder.forField(overtimePercent)
                    .bind(PayrollService.AddPayrollRequest::getOvertimePercent, PayrollService.AddPayrollRequest::setOvertimePercent);

            binder.forField(allowanceMode)
                    .bind(PayrollService.AddPayrollRequest::getAllowanceMode, PayrollService.AddPayrollRequest::setAllowanceMode);
        }

        private void configureLayout() {
            setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("700px", 2));

            Span s1 = new Span("Periode Payroll");
            s1.getStyle().set("font-weight", "600");
            add(s1);
            setColspan(s1, 2);

            addFormItem(yearField, "Year");
            addFormItem(monthField, "Month");

            Span s2 = new Span("Parameter Attendance & Overtime");
            s2.getStyle().set("font-weight", "600");
            add(s2);
            setColspan(s2, 2);

            addFormItem(attendanceDaysField, "Total Hari Kerja (input manual)");
            addFormItem(overtimeMinutesField, "Overtime dihitung (menit 0-60)");

            addFormItem(overtimePaymentType, "Bayaran Overtime");
            addFormItem(overtimeStaticNominal, "Nominal Upah (Rp)");
            addFormItem(overtimePercent, "Persentase (Percentage)");

            Span s3 = new Span("Allowance");
            s3.getStyle().set("font-weight", "600");
            add(s3);
            setColspan(s3, 2);

            addFormItem(allowanceMode, "Allowance Option");
            FormItem fiAllow = addFormItem(allowanceMultiSelect, "Select Allowance");
            setColspan(fiAllow, 2);

            Span s4 = new Span("Add Component");
            s4.getStyle().set("font-weight", "600");
            add(s4);
            setColspan(s4, 2);

            addFormItem(totalBonusField, "Total Bonus (annual)");
            addFormItem(totalOtherDedField, "Total Other Deductions");
            FormItem fiTax = addFormItem(totalTaxableField, "Total Taxable");
            setColspan(fiTax, 2);

            HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);
            buttons.setWidthFull();
            buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
            add(buttons);
            setColspan(buttons, 2);
        }

        private void configureActions() {
            saveButton.addClickListener(e -> {
                PayrollService.AddPayrollRequest bean = binder.getBean();

                if ("SELECT ALLOWANCE".equals(bean.getAllowanceMode())) {
                    bean.setSelectedAllowances(new ArrayList<>(allowanceMultiSelect.getSelectedItems()));
                } else {
                    bean.setSelectedAllowances(new ArrayList<>());
                }

                if (binder.writeBeanIfValid(bean)) {
                    BigDecimal bonus = totalBonusField.getValue() == null ? BigDecimal.ZERO : totalBonusField.getValue();
                    BigDecimal otherDed = totalOtherDedField.getValue() == null ? BigDecimal.ZERO : totalOtherDedField.getValue();
                    BigDecimal taxable = totalTaxableField.getValue() == null ? BigDecimal.ZERO : totalTaxableField.getValue();

                    // gunakan preloading pattern dari view (lihat helper di bawah)
                    // Karena inner class static, kamu bisa panggil via callback yang diberikan dari view,
                    // atau implement preloading di view dan pass sebagai lambda.
                    loader.run("Recalculating payroll...", () -> {
                        payrollService.recalculateSinglePayroll(
                                payroll.getId(),
                                bean,
                                bonus,
                                otherDed,
                                taxable,
                                currentUser.require()
                        );
                    }, () -> {
                        Notification.show("Recalculated successfully", 3000, Notification.Position.MIDDLE);
                        onSaveSuccess.run();
                    });


                    Notification.show("Recalculated successfully", 3000, Notification.Position.MIDDLE);
                    onSaveSuccess.run();
                }
            });

            cancelButton.addClickListener(e -> onSaveSuccess.run());
        }
    }

    private void applyFilters() {
        Integer selectedYear = yearFilter.getValue();
        Integer selectedMonth = monthFilter.getValue();
        String searchTerm = searchField.getValue();

        MutableObject<LocalDate> mFilterDate = new MutableObject<>();
        if (selectedYear != null && selectedMonth != null) {
            mFilterDate.setValue(LocalDate.of(selectedYear, selectedMonth, 1));
        }

        // clear numbering cache setiap applyFilters
        rowNoByPayrollId.clear();

        DataProvider<HrPayroll, Void> dataProvider = DataProvider.fromCallbacks(
                query -> {
                    int offset = query.getOffset();
                    int limit = query.getLimit();

                    List<QuerySortOrder> sortOrders = query.getSortOrders();

                    int page = offset / limit;
                    PageRequest pageRequest = PageRequest.of(page, limit);

                    List<HrPayroll> items = payrollService.getPayrollPage(pageRequest, selectedYear, mFilterDate.getValue(), searchTerm)
                            .getContent();

                    // isi nomor urut berdasarkan offset halaman
                    for (int i = 0; i < items.size(); i++) {
                        HrPayroll p = items.get(i);
                        if (p.getId() != null) {
                            rowNoByPayrollId.put(p.getId(), offset + i + 1);
                        }
                    }

                    return items.stream();
                },
                query -> (int) payrollService.countPayroll(selectedYear, mFilterDate.getValue(), searchTerm)
        );

        grid.setDataProvider(dataProvider);
    }

    private void openAddPayrollDialog() {
        addPayrollDialog.removeAll();

        AddPayrollForm form = new AddPayrollForm(
                payrollService,
                currentUser,
                this::runWithPreloading,   // <-- inject loader
                () -> {
                    addPayrollDialog.close();
                    applyFilters();
                }
        );


        addPayrollDialog.add(form);
        addPayrollDialog.setWidth("900px");
        addPayrollDialog.open();
    }

    public static class AddPayrollForm extends FormLayout {

        private final PayrollService payrollService;
        private final CurrentUser currentUser;
        private final PreloadRunner preloadRunner;
        private final Runnable onSaveSuccess;

        private final ComboBox<Integer> yearField = new ComboBox<>();
        private final ComboBox<Integer> monthField = new ComboBox<>();

        private final IntegerField attendanceDaysField = new IntegerField();
        private final IntegerField overtimeMinutesField = new IntegerField();

        private final RadioButtonGroup<String> overtimePaymentType = new RadioButtonGroup<>();
        private final BigDecimalField overtimeStaticNominal = new BigDecimalField();
        private final IntegerField overtimePercent = new IntegerField();

        private final RadioButtonGroup<String> allowanceMode = new RadioButtonGroup<>();
        private final MultiSelectComboBox<HrSalaryAllowance> allowanceMultiSelect = new MultiSelectComboBox<>();

        private final Button saveButton = new Button("Save");
        private final Button cancelButton = new Button("Cancel");

        private final Binder<PayrollService.AddPayrollRequest> binder =
                new BeanValidationBinder<>(PayrollService.AddPayrollRequest.class);

        public AddPayrollForm(
                PayrollService payrollService,
                CurrentUser currentUser,
                PreloadRunner preloadRunner,
                Runnable onSaveSuccess
        ) {
            this.payrollService = payrollService;
            this.currentUser = currentUser;
            this.preloadRunner = preloadRunner;
            this.onSaveSuccess = onSaveSuccess;

            configureFields();
            configureBinder();
            configureLayout();
            configureActions();
        }
        private Dialog openProcessingDialog() {
            Dialog d = new Dialog();
            d.setCloseOnEsc(false);
            d.setCloseOnOutsideClick(false);
            d.setModal(true);

            com.vaadin.flow.component.progressbar.ProgressBar bar = new com.vaadin.flow.component.progressbar.ProgressBar();
            bar.setIndeterminate(true);

            VerticalLayout vl = new VerticalLayout(new Span("Processing payroll..."), bar);
            vl.setPadding(true);
            vl.setSpacing(true);
            d.add(vl);
            d.open();
            return d;
        }

        private void configureFields() {
            int y = LocalDate.now().getYear();

            yearField.setPlaceholder("Year");
            yearField.setItems(y - 5, y - 4, y - 3, y - 2, y - 1, y, y + 1);
            yearField.setValue(y);
            yearField.setWidthFull();

            monthField.setPlaceholder("Month");
            monthField.setItems(1,2,3,4,5,6,7,8,9,10,11,12);
            monthField.setItemLabelGenerator(m -> Month.of(m).getDisplayName(TextStyle.FULL, new Locale("id", "ID")));
            monthField.setValue(LocalDate.now().getMonthValue());
            monthField.setWidthFull();

            attendanceDaysField.setPlaceholder("0 - 31");
            attendanceDaysField.setMin(0);
            attendanceDaysField.setMax(31);
            attendanceDaysField.setValue(null);
            attendanceDaysField.setWidthFull();

            overtimeMinutesField.setPlaceholder("0 - 60");
            overtimeMinutesField.setMin(0);
            overtimeMinutesField.setMax(60);
            overtimeMinutesField.setValue(null);
            overtimeMinutesField.setWidthFull();

            overtimePaymentType.setLabel(null); // label kita taruh di FormItem
            overtimePaymentType.setItems("STATIC", "PERCENTAGE");
            overtimePaymentType.setValue("STATIC");
            overtimePaymentType.setWidthFull();

            overtimeStaticNominal.setPlaceholder("Rp 0");
            overtimeStaticNominal.setWidthFull();
            overtimeStaticNominal.setValue(null);

            overtimePercent.setPlaceholder("0 - 100 (%)");
            overtimePercent.setMin(0);
            overtimePercent.setMax(100);
            overtimePercent.setValue(null);
            overtimePercent.setWidthFull();

            overtimeStaticNominal.setVisible(true);
            overtimePercent.setVisible(false);

            overtimePaymentType.addValueChangeListener(e -> {
                boolean isStatic = "STATIC".equals(e.getValue());
                overtimeStaticNominal.setVisible(isStatic);
                overtimePercent.setVisible(!isStatic);
            });

            allowanceMode.setLabel(null);
            allowanceMode.setItems("NO ALLOWANCE", "SELECT ALLOWANCE", "BENEFITS PACKAGE");
            allowanceMode.setValue("NO ALLOWANCE");
            allowanceMode.setWidthFull();

            allowanceMultiSelect.setPlaceholder("Select one or more allowances");
            allowanceMultiSelect.setWidthFull();
            allowanceMultiSelect.setVisible(false);

            allowanceMultiSelect.setItemLabelGenerator(a ->
                    a.getName() + " (Rp " + FormattingUtils.formatPayrollAmount(a.getAmount()) + ")"
            );

            refreshAllowanceOptions();

            yearField.addValueChangeListener(e -> refreshAllowanceOptions());
            monthField.addValueChangeListener(e -> refreshAllowanceOptions());

            allowanceMode.addValueChangeListener(e -> {
                boolean show = "SELECT ALLOWANCE".equals(e.getValue());
                allowanceMultiSelect.setVisible(show);
            });

            saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        }

        private void refreshAllowanceOptions() {
            LocalDate payrollDate = LocalDate.of(yearField.getValue(), monthField.getValue(), 1);
            allowanceMultiSelect.setItems(payrollService.getSelectableAllowancesForPayrollDate(payrollDate));
        }

        private void configureBinder() {
            PayrollService.AddPayrollRequest bean = new PayrollService.AddPayrollRequest();
            bean.setYear(yearField.getValue());
            bean.setMonth(monthField.getValue());
            bean.setParamAttendanceDays(0);
            bean.setOvertimeMinutes(0);
            bean.setOvertimePaymentType("STATIC");
            bean.setOvertimeStaticNominal(BigDecimal.ZERO);
            bean.setOvertimePercent(0);
            bean.setAllowanceMode("NO ALLOWANCE");

            binder.setBean(bean);

            binder.forField(yearField).asRequired("Year required")
                    .bind(PayrollService.AddPayrollRequest::getYear, PayrollService.AddPayrollRequest::setYear);

            binder.forField(monthField).asRequired("Month required")
                    .bind(PayrollService.AddPayrollRequest::getMonth, PayrollService.AddPayrollRequest::setMonth);

            binder.forField(attendanceDaysField)
                    .bind(PayrollService.AddPayrollRequest::getParamAttendanceDays, PayrollService.AddPayrollRequest::setParamAttendanceDays);

            binder.forField(overtimeMinutesField)
                    .bind(PayrollService.AddPayrollRequest::getOvertimeMinutes, PayrollService.AddPayrollRequest::setOvertimeMinutes);

            binder.forField(overtimePaymentType)
                    .bind(PayrollService.AddPayrollRequest::getOvertimePaymentType, PayrollService.AddPayrollRequest::setOvertimePaymentType);

            binder.forField(overtimeStaticNominal)
                    .bind(PayrollService.AddPayrollRequest::getOvertimeStaticNominal, PayrollService.AddPayrollRequest::setOvertimeStaticNominal);

            binder.forField(overtimePercent)
                    .bind(PayrollService.AddPayrollRequest::getOvertimePercent, PayrollService.AddPayrollRequest::setOvertimePercent);

            binder.forField(allowanceMode)
                    .bind(PayrollService.AddPayrollRequest::getAllowanceMode, PayrollService.AddPayrollRequest::setAllowanceMode);
        }

        private void configureLayout() {
            setWidthFull();

            setResponsiveSteps(
                    new ResponsiveStep("0", 1),
                    new ResponsiveStep("700px", 2)
            );

            // Section: Periode Payroll
            Span s1 = new Span("Periode Payroll");
            s1.getStyle().set("font-weight", "600");
            s1.getStyle().set("margin-top", "0.25rem");

            add(s1);
            setColspan(s1, 2);

            FormItem fiYear = addFormItem(yearField, "Year");
            FormItem fiMonth = addFormItem(monthField, "Month");
            fiYear.getStyle().set("margin-top", "0");
            fiMonth.getStyle().set("margin-top", "0");

            // Section: Parameter
            Span s2 = new Span("Parameter Attendance & Overtime");
            s2.getStyle().set("font-weight", "600");
            s2.getStyle().set("margin-top", "0.75rem");

            add(s2);
            setColspan(s2, 2);

            addFormItem(attendanceDaysField, "Total Hari Kerja (input manual)");
            addFormItem(overtimeMinutesField, "Overtime dihitung (menit 0-60)");

            addFormItem(overtimePaymentType, "Bayaran Overtime");
            // slot kanan untuk field sesuai pilihan (static / percentage)
            // dibuat rapi: kita taruh keduanya, yang tidak aktif hidden
            addFormItem(overtimeStaticNominal, "Nominal Upah (Rp)");
            addFormItem(overtimePercent, "Persentase (Percentage)");

            // Section: Allowance
            Span s3 = new Span("Allowance");
            s3.getStyle().set("font-weight", "600");
            s3.getStyle().set("margin-top", "0.75rem");

            add(s3);
            setColspan(s3, 2);

            addFormItem(allowanceMode, "Allowance Option");

            // Multi select dibuat full row agar enak
            FormItem fiAllow = addFormItem(allowanceMultiSelect, "Select Allowance");
            setColspan(fiAllow, 2);

            HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);
            buttons.setWidthFull();
            buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

            add(buttons);
            setColspan(buttons, 2);
        }

        private void configureActions() {
            saveButton.addClickListener(e -> {
                PayrollService.AddPayrollRequest bean = binder.getBean();

                if (!binder.writeBeanIfValid(bean)) {
                    Notification.show("Form belum valid", 3000, Notification.Position.MIDDLE);
                    return;
                }

                // ⚠️ Ambil user DI UI THREAD
                AppUserInfo userInfo = currentUser.require();

                preloadRunner.run(
                        "Creating payroll...",
                        () -> payrollService.createPayrollBulk(bean, userInfo),
                        () -> {
                            Notification.show("Payroll created successfully", 3000, Notification.Position.MIDDLE);
                            onSaveSuccess.run();
                        }
                );
            });

            cancelButton.addClickListener(e -> onSaveSuccess.run());
        }

    }

    private final java.util.concurrent.ExecutorService executor =
            java.util.concurrent.Executors.newFixedThreadPool(4);

    private void runWithPreloading(String message, Runnable task, Runnable onDoneUi) {
        Dialog loading = new Dialog();
        loading.setModal(true);
        loading.setCloseOnEsc(false);
        loading.setCloseOnOutsideClick(false);

        VerticalLayout box = new VerticalLayout();
        box.setSpacing(true);
        box.add(new Span(message));
        com.vaadin.flow.component.progressbar.ProgressBar bar =
                new com.vaadin.flow.component.progressbar.ProgressBar();
        bar.setIndeterminate(true);
        box.add(bar);

        loading.add(box);
        loading.open();

        UI ui = UI.getCurrent();

        CompletableFuture
                .runAsync(() -> {
                    log.info("PAYROLL TASK START");
                    task.run();
                    log.info("PAYROLL TASK END");
                }, executor)
                .orTimeout(15, java.util.concurrent.TimeUnit.MINUTES)
                .whenComplete((ok, ex) -> {
                    // UI bisa saja sudah detach (user pindah page)
                    if (ui == null || !ui.isAttached()) return;

                    ui.access(() -> {
                        loading.close();

                        if (ex != null) {
                            Throwable root = (ex instanceof java.util.concurrent.CompletionException && ex.getCause() != null)
                                    ? ex.getCause()
                                    : ex;

                            Notification.show("Process failed: " + root.getMessage(),
                                    6000, Notification.Position.MIDDLE);
                            root.printStackTrace();
                        } else {
                            if (onDoneUi != null) onDoneUi.run();
                        }
                    });
                });
    }

    @FunctionalInterface
    public interface PreloadRunner {
        void run(String message, Runnable task, Runnable onDone);
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

    private String formatPayrollMonth(LocalDate payrollDate) {
        if (payrollDate == null) return "-";
        String monthName = payrollDate.getMonth().getDisplayName(TextStyle.FULL, new Locale("id", "ID"));
        return monthName + " " + payrollDate.getYear();
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    @FunctionalInterface
    public interface UiLoader {
        void run(String message, Runnable task, Runnable onDoneUi);
    }

}
