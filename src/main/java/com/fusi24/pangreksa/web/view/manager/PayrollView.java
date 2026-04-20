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
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.PayrollService;
import com.fusi24.pangreksa.web.service.SystemService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.FooterRow;
import com.vaadin.flow.component.grid.GridVariant;
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
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
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
import lombok.Getter;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Route("payroll-list-page-access")
@PageTitle("Payroll")
@Menu(order = 27, icon = "vaadin:clipboard-check", title = "Payroll")
@RolesAllowed("PAYROLL")
public class PayrollView extends Main {
    public static final String VIEW_NAME = "Payroll";
    private static final long serialVersionUID = 19092L;
    private static final Logger log = LoggerFactory.getLogger(PayrollView.class);
    private static final UUID BPJS_JP_CAP_CONFIG_ID = UUID.fromString("8a06244d-ebe9-4a4a-a71c-e947759dcec6");
    private static final UUID BPJS_JKN_CAP_CONFIG_ID = UUID.fromString("3ae1184a-b6b8-412c-9ee8-13e478796aa4");

    private final CurrentUser currentUser;
    private final CommonService commonService;
    private final SystemService systemService;
    private final PayrollService payrollService;

    private Authorization auth;

    private final Button deleteButton = new Button("Delete");
    private final Grid<HrPayroll> grid = new Grid<>(HrPayroll.class, false);
    private final Grid<BpjsTkRow> bpjsTkGrid = new Grid<>(BpjsTkRow.class, false);
    private final Grid<BpjsJknRow> bpjsJknGrid = new Grid<>(BpjsJknRow.class, false);
    private final Grid<PajakRow> pajakGrid = new Grid<>(PajakRow.class, false);
    private final TextField searchField = new TextField();
    private final ComboBox<Integer> yearFilter = new ComboBox<>();
    private final ComboBox<Integer> monthFilter = new ComboBox<>();
    private final Dialog addPayrollDialog = new Dialog();
    private final Dialog recalculateDialog = new Dialog();
    private final Dialog detailDialog = new Dialog();

    private final Map<Long, Integer> rowNoByPayrollId = new ConcurrentHashMap<>();
    private final Map<Long, HrPayrollCalculation> calculationCache = new ConcurrentHashMap<>();
    private final Map<Long, List<HrPayrollComponent>> componentCache = new ConcurrentHashMap<>();
    private final Tab payrollTab = new Tab("PAYROLL");
    private final Tab bpjsTkTab = new Tab("BPJS TK");
    private final Tab bpjsJknTab = new Tab("BPJS JKN");
    private final Tab pajakTab = new Tab("PAJAK");
    private final Tabs dataTabs = new Tabs(payrollTab, bpjsTkTab, bpjsJknTab, pajakTab);
    private final Map<Tab, com.vaadin.flow.component.Component> tabContents = new LinkedHashMap<>();
    private final VerticalLayout gridContent = new VerticalLayout();
    private final VerticalLayout pajakContent = new VerticalLayout();
    private final Span pajakTitle = new Span("PPh Non Gross-Up");

    private Grid.Column<HrPayroll> payrollNameColumn;
    private Grid.Column<HrPayroll> payrollGapokColumn;
    private Grid.Column<HrPayroll> payrollJabatanColumn;
    private Grid.Column<HrPayroll> payrollKeahlianColumn;
    private Grid.Column<HrPayroll> payrollFixedAllowanceColumn;
    private Grid.Column<HrPayroll> payrollAllowanceColumn;
    private Grid.Column<HrPayroll> payrollBpjsTkColumn;
    private Grid.Column<HrPayroll> payrollBpjsJknColumn;
    private Grid.Column<HrPayroll> payrollVariableAllowanceColumn;
    private Grid.Column<HrPayroll> payrollGrossColumn;
    private Grid.Column<HrPayroll> payrollInsuranceColumn;
    private Grid.Column<HrPayroll> payrollPphColumn;
    private Grid.Column<HrPayroll> payrollThpColumn;

    private Grid.Column<BpjsTkRow> bpjsTkEmployeeNumberColumn;
    private Grid.Column<BpjsTkRow> bpjsTkWageColumn;
    private Grid.Column<BpjsTkRow> bpjsTkJpBaseWageColumn;
    private Grid.Column<BpjsTkRow> bpjsTkEmployeeJpColumn;
    private Grid.Column<BpjsTkRow> bpjsTkCompanyJpColumn;
    private Grid.Column<BpjsTkRow> bpjsTkCompanyJkkColumn;
    private Grid.Column<BpjsTkRow> bpjsTkCompanyJkmColumn;
    private Grid.Column<BpjsTkRow> bpjsTkEmployeeJhtColumn;
    private Grid.Column<BpjsTkRow> bpjsTkCompanyJhtColumn;
    private Grid.Column<BpjsTkRow> bpjsTkTotalContributionColumn;

    private Grid.Column<BpjsJknRow> bpjsJknEmployeeNumberColumn;
    private Grid.Column<BpjsJknRow> bpjsJknWageColumn;
    private Grid.Column<BpjsJknRow> bpjsJknBaseWageColumn;
    private Grid.Column<BpjsJknRow> bpjsJknCompanyColumn;
    private Grid.Column<BpjsJknRow> bpjsJknEmployeeColumn;
    private Grid.Column<BpjsJknRow> bpjsJknPremiumColumn;
    private Grid.Column<PajakRow> pajakNoColumn;
    private Grid.Column<PajakRow> pajakGapokColumn;
    private Grid.Column<PajakRow> pajakJabatanColumn;
    private Grid.Column<PajakRow> pajakKeahlianColumn;
    private Grid.Column<PajakRow> pajakAllowanceColumn;
    private Grid.Column<PajakRow> pajakJkkColumn;
    private Grid.Column<PajakRow> pajakJkmColumn;
    private Grid.Column<PajakRow> pajakJknColumn;
    private Grid.Column<PajakRow> pajakPenghasilanTeraturColumn;
    private Grid.Column<PajakRow> pajakDppTerColumn;
    private Grid.Column<PajakRow> pajakPaid21Column;

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
        configureSupplementaryGrids();
        add(buildMainLayout());
        applyFilters();
    }

    private void configureGrid() {
        grid.setSizeFull();
        grid.setSelectionMode(Grid.SelectionMode.SINGLE);
        grid.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS);
        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);

        // No
        Grid.Column<HrPayroll> noCol = grid.addComponentColumn(payroll -> {
            Integer no = payroll.getId() == null ? null : rowNoByPayrollId.get(payroll.getId());
            return new Span(no == null ? "-" : String.valueOf(no));
        }).setHeader("No").setWidth("70px").setFlexGrow(0);

        // NIP
        Grid.Column<HrPayroll> nikCol = grid.addColumn(payroll ->
                        blankToDash(payroll.getEmployeeNumber()))
                .setHeader("NIP")
                .setWidth("100px")
                .setFlexGrow(0);

        // Nama
        payrollNameColumn = grid.addColumn(item -> {
                    String fullName = Stream.of(
                                    item.getFirstName(),
                                    item.getMiddleName(),
                                    item.getLastName()
                            )
                            .filter(Objects::nonNull)
                            .filter(s -> !s.isBlank())
                            .collect(Collectors.joining(" "));
                    return fullName;
                })
                .setHeader("Nama Karyawan")
                .setWidth("200px")
                .setFlexGrow(1);

        // Gapok
        payrollGapokColumn = grid.addColumn(payroll ->
                        fmt(payroll.getBaseSalary()))
                .setHeader("Gapok")
                .setWidth("140px")
                .setFlexGrow(0);

        // Tunjangan Jabatan
        payrollJabatanColumn = grid.addColumn(payroll ->
                        fmt(findComponentAmountByName(payroll, "Jabatan")))
                .setHeader("Tunjangan Jabatan")
                .setWidth("160px")
                .setFlexGrow(0);

        // Keahlian
        payrollKeahlianColumn = grid.addColumn(payroll ->
                        fmt(findComponentAmountByName(payroll, "Keahlian")))
                .setHeader("Keahlian")
                .setWidth("140px")
                .setFlexGrow(0);

        // Tunjangan Tetap
        payrollFixedAllowanceColumn = grid.addColumn(payroll ->
                        fmt(getCalc(payroll).map(HrPayrollCalculation::getFixedAllowanceTotal).orElse(BigDecimal.ZERO)))
                .setHeader("Tunjangan Tetap")
                .setWidth("160px")
                .setFlexGrow(0);

        // Allowance (variable only)
        payrollAllowanceColumn = grid.addColumn(payroll ->
                        fmt(getCalc(payroll).map(HrPayrollCalculation::getVariableAllowanceTotal).orElse(BigDecimal.ZERO)))
                .setHeader("Allowance")
                .setWidth("140px")
                .setFlexGrow(0);

        // BPJS TK perusahaan = JHT+JP perusahaan
        payrollBpjsTkColumn = grid.addColumn(payroll -> {
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
        payrollBpjsJknColumn = grid.addColumn(payroll -> {
            BigDecimal val = sumComponentByCodes(payroll, Set.of(
                    "BPJS_JKN_COMPANY",
                    "BPJS_JKN"
            ));
            return fmt(val);
        }).setHeader("BPJS JKN").setWidth("140px").setFlexGrow(0);

        // Tunjangan Tidak Tetap = allowance + bpjs tk company + bpjs jkn company
        payrollVariableAllowanceColumn = grid.addColumn(payroll -> {
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
        payrollGrossColumn = grid.addColumn(payroll ->
                        fmt(getCalc(payroll).map(HrPayrollCalculation::getGrossSalary).orElse(BigDecimal.ZERO)))
                .setHeader("Penghasilan Kotor")
                .setWidth("170px")
                .setFlexGrow(0);

        // Asuransi = BPJS TK employee + BPJS JKN employee
        payrollInsuranceColumn = grid.addColumn(payroll -> {
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
        payrollPphColumn = grid.addColumn(payroll ->
                        fmt(getCalc(payroll).map(HrPayrollCalculation::getPph21Deduction).orElse(BigDecimal.ZERO)))
                .setHeader("PPh 21 (TER)")
                .setWidth("140px")
                .setFlexGrow(0);

        // THP
        payrollThpColumn = grid.addColumn(payroll ->
                        fmt(getCalc(payroll).map(HrPayrollCalculation::getNetTakeHomePay).orElse(BigDecimal.ZERO)))
                .setHeader("THP")
                .setWidth("150px")
                .setFlexGrow(0);

        // Actions
        Grid.Column<HrPayroll> actionCol = grid.addComponentColumn(payroll -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            // Button Detail dengan Icon
            Button detailBtn = new Button(new Icon(VaadinIcon.SEARCH)); // Menggunakan ikon kaca pembesar/search
            detailBtn.addThemeVariants(ButtonVariant.LUMO_WARNING, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
            detailBtn.setTooltipText("Detail"); // Menambahkan Tooltip
            detailBtn.addClickListener(e -> openDetailDialog(payroll));

            // Button Recalculate dengan Icon
            Button recalculateBtn = new Button(new Icon(VaadinIcon.REFRESH)); // Menggunakan ikon refresh
            recalculateBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ICON);
            recalculateBtn.setTooltipText("Recalculate"); // Menambahkan Tooltip
            recalculateBtn.addClickListener(e -> openRecalculateDialog(payroll));

            actions.add(detailBtn, recalculateBtn);
            return actions;
        }).setHeader("Actions").setWidth("110px").setFrozenToEnd(true).setFlexGrow(0);

        FooterRow footerRow = grid.appendFooterRow();
        footerRow.getCell(payrollNameColumn).setComponent(createFooterLabel("Total"));

    }

    private VerticalLayout buildMainLayout() {
        searchField.setPlaceholder("Search NIK / nama karyawan");
        searchField.setWidth("360px");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.EAGER);

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

        configureTabsLayout();

        VerticalLayout layout = new VerticalLayout(toolbar, dataTabs, gridContent);
        layout.setSizeFull();
        layout.setPadding(false);
        layout.setSpacing(true);
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
        componentCache.clear();

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

        DataProvider<BpjsTkRow, Void> bpjsTkProvider = createBpjsTkDataProvider();
        DataProvider<BpjsJknRow, Void> bpjsJknProvider = createBpjsJknDataProvider();
        DataProvider<PajakRow, Void> pajakProvider = createPajakDataProvider();

        bpjsTkGrid.setDataProvider(bpjsTkProvider);
        bpjsJknGrid.setDataProvider(bpjsJknProvider);
        pajakGrid.setDataProvider(pajakProvider);

        List<HrPayroll> filteredPayrolls = payrollService.getPayrollList(selectedYear, mFilterDate.getValue(), searchTerm);
        updateFooterSummaries(filteredPayrolls);
    }

    private void resetFilter() {
        yearFilter.clear();
        monthFilter.clear();
        searchField.clear();
    }

    private void configureSupplementaryGrids() {
        configureBpjsTkGrid();
        configureBpjsJknGrid();
        configurePajakGrid();
    }

    private void configureTabsLayout() {
        dataTabs.setWidthFull();

        gridContent.setSizeFull();
        gridContent.setPadding(false);
        gridContent.setSpacing(false);

        tabContents.clear();
        tabContents.put(payrollTab, grid);
        tabContents.put(bpjsTkTab, bpjsTkGrid);
        tabContents.put(bpjsJknTab, bpjsJknGrid);
        pajakTitle.getStyle().set("font-weight", "700");
        pajakContent.setSizeFull();
        pajakContent.setPadding(false);
        pajakContent.setSpacing(true);
        pajakContent.removeAll();
        pajakContent.add(pajakTitle, pajakGrid);
        pajakContent.expand(pajakGrid);
        tabContents.put(pajakTab, pajakContent);

        tabContents.values().forEach(component -> {
            component.setVisible(false);
            if (component instanceof Grid<?> componentGrid) {
                componentGrid.setHeightFull();
            }
            gridContent.add(component);
        });

        showTabContent(payrollTab);
        dataTabs.addSelectedChangeListener(event -> showTabContent(event.getSelectedTab()));
    }

    private void showTabContent(Tab selectedTab) {
        tabContents.forEach((tab, component) -> component.setVisible(Objects.equals(tab, selectedTab)));
    }

    private void configureBpjsTkGrid() {
        bpjsTkGrid.setSizeFull();
        bpjsTkGrid.setSelectionMode(Grid.SelectionMode.NONE);
        bpjsTkGrid.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS, GridVariant.LUMO_ROW_STRIPES);

        bpjsTkEmployeeNumberColumn = bpjsTkGrid.addColumn(BpjsTkRow::getEmployeeNumber).setHeader("NIP").setWidth("120px").setFlexGrow(0);
        bpjsTkGrid.addColumn(BpjsTkRow::getFullName).setHeader("Nama Lengkap").setWidth("220px").setFlexGrow(1);
        bpjsTkGrid.addColumn(BpjsTkRow::getDateOfBirth).setHeader("Tanggal Lahir").setWidth("130px").setFlexGrow(0);
        bpjsTkWageColumn = bpjsTkGrid.addColumn(row -> fmt(row.getWage())).setHeader("Data Upah").setWidth("150px").setFlexGrow(0);
        bpjsTkJpBaseWageColumn = bpjsTkGrid.addColumn(row -> fmt(row.getJpBaseWage())).setHeader("Dasar Upah JP").setWidth("150px").setFlexGrow(0);
        bpjsTkEmployeeJpColumn = bpjsTkGrid.addColumn(row -> fmt(row.getEmployeeJp())).setHeader("Iuran JP TK").setWidth("140px").setFlexGrow(0);
        bpjsTkCompanyJpColumn = bpjsTkGrid.addColumn(row -> fmt(row.getCompanyJp())).setHeader("Iuran JP Perusahaan").setWidth("170px").setFlexGrow(0);
        bpjsTkCompanyJkkColumn = bpjsTkGrid.addColumn(row -> fmt(row.getCompanyJkk())).setHeader("Iuran JKK").setWidth("140px").setFlexGrow(0);
        bpjsTkCompanyJkmColumn = bpjsTkGrid.addColumn(row -> fmt(row.getCompanyJkm())).setHeader("Iuran JKM").setWidth("140px").setFlexGrow(0);
        bpjsTkEmployeeJhtColumn = bpjsTkGrid.addColumn(row -> fmt(row.getEmployeeJht())).setHeader("Iuran JHT TK").setWidth("150px").setFlexGrow(0);
        bpjsTkCompanyJhtColumn = bpjsTkGrid.addColumn(row -> fmt(row.getCompanyJht())).setHeader("Iuran JHT Perusahaan").setWidth("180px").setFlexGrow(0);
        bpjsTkTotalContributionColumn = bpjsTkGrid.addColumn(row -> fmt(row.getTotalContribution())).setHeader("Total Iuran").setWidth("150px").setFlexGrow(0);
        bpjsTkGrid.setEmptyStateText("Belum ada data BPJS TK");
        bpjsTkGrid.appendFooterRow().getCell(bpjsTkEmployeeNumberColumn).setComponent(createFooterLabel("Total"));
    }

    private void configureBpjsJknGrid() {
        bpjsJknGrid.setSizeFull();
        bpjsJknGrid.setSelectionMode(Grid.SelectionMode.NONE);
        bpjsJknGrid.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS, GridVariant.LUMO_ROW_STRIPES);

        bpjsJknGrid.addColumn(BpjsJknRow::getFullName).setHeader("Nama Lengkap").setWidth("220px").setFlexGrow(1);
        bpjsJknEmployeeNumberColumn = bpjsJknGrid.addColumn(BpjsJknRow::getEmployeeNumber).setHeader("NIP").setWidth("120px").setFlexGrow(0);
        bpjsJknWageColumn = bpjsJknGrid.addColumn(row -> fmt(row.getWage())).setHeader("Data Upah").setWidth("150px").setFlexGrow(0);
        bpjsJknBaseWageColumn = bpjsJknGrid.addColumn(row -> fmt(row.getJknBaseWage())).setHeader("Dasar Upah JKN").setWidth("160px").setFlexGrow(0);
        bpjsJknCompanyColumn = bpjsJknGrid.addColumn(row -> fmt(row.getCompanyJkn())).setHeader("JKN Perusahaan").setWidth("150px").setFlexGrow(0);
        bpjsJknEmployeeColumn = bpjsJknGrid.addColumn(row -> fmt(row.getEmployeeJkn())).setHeader("JKN TK").setWidth("120px").setFlexGrow(0);
        bpjsJknPremiumColumn = bpjsJknGrid.addColumn(row -> fmt(row.getPremium())).setHeader("Premi").setWidth("130px").setFlexGrow(0);
        bpjsJknGrid.setEmptyStateText("Belum ada data BPJS JKN");
        bpjsJknGrid.appendFooterRow().getCell(bpjsJknEmployeeNumberColumn).setComponent(createFooterLabel("Total"));
    }

    private void configurePajakGrid() {
        pajakGrid.setSizeFull();
        pajakGrid.setSelectionMode(Grid.SelectionMode.NONE);
        pajakGrid.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS, GridVariant.LUMO_ROW_STRIPES);

        pajakNoColumn = pajakGrid.addColumn(PajakRow::getRowNumber).setHeader("No").setWidth("70px").setFlexGrow(0);
        pajakGrid.addColumn(PajakRow::getEmployeeNumber).setHeader("NIP").setWidth("120px").setFlexGrow(0);
        pajakGrid.addColumn(PajakRow::getFullName).setHeader("Nama Lengkap").setWidth("220px").setFlexGrow(1);
        pajakGrid.addColumn(PajakRow::getPtkpCode).setHeader("Status").setWidth("110px").setFlexGrow(0);
        pajakGapokColumn = pajakGrid.addColumn(row -> fmt(row.getBaseSalary())).setHeader("Gapok").setWidth("130px").setFlexGrow(0);
        pajakJabatanColumn = pajakGrid.addColumn(row -> fmt(row.getPositionAllowance())).setHeader("Tunjangan Jabatan").setWidth("160px").setFlexGrow(0);
        pajakKeahlianColumn = pajakGrid.addColumn(row -> fmt(row.getSkillAllowance())).setHeader("Tunjangan Keahlian").setWidth("160px").setFlexGrow(0);
        pajakAllowanceColumn = pajakGrid.addColumn(row -> fmt(row.getVariableAllowance())).setHeader("Allowance").setWidth("140px").setFlexGrow(0);
        pajakJkkColumn = pajakGrid.addColumn(row -> fmt(row.getCompanyJkk())).setHeader("JKK").setWidth("130px").setFlexGrow(0);
        pajakJkmColumn = pajakGrid.addColumn(row -> fmt(row.getCompanyJkm())).setHeader("JKM").setWidth("130px").setFlexGrow(0);
        pajakJknColumn = pajakGrid.addColumn(row -> fmt(row.getCompanyJkn())).setHeader("JKN").setWidth("130px").setFlexGrow(0);
        pajakPenghasilanTeraturColumn = pajakGrid.addColumn(row -> fmt(row.getPenghasilanTeratur())).setHeader("Penghasilan Teratur").setWidth("170px").setFlexGrow(0);
        pajakDppTerColumn = pajakGrid.addColumn(row -> fmt(row.getTerDpp())).setHeader("DPP TER").setWidth("140px").setFlexGrow(0);
        pajakGrid.addColumn(PajakRow::getTerCategory).setHeader("Kategori TER").setWidth("130px").setFlexGrow(0);
        pajakGrid.addColumn(PajakRow::getTerRateDisplay).setHeader("Tarif TER").setWidth("110px").setFlexGrow(0);
        pajakPaid21Column = pajakGrid.addColumn(row -> fmt(row.getPph21Paid())).setHeader("21 PAID").setWidth("130px").setFlexGrow(0);
        pajakGrid.setEmptyStateText("Belum ada data PAJAK");
        pajakGrid.appendFooterRow().getCell(pajakNoColumn).setComponent(createFooterLabel("Total"));
    }

    private void configureDummyGrid(Grid<DummyPayrollRow> targetGrid, String sectionName) {
        targetGrid.setSizeFull();
        targetGrid.setSelectionMode(Grid.SelectionMode.NONE);
        targetGrid.addThemeVariants(GridVariant.LUMO_COLUMN_BORDERS, GridVariant.LUMO_ROW_STRIPES);

        targetGrid.addColumn(DummyPayrollRow::getColumnA).setHeader("A").setAutoWidth(true).setFlexGrow(1);
        targetGrid.addColumn(DummyPayrollRow::getColumnB).setHeader("B").setAutoWidth(true).setFlexGrow(1);
        targetGrid.addColumn(DummyPayrollRow::getColumnC).setHeader("C").setAutoWidth(true).setFlexGrow(1);
        targetGrid.addColumn(DummyPayrollRow::getColumnD).setHeader("D").setAutoWidth(true).setFlexGrow(1);
        targetGrid.addColumn(DummyPayrollRow::getColumnE).setHeader("E").setAutoWidth(true).setFlexGrow(1);
        targetGrid.addColumn(DummyPayrollRow::getColumnF).setHeader("F").setAutoWidth(true).setFlexGrow(1);
        targetGrid.setEmptyStateText("Belum ada data " + sectionName);
    }

    private DataProvider<BpjsTkRow, Void> createBpjsTkDataProvider() {
        Integer selectedYear = yearFilter.getValue();
        Integer selectedMonth = monthFilter.getValue();
        String searchTerm = searchField.getValue();

        MutableObject<LocalDate> mFilterDate = new MutableObject<>();
        if (selectedYear != null && selectedMonth != null) {
            mFilterDate.setValue(LocalDate.of(selectedYear, selectedMonth, 1));
        }

        return DataProvider.fromCallbacks(
                query -> payrollService.getPayrollPage(
                                PageRequest.of(query.getOffset() / query.getLimit(), query.getLimit()),
                                selectedYear,
                                mFilterDate.getValue(),
                                searchTerm
                        ).getContent().stream()
                        .map(this::mapToBpjsTkRow),
                query -> (int) payrollService.countPayroll(selectedYear, mFilterDate.getValue(), searchTerm)
        );
    }

    private DataProvider<BpjsJknRow, Void> createBpjsJknDataProvider() {
        Integer selectedYear = yearFilter.getValue();
        Integer selectedMonth = monthFilter.getValue();
        String searchTerm = searchField.getValue();

        MutableObject<LocalDate> mFilterDate = new MutableObject<>();
        if (selectedYear != null && selectedMonth != null) {
            mFilterDate.setValue(LocalDate.of(selectedYear, selectedMonth, 1));
        }

        return DataProvider.fromCallbacks(
                query -> payrollService.getPayrollPage(
                                PageRequest.of(query.getOffset() / query.getLimit(), query.getLimit()),
                                selectedYear,
                                mFilterDate.getValue(),
                                searchTerm
                        ).getContent().stream()
                        .map(this::mapToBpjsJknRow),
                query -> (int) payrollService.countPayroll(selectedYear, mFilterDate.getValue(), searchTerm)
        );
    }

    private DataProvider<PajakRow, Void> createPajakDataProvider() {
        Integer selectedYear = yearFilter.getValue();
        Integer selectedMonth = monthFilter.getValue();
        String searchTerm = searchField.getValue();

        MutableObject<LocalDate> mFilterDate = new MutableObject<>();
        if (selectedYear != null && selectedMonth != null) {
            mFilterDate.setValue(LocalDate.of(selectedYear, selectedMonth, 1));
        }

        return DataProvider.fromCallbacks(
                query -> {
                    List<HrPayroll> items = payrollService.getPayrollPage(
                            PageRequest.of(query.getOffset() / query.getLimit(), query.getLimit()),
                            selectedYear,
                            mFilterDate.getValue(),
                            searchTerm
                    ).getContent();

                    List<PajakRow> rows = new ArrayList<>();
                    for (int i = 0; i < items.size(); i++) {
                        rows.add(mapToPajakRow(items.get(i), query.getOffset() + i));
                    }
                    return rows.stream();
                },
                query -> (int) payrollService.countPayroll(selectedYear, mFilterDate.getValue(), searchTerm)
        );
    }

    private DataProvider<DummyPayrollRow, Void> createDummyDataProvider(String sectionName) {
        Integer selectedYear = yearFilter.getValue();
        Integer selectedMonth = monthFilter.getValue();
        String searchTerm = searchField.getValue();

        MutableObject<LocalDate> mFilterDate = new MutableObject<>();
        if (selectedYear != null && selectedMonth != null) {
            mFilterDate.setValue(LocalDate.of(selectedYear, selectedMonth, 1));
        }

        return DataProvider.fromCallbacks(
                query -> payrollService.getPayrollPage(
                                PageRequest.of(query.getOffset() / query.getLimit(), query.getLimit()),
                                selectedYear,
                                mFilterDate.getValue(),
                                searchTerm
                        ).getContent().stream()
                        .map(payroll -> DummyPayrollRow.from(sectionName, payroll)),
                query -> (int) payrollService.countPayroll(selectedYear, mFilterDate.getValue(), searchTerm)
        );
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
            addReadOnlyField(summary, "PPh 21 (TER)", fmt(calc.getPph21Deduction()));
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

    private List<HrPayrollComponent> getComponents(HrPayroll payroll) {
        HrPayrollCalculation calc = getCalc(payroll).orElse(null);
        if (calc == null || calc.getId() == null) {
            return Collections.emptyList();
        }

        return componentCache.computeIfAbsent(
                calc.getId(),
                payrollService::getPayrollComponentsByCalculationId
        );
    }

    private BigDecimal findComponentAmountByName(HrPayroll payroll, String componentName) {
        return getComponents(payroll).stream()
                .filter(c -> c.getComponentName() != null)
                .filter(c -> c.getComponentName().equalsIgnoreCase(componentName))
                .map(HrPayrollComponent::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BpjsTkRow mapToBpjsTkRow(HrPayroll payroll) {
        HrPayrollCalculation calc = getCalc(payroll).orElse(null);
        BigDecimal baseSalary = calc == null ? BigDecimal.ZERO : nvl(calc.getBaseSalary());
        BigDecimal fixedAllowance = calc == null ? BigDecimal.ZERO : nvl(calc.getFixedAllowanceTotal());
        BigDecimal wage = baseSalary.add(fixedAllowance);
        BigDecimal jpBaseWage = resolveBpjsBaseFromComponentOrConfig(payroll, "BPJS_JP_BASE", wage, getBpjsJpCap());

        BigDecimal employeeJp = sumComponentAmounts(payroll, Set.of("BPJS_JP"));
        BigDecimal companyJp = sumComponentAmounts(payroll, Set.of("BPJS_JP_COMPANY"));
        BigDecimal companyJkk = sumComponentAmounts(payroll, Set.of("BPJS_JKK_COMPANY"));
        BigDecimal companyJkm = sumComponentAmounts(payroll, Set.of("BPJS_JK_COMPANY"));
        BigDecimal employeeJht = sumComponentAmounts(payroll, Set.of("BPJS_JHT"));
        BigDecimal companyJht = sumComponentAmounts(payroll, Set.of("BPJS_JHT_COMPANY"));

        return new BpjsTkRow(
                blankToDash(payroll.getEmployeeNumber()),
                getEmployeeName(payroll),
                payroll.getDob() == null ? "-" : payroll.getDob().toString(),
                wage,
                jpBaseWage,
                employeeJp,
                companyJp,
                companyJkk,
                companyJkm,
                employeeJht,
                companyJht,
                employeeJp.add(companyJp).add(companyJkk).add(companyJkm).add(employeeJht).add(companyJht)
        );
    }

    private BpjsJknRow mapToBpjsJknRow(HrPayroll payroll) {
        HrPayrollCalculation calc = getCalc(payroll).orElse(null);
        BigDecimal baseSalary = calc == null ? BigDecimal.ZERO : nvl(calc.getBaseSalary());
        BigDecimal fixedAllowance = calc == null ? BigDecimal.ZERO : nvl(calc.getFixedAllowanceTotal());
        BigDecimal wage = baseSalary.add(fixedAllowance);
        BigDecimal jknBaseWage = resolveBpjsBaseFromComponentOrConfig(payroll, "BPJS_JKN_BASE", wage, getBpjsJknCap());

        BigDecimal companyJkn = sumComponentAmounts(payroll, Set.of("BPJS_JKN_COMPANY"));
        BigDecimal employeeJkn = sumComponentAmounts(payroll, Set.of("BPJS_JKN"));

        return new BpjsJknRow(
                getEmployeeName(payroll),
                blankToDash(payroll.getEmployeeNumber()),
                wage,
                jknBaseWage,
                companyJkn,
                employeeJkn,
                companyJkn.add(employeeJkn)
        );
    }

    private PajakRow mapToPajakRow(HrPayroll payroll, int offset) {
        HrPayrollCalculation calc = getCalc(payroll).orElse(null);
        BigDecimal baseSalary = calc == null ? BigDecimal.ZERO : nvl(calc.getBaseSalary());
        BigDecimal penghasilanTeratur = calc == null ? BigDecimal.ZERO : nvl(calc.getPenghasilanTeraturAmount());
        BigDecimal terDpp = calc == null ? BigDecimal.ZERO : nvl(calc.getTerDppAmount());
        String terCategory = calc == null ? "-" : blankToDash(calc.getTerCategory());
        BigDecimal terRate = calc == null ? BigDecimal.ZERO : nvl(calc.getTerRatePercent());

        return new PajakRow(
                payroll.getId() == null ? offset + 1 : rowNoByPayrollId.getOrDefault(payroll.getId(), offset + 1),
                blankToDash(payroll.getEmployeeNumber()),
                getEmployeeName(payroll),
                blankToDash(payroll.getPtkpCode()),
                baseSalary,
                sumComponentAmounts(payroll, Set.of("JABATAN")),
                sumComponentAmounts(payroll, Set.of("KEAHLIAN")),
                sumComponentAmountsByGroup(payroll, "VARIABLE_ALLOWANCE"),
                sumComponentAmounts(payroll, Set.of("BPJS_JKK_COMPANY")),
                sumComponentAmounts(payroll, Set.of("BPJS_JK_COMPANY")),
                sumComponentAmounts(payroll, Set.of("BPJS_JKN_COMPANY")),
                penghasilanTeratur,
                terDpp,
                terCategory,
                formatPercent(terRate),
                sumComponentAmounts(payroll, Set.of("PPH21"))
        );
    }

    private BigDecimal sumComponentAmounts(HrPayroll payroll, Set<String> codes) {
        return getComponents(payroll).stream()
                .filter(component -> component.getComponentCode() != null)
                .filter(component -> codes.contains(component.getComponentCode().toUpperCase()))
                .map(HrPayrollComponent::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumComponentAmountsByGroup(HrPayroll payroll, String componentGroup) {
        if (componentGroup == null || componentGroup.isBlank()) {
            return BigDecimal.ZERO;
        }
        return getComponents(payroll).stream()
                .filter(component -> component.getComponentGroup() != null)
                .filter(component -> componentGroup.equalsIgnoreCase(component.getComponentGroup()))
                .map(HrPayrollComponent::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getBpjsJpCap() {
        try {
            return parseSystemNumber(systemService.findSystemById(BPJS_JP_CAP_CONFIG_ID));
        } catch (Exception ex) {
            log.warn("Gagal membaca config batas upah JP", ex);
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal getBpjsJknCap() {
        try {
            return parseSystemNumber(systemService.findSystemById(BPJS_JKN_CAP_CONFIG_ID));
        } catch (Exception ex) {
            log.warn("Gagal membaca config batas upah JKN", ex);
            return BigDecimal.ZERO;
        }
    }

    private BigDecimal parseSystemNumber(com.fusi24.pangreksa.web.model.entity.FwSystem config) {
        if (config == null) {
            return BigDecimal.ZERO;
        }
        if (config.getStringVal() != null && !config.getStringVal().isBlank()) {
            return new BigDecimal(config.getStringVal().replace(",", "").trim());
        }
        if (config.getDecimalVal() != null) {
            return config.getDecimalVal();
        }
        if (config.getIntVal() != null) {
            return BigDecimal.valueOf(config.getIntVal());
        }
        if (config.getKey() != null && !config.getKey().isBlank()) {
            return new BigDecimal(config.getKey().replace(",", "").trim());
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal resolveBpjsBaseFromComponentOrConfig(HrPayroll payroll,
                                                            String componentCode,
                                                            BigDecimal wage,
                                                            BigDecimal fallbackCap) {
        BigDecimal storedBase = sumComponentAmounts(payroll, Set.of(componentCode));
        if (storedBase.signum() > 0) {
            return storedBase;
        }
        return applyCap(wage, fallbackCap);
    }

    private BigDecimal applyCap(BigDecimal amount, BigDecimal cap) {
        BigDecimal normalizedAmount = nvl(amount);
        BigDecimal normalizedCap = nvl(cap);
        if (normalizedCap.signum() <= 0) {
            return normalizedAmount;
        }
        return normalizedAmount.min(normalizedCap);
    }

    private String formatPercent(BigDecimal value) {
        return nvl(value).stripTrailingZeros().toPlainString() + "%";
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

            allowanceMode.setItems("NO ALLOWANCE", "BENEFITS PACKAGE");
            allowanceMode.setValue("BENEFITS PACKAGE");
            allowanceMode.setWidthFull();

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
            FormItem overtimeStaticFormItem = addFormItem(overtimeStaticNominal, "Nominal Upah (Rp)");
            FormItem overtimePercentFormItem = addFormItem(overtimePercent, "Persentase (%)");
            overtimePercentFormItem.setVisible(false);

            overtimePaymentType.addValueChangeListener(e -> {
                boolean isStatic = "STATIC".equalsIgnoreCase(e.getValue());
                overtimeStaticNominal.setVisible(isStatic);
                overtimeStaticFormItem.setVisible(isStatic);
                overtimePercent.setVisible(!isStatic);
                overtimePercentFormItem.setVisible(!isStatic);
            });

            Span s3 = new Span("Allowance");
            s3.getStyle().set("font-weight", "600");
            add(s3);
            setColspan(s3, 2);

            addFormItem(allowanceMode, "Allowance Option");

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

                bean.setSelectedAllowances(new ArrayList<>());

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

            allowanceMode.setItems("NO ALLOWANCE", "BENEFITS PACKAGE");
            allowanceMode.setValue("NO ALLOWANCE");
            allowanceMode.setWidthFull();

            saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
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
            FormItem overtimeStaticFormItem = addFormItem(overtimeStaticNominal, "Nominal Upah (Rp)");
            FormItem overtimePercentFormItem = addFormItem(overtimePercent, "Persentase (%)");
            overtimePercentFormItem.setVisible(false);

            overtimePaymentType.addValueChangeListener(e -> {
                boolean isStatic = "STATIC".equalsIgnoreCase(e.getValue());
                overtimeStaticNominal.setVisible(isStatic);
                overtimeStaticFormItem.setVisible(isStatic);
                overtimePercent.setVisible(!isStatic);
                overtimePercentFormItem.setVisible(!isStatic);
            });

            Span s3 = new Span("Allowance");
            s3.getStyle().set("font-weight", "600");
            add(s3);
            setColspan(s3, 2);

            addFormItem(allowanceMode, "Allowance Option");

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

                bean.setSelectedAllowances(new ArrayList<>());

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

    private void updateFooterSummaries(List<HrPayroll> filteredPayrolls) {
        updatePayrollFooter(filteredPayrolls);

        List<BpjsTkRow> bpjsTkRows = filteredPayrolls.stream()
                .map(this::mapToBpjsTkRow)
                .toList();
        updateBpjsTkFooter(bpjsTkRows);

        List<BpjsJknRow> bpjsJknRows = filteredPayrolls.stream()
                .map(this::mapToBpjsJknRow)
                .toList();
        updateBpjsJknFooter(bpjsJknRows);

        List<PajakRow> pajakRows = new ArrayList<>();
        for (int i = 0; i < filteredPayrolls.size(); i++) {
            pajakRows.add(mapToPajakRow(filteredPayrolls.get(i), i));
        }
        updatePajakFooter(pajakRows);
    }

    private void updatePayrollFooter(List<HrPayroll> filteredPayrolls) {
        FooterRow footerRow = grid.getFooterRows().get(0);
        footerRow.getCell(payrollGapokColumn).setComponent(createFooterLabel(fmt(filteredPayrolls.stream().map(HrPayroll::getBaseSalary).reduce(BigDecimal.ZERO, this::safeAdd))));
        footerRow.getCell(payrollJabatanColumn).setComponent(createFooterLabel(fmt(sumPayrolls(filteredPayrolls, payroll -> findComponentAmountByName(payroll, "Jabatan")))));
        footerRow.getCell(payrollKeahlianColumn).setComponent(createFooterLabel(fmt(sumPayrolls(filteredPayrolls, payroll -> findComponentAmountByName(payroll, "Keahlian")))));
        footerRow.getCell(payrollFixedAllowanceColumn).setComponent(createFooterLabel(fmt(sumPayrolls(filteredPayrolls, payroll -> getCalc(payroll).map(HrPayrollCalculation::getFixedAllowanceTotal).orElse(BigDecimal.ZERO)))));
        footerRow.getCell(payrollAllowanceColumn).setComponent(createFooterLabel(fmt(sumPayrolls(filteredPayrolls, payroll -> getCalc(payroll).map(HrPayrollCalculation::getVariableAllowanceTotal).orElse(BigDecimal.ZERO)))));
        footerRow.getCell(payrollBpjsTkColumn).setComponent(createFooterLabel(fmt(sumPayrolls(filteredPayrolls, payroll -> sumComponentByCodes(payroll, Set.of("BPJS_JHT_COMPANY", "BPJS_JP_COMPANY", "BPJS_JKK_COMPANY", "BPJS_JK_COMPANY", "BPJS_JHT", "BPJS_JP", "BPJS_JKK", "BPJS_JK"))))));
        footerRow.getCell(payrollBpjsJknColumn).setComponent(createFooterLabel(fmt(sumPayrolls(filteredPayrolls, payroll -> sumComponentByCodes(payroll, Set.of("BPJS_JKN_COMPANY", "BPJS_JKN"))))));
        footerRow.getCell(payrollVariableAllowanceColumn).setComponent(createFooterLabel(fmt(sumPayrolls(filteredPayrolls, payroll -> {
            HrPayrollCalculation calc = getCalc(payroll).orElse(null);
            if (calc == null) return BigDecimal.ZERO;
            BigDecimal bpjsTk = sumComponentByCodes(payroll, Set.of("BPJS_JHT_COMPANY", "BPJS_JP_COMPANY", "BPJS_JKK_COMPANY", "BPJS_JK_COMPANY", "BPJS_JHT", "BPJS_JP", "BPJS_JKK", "BPJS_JK"));
            BigDecimal bpjsJkn = sumComponentByCodes(payroll, Set.of("BPJS_JKN_COMPANY", "BPJS_JKN"));
            return nvl(calc.getVariableAllowanceTotal()).add(bpjsTk).add(bpjsJkn);
        }))));
        footerRow.getCell(payrollGrossColumn).setComponent(createFooterLabel(fmt(sumPayrolls(filteredPayrolls, payroll -> getCalc(payroll).map(HrPayrollCalculation::getGrossSalary).orElse(BigDecimal.ZERO)))));
        footerRow.getCell(payrollInsuranceColumn).setComponent(createFooterLabel(fmt(sumPayrolls(filteredPayrolls, payroll -> {
            BigDecimal bpjsTk = sumComponentByCodes(payroll, Set.of("BPJS_JHT_COMPANY", "BPJS_JP_COMPANY", "BPJS_JKK_COMPANY", "BPJS_JK_COMPANY", "BPJS_JHT", "BPJS_JP", "BPJS_JKK", "BPJS_JK"));
            BigDecimal bpjsJkn = sumComponentByCodes(payroll, Set.of("BPJS_JKN_COMPANY", "BPJS_JKN"));
            return bpjsTk.add(bpjsJkn);
        }))));
        footerRow.getCell(payrollPphColumn).setComponent(createFooterLabel(fmt(sumPayrolls(filteredPayrolls, payroll -> getCalc(payroll).map(HrPayrollCalculation::getPph21Deduction).orElse(BigDecimal.ZERO)))));
        footerRow.getCell(payrollThpColumn).setComponent(createFooterLabel(fmt(sumPayrolls(filteredPayrolls, payroll -> getCalc(payroll).map(HrPayrollCalculation::getNetTakeHomePay).orElse(BigDecimal.ZERO)))));
    }

    private void updateBpjsTkFooter(List<BpjsTkRow> rows) {
        FooterRow footerRow = bpjsTkGrid.getFooterRows().get(0);
        footerRow.getCell(bpjsTkWageColumn).setComponent(createFooterLabel(fmt(sumRows(rows, BpjsTkRow::getWage))));
        footerRow.getCell(bpjsTkJpBaseWageColumn).setComponent(createFooterLabel(fmt(sumRows(rows, BpjsTkRow::getJpBaseWage))));
        footerRow.getCell(bpjsTkEmployeeJpColumn).setComponent(createFooterLabel(fmt(sumRows(rows, BpjsTkRow::getEmployeeJp))));
        footerRow.getCell(bpjsTkCompanyJpColumn).setComponent(createFooterLabel(fmt(sumRows(rows, BpjsTkRow::getCompanyJp))));
        footerRow.getCell(bpjsTkCompanyJkkColumn).setComponent(createFooterLabel(fmt(sumRows(rows, BpjsTkRow::getCompanyJkk))));
        footerRow.getCell(bpjsTkCompanyJkmColumn).setComponent(createFooterLabel(fmt(sumRows(rows, BpjsTkRow::getCompanyJkm))));
        footerRow.getCell(bpjsTkEmployeeJhtColumn).setComponent(createFooterLabel(fmt(sumRows(rows, BpjsTkRow::getEmployeeJht))));
        footerRow.getCell(bpjsTkCompanyJhtColumn).setComponent(createFooterLabel(fmt(sumRows(rows, BpjsTkRow::getCompanyJht))));
        footerRow.getCell(bpjsTkTotalContributionColumn).setComponent(createFooterLabel(fmt(sumRows(rows, BpjsTkRow::getTotalContribution))));
    }

    private void updateBpjsJknFooter(List<BpjsJknRow> rows) {
        FooterRow footerRow = bpjsJknGrid.getFooterRows().get(0);
        footerRow.getCell(bpjsJknWageColumn).setComponent(createFooterLabel(fmt(sumRows(rows, BpjsJknRow::getWage))));
        footerRow.getCell(bpjsJknBaseWageColumn).setComponent(createFooterLabel(fmt(sumRows(rows, BpjsJknRow::getJknBaseWage))));
        footerRow.getCell(bpjsJknCompanyColumn).setComponent(createFooterLabel(fmt(sumRows(rows, BpjsJknRow::getCompanyJkn))));
        footerRow.getCell(bpjsJknEmployeeColumn).setComponent(createFooterLabel(fmt(sumRows(rows, BpjsJknRow::getEmployeeJkn))));
        footerRow.getCell(bpjsJknPremiumColumn).setComponent(createFooterLabel(fmt(sumRows(rows, BpjsJknRow::getPremium))));
    }

    private void updatePajakFooter(List<PajakRow> rows) {
        FooterRow footerRow = pajakGrid.getFooterRows().get(0);
        footerRow.getCell(pajakGapokColumn).setComponent(createFooterLabel(fmt(sumRows(rows, PajakRow::getBaseSalary))));
        footerRow.getCell(pajakJabatanColumn).setComponent(createFooterLabel(fmt(sumRows(rows, PajakRow::getPositionAllowance))));
        footerRow.getCell(pajakKeahlianColumn).setComponent(createFooterLabel(fmt(sumRows(rows, PajakRow::getSkillAllowance))));
        footerRow.getCell(pajakAllowanceColumn).setComponent(createFooterLabel(fmt(sumRows(rows, PajakRow::getVariableAllowance))));
        footerRow.getCell(pajakJkkColumn).setComponent(createFooterLabel(fmt(sumRows(rows, PajakRow::getCompanyJkk))));
        footerRow.getCell(pajakJkmColumn).setComponent(createFooterLabel(fmt(sumRows(rows, PajakRow::getCompanyJkm))));
        footerRow.getCell(pajakJknColumn).setComponent(createFooterLabel(fmt(sumRows(rows, PajakRow::getCompanyJkn))));
        footerRow.getCell(pajakPenghasilanTeraturColumn).setComponent(createFooterLabel(fmt(sumRows(rows, PajakRow::getPenghasilanTeratur))));
        footerRow.getCell(pajakDppTerColumn).setComponent(createFooterLabel(fmt(sumRows(rows, PajakRow::getTerDpp))));
        footerRow.getCell(pajakPaid21Column).setComponent(createFooterLabel(fmt(sumRows(rows, PajakRow::getPph21Paid))));
    }

    private Span createFooterLabel(String text) {
        Span span = new Span(text);
        span.getStyle().set("font-weight", "700");
        return span;
    }

    private BigDecimal safeAdd(BigDecimal left, BigDecimal right) {
        return nvl(left).add(nvl(right));
    }

    private BigDecimal sumPayrolls(List<HrPayroll> payrolls, java.util.function.Function<HrPayroll, BigDecimal> extractor) {
        return payrolls.stream()
                .map(extractor)
                .reduce(BigDecimal.ZERO, this::safeAdd);
    }

    private <T> BigDecimal sumRows(List<T> rows, java.util.function.Function<T, BigDecimal> extractor) {
        return rows.stream()
                .map(extractor)
                .reduce(BigDecimal.ZERO, this::safeAdd);
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
        return sumComponentAmounts(payroll, codes);
    }

    @Getter
    private static class DummyPayrollRow {
        private final String columnA;
        private final String columnB;
        private final String columnC;
        private final String columnD;
        private final String columnE;
        private final String columnF;

        private DummyPayrollRow(String columnA, String columnB, String columnC, String columnD, String columnE, String columnF) {
            this.columnA = columnA;
            this.columnB = columnB;
            this.columnC = columnC;
            this.columnD = columnD;
            this.columnE = columnE;
            this.columnF = columnF;
        }

        private static DummyPayrollRow from(String sectionName, HrPayroll payroll) {
            String payrollMonth = payroll.getPayrollDate() == null
                    ? "-"
                    : payroll.getPayrollDate().getMonth().getDisplayName(TextStyle.SHORT, new Locale("id", "ID"))
                      + " " + payroll.getPayrollDate().getYear();

            return new DummyPayrollRow(
                    sectionName,
                    payroll.getEmployeeNumber() == null ? "-" : payroll.getEmployeeNumber(),
                    Stream.of(payroll.getFirstName(), payroll.getMiddleName(), payroll.getLastName())
                            .filter(Objects::nonNull)
                            .filter(name -> !name.isBlank())
                            .collect(Collectors.joining(" ")),
                    payroll.getDepartment() == null ? "-" : payroll.getDepartment(),
                    payrollMonth,
                    FormattingUtils.formatPayrollAmount(payroll.getBaseSalary())
            );
        }
    }

    @Getter
    private static class BpjsTkRow {
        private final String employeeNumber;
        private final String fullName;
        private final String dateOfBirth;
        private final BigDecimal wage;
        private final BigDecimal jpBaseWage;
        private final BigDecimal employeeJp;
        private final BigDecimal companyJp;
        private final BigDecimal companyJkk;
        private final BigDecimal companyJkm;
        private final BigDecimal employeeJht;
        private final BigDecimal companyJht;
        private final BigDecimal totalContribution;

        private BpjsTkRow(String employeeNumber,
                          String fullName,
                          String dateOfBirth,
                          BigDecimal wage,
                          BigDecimal jpBaseWage,
                          BigDecimal employeeJp,
                          BigDecimal companyJp,
                          BigDecimal companyJkk,
                          BigDecimal companyJkm,
                          BigDecimal employeeJht,
                          BigDecimal companyJht,
                          BigDecimal totalContribution) {
            this.employeeNumber = employeeNumber;
            this.fullName = fullName;
            this.dateOfBirth = dateOfBirth;
            this.wage = wage;
            this.jpBaseWage = jpBaseWage;
            this.employeeJp = employeeJp;
            this.companyJp = companyJp;
            this.companyJkk = companyJkk;
            this.companyJkm = companyJkm;
            this.employeeJht = employeeJht;
            this.companyJht = companyJht;
            this.totalContribution = totalContribution;
        }
    }

    @Getter
    private static class BpjsJknRow {
        private final String fullName;
        private final String employeeNumber;
        private final BigDecimal wage;
        private final BigDecimal jknBaseWage;
        private final BigDecimal companyJkn;
        private final BigDecimal employeeJkn;
        private final BigDecimal premium;

        private BpjsJknRow(String fullName,
                           String employeeNumber,
                           BigDecimal wage,
                           BigDecimal jknBaseWage,
                           BigDecimal companyJkn,
                           BigDecimal employeeJkn,
                           BigDecimal premium) {
            this.fullName = fullName;
            this.employeeNumber = employeeNumber;
            this.wage = wage;
            this.jknBaseWage = jknBaseWage;
            this.companyJkn = companyJkn;
            this.employeeJkn = employeeJkn;
            this.premium = premium;
        }
    }

    @Getter
    private static class PajakRow {
        private final Integer rowNumber;
        private final String employeeNumber;
        private final String fullName;
        private final String ptkpCode;
        private final BigDecimal baseSalary;
        private final BigDecimal positionAllowance;
        private final BigDecimal skillAllowance;
        private final BigDecimal variableAllowance;
        private final BigDecimal companyJkk;
        private final BigDecimal companyJkm;
        private final BigDecimal companyJkn;
        private final BigDecimal penghasilanTeratur;
        private final BigDecimal terDpp;
        private final String terCategory;
        private final String terRateDisplay;
        private final BigDecimal pph21Paid;

        private PajakRow(Integer rowNumber,
                         String employeeNumber,
                         String fullName,
                         String ptkpCode,
                         BigDecimal baseSalary,
                         BigDecimal positionAllowance,
                         BigDecimal skillAllowance,
                         BigDecimal variableAllowance,
                         BigDecimal companyJkk,
                         BigDecimal companyJkm,
                         BigDecimal companyJkn,
                         BigDecimal penghasilanTeratur,
                         BigDecimal terDpp,
                         String terCategory,
                         String terRateDisplay,
                         BigDecimal pph21Paid) {
            this.rowNumber = rowNumber;
            this.employeeNumber = employeeNumber;
            this.fullName = fullName;
            this.ptkpCode = ptkpCode;
            this.baseSalary = baseSalary;
            this.positionAllowance = positionAllowance;
            this.skillAllowance = skillAllowance;
            this.variableAllowance = variableAllowance;
            this.companyJkk = companyJkk;
            this.companyJkm = companyJkm;
            this.companyJkn = companyJkn;
            this.penghasilanTeratur = penghasilanTeratur;
            this.terDpp = terDpp;
            this.terCategory = terCategory;
            this.terRateDisplay = terRateDisplay;
            this.pph21Paid = pph21Paid;
        }
    }

}
