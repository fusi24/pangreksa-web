package com.fusi24.pangreksa.web.view.employee;

import com.fusi24.pangreksa.base.ui.TailwindUtility.*;
import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.base.ui.notification.AppNotification;
import com.fusi24.pangreksa.security.CurrentUser;
import com.pangreksa.service.model.entity.FwAppUser;
import com.pangreksa.service.model.entity.HrContract;
import com.pangreksa.service.model.enumerate.ContractStatusEnum;
import com.pangreksa.service.model.repo.FwAppUserRepository;
import com.pangreksa.service.service.ContractService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.RolesAllowed;

import java.io.File;
import java.util.List;

@Route("contract-approval")
@PageTitle("Approval Kontrak")
@Menu(
        order = 41,
        icon = "vaadin:check",
        title = "Approval Kontrak"
)
@RolesAllowed("CONTRACT_APPR")
public class ContractApprovalView extends Main {

    private final ContractService contractService;
    private final CurrentUser currentUser;
    private final FwAppUserRepository appUserRepository;

    private final Grid<HrContract> grid =
            new Grid<>(HrContract.class, false);

    public ContractApprovalView(
            ContractService contractService,
            CurrentUser currentUser,
            FwAppUserRepository appUserRepository
    ) {

        this.contractService = contractService;
        this.currentUser = currentUser;
        this.appUserRepository = appUserRepository;

        addClassNames(
                BoxSizing.BORDER,
                Display.FLEX,
                FlexDirection.COLUMN,
                Padding.MEDIUM,
                Gap.SMALL
        );

        add(
                new ViewToolbar(
                        "Approval Kontrak"
                )
        );

        configureGrid();

        loadData();
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
                .setHeader("Tipe");

        grid.addColumn(HrContract::getStartDate)
                .setHeader("Tanggal Mulai");

        grid.addColumn(HrContract::getEndDate)
                .setHeader("Tanggal Berakhir");

        // DOWNLOAD FILE
        grid.addComponentColumn(this::buildDownloadButton)
                .setHeader("Dokumen");

        // ACTION
        grid.addComponentColumn(this::buildActionButtons)
                .setHeader("Aksi");

        grid.setWidthFull();

        add(grid);
    }

    // =====================================================
    // DOWNLOAD BUTTON
    // =====================================================

    private Component buildDownloadButton(
            HrContract contract
    ) {

        if (contract.getAttachmentPath() == null) {
            return new Span("-");
        }

        File file =
                new File(contract.getAttachmentPath());

        Anchor download =
                new Anchor(
                        "/"
                                + file.getName(),
                        "Download"
                );

        download.getElement()
                .setAttribute("download", true);

        return download;
    }

    // =====================================================
    // ACTION BUTTON
    // =====================================================

    private Component buildActionButtons(
            HrContract contract
    ) {

        HorizontalLayout actions =
                new HorizontalLayout();

        // APPROVE
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

        // REJECT
        Button reject =
                new Button(
                        VaadinIcon.CLOSE.create()
                );

        reject.addThemeVariants(
                ButtonVariant.LUMO_ERROR,
                ButtonVariant.LUMO_TERTIARY_INLINE
        );

        reject.addClickListener(e ->
                openRejectDialog(contract)
        );

        actions.add(
                approve,
                reject
        );

        return actions;
    }

    // =====================================================
    // REJECT DIALOG
    // =====================================================

    private void openRejectDialog(
            HrContract contract
    ) {

        Dialog dialog =
                new Dialog();

        dialog.setHeaderTitle(
                "Reject Contract"
        );

        TextArea notes =
                new TextArea(
                        "Alasan Reject"
                );

        notes.setWidthFull();

        Button submit =
                new Button(
                        "Reject"
                );

        submit.addThemeVariants(
                ButtonVariant.LUMO_ERROR
        );

        submit.addClickListener(e -> {

            try {

                contractService.rejectContract(
                        contract,
                        getCurrentAppUser(),
                        notes.getValue()
                );

                AppNotification.success(
                        "Kontrak berhasil ditolak"
                );

                dialog.close();

                loadData();

            } catch (Exception ex) {

                AppNotification.error(
                        ex.getMessage()
                );
            }
        });

        dialog.add(
                notes,
                submit
        );

        dialog.open();
    }

    // =====================================================
    // LOAD DATA
    // =====================================================

    private void loadData() {

        FwAppUser currentUser =
                getCurrentAppUser();

        List<HrContract> contracts =
                contractService.findWaitingApprovalByApprover(
                        currentUser.getPerson()
                );

        grid.setItems(contracts);
    }

    // =====================================================
    // CURRENT USER
    // =====================================================

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