package com.fusi24.pangreksa.web.view.admin;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.HrPosition; // [Ubah] Gunakan HrPosition
import com.fusi24.pangreksa.web.model.entity.HrOrgStructure;
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.HrPositionService; // [Ubah] Asumsi ada service ini
import com.fusi24.pangreksa.web.repo.HrOrgStructureRepository;
import com.fusi24.pangreksa.web.repo.HrPositionRepository; // [Ubah] Tambahkan repo jika perlu list parent

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

@Route("master-position") // [Opsional] Route mungkin perlu disesuaikan
@PageTitle("Master Position")
@Menu(order = 33, icon = "vaadin:briefcase", title = "Positions")
@RolesAllowed("POSITION_MGT")
public class PositionLevelView extends Main {

    private static final long serialVersionUID = 33L;
    private static final Logger log = LoggerFactory.getLogger(PositionLevelView.class);

    private final CurrentUser currentUser;
    private final CommonService commonService;

    // [Ubah] Ganti Service ke HrPositionService
    private final HrPositionService hrPositionService;
    private final HrOrgStructureRepository orgStructureRepo;
    // [Tambahan] Repo untuk load list 'Reports To' jika perlu list lengkap
    private final HrPositionRepository positionRepo;

    private Authorization auth;

    // UI components
    private VerticalLayout body;
    private TextField searchField;
    private Button populateButton;
    private Button saveButton;
    private Button addButton;
    private Button deleteButton;

    // [Ubah] Grid menggunakan HrPosition
    private Grid<HrPosition> grid;

    // data
    private final List<HrPosition> items = new ArrayList<>();

    // Cache lists for ComboBoxes
    private List<HrOrgStructure> orgStructureList = new ArrayList<>();
    private List<HrPosition> allPositionsList = new ArrayList<>();

    public PositionLevelView(CurrentUser currentUser,
                             CommonService commonService,
                             HrPositionService hrPositionService,
                             HrOrgStructureRepository orgStructureRepo,
                             HrPositionRepository positionRepo) {
        this.currentUser = currentUser;
        this.commonService = commonService;
        this.hrPositionService = hrPositionService;
        this.orgStructureRepo = orgStructureRepo;
        this.positionRepo = positionRepo;

        // ambil authorization
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

        add(new ViewToolbar("Master Position"));
        createBody();
        setListener();
        setAuthorization();

        // Initial Load reference data
        refreshReferenceData();
    }

    private void refreshReferenceData() {
        try {
            orgStructureList = (List<HrOrgStructure>) orgStructureRepo.findAll();
            allPositionsList = positionRepo.findAll(); // Load semua posisi untuk 'Reports To'
        } catch (Exception e) {
            log.error("Failed to load reference data", e);
        }
    }

    private void createBody() {
        body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);
        body.setSizeFull();

        searchField = new TextField("Search");
        searchField.setPlaceholder("Cari nama / kode...");
        populateButton = new Button("Populate");
        saveButton = new Button("Save");
        addButton = new Button("Add Position");
        deleteButton = new Button("Delete");
        deleteButton.setEnabled(false);

        HorizontalLayout left = new HorizontalLayout(searchField, populateButton);
        left.setSpacing(true);
        left.setAlignItems(FlexComponent.Alignment.BASELINE);

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.add(left);
        header.add(deleteButton, saveButton, addButton);
        header.setAlignItems(FlexComponent.Alignment.BASELINE);
        header.expand(left);

        grid = new Grid<>(HrPosition.class, false);
        grid.setSizeFull();
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.setItems(items);

        grid.addSelectionListener(ev -> deleteButton.setEnabled(!ev.getAllSelectedItems().isEmpty()));

        // --- KOLOM 1: Org Structure (Department) ---
        grid.addColumn(new ComponentRenderer<>(row -> {
            ComboBox<HrOrgStructure> cb = new ComboBox<>();
            cb.setWidthFull();
            cb.setPlaceholder("Select Org");
            cb.setItems(orgStructureList);
            cb.setItemLabelGenerator(HrOrgStructure::getName);
            cb.setValue(row.getOrgStructure());
            cb.addValueChangeListener(e -> row.setOrgStructure(e.getValue()));
            return cb;
        })).setHeader("Org Structure").setAutoWidth(true).setFlexGrow(2);

        // --- KOLOM 2: Code ---
        grid.addColumn(new ComponentRenderer<>(row -> {
            TextField tf = new TextField();
            tf.setWidthFull();
            tf.setPlaceholder("Code");
            tf.setValue(row.getCode() != null ? row.getCode() : "");
            tf.addValueChangeListener(e -> row.setCode(e.getValue()));
            return tf;
        })).setHeader("Code").setWidth("120px").setFlexGrow(0);

        // --- KOLOM 3: Name (Position Name) ---
        grid.addColumn(new ComponentRenderer<>(row -> {
            TextField tf = new TextField();
            tf.setWidthFull();
            tf.setPlaceholder("Position Name");
            tf.setValue(row.getName() != null ? row.getName() : "");
            tf.addValueChangeListener(e -> row.setName(e.getValue()));
            return tf;
        })).setHeader("Name").setAutoWidth(true).setFlexGrow(2);

        // --- KOLOM 4: Level ---
        grid.addColumn(new ComponentRenderer<>(row -> {
            IntegerField num = new IntegerField();
            num.setWidthFull();
            num.setPlaceholder("Lvl");
            num.setValue(row.getLevel());
            num.addValueChangeListener(e -> row.setLevel(e.getValue()));
            return num;
        })).setHeader("Level").setWidth("100px").setFlexGrow(0);

        // --- KOLOM 5: Is Managerial ---
        grid.addColumn(new ComponentRenderer<>(row -> {
            Checkbox cb = new Checkbox();
            cb.setValue(Boolean.TRUE.equals(row.getIsManagerial()));
            cb.addValueChangeListener(e -> row.setIsManagerial(e.getValue()));
            return cb;
        })).setHeader("Mgr?").setWidth("70px").setFlexGrow(0);

        // --- KOLOM 6: Reports To ---
        grid.addColumn(new ComponentRenderer<>(row -> {
            ComboBox<HrPosition> cb = new ComboBox<>();
            cb.setWidthFull();
            cb.setPlaceholder("Reports To");
            // Filter agar tidak memilih diri sendiri (loop sederhana untuk UX)
            List<HrPosition> parents = new ArrayList<>(allPositionsList);
            if(row.getId() != null) {
                parents.removeIf(p -> p.getId().equals(row.getId()));
            }
            cb.setItems(parents);
            cb.setItemLabelGenerator(p -> p.getName() + " (" + p.getCode() + ")");
            cb.setValue(row.getReportsTo());
            cb.addValueChangeListener(e -> row.setReportsTo(e.getValue()));
            return cb;
        })).setHeader("Reports To").setAutoWidth(true).setFlexGrow(2);

        // --- KOLOM 7: Notes (Description) ---
        grid.addColumn(new ComponentRenderer<>(row -> {
            TextField tf = new TextField();
            tf.setWidthFull();
            tf.setPlaceholder("Notes");
            tf.setValue(row.getNotes() != null ? row.getNotes() : "");
            tf.addValueChangeListener(e -> row.setNotes(e.getValue()));
            return tf;
        })).setHeader("Notes").setAutoWidth(true).setFlexGrow(2);

        // --- KOLOM ACTION ---
        grid.addComponentColumn(row -> {
            // Baris baru (belum punya ID) -> tombol Cancel
            if (row.getId() == null) {
                Button cancel = new Button(new Icon(VaadinIcon.CLOSE_SMALL), e -> {
                    items.remove(row);
                    grid.getDataProvider().refreshAll();
                    notifyWarn("Penambahan data baru dibatalkan.");
                });
                cancel.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE);
                cancel.getElement().setProperty("title", "Batalkan baris ini");
                return cancel;
            }
            // Baris persisted -> tombol Delete (single row)
            Button del = new Button(new Icon(VaadinIcon.TRASH), e -> {
                if (!auth.canDelete) return;
                ConfirmDialog cd = new ConfirmDialog();
                cd.setHeader("Hapus data ini?");
                cd.setText("Tindakan ini tidak dapat dibatalkan.");
                cd.setCancelable(true);
                cd.setConfirmText("Hapus");
                cd.setConfirmButtonTheme("error primary");
                cd.addConfirmListener(ev -> {
                    // [Ubah] Panggil delete pada hrPositionService
                    hrPositionService.deleteByIds(List.of(row.getId()), currentUser.require());
                    notifySuccess("Data terhapus.");
                    populateButton.click();
                });
                cd.open();
            });
            del.addThemeVariants(com.vaadin.flow.component.button.ButtonVariant.LUMO_TERTIARY_INLINE,
                    com.vaadin.flow.component.button.ButtonVariant.LUMO_ERROR);
            del.getElement().setProperty("title", "Hapus data ini");
            return del;
        }).setHeader("Aksi").setWidth("100px").setFlexGrow(0);

        body.add(header, grid);
        body.setFlexGrow(1, grid);
        add(body);
        setSizeFull();
    }

    private void setListener() {
        populateButton.addClickListener(e -> {
            if (!auth.canView) return;
            String kw = searchField.getValue();

            // Refresh reference data agar dropdown 'Reports To' up-to-date
            refreshReferenceData();

            // [Ubah] Panggil findAllOrSearch pada hrPositionService
            List<HrPosition> result = hrPositionService.findAllOrSearch(kw == null ? "" : kw.trim());

            items.clear();
            items.addAll(result);
            grid.getDataProvider().refreshAll();

            if(result.isEmpty()) notifyWarn("Data tidak ditemukan.");
            else notifySuccess("Data dimuat: " + result.size());
        });

        saveButton.addClickListener(e -> {
            if (!auth.canCreate && !auth.canEdit) return;
            try {
                // [Ubah] Simpan menggunakan hrPositionService
                hrPositionService.saveAll(new ArrayList<>(items), currentUser.require());
                notifySuccess("Perubahan berhasil disimpan.");
                populateButton.click(); // refresh dari DB
            } catch (Exception ex) {
                log.error("Gagal menyimpan Position", ex);
                notifyError("Gagal menyimpan. Cek kembali kelengkapan data.");
            }
        });

        addButton.addClickListener(e -> {
            if (!auth.canCreate) return;
            try {
                // [Ubah] Instansiasi HrPosition
                HrPosition row = new HrPosition();
                row.setName("");
                row.setCode("");
                row.setIsManagerial(false);
                row.setNotes("");
                // Default values lain jika perlu

                items.add(row);
                grid.getDataProvider().refreshAll();
                // Scroll ke bawah (optional, grid vaadin kadang auto scroll)
                grid.scrollToEnd();
                notifySuccess("Baris baru ditambahkan.");
            } catch (Exception ex) {
                log.error("Gagal menambah baris Position", ex);
                notifyError("Gagal menambah baris.");
            }
        });

        // Delete Button (Bulk)
        deleteButton.addClickListener(e -> {
            if (!auth.canDelete) return;

            var selected = new HashSet<>(grid.getSelectedItems());
            if (selected.isEmpty()) return;

            var unsaved = selected.stream().filter(x -> x.getId() == null).toList();
            var persistedIds = selected.stream().map(HrPosition::getId)
                    .filter(Objects::nonNull).toList();

            if (!unsaved.isEmpty()) {
                items.removeAll(unsaved);
                grid.getDataProvider().refreshAll();
                notifyWarn(unsaved.size() + " baris baru dibatalkan.");
            }

            if (persistedIds.isEmpty()) return;

            ConfirmDialog cd = new ConfirmDialog();
            cd.setHeader("Hapus " + persistedIds.size() + " data?");
            cd.setText("Tindakan ini tidak dapat dibatalkan.");
            cd.setCancelable(true);
            cd.setConfirmText("Hapus");
            cd.setConfirmButtonTheme("error primary");
            cd.addConfirmListener(ev -> {
                try {
                    // [Ubah] Delete via Service
                    hrPositionService.deleteByIds(persistedIds, currentUser.require());
                    notifySuccess(persistedIds.size() + " data terhapus.");
                    populateButton.click();
                } catch (Exception ex) {
                    log.error("Gagal menghapus Position", ex);
                    notifyError("Gagal menghapus. Coba lagi.");
                }
            });
            cd.open();
        });
    }

    private void setAuthorization() {
        if (!auth.canView) {
            populateButton.setEnabled(false);
            grid.setEnabled(false);
        }
        if (!auth.canCreate && !auth.canEdit) {
            saveButton.setEnabled(false);
            addButton.setEnabled(false);
        } else if (!auth.canCreate) {
            addButton.setEnabled(false);
        }
        if (!auth.canDelete) {
            deleteButton.setEnabled(false);
        }
    }

    private void notifySuccess(String msg) {
        Notification n = Notification.show(msg);
        n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        n.setPosition(Notification.Position.TOP_END);
    }

    private void notifyWarn(String msg) {
        Notification n = Notification.show(msg);
        n.addThemeVariants(NotificationVariant.LUMO_WARNING);
        n.setPosition(Notification.Position.TOP_END);
    }

    private void notifyError(String msg) {
        Notification n = Notification.show(msg);
        n.addThemeVariants(NotificationVariant.LUMO_ERROR);
        n.setPosition(Notification.Position.TOP_END);
    }
}