package com.fusi24.pangreksa.web.view.admin;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.entity.HrSalaryBaseLevel;
import com.fusi24.pangreksa.web.repo.HrSalaryEmployeeLevelRepository.UserLevelProjection;
import com.fusi24.pangreksa.web.service.AdminService;
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.PersonService;
import com.fusi24.pangreksa.web.service.SalaryLevelService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;

@Route("employee-mapping-level")
@PageTitle("Mapping Employee Level")
@Menu(order = 33, icon = "vaadin:user", title = "Employee Level Mapping")
@RolesAllowed("USERS_MGT")
public class EmployeeMappingLevel extends Main {
    private static final long serialVersionUID = 33L;
    private static final Logger log = LoggerFactory.getLogger(EmployeeMappingLevel.class);
    private final CurrentUser currentUser;
    private final CommonService commonService;
    private final AdminService adminService;
    private final PersonService personService;
    private final SalaryLevelService salaryLevelService;

    public static final String VIEW_NAME = "Employee Level Mapping";

    private List<UserLevelProjection> rows = new ArrayList<>();
    private Grid<UserLevelProjection> grid;
    private Button populateButton;
    private Button saveButton;
    private com.vaadin.flow.component.textfield.TextField searchField;
    private VerticalLayout body;

    /** menampung pilihan level user yang diubah di UI (userId -> baseLevelId) */
    private final Map<Long, Long> pendingLevelByUserId = new HashMap<>();
    private final Map<Long, BigDecimal> pendingSalaryByUserId = new HashMap<>();


    public EmployeeMappingLevel(CurrentUser currentUser,
                                CommonService commonService,
                                AdminService adminService,
                                PersonService personService,
                                SalaryLevelService salaryLevelService) {
        this.currentUser = currentUser;
        this.commonService = commonService;
        this.adminService = adminService;
        this.personService = personService;
        this.salaryLevelService = salaryLevelService;

        addClassNames(
                LumoUtility.BoxSizing.BORDER,
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Gap.SMALL
        );

        setHeightFull();

        add(new ViewToolbar(VIEW_NAME));


        createBody();

        setListener();
    }


    private void createBody() {
        this.body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);
        body.setSizeFull();

        searchField = new com.vaadin.flow.component.textfield.TextField("Search Filter");
        populateButton = new Button("Populate");
        saveButton = new Button("Save");

        HorizontalLayout leftLayout = new HorizontalLayout(searchField, populateButton);
        leftLayout.setSpacing(true);
        leftLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.add(leftLayout);
        row.add(saveButton);
        row.setAlignItems(HorizontalLayout.Alignment.BASELINE);
        row.expand(leftLayout);

        // === Grid ===
        grid = new Grid<>(UserLevelProjection.class, false);

        // 1) Kolom User (Nickname / displayName)
        grid.addColumn(UserLevelProjection::getDisplayName)
                .setHeader("User (Nickname)")
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(true);

        // 2) Kolom Level (ComboBox)
        grid.addColumn(new ComponentRenderer<>(rowItem -> {
                    ComboBox<HrSalaryBaseLevel> cb = new ComboBox<>();
                    cb.setPlaceholder("Pilih level");
                    cb.setClearButtonVisible(true);
                    cb.setItemLabelGenerator(HrSalaryBaseLevel::getLevelCode);

                    Long companyId = rowItem.getCompanyId();
                    List<HrSalaryBaseLevel> options =
                            salaryLevelService.findActiveBaseLevels(LocalDate.now(), companyId);
                    cb.setItems(options);

                    // set nilai awal: prioritas perubahan pending; fallback dari data existing
                    Long initialId = Optional.ofNullable(pendingLevelByUserId.get(rowItem.getUserId()))
                            .orElse(rowItem.getSelectedBaseLevelId());

                    if (initialId != null) {
                        options.stream()
                                .filter(b -> Objects.equals(b.getId(), initialId))
                                .findFirst()
                                .ifPresent(sel -> {
                                    cb.setValue(sel);
                                    // pastikan base salary tampak sesuai pilihan pending
                                    pendingSalaryByUserId.put(rowItem.getUserId(), sel.getBaseSalary());
                                });
                    }

                    cb.addValueChangeListener(e -> {
                        HrSalaryBaseLevel v = e.getValue();
                        Long userId = rowItem.getUserId();
                        if (v != null) {
                            pendingLevelByUserId.put(userId, v.getId());
                            pendingSalaryByUserId.put(userId, v.getBaseSalary()); // <- kunci: simpan gaji terbaru
                        } else {
                            pendingLevelByUserId.remove(userId);
                            pendingSalaryByUserId.remove(userId);
                        }
                        grid.getDataProvider().refreshItem(rowItem); // <- trigger re-render baris
                    });

                    return cb;
                })).setHeader("Level")
                .setAutoWidth(true)
                .setFlexGrow(1);

        // 3) Kolom Base Salary (read-only, perlihatkan pilihan terbaru kalau ada)
        grid.addColumn(rowItem -> {
                    BigDecimal val = pendingSalaryByUserId.getOrDefault(
                            rowItem.getUserId(), rowItem.getSelectedBaseSalary());
                    return formatCurrency(val);
                }).setHeader("Base Salary")
                .setAutoWidth(true)
                .setFlexGrow(1);
        grid.setSizeFull();

        body.add(row, grid);
        body.setFlexGrow(1, grid);
        add(body);

    }

    private void setListener() {
        populateButton.addClickListener(e -> {
            String keyword = (searchField.getValue() == null || searchField.getValue().isBlank())
                    ? null
                    : searchField.getValue().trim();

            rows = salaryLevelService.findUserLevelRows(keyword);
            grid.setItems(rows);
        });

        saveButton.addClickListener(e -> {
            try {
                // kirim hanya perubahan yang pending
                salaryLevelService.upsertMappings(pendingLevelByUserId, currentUser.require());

                // bersihkan state & reload
                pendingLevelByUserId.clear();
                pendingSalaryByUserId.clear();
                populateButton.click();

                // <- notifikasi sukses
                Notification n = Notification.show("Berhasil menyimpan level employee.");
                n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                n.setPosition(Notification.Position.TOP_END);
            } catch (Exception ex) {
                // opsional: log & tampilkan notif error
                log.error("Gagal menyimpan mapping level", ex);
                Notification n = Notification.show("Gagal menyimpan. Silakan coba lagi.");
                n.addThemeVariants(NotificationVariant.LUMO_ERROR);
                n.setPosition(Notification.Position.TOP_END);
            }
        });

        saveButton.addClickListener(e -> {
            if (pendingLevelByUserId.isEmpty()) {
                populateButton.click();
                return;
            }
            salaryLevelService.upsertMappings(pendingLevelByUserId, currentUser.require());
            pendingLevelByUserId.clear();
            pendingSalaryByUserId.clear();
            populateButton.click();
        });
    }

    private String formatCurrency(BigDecimal v) {
        if (v == null) return "";
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("id", "ID"));
        nf.setGroupingUsed(true);
        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(2);
        return nf.format(v);
    }
}
