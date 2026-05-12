package com.fusi24.pangreksa.web.view.employee;

import com.fusi24.pangreksa.base.ui.ThemeUtility;
import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.base.ui.notification.AppNotification;
import com.fusi24.pangreksa.security.CurrentUser;
import com.pangreksa.service.model.entity.FwAppUser;
import com.pangreksa.service.model.entity.HrContract;
import com.pangreksa.service.model.enumerate.ContractStatusEnum;
import com.pangreksa.service.service.ContractService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;
import com.pangreksa.service.model.repo.FwAppUserRepository;
import com.pangreksa.service.model.repo.FwSystemRepository;
import com.pangreksa.service.model.repo.HrPersonRepository;

import java.time.LocalDate;

@Route("contract-management")
@PageTitle("Manajemen Kontrak")
@Menu(order = 40, icon = "vaadin:file-text", title = "Manajemen Kontrak")
@RolesAllowed("CONTRACT_MGT")
public class ContractManagementView extends Main {

    private final HrPersonRepository personRepository;

    private final FwAppUserRepository appUserRepository;

    private final FwSystemRepository systemRepository;
    private final ContractService contractService;
    private final CurrentUser currentUser;

    private Grid<HrContract> grid =
            new Grid<>(HrContract.class, false);

    private ComboBox<ContractStatusEnum> statusFilter =
            new ComboBox<>("Filter Status");

    public ContractManagementView(
            ContractService contractService,
            CurrentUser currentUser,
            HrPersonRepository personRepository,
            FwAppUserRepository appUserRepository,
            FwSystemRepository systemRepository
    ){

        this.contractService = contractService;
        this.currentUser = currentUser;
        this.personRepository = personRepository;

        this.appUserRepository = appUserRepository;

        this.systemRepository = systemRepository;

        addClassNames(
                ThemeUtility.BoxSizing.BORDER,
                ThemeUtility.Display.FLEX,
                ThemeUtility.FlexDirection.COLUMN,
                ThemeUtility.Padding.MEDIUM,
                ThemeUtility.Gap.SMALL
        );

        add(new ViewToolbar("Manajemen Kontrak"));

        configureGrid();

        configureToolbar();

        loadData();
    }

    // =====================================================
    // TOOLBAR
    // =====================================================
    private void configureToolbar() {

        statusFilter.setItems(ContractStatusEnum.values());

        statusFilter.addValueChangeListener(e -> loadData());

        Button addButton =
                new Button(
                        "Tambah Kontrak",
                        VaadinIcon.PLUS.create()
                );

        addButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        addButton.addClickListener(e -> {

            ContractFormDialog dialog =
                    new ContractFormDialog(
                            contractService,
                            currentUser,
                            personRepository,
                            appUserRepository,
                            systemRepository,
                            this::loadData
                    );

            dialog.open();
        });

        HorizontalLayout toolbar =
                new HorizontalLayout(
                        statusFilter,
                        addButton
                );

        add(toolbar);
    }

    // =====================================================
    // GRID
    // =====================================================
    private void configureGrid() {

        grid.addColumn(HrContract::getContractNumber)
                .setHeader("No Kontrak")
                .setAutoWidth(true);

        grid.addColumn(contract ->
                        contract.getPerson() != null
                                ? contract.getPerson().getFirstName()
                                : "-"
                )
                .setHeader("Karyawan")
                .setAutoWidth(true);

        grid.addColumn(contract ->
                        contract.getContractType() != null
                                ? contract.getContractType().name()
                                : "-"
                )
                .setHeader("Tipe")
                .setAutoWidth(true);

        grid.addColumn(HrContract::getStartDate)
                .setHeader("Tanggal Mulai");

        grid.addColumn(HrContract::getEndDate)
                .setHeader("Tanggal Berakhir");

        // STATUS BADGE
        grid.addComponentColumn(this::buildStatusBadge)
                .setHeader("Status")
                .setAutoWidth(true);

        // SISA HARI
        grid.addColumn(contract -> {

                    if (contract.getEndDate() == null) {
                        return "Permanent";
                    }

                    long days =
                            java.time.temporal.ChronoUnit.DAYS
                                    .between(
                                            LocalDate.now(),
                                            contract.getEndDate()
                                    );

                    if (days < 0) {
                        return "Expired";
                    }

                    if (days <= 30) {
                        return "H-30";
                    }

                    if (days <= 60) {
                        return "H-60";
                    }

                    return days + " hari";
                })
                .setHeader("Masa Kontrak");

        // ACTION
        grid.addComponentColumn(this::buildActionButtons)
                .setHeader("Aksi");

        grid.setWidthFull();

        add(grid);
    }

    // =====================================================
    // STATUS BADGE
    // =====================================================
    private Component buildStatusBadge(HrContract contract) {

        Span badge =
                new Span(contract.getStatus().name());

        badge.getStyle()
                .set("padding", "4px 10px")
                .set("border-radius", "12px")
                .set("color", "white")
                .set("font-size", "12px")
                .set("font-weight", "600");

        switch (contract.getStatus()) {

            case ACTIVE ->
                    badge.getStyle()
                            .set("background-color", "#22c55e");

            case WAITING_APPROVAL ->
                    badge.getStyle()
                            .set("background-color", "#f59e0b");

            case APPROVED ->
                    badge.getStyle()
                            .set("background-color", "#3b82f6");

            case EXPIRING_SOON ->
                    badge.getStyle()
                            .set("background-color", "#ef4444");

            case TERMINATED ->
                    badge.getStyle()
                            .set("background-color", "#6b7280");

            default ->
                    badge.getStyle()
                            .set("background-color", "#94a3b8");
        }

        return badge;
    }

    // =====================================================
    // ACTION BUTTON
    // =====================================================
    private Component buildActionButtons(HrContract contract) {

        HorizontalLayout actions =
                new HorizontalLayout();

        // APPROVE
        if (contract.getStatus()
                == ContractStatusEnum.WAITING_APPROVAL) {

            Button approve =
                    new Button(
                            VaadinIcon.CHECK.create()
                    );

            approve.addThemeVariants(
                    ButtonVariant.LUMO_SUCCESS,
                    ButtonVariant.LUMO_TERTIARY_INLINE
            );

            approve.addClickListener(e -> {

                try {

                    contractService.approveContract(
                            contract,
                            getCurrentAppUser()
                    );

                    AppNotification.success(
                            "Kontrak berhasil disetujui"
                    );

                    loadData();

                } catch (Exception ex) {

                    AppNotification.error(
                            ex.getMessage()
                    );
                }
            });

            actions.add(approve);
        }

        // ACTIVATE
        if (contract.getStatus()
                == ContractStatusEnum.APPROVED) {

            Button activate =
                    new Button(
                            VaadinIcon.PLAY.create()
                    );

            activate.addThemeVariants(
                    ButtonVariant.LUMO_PRIMARY,
                    ButtonVariant.LUMO_TERTIARY_INLINE
            );

            activate.addClickListener(e -> {

                try {

                    contractService.activateContract(
                            contract,
                            getCurrentAppUser()
                    );

                    AppNotification.success(
                            "Kontrak berhasil diaktifkan"
                    );

                    loadData();

                } catch (Exception ex) {

                    AppNotification.error(
                            ex.getMessage()
                    );
                }
            });

            actions.add(activate);
        }

        // TERMINATE
        if (contract.getStatus()
                == ContractStatusEnum.ACTIVE) {

            Button terminate =
                    new Button(
                            VaadinIcon.CLOSE.create()
                    );

            terminate.addThemeVariants(
                    ButtonVariant.LUMO_ERROR,
                    ButtonVariant.LUMO_TERTIARY_INLINE
            );

            terminate.addClickListener(e -> {

                try {

                    contractService.terminateContract(
                            contract,
                            getCurrentAppUser()
                    );

                    AppNotification.success(
                            "Kontrak berhasil diakhiri"
                    );

                    loadData();

                } catch (Exception ex) {

                    AppNotification.error(
                            ex.getMessage()
                    );
                }
            });

            actions.add(terminate);
        }

        return actions;
    }

    // =====================================================
    // LOAD DATA
    // =====================================================
    private void loadData() {

        if (statusFilter.getValue() != null) {

            grid.setItems(
                    contractService.findByStatus(
                            statusFilter.getValue()
                    )
            );

        } else {

            grid.setItems(
                    contractService.findAll()
            );
        }
    }

    private FwAppUser getCurrentAppUser() {

        String username =
                currentUser.require()
                        .getUserId()
                        .toString();

        return appUserRepository
                .findByUsername(username)
                .orElseThrow();
    }
}