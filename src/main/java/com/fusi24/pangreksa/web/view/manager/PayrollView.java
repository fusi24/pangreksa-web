package com.fusi24.pangreksa.web.view.manager;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.base.ui.notification.AppNotification;
import com.fusi24.pangreksa.base.util.FormattingUtils;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.entity.*;
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
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.BigDecimalField;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.concurrent.DelegatingSecurityContextExecutorService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;

@Route("payroll-list-page-access")
@PageTitle("Payroll")
@Menu(order = 27, icon = "vaadin:clipboard-check", title = "Payroll")
@RolesAllowed("PAYROLL")
public class PayrollView extends Main {
    private static final Logger log = LoggerFactory.getLogger(PayrollView.class);

    private final CurrentUser currentUser;
    private final PayrollService payrollService;
    private final SystemService systemService;

    private final TextField searchField = new TextField();
    private final ComboBox<Integer> yearFilter = new ComboBox<>();
    private final ComboBox<Integer> monthFilter = new ComboBox<>();
    private final TabSheet tabSheet = new TabSheet();
    private final Map<Long, HrPayrollCalculation> calculationCache = new ConcurrentHashMap<>();

    private final DelegatingSecurityContextExecutorService executor =
            new DelegatingSecurityContextExecutorService(Executors.newFixedThreadPool(4));

    public PayrollView(CurrentUser currentUser, SystemService systemService, PayrollService payrollService) {
        this.currentUser = currentUser;
        this.systemService = systemService;
        this.payrollService = payrollService;

        // FIX: Penting agar tidak "App user is not set"
        this.payrollService.setUser(currentUser.require());

        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN, LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar("Payroll Management"));
        initializeView();
    }

    private void initializeView() {
        setHeightFull();
        setupFilters();

        tabSheet.add("Ringkasan Gaji", createPayrollGrid());
        tabSheet.add("BPJS TK (Ketenagakerjaan)", createBpjsTkGrid());
        tabSheet.add("BPJS JKN (Kesehatan)", createBpjsJknGrid());
        tabSheet.setSizeFull();

        add(tabSheet);
        applyFilters();
    }

    private void setupFilters() {
        searchField.setPlaceholder("Cari NIK / Nama...");
        searchField.setWidth("300px");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.LAZY);

        yearFilter.setPlaceholder("Tahun");
        yearFilter.setItems(2025, 2026);
        yearFilter.setValue(2026);
        yearFilter.setWidth("120px");

        monthFilter.setPlaceholder("Bulan");
        monthFilter.setItems(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12);
        monthFilter.setItemLabelGenerator(m -> Month.of(m).getDisplayName(TextStyle.FULL, Locale.forLanguageTag("id-ID")));
        monthFilter.setValue(LocalDate.now().getMonthValue());
        monthFilter.setWidth("150px");

        Button searchBtn = new Button(new Icon(VaadinIcon.SEARCH), e -> applyFilters());
        searchBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        searchBtn.setTooltipText("Muat Data");

        Button resetBtn = new Button(new Icon(VaadinIcon.RECYCLE), e -> {
            searchField.clear();
            yearFilter.setValue(2026);
            monthFilter.setValue(LocalDate.now().getMonthValue());
            applyFilters();
        });
        resetBtn.setTooltipText("Reset Filter");

        Button addButton = new Button("Add Payroll", new Icon(VaadinIcon.PLUS), e -> openAddPayrollDialog());
        addButton.addThemeVariants(ButtonVariant.LUMO_SUCCESS);

        HorizontalLayout filterBar = new HorizontalLayout(yearFilter, monthFilter, searchField, searchBtn, resetBtn, addButton);
        filterBar.setAlignItems(FlexComponent.Alignment.END);
        filterBar.setWidthFull();
        filterBar.expand(searchField);
        add(filterBar);
    }

    private Grid<HrPayroll> createPayrollGrid() {
        Grid<HrPayroll> grid = new Grid<>(HrPayroll.class, false);
        grid.setSelectionMode(Grid.SelectionMode.MULTI);

        // 1. Karyawan Group
//        grid.addColumn(p -> "").setHeader("No").setWidth("60px").setFlexGrow(0); // No urut bisa dihandle di dataprovider atau manual
        grid.addColumn(HrPayroll::getEmployeeNumber).setHeader("NIK").setWidth("140px").setFlexGrow(0);
        grid.addColumn(this::getEmployeeName).setHeader("Nama Karyawan").setAutoWidth(true).setFlexGrow(1);

        // 2. Penghasilan Group
        grid.addColumn(p -> fmt(getCalc(p).getBaseSalary())).setHeader("Gapok").setWidth("140px");

        // Tunjangan spesifik (Jabatan & Keahlian) diambil dari komponen
        grid.addColumn(p -> fmt(findComponentAmountByName(p, "Jabatan"))).setHeader("T. Jabatan").setWidth("140px");
        grid.addColumn(p -> fmt(findComponentAmountByName(p, "Keahlian"))).setHeader("Keahlian").setWidth("140px");

        grid.addColumn(p -> fmt(getCalc(p).getFixedAllowanceTotal())).setHeader("Tunj. Tetap").setWidth("140px");
        grid.addColumn(p -> fmt(getCalc(p).getVariableAllowanceTotal())).setHeader("Allowance").setWidth("140px");

        // BPJS sisi Perusahaan (JHT + JP + JKK + JKM)
        grid.addColumn(p -> {
            HrPayrollCalculation c = getCalc(p);
            BigDecimal totalTkCo = nvl(c.getBpjsJhtCompany())
                    .add(nvl(c.getBpjsJpCompany()))
                    .add(nvl(c.getBpjsJkkCompany()))
                    .add(nvl(c.getBpjsJkmCompany()));
            return fmt(totalTkCo);
        }).setHeader("BPJS TK").setWidth("140px");

        grid.addColumn(p -> fmt(getCalc(p).getBpjsJknCompany())).setHeader("BPJS JKN").setWidth("140px");

        // Tunjangan Tidak Tetap (Allowance + BPJS Perusahaan)
        grid.addColumn(p -> {
            HrPayrollCalculation c = getCalc(p);
            BigDecimal totalVar = nvl(c.getVariableAllowanceTotal())
                    .add(nvl(c.getBpjsJhtCompany()))
                    .add(nvl(c.getBpjsJpCompany()))
                    .add(nvl(c.getBpjsJkkCompany()))
                    .add(nvl(c.getBpjsJkmCompany()))
                    .add(nvl(c.getBpjsJknCompany()));
            return fmt(totalVar);
        }).setHeader("Tunj. Tidak Tetap").setWidth("180px");

        grid.addColumn(p -> fmt(getCalc(p).getGrossSalary())).setHeader("Penghasilan Kotor").setWidth("170px");

        // 3. Potongan & Hasil Group
        // Asuransi (Potongan JHT + JP + JKN dari karyawan)
        grid.addColumn(p -> {
            HrPayrollCalculation c = getCalc(p);
            BigDecimal totalIns = nvl(c.getBpjsJhtDeduction())
                    .add(nvl(c.getBpjsJpDeduction()))
                    .add(nvl(c.getBpjsJknDeduction()));
            return fmt(totalIns);
        }).setHeader("Asuransi").setWidth("140px");

        grid.addColumn(p -> fmt(getCalc(p).getPph21Deduction())).setHeader("PPh 21").setWidth("140px");
        grid.addColumn(p -> fmt(getCalc(p).getNetTakeHomePay())).setHeader("THP").setWidth("150px").setClassNameGenerator(p -> "font-bold");

        // 4. Actions
        grid.addComponentColumn(payroll -> {
            HorizontalLayout hl = new HorizontalLayout();
            Button det = new Button("Detail", e -> openDetailDialog(payroll));
            det.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_TERTIARY);
            Button rec = new Button("Recalculate", e -> openRecalculateDialog(payroll));
            rec.addThemeVariants(ButtonVariant.LUMO_SMALL, ButtonVariant.LUMO_ERROR);
            hl.add(det, rec);
            return hl;
        }).setHeader("Actions").setFrozenToEnd(true).setWidth("200px");

        setupGridDefaults(grid);
        return grid;
    }

    private BigDecimal findComponentAmountByName(HrPayroll payroll, String componentName) {
        HrPayrollCalculation calc = getCalc(payroll);
        if (calc == null || calc.getId() == null) return BigDecimal.ZERO;

        // Ambil detail komponen dari database
        List<HrPayrollComponent> components = payrollService.getPayrollComponentsByCalculationId(calc.getId());
        return components.stream()
                .filter(c -> c.getComponentName() != null)
                .filter(c -> c.getComponentName().toLowerCase().contains(componentName.toLowerCase()))
                .map(HrPayrollComponent::getAmount)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private Grid<HrPayroll> createBpjsTkGrid() {
        Grid<HrPayroll> grid = new Grid<>(HrPayroll.class, false);

        // Kolom-kolom Dasar (Identitas)
//        grid.addColumn(p -> "").setHeader("No").setWidth("50px").setFlexGrow(0);
        grid.addColumn(p -> p.getPerson() != null ? p.getPerson().getKtpNumber() : "-").setHeader("Nomor KTP").setWidth("180px");
        grid.addColumn(HrPayroll::getEmployeeNumber).setHeader("NIK").setWidth("140px");
        grid.addColumn(this::getEmployeeName).setHeader("Nama Tenaga Kerja").setWidth("200px");

        grid.addColumn(p -> p.getDob() != null ? p.getDob().toString() : "-").setHeader("Tgl Lahir").setWidth("120px");

        // Data Upah (Gapok + Fixed)
        grid.addColumn(p -> fmt(getCalc(p).getBaseSalary().add(getCalc(p).getFixedAllowanceTotal())))
                .setHeader("Data Upah (Rp.)").setWidth("150px");

        // Dasar Upah JP (Capped)
        grid.addColumn(p -> {
            BigDecimal totalUpah = nvl(getCalc(p).getBaseSalary()).add(nvl(getCalc(p).getFixedAllowanceTotal()));
            BigDecimal capJp = new BigDecimal("10547400"); // Sesuai data 2026
            return fmt(totalUpah.min(capJp));
        }).setHeader("Dasar Upah JP").setWidth("150px");

        // Iuran JP (TK & Prsh)
        Grid.Column<HrPayroll> jpTkCol = grid.addColumn(p -> fmt(getCalc(p).getBpjsJpDeduction())).setHeader("TK (1%)").setWidth("120px");
        Grid.Column<HrPayroll> jpPrshCol = grid.addColumn(p -> fmt(getCalc(p).getBpjsJpCompany())).setHeader("Prsh (2%)").setWidth("120px");

        // JKK & JKM
        Grid.Column<HrPayroll> jkkCol = grid.addColumn(p -> fmt(getCalc(p).getBpjsJkkCompany())).setHeader("JKK (0.24%)").setWidth("120px");
        Grid.Column<HrPayroll> jkmCol = grid.addColumn(p -> fmt(getCalc(p).getBpjsJkmCompany())).setHeader("JKM (0.3%)").setWidth("120px");

        // Iuran JHT (TK & Prsh)
        Grid.Column<HrPayroll> jhtTkCol = grid.addColumn(p -> fmt(getCalc(p).getBpjsJhtDeduction())).setHeader("TK (2%)").setWidth("120px");
        Grid.Column<HrPayroll> jhtPrshCol = grid.addColumn(p -> fmt(getCalc(p).getBpjsJhtCompany())).setHeader("Prsh (3.7%)").setWidth("120px");

        // Total Iuran (Sisi Perusahaan + Sisi Karyawan)
        grid.addColumn(p -> {
            HrPayrollCalculation c = getCalc(p);
            BigDecimal total = nvl(c.getBpjsJpCompany()).add(nvl(c.getBpjsJpDeduction()))
                    .add(nvl(c.getBpjsJkkCompany()))
                    .add(nvl(c.getBpjsJkmCompany()))
                    .add(nvl(c.getBpjsJhtCompany())).add(nvl(c.getBpjsJhtDeduction()));
            return fmt(total);
        }).setHeader("Total Iuran").setWidth("150px");

        // Kolom Tambahan Excel: JKK+JKM dan JHT+JP TK
//        grid.addColumn(p -> {
//            HrPayrollCalculation c = getCalc(p);
//            return fmt(nvl(c.getBpjsJkkCompany()).add(nvl(c.getBpjsJkmCompany())));
//        }).setHeader("JKK + JKM").setWidth("130px");
//
//        grid.addColumn(p -> {
//            HrPayrollCalculation c = getCalc(p);
//            return fmt(nvl(c.getBpjsJhtDeduction()).add(nvl(c.getBpjsJpDeduction())));
//        }).setHeader("JHT + JP TK").setWidth("130px");

        // --- MEMBUAT HEADER BERTINGKAT (MERGED CELLS) ---
        var headerRow = grid.prependHeaderRow();

        headerRow.join(jpTkCol, jpPrshCol).setText("Iuran JP");
        headerRow.join(jkkCol, jkmCol).setText("Iuran Kecelakaan & Kematian");
        headerRow.join(jhtTkCol, jhtPrshCol).setText("Iuran JHT");

        setupGridDefaults(grid);
        return grid;
    }

    private Grid<HrPayroll> createBpjsJknGrid() {
        Grid<HrPayroll> grid = new Grid<>(HrPayroll.class, false);
        grid.addColumn(this::getEmployeeName).setHeader("Karyawan").setAutoWidth(true);
        grid.addColumn(p -> fmt(getCalc(p).getBpjsJknCompany())).setHeader("JKN Prsh (4%)");
        grid.addColumn(p -> fmt(getCalc(p).getBpjsJknDeduction())).setHeader("JKN TK (1%)");
        setupGridDefaults(grid);
        return grid;
    }

    private void setupGridDefaults(Grid<HrPayroll> grid) {
        grid.setSizeFull();
        grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES, com.vaadin.flow.component.grid.GridVariant.LUMO_COLUMN_BORDERS);
    }

    @SuppressWarnings("unchecked")
    private void applyFilters() {
        calculationCache.clear();
        Integer y = yearFilter.getValue();
        Integer m = monthFilter.getValue();
        LocalDate date = LocalDate.of(y, m, 1);
        String search = searchField.getValue();

        log.info("Memuat data periode: {} - {}", y, m);

        DataProvider<HrPayroll, Void> dp = DataProvider.fromCallbacks(
                q -> payrollService.getPayrollPage(PageRequest.of(q.getOffset()/q.getLimit(), q.getLimit()), y, date, search).stream(),
                q -> (int) payrollService.countPayroll(y, date, search)
        );

        tabSheet.getChildren().filter(c -> c instanceof Grid).forEach(c -> ((Grid<HrPayroll>) c).setDataProvider(dp));
    }

    private void openAddPayrollDialog() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Generate New Payroll");
        AddPayrollForm form = new AddPayrollForm(payrollService, currentUser, this::runAsync, () -> {
            dialog.close();
            applyFilters();
        });
        dialog.add(new Scroller(form));
        dialog.setWidth("750px");
        dialog.open();
    }

    private void openRecalculateDialog(HrPayroll payroll) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Recalculate: " + payroll.getFirstName());
        RecalculateForm form = new RecalculateForm(payrollService, currentUser, this::runAsync, payroll, () -> {
            dialog.close();
            applyFilters();
        });
        dialog.add(new Scroller(form));
        dialog.setWidth("600px");
        dialog.open();
    }

    private void openDetailDialog(HrPayroll payroll) {
        Dialog dialog = new Dialog("Komponen Gaji: " + payroll.getFirstName());
        HrPayrollCalculation calc = getCalc(payroll);
        List<HrPayrollComponent> components = payrollService.getPayrollComponentsByCalculationId(calc.getId());
        Grid<HrPayrollComponent> grid = new Grid<>(HrPayrollComponent.class, false);
        grid.setItems(components);
        grid.addColumn(HrPayrollComponent::getComponentName).setHeader("Komponen");
        grid.addColumn(c -> fmt(c.getAmount())).setHeader("Nilai").setTextAlign(com.vaadin.flow.component.grid.ColumnTextAlign.END);
        dialog.add(grid);
        dialog.setWidth("600px");
        dialog.open();
    }

    private void runAsync(String msg, Runnable task, Runnable onDone) {
        Dialog loading = new Dialog();
        loading.add(new VerticalLayout(new Span(msg), new ProgressBar()));
        loading.setCloseOnEsc(false);
        loading.open();

        UI ui = UI.getCurrent();
        CompletableFuture.runAsync(task, executor).whenComplete((res, ex) -> {
            ui.access(() -> {
                loading.close();
                if (ex != null) AppNotification.error("Error: " + ex.getMessage());
                else onDone.run();
                ui.push();
            });
        });
    }

    private HrPayrollCalculation getCalc(HrPayroll p) {
        return calculationCache.computeIfAbsent(p.getId(), payrollService::getCalculationByPayrollId);
    }

    private String getEmployeeName(HrPayroll p) {
        return (p.getFirstName() + " " + (p.getLastName() == null ? "" : p.getLastName())).trim();
    }

    private String fmt(BigDecimal v) {
        return FormattingUtils.formatPayrollAmount(v == null ? BigDecimal.ZERO : v);
    }

    // --- INNER FORM CLASSES ---

    public interface AsyncRunner { void run(String msg, Runnable t, Runnable d); }

    public static class AddPayrollForm extends FormLayout {
        private final Binder<PayrollService.AddPayrollRequest> binder = new BeanValidationBinder<>(PayrollService.AddPayrollRequest.class);

        public AddPayrollForm(PayrollService service, CurrentUser user, AsyncRunner runner, Runnable done) {
            setResponsiveSteps(new ResponsiveStep("0", 1), new ResponsiveStep("500px", 2));

            ComboBox<Integer> year = new ComboBox<>("Tahun", List.of(2025, 2026));
            ComboBox<Integer> month = new ComboBox<>("Bulan", List.of(1,2,3,4,5,6,7,8,9,10,11,12));
            IntegerField attend = new IntegerField("Param Hari Kerja");
            IntegerField otMin = new IntegerField("Overtime (Menit)");

            BigDecimalField otNominal = new BigDecimalField("Nominal Lembur Per Menit");
            RadioButtonGroup<String> allowMode = new RadioButtonGroup<>("Metode Allowance");
            allowMode.setItems("NO ALLOWANCE", "BENEFITS PACKAGE");
            allowMode.setValue("BENEFITS PACKAGE");

            Button save = new Button("Generate Payroll", e -> {
                PayrollService.AddPayrollRequest req = new PayrollService.AddPayrollRequest();
                if (binder.writeBeanIfValid(req)) {
                    runner.run("Generating Data...", () -> service.createPayrollBulk(req, user.require()), done);
                }
            });
            save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            binder.setBean(new PayrollService.AddPayrollRequest());
            binder.forField(year).asRequired().bind(PayrollService.AddPayrollRequest::getYear, PayrollService.AddPayrollRequest::setYear);
            binder.forField(month).asRequired().bind(PayrollService.AddPayrollRequest::getMonth, PayrollService.AddPayrollRequest::setMonth);
            binder.forField(attend).bind(PayrollService.AddPayrollRequest::getParamAttendanceDays, PayrollService.AddPayrollRequest::setParamAttendanceDays);
            binder.forField(otMin).bind(PayrollService.AddPayrollRequest::getOvertimeMinutes, PayrollService.AddPayrollRequest::setOvertimeMinutes);
            binder.forField(otNominal).bind(PayrollService.AddPayrollRequest::getOvertimeStaticNominal, PayrollService.AddPayrollRequest::setOvertimeStaticNominal);
            binder.forField(allowMode).bind(PayrollService.AddPayrollRequest::getAllowanceMode, PayrollService.AddPayrollRequest::setAllowanceMode);

            add(year, month, attend, otMin, otNominal, allowMode, save);
            setColspan(allowMode, 2);
            setColspan(save, 2);
        }
    }

    public static class RecalculateForm extends FormLayout {
        public RecalculateForm(PayrollService service, CurrentUser user, AsyncRunner runner, HrPayroll p, Runnable done) {
            IntegerField days = new IntegerField("Hari Kerja");
            days.setValue(p.getParamAttendanceDays());
            BigDecimalField bonus = new BigDecimalField("Bonus Tambahan");
            bonus.setValue(BigDecimal.ZERO);

            Button b = new Button("Proses", e -> {
                PayrollService.AddPayrollRequest req = new PayrollService.AddPayrollRequest();
                req.setYear(p.getPayrollDate().getYear());
                req.setMonth(p.getPayrollDate().getMonthValue());
                req.setParamAttendanceDays(days.getValue());
                runner.run("Menghitung...", () ->
                        service.recalculateSinglePayroll(p.getId(), req, bonus.getValue(), BigDecimal.ZERO, BigDecimal.ZERO, user.require()), done);
            });
            b.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            add(days, bonus, b);
        }
    }
}