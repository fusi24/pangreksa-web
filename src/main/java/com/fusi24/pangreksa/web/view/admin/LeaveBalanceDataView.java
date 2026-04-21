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
import java.time.format.DateTimeFormatter;
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

    public static final String VIEW_NAME = "Data Saldo Cuti";

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
        body.setSpacing(true);
        body.setSizeFull();
        companyDropdown = new ComboBox<>("Perusahaan");
        companyDropdown.setItems(companyService.getallCompanies());
        companyDropdown.setItemLabelGenerator(HrCompany::getName);
        companyDropdown.setPlaceholder("Pilih Perusahaan...");
        companyDropdown.setClearButtonVisible(true);
        companyDropdown.getStyle().setWidth("350px");

        yearDropdown = new ComboBox<>("Tahun");
        // put 5 years back and next 5 years
        int currentYear = LocalDate.now().getYear();
        yearDropdown.setItems(currentYear - 5, currentYear - 4, currentYear - 3, currentYear - 2, currentYear - 1,
                currentYear, currentYear + 1, currentYear + 2, currentYear + 3, currentYear + 4, currentYear + 5);
        yearDropdown.setItemLabelGenerator(String::valueOf);
        yearDropdown.setValue(currentYear);

        checkButton = new Button("Cek Saldo Cuti");
        checkButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout headerFunction = new HorizontalLayout(companyDropdown, yearDropdown, checkButton);
        headerFunction.setWidthFull();
        headerFunction.setAlignItems(FlexComponent.Alignment.BASELINE);
        headerFunction.addClassNames(
                LumoUtility.Padding.Left.MEDIUM,
                LumoUtility.Padding.Right.MEDIUM,
                LumoUtility.Padding.Bottom.SMALL,
                LumoUtility.Background.CONTRAST_5, // Beri background abu-abu tipis
                LumoUtility.BorderRadius.MEDIUM
        );
        headerFunction.setFlexGrow(1, companyDropdown);

        Grid<HrLeaveGenerationLog> grid = createLeaveGenerationLogGrid();
        grid.setSizeFull();

        body.add(headerFunction, grid);
        body.setFlexGrow(1, grid);

        add(body);
    }



    private Grid<HrLeaveGenerationLog> createLeaveGenerationLogGrid() {
        leaveGenerationLogGrid = new Grid<>(HrLeaveGenerationLog.class, false);
        leaveGenerationLogGrid.addColumn(log -> log.getCompany().getName())
                .setHeader("Perusahaan")
                .setResizable(true)
                .setFlexGrow(1);

        DateTimeFormatter dtf =
                DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

        // Kolom penyeimbang (SATU SAJA yang fleksibel)
        leaveGenerationLogGrid.addColumn(log -> log.getCompany().getName())
                .setHeader("Perusahaan")
                .setWidth("260px")
                .setFlexGrow(0);

        // Kolom kecil
        leaveGenerationLogGrid.addColumn(HrLeaveGenerationLog::getYear)
                .setHeader("Tahun")
                .setWidth("80px")
                .setFlexGrow(0);

        leaveGenerationLogGrid.addColumn(HrLeaveGenerationLog::getDataGenerated)
                .setHeader("Total Data")
                .setWidth("110px")
                .setFlexGrow(0);

        leaveGenerationLogGrid.addColumn(log -> log.getCreatedBy().getUsername())
                .setHeader("Dibuat Oleh")
                .setWidth("120px")
                .setFlexGrow(0);

        // Tanggal → format pendek + width cukup
        leaveGenerationLogGrid.addColumn(log ->
                        log.getCreatedAt() != null
                                ? log.getCreatedAt().format(dtf)
                                : "-"
                )
                .setHeader("Tanggal Dibuat")
                .setWidth("170px")
                .setFlexGrow(0);

        // Behavior grid
        leaveGenerationLogGrid.setSizeFull();
        leaveGenerationLogGrid.setAllRowsVisible(false);
        leaveGenerationLogGrid.setColumnReorderingAllowed(true);

        return leaveGenerationLogGrid;
    }





    private void setListener() {
        checkButton.addClickListener( e -> {
            if (this.auth.canView) {
                HrCompany company = this.companyDropdown.getValue();
                if (company == null) {
                    com.fusi24.pangreksa.base.ui.notification.AppNotification.error("Pilih perusahaan terlebih dahulu");
                    return;
                }

                int year = this.yearDropdown.getValue();
                Long leaveServerCount = leaveService.countLeaveBalanceRowPerCompany(company, year);
                Long personCount = leaveService.countPersonPerCompany(company, year);
                int leaveTypeCount = leaveService.getLeaveAbsenceTypesList().size();
                int totalRowGenerated = Math.toIntExact((int) (personCount * leaveTypeCount) - leaveServerCount);

                Dialog dialog = new Dialog();
                dialog.setHeaderTitle("Ringkasan Saldo Cuti - " + year);

                VerticalLayout dialogContent = new VerticalLayout();
                dialogContent.setPadding(false);
                dialogContent.setSpacing(false);

                // Fungsi pembantu untuk membuat baris informasi yang cantik
                autoCreateRow(dialogContent, "Data Terdaftar (DB)", String.valueOf(leaveServerCount));
                autoCreateRow(dialogContent, "Total Karyawan Aktif", String.valueOf(personCount));
                autoCreateRow(dialogContent, "Jumlah Tipe Cuti", String.valueOf(leaveTypeCount));

                Span statusLabel = new Span();
                statusLabel.addClassNames(LumoUtility.Margin.Top.MEDIUM, LumoUtility.FontSize.MEDIUM);

                Button saveButton = new Button("Generate Data");
                saveButton.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_PRIMARY, com.vaadin.flow.component.button.ButtonVariant.LUMO_SUCCESS);

                if (totalRowGenerated > 0) {
                    statusLabel.setText("Sistem akan membuat " + totalRowGenerated + " baris data saldo baru.");
                    statusLabel.getStyle().set("color", "var(--lumo-primary-text-color)");
                } else {
                    statusLabel.setText("Saldo sudah lengkap di database.");
                    statusLabel.addClassName(LumoUtility.TextColor.SUCCESS);
                    saveButton.setEnabled(false);
                }

                if(!this.auth.canEdit) saveButton.setEnabled(false);

                dialogContent.add(new com.vaadin.flow.component.html.Hr(), statusLabel);

                Button cancelButton = new Button("Tutup", event -> dialog.close());
                dialog.getFooter().add(cancelButton, saveButton);

                saveButton.addClickListener(event -> {
                    leaveService.generateLeaveBalanceData(company, year, currentUser.require());
                    com.fusi24.pangreksa.base.ui.notification.AppNotification.success("Proses Generate Berhasil");
                    leaveGenerationLogGrid.setItems(leaveService.getLeaveGenerationLogs(company));
                    dialog.close();
                });

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

    private void autoCreateRow(VerticalLayout container, String labelText, String valueText) {
        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        row.addClassNames(LumoUtility.Padding.Vertical.XSMALL, LumoUtility.Border.BOTTOM, LumoUtility.BorderColor.CONTRAST_10);

        Span label = new Span(labelText);
        label.addClassName(LumoUtility.TextColor.SECONDARY);

        Span value = new Span(valueText);
        value.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.BODY);

        row.add(label, value);
        container.add(row);
    }
}
