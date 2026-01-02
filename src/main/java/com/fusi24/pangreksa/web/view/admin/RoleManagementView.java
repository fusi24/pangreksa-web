package com.fusi24.pangreksa.web.view.admin;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.security.AppUserInfo;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.repo.FwAppuserRespRepository.GridRow;
import com.fusi24.pangreksa.web.repo.FwAppuserRespRepository.OptionRow;
import com.fusi24.pangreksa.web.service.RoleManagementService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Manajemen Role User (Grid only):
 * - Kiri: Input Nama Karyawan (live) + Populate
 * - Kanan: Add, Save
 * - Grid: ID Pengguna, Nama, Role (ComboBox), Status (Checkbox)
 * - Add: pilih Pengguna & Role -> simpan -> auto populate
 * - Save: commit perubahan grid -> auto populate
 */
@Route("mapping-role-user")
@PageTitle("Manajemen Role User")
@Menu(order = 33, icon = "vaadin:user", title = "Manajemen Role User")
@RolesAllowed("USERS_MGT")
public class RoleManagementView extends Main {
    private static final long serialVersionUID = 33L;
    private static final Logger log = LoggerFactory.getLogger(RoleManagementView.class);

    public static final String VIEW_NAME = "Manajemen Role User";

    private final CurrentUser currentUser;
    private final RoleManagementService roleService;

    private VerticalLayout body;
    private TextField searchField;
    private Button populateButton;
    private Button addButton;
    private Button saveButton;

    private Grid<RowModel> grid;

    // state grid
    private final List<RowModel> items = new ArrayList<>();
    private final Set<Long> dirtyIds = new HashSet<>();
    // cache options
    private List<OptionRow> cachedRoles = List.of();

    public RoleManagementView(CurrentUser currentUser,
                              RoleManagementService roleService) {
        this.currentUser = currentUser;
        this.roleService = roleService;

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
        setListeners();
    }

    /* ===== Helpers ===== */
    private AppUserInfo actor() {
        // ikuti pola contohmu: service menerima AppUserInfo dari currentUser.require()
        return currentUser.require();
    }
    private String blankToNull(String s) { return (s == null || s.isBlank()) ? null : s.trim(); }
    private void notifySuccess(String msg) {
        var n = Notification.show(msg);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        n.setPosition(Notification.Position.TOP_END);
    }
    private void notifyWarn(String msg) {
        var n = Notification.show(msg);
        n.addThemeVariants(NotificationVariant.LUMO_WARNING);
        n.setPosition(Notification.Position.TOP_END);
    }
    private void notifyError(String msg) {
        var n = Notification.show(msg);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        n.setPosition(Notification.Position.TOP_END);
    }

    /* ===== UI ===== */
    private void createBody() {
        body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);
        body.setSizeFull();

        searchField = new TextField("Input Nama Karyawan");
        searchField.setPlaceholder("Ketik nama karyawan...");
        searchField.setClearButtonVisible(true);

        populateButton = new Button("Populate");
        addButton = new Button("Add");
        saveButton = new Button("Save");

        // Toolbar kiri
        HorizontalLayout left = new HorizontalLayout(searchField, populateButton);
        left.setSpacing(true);
        left.setAlignItems(FlexComponent.Alignment.BASELINE);

        // Toolbar kanan (Add, Save) — Save paling kanan
        HorizontalLayout right = new HorizontalLayout(addButton, saveButton);
        right.setSpacing(true);
        right.setAlignItems(FlexComponent.Alignment.BASELINE);

        HorizontalLayout bar = new HorizontalLayout();
        bar.setWidthFull();
        bar.add(left, right);
        bar.expand(left);
        bar.setAlignItems(HorizontalLayout.Alignment.BASELINE);

        // Grid
        grid = new Grid<>(RowModel.class, false);
        grid.setWidthFull();
        grid.addColumn(RowModel::getUsername)
                .setHeader("ID Pengguna")
                .setWidth("70px")
                .setResizable(true)
                .setFrozen(true);

        grid.addColumn(RowModel::getNickname)
                .setHeader("Nama")
                .setWidth("70px")
                .setResizable(true);

        // Role (editable ComboBox)
        grid.addComponentColumn(rowItem -> {
            ComboBox<OptionRow> cb = new ComboBox<>();
            cb.setItems(cachedRoles);
            cb.setItemLabelGenerator(OptionRow::getLabel);
            cb.setWidth("320px");

            cachedRoles.stream()
                    .filter(o -> Objects.equals(o.getId(), rowItem.getResponsibilityId()))
                    .findFirst()
                    .ifPresent(cb::setValue);

            cb.addValueChangeListener(e -> {
                var v = e.getValue();
                rowItem.setResponsibilityId(v != null ? v.getId() : null);
                dirtyIds.add(rowItem.getId());
            });
            return cb;
        }).setHeader("Role").setAutoWidth(true).setResizable(true);

        // Status (editable Checkbox)
        grid.addComponentColumn(rowItem -> {
                    Checkbox cb = new Checkbox(Boolean.TRUE.equals(rowItem.getIsActive()));
                    cb.addValueChangeListener(e -> {
                        rowItem.setIsActive(e.getValue());
                        dirtyIds.add(rowItem.getId());
                    });
                    return cb;
                }).setHeader("Status")
                .setWidth("80px")
                .setFlexGrow(0)
                .setFrozenToEnd(true);

        body.add(bar, grid);
        body.setFlexGrow(1, grid);
        add(body);
    }

    private void setListeners() {
        // live search
        searchField.addValueChangeListener(e -> populate());

        populateButton.addClickListener(e -> populate());

        addButton.addClickListener(e -> openAddDialog());

        saveButton.addClickListener(e -> onSave());
    }

    /* ===== Actions ===== */
    private void populate() {
        try {
            // cache roles untuk editor
            cachedRoles = roleService.getResponsibilityOptions();

            String kw = blankToNull(searchField.getValue());
            List<GridRow> rows = roleService.findRoleRows(kw);

            items.clear();
            for (GridRow r : rows) {
                RowModel m = new RowModel();
                m.setId(r.getId());
                m.setAppuserId(r.getAppuserId());
                m.setResponsibilityId(r.getResponsibilityId());
                m.setUsername(r.getUsername());
                m.setNickname(r.getNickname());
                m.setRole(r.getRole()); // display text (optional)
                m.setIsActive(r.getIsActive());
                items.add(m);
            }
            grid.setItems(items);
            dirtyIds.clear();
        } catch (Exception ex) {
            log.error("Gagal populate data role", ex);
            notifyError("Gagal memuat data. Coba lagi.");
            notifyError("Gagal memuat data. Coba lagi.");
        }
    }

    private void openAddDialog() {
        Dialog dlg = new Dialog();
        dlg.setHeaderTitle("Tambah Role Pengguna");

        TextField tfUserSearch = new TextField();
        tfUserSearch.setPlaceholder("Cari nama karyawan...");
        tfUserSearch.setClearButtonVisible(true);

        ComboBox<OptionRow> cbUser = new ComboBox<>("Pengguna");
        cbUser.setItemLabelGenerator(OptionRow::getLabel);
        cbUser.setClearButtonVisible(true);
        cbUser.setWidth("360px");
        // --- TAMBAHAN: Tandai sebagai Wajib (visual) ---
        cbUser.setRequiredIndicatorVisible(true);
        // --- TAMBAHAN: Hapus error jika user memilih item ---
        cbUser.addValueChangeListener(e -> cbUser.setInvalid(false));

        tfUserSearch.addValueChangeListener(e -> cbUser.setItems(roleService.searchUserOptions(e.getValue())));
        cbUser.setItems(roleService.searchUserOptions(""));

        ComboBox<OptionRow> cbRole = new ComboBox<>("Role");
        cbRole.setItemLabelGenerator(OptionRow::getLabel);
        cbRole.setItems(roleService.getResponsibilityOptions());
        cbRole.setWidth("360px");
        // --- TAMBAHAN: Tandai sebagai Wajib (visual) ---
        cbRole.setRequiredIndicatorVisible(true);
        // --- TAMBAHAN: Hapus error jika user memilih item ---
        cbRole.addValueChangeListener(e -> cbRole.setInvalid(false));


        VerticalLayout content = new VerticalLayout(tfUserSearch, cbUser, cbRole);
        content.setPadding(false);
        dlg.add(content);

        Button cancel = new Button("Batal", ev -> dlg.close());
        Button save = new Button("Simpan", ev -> {
            var u = cbUser.getValue();
            var r = cbRole.getValue();

            // --- TAMBAHAN: Validasi Wajib yang lebih kuat ---
            boolean isValid = true;
            if (u == null) {
                cbUser.setInvalid(true); // Merahkan field Pengguna
                isValid = false;
            }
            if (r == null) {
                cbRole.setInvalid(true); // Merahkan field Role
                isValid = false;
            }
            if (!isValid) {
                notifyWarn("Pilih Pengguna dan Role terlebih dahulu.");
                return;
            }
            // --- Akhir Validasi Wajib ---

            try {
                roleService.addMapping(u.getId(), r.getId(), actor());
                dlg.close();
                populate();
                notifySuccess("Data baru berhasil disimpan.");
            } catch (Exception ex) {
                log.error("Gagal menambah mapping role", ex);

                // --- TAMBAHAN: Menampilkan notifikasi galat (duplikat) dari service ---
                String errorMessage = ex.getMessage();
                if (errorMessage == null || errorMessage.isBlank()) {
                    errorMessage = "Silakan coba lagi.";
                }
                // Jika service melempar "Mapping sudah ada", maka akan tampil di sini
                notifyError("Gagal menyimpan: " + errorMessage);
            }
        });

        dlg.getFooter().add(new HorizontalLayout(cancel, save));
        dlg.open();
    }

    private void onSave() {
        if (dirtyIds.isEmpty()) {
            populateButton.click();
            return;
        }
        try {
            var idSet = new HashSet<>(dirtyIds);
            var updates = items.stream()
                    .filter(m -> idSet.contains(m.getId()))
                    .map(m -> {
                        RoleManagementService.UpdateRowDto dto = new RoleManagementService.UpdateRowDto();
                        dto.id = m.getId();
                        dto.responsibilityId = m.getResponsibilityId();
                        dto.isActive = m.getIsActive();
                        return dto;
                    }).toList();

            roleService.saveBatch(updates, actor());
            dirtyIds.clear();
            populate();
            notifySuccess("Perubahan berhasil disimpan.");
        } catch (Exception ex) {
            log.error("Gagal menyimpan perubahan role", ex);
            notifyError("Gagal menyimpan. Silakan coba lagi.");
        }
    }

    /* ===== Model untuk Grid ===== */
    public static class RowModel {
        private Long id;
        private Long appuserId;
        private Long responsibilityId;
        private String username;
        private String nickname;
        private String role; // display text
        private Boolean isActive;

        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        public Long getAppuserId() { return appuserId; }
        public void setAppuserId(Long appuserId) { this.appuserId = appuserId; }
        public Long getResponsibilityId() { return responsibilityId; }
        public void setResponsibilityId(Long responsibilityId) { this.responsibilityId = responsibilityId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getNickname() { return nickname; }
        public void setNickname(String nickname) { this.nickname = nickname; }
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        public Boolean getIsActive() { return isActive; }
        public void setIsActive(Boolean active) { isActive = active; }
    }
}
