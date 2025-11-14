package com.fusi24.pangreksa.web.view.admin;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.HrPositionLevel;
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.PositionLevelService;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
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
import java.util.List;

@Route("master-position-level")
@PageTitle("Position Level")
@Menu(order = 33, icon = "vaadin:briefcase", title = "Position Level")
@RolesAllowed("POSITION_MGT")
public class PositionLevelView extends Main {

    private static final long serialVersionUID = 33L;
    private static final Logger log = LoggerFactory.getLogger(PositionLevelView.class);

    private final CurrentUser currentUser;
    private final CommonService commonService;
    private final PositionLevelService positionLevelService;

    private Authorization auth;

    // UI components
    private VerticalLayout body;
    private TextField searchField;
    private Button populateButton;
    private Button saveButton;
    private Button addButton;
    private Button deleteButton;

    private Grid<HrPositionLevel> grid;

    // data
    // private List<HrPositionLevel> rows;
    private final List<HrPositionLevel> items = new ArrayList<>();

    public PositionLevelView(CurrentUser currentUser,
                             CommonService commonService,
                             PositionLevelService positionLevelService) {
        this.currentUser = currentUser;
        this.commonService = commonService;
        this.positionLevelService = positionLevelService;

        // ambil authorization sesuai pola existing
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

        add(new ViewToolbar("Position Level"));
        createBody();
        setListener();
        setAuthorization();

        log.debug("Page Position Level, Authorization: view={} create={} edit={} delete={}",
                auth.canView, auth.canCreate, auth.canEdit, auth.canDelete);
    }

    private void createBody() {
        body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);
        body.setSizeFull();

        searchField = new TextField("Search");
        searchField.setPlaceholder("Cari position / description…");
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

        grid = new Grid<>(HrPositionLevel.class, false);
        grid.setSizeFull();

        // enable multi-select
        grid.setSelectionMode(Grid.SelectionMode.MULTI);

        // data provider awal
        grid.setItems(items);

        // aktifkan tombol delete saat ada pilihan
        grid.addSelectionListener(ev -> deleteButton.setEnabled(!ev.getAllSelectedItems().isEmpty()));

        // Kolom editable: position
        grid.addColumn(new ComponentRenderer<>(row -> {
            TextField tf = new TextField();
            tf.setWidthFull();
            tf.setValue(row.getPosition() != null ? row.getPosition() : "");
            tf.setPlaceholder("Position");
            tf.addValueChangeListener(e -> {
                row.setPosition(e.getValue());
            });
            return tf;
        })).setHeader("Position").setAutoWidth(true).setSortable(true).setFlexGrow(2);

        // Kolom editable: positionDescription
        grid.addColumn(new ComponentRenderer<>(row -> {
            TextField tf = new TextField();
            tf.setWidthFull();
            tf.setValue(row.getPosition_description() != null ? row.getPosition_description() : "");
            tf.setPlaceholder("Description");
            tf.addValueChangeListener(e -> {
                row.setPosition_description(e.getValue());
            });
            return tf;
        })).setHeader("Description").setAutoWidth(true).setSortable(true).setFlexGrow(2);

        // (Opsional) kolom read-only created/updated
        grid.addColumn(HrPositionLevel::getCreatedAt)
                .setHeader("Created At").setAutoWidth(true).setFlexGrow(1);
        grid.addColumn(HrPositionLevel::getUpdatedAt)
                .setHeader("Updated At").setAutoWidth(true).setFlexGrow(1);
        grid.addComponentColumn(row -> {
            // Baris baru (belum punya ID) -> tombol Cancel
            if (row.getId() == null) {
                Button cancel = new Button(new Icon(VaadinIcon.CLOSE_SMALL), e -> {
                    items.remove(row);
                    grid.getDataProvider().refreshAll();
                    notifyWarn("Penambahan data baru dibatalkan.");
                });
                cancel.getElement().setProperty("title", "Batalkan data baru");
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
                    positionLevelService.deleteByIds(List.of(row.getId()), currentUser.require());
                    notifySuccess("Data terhapus.");
                    populateButton.click();
                });
                cd.open();
            });
            del.getElement().setProperty("title", "Hapus data ini");
            return del;
        }).setHeader("Aksi").setWidth("120px").setFlexGrow(0);

        body.add(header, grid);
        body.setFlexGrow(1, grid);
        add(body);
        setSizeFull();
    }

    private void setListener() {
//        populateButton.addClickListener(e -> {
//            if (!auth.canView) return;
//            try {
//                String kw = searchField.getValue();
//                List<HrPositionLevel> result = positionLevelService.findAllOrSearch(kw == null ? "" : kw.trim());
//
//                items.clear();
//                items.addAll(result);
//                grid.getDataProvider().refreshAll();
//
//                if (items.isEmpty()) {
//                    notifyWarn("Tidak ada data yang cocok.");
//                } else {
//                    notifySuccess("Data dimuat: " + items.size() + " baris.");
//                }
//            } catch (Exception ex) {
//                log.error("Gagal populate Position Level", ex);
//                notifyError("Gagal memuat data. Silakan coba lagi.");
//            }
//        });

        populateButton.addClickListener(e -> {
            if (!auth.canView) return;
            String kw = searchField.getValue();
            List<HrPositionLevel> result = positionLevelService.findAllOrSearch(kw == null ? "" : kw.trim());

            items.clear();          // ✅ pakai list mutable
            items.addAll(result);   // ✅ isi ulang
            grid.getDataProvider().refreshAll(); // ✅ refresh tampilan
        });


        saveButton.addClickListener(e -> {
            if (!auth.canCreate && !auth.canEdit) return;
            try {
                positionLevelService.saveAll(new ArrayList<>(items), currentUser.require());
                notifySuccess("Perubahan berhasil disimpan.");
                populateButton.click(); // refresh dari DB
            } catch (Exception ex) {
                log.error("Gagal menyimpan Position Level", ex);
                notifyError("Gagal menyimpan. Silakan cek data & coba lagi.");
            }
        });


        addButton.addClickListener(e -> {
            if (!auth.canCreate) return;
            try {
                HrPositionLevel row = new HrPositionLevel();
                row.setPosition("");
                row.setPosition_description("");
                items.add(row);
                grid.getDataProvider().refreshAll();
                notifySuccess("Baris baru ditambahkan. Lengkapi data lalu klik Save.");
            } catch (Exception ex) {
                log.error("Gagal menambah baris Position Level", ex);
                notifyError("Gagal menambah baris. Coba lagi.");
            }
        });

        // tombol Delete (bulk) – pakai variable deleteButton yang dibuat di createBody()
        deleteButton.addClickListener(e -> {
            if (!auth.canDelete) return;

            var selected = new java.util.HashSet<>(grid.getSelectedItems());
            if (selected.isEmpty()) return;

            // pisahkan baris baru (id null) dan persisted
            var unsaved = selected.stream().filter(x -> x.getId() == null).toList();
            var persistedIds = selected.stream().map(HrPositionLevel::getId)
                    .filter(java.util.Objects::nonNull).toList();

            // hapus lokal baris baru
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
                    positionLevelService.deleteByIds(persistedIds, currentUser.require());
                    notifySuccess(persistedIds.size() + " data terhapus.");
                    populateButton.click();
                } catch (Exception ex) {
                    log.error("Gagal menghapus Position Level", ex);
                    notifyError("Gagal menghapus. Coba lagi.");
                }
            });
            cd.open();
        });

    }

    private void setAuthorization() {
        // view
        if (!auth.canView) {
            populateButton.setEnabled(false);
            grid.setEnabled(false);
        }
        // create/edit
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
