package com.fusi24.pangreksa.web.view.manager;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.base.ui.notification.AppNotification;
import com.fusi24.pangreksa.base.util.FormattingUtils;
import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.HrPayroll;
import com.fusi24.pangreksa.web.model.entity.HrPayrollCalculation;
import com.fusi24.pangreksa.web.model.entity.HrPayrollComponent;
import com.fusi24.pangreksa.web.model.entity.HrSalaryAllowance;
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.PayrollService;
import com.fusi24.pangreksa.web.service.SystemService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.HeaderRow;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.Scroller;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
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
    private final Grid<HrPayroll> grid = new Grid<>(HrPayroll.class, false);
    private final TextField searchField = new TextField();
    private final ComboBox<Integer> yearFilter = new ComboBox<>();
    private final ComboBox<Integer> monthFilter = new ComboBox<>();
    private final Dialog addPayrollDialog = new Dialog();
    private final Dialog recalculateDialog = new Dialog();
    private final Dialog detailDialog = new Dialog();

    private final Map<Long, Integer> rowNoByPayrollId = new ConcurrentHashMap<>();
    private final Map<Long, HrPayrollCalculation> calculationCache = new ConcurrentHashMap<>();

    public PayrollView(CurrentUser currentUser,
                       CommonService commonService,
                       SystemService systemService,
                       PayrollService payrollService) {
        this.currentUser = currentUser;
        this.commonService = commonService;
        this.systemService = systemService;
        this.payrollService = payrollService;

        this.payrollService.setUser(currentUser.require());

        this.auth = commonService.getAuthorization(
                currentUser.require(),
                (String) UI.getCurrent().getSession().getAttribute("responsibility"),
                this.serialVersionUID
        );

        addClassNames(
                LumoUtility.BoxSizing.BORDER,
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Gap.SMALL
        );

        add(new ViewToolbar(VIEW_NAME));

        log.debug("Page {}, Authorization: {} {} {} {}",
                VIEW_NAME, auth.canView, auth.canCreate, auth.canEdit, auth.canDelete);

        initializeView();
    }

    private void initializeView() {
        setHeightFull();
        configureGrid();
        add(buildMainLayout());
        applyFilters();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.setSelectionMode(Grid.SelectionMode.MULTI);

        // No
        Grid.Column<HrPayroll> noCol = grid.addComponentColumn(payroll -> {
            Integer no = payroll.getId() == null ? null : rowNoByPayrollId.get(payroll.getId());
            return new Span(no == null ? "-" : String.valueOf(no));
        }).setHeader("No").setWidth("70px").setFlexGrow(0);

        // NIK
        Grid.Column<HrPayroll> nikCol = grid.addColumn(payroll ->
                        blankToDash(payroll.getEmployeeNumber()))
                .setHeader("NIK")
                .setWidth("140px")
                .setFlexGrow(0);

        // Nama
        Grid.Column<HrPayroll> nameCol = grid.addColumn(this::getEmployeeName)
                .setHeader("Nama Karyawan")
                .setAutoWidth(true)
                .setFlexGrow(1);

        // Gapok
        Grid.Column<HrPayroll> gapokCol = grid.addColumn(payroll ->
                        fmt(payroll.getBaseSalary()))
                .setHeader("Gapok")
                .setWidth("140px")
                .setFlexGrow(0);

        // Tunjangan Jabatan
        Grid.Column<HrPayroll> jabatanCol = grid.addColumn(payroll ->
                        fmt(findComponentAmountByName(payroll, "Jabatan")))
                .setHeader("Tunjangan Jabatan")
                .setWidth("160px")
                .setFlexGrow(0);

        // Keahlian
        Grid.Column<HrPayroll> keahlianCol = grid.addColumn(payroll ->
                        fmt(findComponentAmountByName(payroll, "Keahlian")))
                .setHeader("Keahlian")
                .setWidth("140px")
                .setFlexGrow(0);

        // Tunjangan Tetap
        Grid.Column<HrPayroll> fixedCol = grid.addColumn(payroll ->
                        fmt(getCalc(payroll).map(HrPayrollCalculation::getFixedAllowanceTotal).orElse(BigDecimal.ZERO)))
                .setHeader("Tunjangan Tetap")
                .setWidth("160px")
                .setFlexGrow(0);

        // Allowance (variable only)
        Grid.Column<HrPayroll> allowanceCol = grid.addColumn(payroll ->
                        fmt(getCalc(payroll).map(HrPayrollCalculation::getVariableAllowanceTotal).orElse(BigDecimal.ZERO)))
                .setHeader("Allowance")
                .setWidth("140px")
                .setFlexGrow(0);

        // BPJS TK perusahaan = JHT+JP perusahaan
        Grid.Column<HrPayroll> bpjsTkCompanyCol = grid.addColumn(payroll -> {
            BigDecimal val = sumComponentByCodes(payroll, Set.of(
                    "BPJS_JHT_COMPANY",
                    "BPJS_JP_COMPANY",
                    "BPJS_JKK_COMPANY",
                    "BPJS_JK_COMPANY",
                    "BPJS_JHT",
                    "BPJS_JP",
                    "BPJS_JKK",
                    "BPJS_JK"
            ));
            return fmt(val);
        }).setHeader("BPJS TK").setWidth("140px").setFlexGrow(0);

        // BPJS JKN perusahaan
        Grid.Column<HrPayroll> bpjsJknCompanyCol = grid.addColumn(payroll -> {
            BigDecimal val = sumComponentByCodes(payroll, Set.of(
                    "BPJS_JKN_COMPANY",
                    "BPJS_JKN"
            ));
            return fmt(val);
        }).setHeader("BPJS JKN").setWidth("140px").setFlexGrow(0);

        // Tunjangan Tidak Tetap = allowance + bpjs tk company + bpjs jkn company
        Grid.Column<HrPayroll> variableTotalCol = grid.addColumn(payroll -> {
            HrPayrollCalculation calc = getCalc(payroll).orElse(null);
            if (calc == null) return fmt(BigDecimal.ZERO);

            BigDecimal bpjsTk = sumComponentByCodes(payroll, Set.of(
                    "BPJS_JHT_COMPANY",
                    "BPJS_JP_COMPANY",
                    "BPJS_JKK_COMPANY",
                    "BPJS_JK_COMPANY",
                    "BPJS_JHT",
                    "BPJS_JP",
                    "BPJS_JKK",
                    "BPJS_JK"
            ));

            BigDecimal bpjsJkn = sumComponentByCodes(payroll, Set.of(
                    "BPJS_JKN_COMPANY",
                    "BPJS_JKN"
            ));

            BigDecimal val = nvl(calc.getVariableAllowanceTotal())
                    .add(bpjsTk)
                    .add(bpjsJkn);

            return fmt(val);
        }).setHeader("Tunjangan Tidak Tetap").setWidth("180px").setFlexGrow(0);

        // Penghasilan Kotor
        Grid.Column<HrPayroll> grossCol = grid.addColumn(payroll ->
                        fmt(getCalc(payroll).map(HrPayrollCalculation::getGrossSalary).orElse(BigDecimal.ZERO)))
                .setHeader("Penghasilan Kotor")
                .setWidth("170px")
                .setFlexGrow(0);

        // Asuransi = BPJS TK employee + BPJS JKN employee
        Grid.Column<HrPayroll> insuranceCol = grid.addColumn(payroll -> {
            BigDecimal bpjsTk = sumComponentByCodes(payroll, Set.of(
                    "BPJS_JHT_COMPANY",
                    "BPJS_JP_COMPANY",
                    "BPJS_JKK_COMPANY",
                    "BPJS_JK_COMPANY",
                    "BPJS_JHT",
                    "BPJS_JP",
                    "BPJS_JKK",
                    "BPJS_JK"
            ));

            BigDecimal bpjsJkn = sumComponentByCodes(payroll, Set.of(
                    "BPJS_JKN_COMPANY",
                    "BPJS_JKN"
            ));

            return fmt(bpjsTk.add(bpjsJkn));
        }).setHeader("Asuransi").setWidth("140px").setFlexGrow(0);

        // PPh21
        Grid.Column<HrPayroll> pphCol = grid.addColumn(payroll ->
                        fmt(getCalc(payroll).map(HrPayrollCalculation::getPph21Deduction).orElse(BigDecimal.ZERO)))
                .setHeader("PPh 21")
                .setWidth("140px")
                .setFlexGrow(0);

        // THP
        Grid.Column<HrPayroll> thpCol = grid.addColumn(payroll ->
                        fmt(getCalc(payroll).map(HrPayrollCalculation::getNetTakeHomePay).orElse(BigDecimal.ZERO)))
                .setHeader("THP")
                .setWidth("150px")
                .setFlexGrow(0);

        // Actions
        Grid.Column<HrPayroll> actionCol = grid.addComponentColumn(payroll -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            Button detailBtn = new Button("Detail", e -> openDetailDialog(payroll));
            detailBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);

            Button recalculateBtn = new Button("Recalculate", e -> openRecalculateDialog(payroll));
            recalculateBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);

            actions.add(detailBtn, recalculateBtn);
            return actions;
        }).setHeader("Actions").setWidth("230px").setFlexGrow(0);

        HeaderRow top = grid.prependHeaderRow();
        top.join(noCol, nikCol, nameCol).setText("Karyawan");
        top.join(gapokCol, jabatanCol, keahlianCol, fixedCol, allowanceCol, bpjsTkCompanyCol, bpjsJknCompanyCol, variableTotalCol, grossCol)
                .setText("Penghasilan");
        top.join(insuranceCol, pphCol, thpCol).setText("Potongan / Hasil");
        top.getCell(actionCol).setText("Aksi");
    }

    private VerticalLayout buildMainLayout() {
        // Search
        searchField.setPlaceholder("Search NIK / nama karyawan");
        searchField.setWidth("360px");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.EAGER);

        // Filters
        yearFilter.setPlaceholder("Year");
        yearFilter.setItems(getRecentYears(5));
        yearFilter.setValue(LocalDate.now().getYear());
        yearFilter.setClearButtonVisible(true);
        yearFilter.setWidth("120px");

        monthFilter.setPlaceholder("Month");
        monthFilter.setItems(1,2,3,4,5,6,7,8,9,10,11,12);
        monthFilter.setItemLabelGenerator(this::getMonthName);
        monthFilter.setValue(LocalDate.now().getMonthValue());
        monthFilter.setClearButtonVisible(true);
        monthFilter.setWidth("140px");

        Button searchButton = new Button(new Icon(VaadinIcon.SEARCH), e -> applyFilters());
        searchButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button resetButton = new Button(new Icon(VaadinIcon.RECYCLE), e -> {
            resetFilter();
            applyFilters();
        });
        resetButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        Button addButton = new Button("Add Payroll", e -> openAddPayrollDialog());
        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        addButton.setVisible(true);

        deleteButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
        deleteButton.setVisible(false);
        deleteButton.setEnabled(true);

        grid.addSelectionListener(e -> deleteButton.setVisible(auth.canDelete && !grid.getSelectedItems().isEmpty()));

        deleteButton.addClickListener(e -> {
            List<Long> ids = grid.getSelectedItems().stream()
                    .map(HrPayroll::getId)
                    .filter(Objects::nonNull)
                    .toList();

            if (ids.isEmpty()) {
                AppNotification.error("Tidak ada data yang dipilih.");
                return;
            }

            runWithPreloading("Menghapus data payroll...", () -> {
                payrollService.deletePayrolls(ids);
            }, () -> {
                grid.deselectAll();
                applyFilters();
                deleteButton.setVisible(false);
                AppNotification.success("Berhasil menghapus " + ids.size() + " data payroll.");
            });
        });

        HorizontalLayout filterBar = new HorizontalLayout(yearFilter, monthFilter, searchField, searchButton, resetButton);
        filterBar.setAlignItems(FlexComponent.Alignment.END);
        filterBar.setSpacing(true);

        HorizontalLayout actionBar = new HorizontalLayout(deleteButton, addButton);
        actionBar.setAlignItems(FlexComponent.Alignment.END);
        actionBar.setSpacing(true);

        HorizontalLayout toolbar = new HorizontalLayout(filterBar, actionBar);
        toolbar.setWidthFull();
        toolbar.expand(filterBar);
        toolbar.setAlignItems(FlexComponent.Alignment.END);

        VerticalLayout layout = new VerticalLayout(toolbar, grid);
        layout.setSizeFull();
        layout.setPadding(false);
        layout.setSpacing(false);
        return layout;
    }

    private void applyFilters() {
        Integer selectedYear = yearFilter.getValue();
        Integer selectedMonth = monthFilter.getValue();
        String searchTerm = searchField.getValue();

        MutableObject<LocalDate> mFilterDate = new MutableObject<>();
        if (selectedYear != null && selectedMonth != null) {
            mFilterDate.setValue(LocalDate.of(selectedYear, selectedMonth, 1));
        }

        rowNoByPayrollId.clear();
        calculationCache.clear();

        DataProvider<HrPayroll, Void> dataProvider = DataProvider.fromCallbacks(
                query -> {
                    int offset = query.getOffset();
                    int limit = query.getLimit();
                    int page = offset / limit;

                    List<QuerySortOrder> ignored = query.getSortOrders();

                    List<HrPayroll> items = payrollService.getPayrollPage(
                            PageRequest.of(page, limit),
                            selectedYear,
                            mFilterDate.getValue(),
                            searchTerm
                    ).getContent();

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

    private void resetFilter() {
        yearFilter.clear();
        monthFilter.clear();
        searchField.clear();
    }

    private void openAddPayrollDialog() {
        addPayrollDialog.removeAll();

        AddPayrollForm form = new AddPayrollForm(
                payrollService,
                currentUser,
                this::runWithPreloading,
                () -> {
                    addPayrollDialog.close();
                    applyFilters();
                }
        );

        addPayrollDialog.add(form);
        addPayrollDialog.setWidth("900px");
        addPayrollDialog.open();
    }

    private void openRecalculateDialog(HrPayroll payroll) {
        recalculateDialog.removeAll();

        RecalculateForm form = new RecalculateForm(
                payrollService,
                currentUser,
                this::runWithPreloading,
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

    private void openDetailDialog(HrPayroll payroll) {
        detailDialog.removeAll();
        detailDialog.setWidth("1000px");
        detailDialog.setCloseOnEsc(true);
        detailDialog.setCloseOnOutsideClick(true);

        HrPayrollCalculation calc = getCalc(payroll).orElse(null);
        List<HrPayrollComponent> components = calc == null
                ? Collections.emptyList()
                : payrollService.getPayrollComponentsByCalculationId(calc.getId());

        VerticalLayout root = new VerticalLayout();
        root.setPadding(false);
        root.setSpacing(true);
        root.setWidthFull();

        Span title = new Span("Payroll Detail");
        title.getStyle().set("font-weight", "600");
        title.getStyle().set("font-size", "1.1rem");

        FormLayout header = new FormLayout();
        header.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("720px", 2)
        );

        addReadOnlyField(header, "NIK", blankToDash(payroll.getEmployeeNumber()));
        addReadOnlyField(header, "Nama", getEmployeeName(payroll));
        addReadOnlyField(header, "Jabatan", blankToDash(payroll.getPosition()));
        addReadOnlyField(header, "Departement", blankToDash(payroll.getDepartment()));
        addReadOnlyField(header, "Status Pajak", blankToDash(payroll.getPtkpCode()));
        addReadOnlyField(header, "Tanggal Masuk", payroll.getJoinDate() == null ? "-" : payroll.getJoinDate().toString());
        addReadOnlyField(header, "Periode Payroll", formatPayrollMonth(payroll.getPayrollDate()));
        addReadOnlyField(header, "Total Hari Kerja", String.valueOf(payroll.getParamAttendanceDays() == null ? 0 : payroll.getParamAttendanceDays()));
        addReadOnlyField(header, "Total Hadir", String.valueOf(payroll.getSumAttendance() == null ? 0 : payroll.getSumAttendance()));

        Grid<HrPayrollComponent> componentGrid = new Grid<>(HrPayrollComponent.class, false);
        componentGrid.setWidthFull();
        componentGrid.setHeight("400px");
        componentGrid.setItems(components);

        componentGrid.addColumn(HrPayrollComponent::getComponentType).setHeader("Type").setWidth("120px").setFlexGrow(0);
        componentGrid.addColumn(HrPayrollComponent::getComponentGroup).setHeader("Group").setWidth("180px").setFlexGrow(0);
        componentGrid.addColumn(HrPayrollComponent::getComponentCode).setHeader("Code").setWidth("180px").setFlexGrow(0);
        componentGrid.addColumn(HrPayrollComponent::getComponentName).setHeader("Name").setAutoWidth(true).setFlexGrow(1);
        componentGrid.addColumn(c -> fmt(c.getAmount())).setHeader("Amount").setWidth("160px").setFlexGrow(0);

        FormLayout summary = new FormLayout();
        summary.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("720px", 2)
        );

        if (calc != null) {
            BigDecimal bpjsTkCompany = nvl(calc.getBpjsJhtCompany()).add(nvl(calc.getBpjsJpCompany()));
            BigDecimal tunjanganTidakTetap = nvl(calc.getVariableAllowanceTotal()).add(bpjsTkCompany).add(nvl(calc.getBpjsJknCompany()));
            BigDecimal asuransi = nvl(calc.getBpjsJhtDeduction()).add(nvl(calc.getBpjsJpDeduction())).add(nvl(calc.getBpjsJknDeduction()));

            addReadOnlyField(summary, "Gapok", fmt(calc.getBaseSalary()));
            addReadOnlyField(summary, "Tunjangan Tetap", fmt(calc.getFixedAllowanceTotal()));
            addReadOnlyField(summary, "Tunjangan Tidak Tetap", fmt(tunjanganTidakTetap));
            addReadOnlyField(summary, "Penghasilan Kotor", fmt(calc.getGrossSalary()));
            addReadOnlyField(summary, "Asuransi", fmt(asuransi));
            addReadOnlyField(summary, "PPh 21", fmt(calc.getPph21Deduction()));
            addReadOnlyField(summary, "Total Potongan", fmt(calc.getTotalDeduction()));
            addReadOnlyField(summary, "THP", fmt(calc.getNetTakeHomePay()));
        }

        Button close = new Button("Close", e -> detailDialog.close());
        close.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        root.add(title, header, new Span("Komponen Payroll"), componentGrid, new Span("Summary"), summary, close);

        detailDialog.add(new Scroller(root));
        detailDialog.open();
    }

    private Optional<HrPayrollCalculation> getCalc(HrPayroll payroll) {
        if (payroll == null || payroll.getId() == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(
                calculationCache.computeIfAbsent(
                        payroll.getId(),
                        payrollService::getCalculationByPayrollId
                )
        );
    }

    private BigDecimal findComponentAmountByName(HrPayroll payroll, String componentName) {
        HrPayrollCalculation calc = getCalc(payroll).orElse(null);
        if (calc == null) return BigDecimal.ZERO;

        List<HrPayrollComponent> components = payrollService.getPayrollComponentsByCalculationId(calc.getId());
        return components.stream()
                .filter(c -> c.getComponentName() != null)
                .filter(c -> c.getComponentName().equalsIgnoreCase(componentName))
                .map(HrPayrollComponent::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String getEmployeeName(HrPayroll payroll) {
        String fn = payroll.getFirstName() == null ? "" : payroll.getFirstName();
        String mn = payroll.getMiddleName() == null ? "" : payroll.getMiddleName();
        String ln = payroll.getLastName() == null ? "" : payroll.getLastName();
        String full = (fn + " " + mn + " " + ln).trim().replaceAll("\\s+", " ");
        return full.isBlank() ? "-" : full;
    }

    private void addReadOnlyField(FormLayout form, String label, String value) {
        TextField tf = new TextField();
        tf.setValue(value == null ? "-" : value);
        tf.setReadOnly(true);
        tf.setWidthFull();
        form.addFormItem(tf, label);
    }

    public static class RecalculateForm extends FormLayout {
        private final PayrollService payrollService;
        private final CurrentUser currentUser;
        private final HrPayroll payroll;
        private final Runnable onSaveSuccess;
        private final UiLoader loader;

        private final ComboBox<Integer> yearField = new ComboBox<>();
        private final ComboBox<Integer> monthField = new ComboBox<>();
        private final IntegerField attendanceDaysField = new IntegerField();
        private final IntegerField overtimeMinutesField = new IntegerField();

        private final RadioButtonGroup<String> overtimePaymentType = new RadioButtonGroup<>();
        private final BigDecimalField overtimeStaticNominal = new BigDecimalField();
        private final IntegerField overtimePercent = new IntegerField();

        private final RadioButtonGroup<String> allowanceMode = new RadioButtonGroup<>();
        private final MultiSelectComboBox<HrSalaryAllowance> allowanceMultiSelect = new MultiSelectComboBox<>();

        private final BigDecimalField totalBonusField = new BigDecimalField();
        private final BigDecimalField totalOtherDedField = new BigDecimalField();

        private final Button saveButton = new Button("Recalculate");
        private final Button cancelButton = new Button("Cancel");

        private final Binder<PayrollService.AddPayrollRequest> binder =
                new BeanValidationBinder<>(PayrollService.AddPayrollRequest.class);

        public RecalculateForm(PayrollService payrollService,
                               CurrentUser currentUser,
                               UiLoader loader,
                               HrPayroll payroll,
                               Runnable onSaveSuccess) {
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
            LocalDate payrollDate = payroll.getPayrollDate() == null
                    ? LocalDate.now().withDayOfMonth(1)
                    : payroll.getPayrollDate().withDayOfMonth(1);

            yearField.setItems(payrollDate.getYear());
            yearField.setValue(payrollDate.getYear());
            yearField.setEnabled(false);
            yearField.setWidthFull();

            monthField.setItems(payrollDate.getMonthValue());
            monthField.setValue(payrollDate.getMonthValue());
            monthField.setEnabled(false);
            monthField.setWidthFull();

            attendanceDaysField.setMin(0);
            attendanceDaysField.setMax(31);
            attendanceDaysField.setValue(payroll.getParamAttendanceDays() == null ? 0 : payroll.getParamAttendanceDays());
            attendanceDaysField.setWidthFull();

            overtimeMinutesField.setMin(0);
            overtimeMinutesField.setMax(60);
            overtimeMinutesField.setValue(payroll.getOvertimeHours() == null ? 0 : payroll.getOvertimeHours().intValue());
            overtimeMinutesField.setWidthFull();

            overtimePaymentType.setItems("STATIC", "PERCENTAGE");
            overtimePaymentType.setValue("STATIC");
            overtimePaymentType.setWidthFull();

            overtimeStaticNominal.setValue(payroll.getOvertimeAmount() == null ? BigDecimal.ZERO : payroll.getOvertimeAmount());
            overtimeStaticNominal.setWidthFull();

            overtimePercent.setMin(0);
            overtimePercent.setMax(100);
            overtimePercent.setValue(0);
            overtimePercent.setWidthFull();
            overtimePercent.setVisible(false);

            overtimePaymentType.addValueChangeListener(e -> {
                boolean isStatic = "STATIC".equalsIgnoreCase(e.getValue());
                overtimeStaticNominal.setVisible(isStatic);
                overtimePercent.setVisible(!isStatic);
            });

            allowanceMode.setItems("NO ALLOWANCE", "SELECT ALLOWANCE", "BENEFITS PACKAGE");
            allowanceMode.setValue("BENEFITS PACKAGE");
            allowanceMode.setWidthFull();

            allowanceMultiSelect.setPlaceholder("Select one or more allowances");
            allowanceMultiSelect.setItems(payrollService.getSelectableAllowancesForPayrollDate(payrollDate));
            allowanceMultiSelect.setItemLabelGenerator(a ->
                    a.getName() + " (Rp " + FormattingUtils.formatPayrollAmount(a.getAmount()) + ")"
            );
            allowanceMultiSelect.setVisible(false);
            allowanceMultiSelect.setWidthFull();

            allowanceMode.addValueChangeListener(e -> {
                boolean show = "SELECT ALLOWANCE".equalsIgnoreCase(e.getValue());
                allowanceMultiSelect.setVisible(show);
                if (!show) {
                    allowanceMultiSelect.deselectAll();
                }
            });

            HrPayrollCalculation calc = payrollService.getCalculationByPayrollId(payroll.getId());

            totalBonusField.setValue(calc == null || calc.getBonusAmount() == null ? BigDecimal.ZERO : calc.getBonusAmount());
            totalBonusField.setWidthFull();

            totalOtherDedField.setValue(BigDecimal.ZERO);
            totalOtherDedField.setWidthFull();

            saveButton.addThemeVariants(ButtonVariant.LUMO_ERROR);
            cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        }

        private void configureBinder() {
            PayrollService.AddPayrollRequest bean = new PayrollService.AddPayrollRequest();

            LocalDate payrollDate = payroll.getPayrollDate() == null
                    ? LocalDate.now().withDayOfMonth(1)
                    : payroll.getPayrollDate().withDayOfMonth(1);

            bean.setYear(payrollDate.getYear());
            bean.setMonth(payrollDate.getMonthValue());
            bean.setParamAttendanceDays(payroll.getParamAttendanceDays() == null ? 0 : payroll.getParamAttendanceDays());
            bean.setOvertimeMinutes(payroll.getOvertimeHours() == null ? 0 : payroll.getOvertimeHours().intValue());
            bean.setOvertimePaymentType("STATIC");
            bean.setOvertimeStaticNominal(payroll.getOvertimeAmount() == null ? BigDecimal.ZERO : payroll.getOvertimeAmount());
            bean.setOvertimePercent(0);
            bean.setAllowanceMode("BENEFITS PACKAGE");
            bean.setSelectedAllowances(new ArrayList<>());

            binder.setBean(bean);

            binder.forField(yearField).bind(PayrollService.AddPayrollRequest::getYear, PayrollService.AddPayrollRequest::setYear);
            binder.forField(monthField).bind(PayrollService.AddPayrollRequest::getMonth, PayrollService.AddPayrollRequest::setMonth);
            binder.forField(attendanceDaysField).bind(PayrollService.AddPayrollRequest::getParamAttendanceDays, PayrollService.AddPayrollRequest::setParamAttendanceDays);
            binder.forField(overtimeMinutesField).bind(PayrollService.AddPayrollRequest::getOvertimeMinutes, PayrollService.AddPayrollRequest::setOvertimeMinutes);
            binder.forField(overtimePaymentType).bind(PayrollService.AddPayrollRequest::getOvertimePaymentType, PayrollService.AddPayrollRequest::setOvertimePaymentType);
            binder.forField(overtimeStaticNominal).bind(PayrollService.AddPayrollRequest::getOvertimeStaticNominal, PayrollService.AddPayrollRequest::setOvertimeStaticNominal);
            binder.forField(overtimePercent).bind(PayrollService.AddPayrollRequest::getOvertimePercent, PayrollService.AddPayrollRequest::setOvertimePercent);
            binder.forField(allowanceMode).bind(PayrollService.AddPayrollRequest::getAllowanceMode, PayrollService.AddPayrollRequest::setAllowanceMode);
        }

        private void configureLayout() {
            setResponsiveSteps(
                    new ResponsiveStep("0", 1),
                    new ResponsiveStep("700px", 2)
            );

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

            addFormItem(attendanceDaysField, "Total Hari Kerja");
            addFormItem(overtimeMinutesField, "Overtime dihitung (menit)");

            addFormItem(overtimePaymentType, "Bayaran Overtime");
            addFormItem(overtimeStaticNominal, "Nominal Upah (Rp)");
            addFormItem(overtimePercent, "Persentase (%)");

            Span s3 = new Span("Allowance");
            s3.getStyle().set("font-weight", "600");
            add(s3);
            setColspan(s3, 2);

            addFormItem(allowanceMode, "Allowance Option");
            FormItem fiAllow = addFormItem(allowanceMultiSelect, "Select Allowance");
            setColspan(fiAllow, 2);

            Span s4 = new Span("Komponen Tambahan");
            s4.getStyle().set("font-weight", "600");
            add(s4);
            setColspan(s4, 2);

            addFormItem(totalBonusField, "Bonus");
            addFormItem(totalOtherDedField, "Other Deductions");

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
                    AppNotification.error("Form belum valid");
                    return;
                }

                String mode = bean.getAllowanceMode() == null ? "" : bean.getAllowanceMode().trim();
                if ("SELECT ALLOWANCE".equalsIgnoreCase(mode)) {
                    bean.setSelectedAllowances(new ArrayList<>(allowanceMultiSelect.getSelectedItems()));
                } else {
                    bean.setSelectedAllowances(new ArrayList<>());
                }

                AppUserInfo userInfo = currentUser.require();

                BigDecimal bonus = totalBonusField.getValue() == null ? BigDecimal.ZERO : totalBonusField.getValue();
                BigDecimal otherDed = totalOtherDedField.getValue() == null ? BigDecimal.ZERO : totalOtherDedField.getValue();

                loader.run("Recalculating payroll...", () -> {
                    payrollService.recalculateSinglePayroll(
                            payroll.getId(),
                            bean,
                            bonus,
                            otherDed,
                            BigDecimal.ZERO,
                            userInfo
                    );
                }, () -> {
                    AppNotification.success("Perhitungan ulang berhasil.");
                    onSaveSuccess.run();
                });
            });

            cancelButton.addClickListener(e -> onSaveSuccess.run());
        }
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

        public AddPayrollForm(PayrollService payrollService,
                              CurrentUser currentUser,
                              PreloadRunner preloadRunner,
                              Runnable onSaveSuccess) {
            this.payrollService = payrollService;
            this.currentUser = currentUser;
            this.preloadRunner = preloadRunner;
            this.onSaveSuccess = onSaveSuccess;

            configureFields();
            configureBinder();
            configureLayout();
            configureActions();
        }

        private void configureFields() {
            int y = LocalDate.now().getYear();

            yearField.setItems(y - 5, y - 4, y - 3, y - 2, y - 1, y, y + 1);
            yearField.setValue(y);
            yearField.setWidthFull();

            monthField.setItems(1,2,3,4,5,6,7,8,9,10,11,12);
            monthField.setItemLabelGenerator(m -> Month.of(m).getDisplayName(TextStyle.FULL, new Locale("id", "ID")));
            monthField.setValue(LocalDate.now().getMonthValue());
            monthField.setWidthFull();

            attendanceDaysField.setMin(0);
            attendanceDaysField.setMax(31);
            attendanceDaysField.setValue(null);
            attendanceDaysField.setWidthFull();

            overtimeMinutesField.setMin(0);
            overtimeMinutesField.setMax(60);
            overtimeMinutesField.setValue(null);
            overtimeMinutesField.setWidthFull();

            overtimePaymentType.setItems("STATIC", "PERCENTAGE");
            overtimePaymentType.setValue("STATIC");
            overtimePaymentType.setWidthFull();

            overtimeStaticNominal.setPlaceholder("Rp 0");
            overtimeStaticNominal.setWidthFull();
            overtimeStaticNominal.setVisible(true);

            overtimePercent.setMin(0);
            overtimePercent.setMax(100);
            overtimePercent.setValue(null);
            overtimePercent.setWidthFull();
            overtimePercent.setVisible(false);

            overtimePaymentType.addValueChangeListener(e -> {
                boolean isStatic = "STATIC".equalsIgnoreCase(e.getValue());
                overtimeStaticNominal.setVisible(isStatic);
                overtimePercent.setVisible(!isStatic);
            });

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
                boolean show = "SELECT ALLOWANCE".equalsIgnoreCase(e.getValue());
                allowanceMultiSelect.setVisible(show);
                if (!show) {
                    allowanceMultiSelect.deselectAll();
                }
            });

            saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        }

        private void refreshAllowanceOptions() {
            Integer year = yearField.getValue();
            Integer month = monthField.getValue();
            if (year == null || month == null) return;

            LocalDate payrollDate = LocalDate.of(year, month, 1);
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
            setResponsiveSteps(
                    new ResponsiveStep("0", 1),
                    new ResponsiveStep("700px", 2)
            );

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

            addFormItem(attendanceDaysField, "Total Hari Kerja");
            addFormItem(overtimeMinutesField, "Overtime dihitung (menit)");

            addFormItem(overtimePaymentType, "Bayaran Overtime");
            addFormItem(overtimeStaticNominal, "Nominal Upah (Rp)");
            addFormItem(overtimePercent, "Persentase (%)");

            Span s3 = new Span("Allowance");
            s3.getStyle().set("font-weight", "600");
            add(s3);
            setColspan(s3, 2);

            addFormItem(allowanceMode, "Allowance Option");
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
                    AppNotification.error("Form belum valid");
                    return;
                }

                String mode = bean.getAllowanceMode() == null ? "" : bean.getAllowanceMode().trim();
                if ("SELECT ALLOWANCE".equalsIgnoreCase(mode)) {
                    bean.setSelectedAllowances(new ArrayList<>(allowanceMultiSelect.getSelectedItems()));
                    if (bean.getSelectedAllowances().isEmpty()) {
                        AppNotification.error("Silakan pilih allowance minimal 1 item");
                        return;
                    }
                } else {
                    bean.setSelectedAllowances(new ArrayList<>());
                }

                AppUserInfo userInfo = currentUser.require();

                preloadRunner.run(
                        "Creating payroll...",
                        () -> payrollService.createPayrollBulk(bean, userInfo),
                        () -> {
                            AppNotification.success("Payroll created successfully");
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

        ProgressBar bar = new ProgressBar();
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
                    if (ui == null || !ui.isAttached()) return;

                    ui.access(() -> {
                        loading.close();

                        if (ex != null) {
                            Throwable root = (ex instanceof java.util.concurrent.CompletionException && ex.getCause() != null)
                                    ? ex.getCause()
                                    : ex;

                            AppNotification.error("Proses gagal: " + root.getMessage());
                            root.printStackTrace();
                        } else {
                            if (onDoneUi != null) onDoneUi.run();
                        }
                        ui.push();
                    });
                });
    }

    @FunctionalInterface
    public interface PreloadRunner {
        void run(String message, Runnable task, Runnable onDone);
    }

    @FunctionalInterface
    public interface UiLoader {
        void run(String message, Runnable task, Runnable onDoneUi);
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

    private String fmt(BigDecimal v) {
        return FormattingUtils.formatPayrollAmount(nvl(v));
    }

    private String blankToDash(String v) {
        return (v == null || v.trim().isEmpty()) ? "-" : v;
    }

    private BigDecimal sumComponentByCodes(HrPayroll payroll, Set<String> codes) {
        HrPayrollCalculation calc = getCalc(payroll).orElse(null);
        if (calc == null) return BigDecimal.ZERO;

        List<HrPayrollComponent> components = payrollService.getPayrollComponentsByCalculationId(calc.getId());
        if (components == null || components.isEmpty()) {
            return BigDecimal.ZERO;
        }

        return components.stream()
                .filter(c -> c.getComponentCode() != null)
                .filter(c -> codes.contains(c.getComponentCode().toUpperCase()))
                .map(HrPayrollComponent::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

}