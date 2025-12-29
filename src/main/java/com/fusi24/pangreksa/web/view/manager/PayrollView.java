package com.fusi24.pangreksa.web.view.manager;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.base.util.FormattingUtils;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.HrPayroll;
import com.fusi24.pangreksa.web.model.entity.HrPayrollCalculation;
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.PayrollService;
import com.fusi24.pangreksa.web.service.SystemService;
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
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    private Grid<HrPayroll> grid = new Grid<>(HrPayroll.class, false);
    private TextField searchField = new TextField();

    private ComboBox<Integer> yearFilter = new ComboBox<>();
    private ComboBox<Integer> monthFilter = new ComboBox<>();

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

            Button recalculateBtn = new Button("Recalculate", e -> {
                try {
                    payrollService.calculatePayroll(payroll);
                    Notification.show("Recalculated successfully", 3000, Notification.Position.MIDDLE);
                    applyFilters();
                } catch (Exception ex) {
                    Notification.show("Recalculation failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                    ex.printStackTrace();
                }
            });
            recalculateBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            recalculateBtn.setAriaLabel("Recalculate Payroll");

            Button addKomponenBtn = new Button("Add Komponen", e -> {
                // TODO: nanti kita bahas implementasi action ini (dialog / navigate)
                Notification.show("Add Komponen clicked", 2000, Notification.Position.MIDDLE);
            });
            addKomponenBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            addKomponenBtn.setAriaLabel("Add Komponen");

            actions.add(recalculateBtn, addKomponenBtn);
            actions.setSpacing(true);
            return actions;
        }).setHeader("Actions").setAutoWidth(true).setWidth("220px").setFlexGrow(0);

        // =========================
        // Search / Filter toolbar
        // =========================

        // Search Field (employee)
        searchField.setPlaceholder("Search employee");
        searchField.setWidth("40%");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.EAGER);

        // Add Payroll Button (placeholder - nanti kita bahas)
        Button addButton = new Button("Add Payroll", e -> {
            Notification.show("Add Payroll clicked", 2000, Notification.Position.MIDDLE);
        });
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

        HorizontalLayout toolbar = new HorizontalLayout(toolbarLeft, addButton);
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.END);
        toolbar.expand(toolbarLeft);

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
}
