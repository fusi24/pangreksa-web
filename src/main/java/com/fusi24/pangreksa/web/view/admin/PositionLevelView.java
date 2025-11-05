package com.fusi24.pangreksa.web.view.admin;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.HrPositionLevel;
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.PositionLevelService;
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

        HorizontalLayout left = new HorizontalLayout(searchField, populateButton);
        left.setSpacing(true);
        left.setAlignItems(FlexComponent.Alignment.BASELINE);

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.add(left);
        header.add(saveButton, addButton);
        header.setAlignItems(FlexComponent.Alignment.BASELINE);
        header.expand(left);

        grid = new Grid<>(HrPositionLevel.class, false);
        grid.setSizeFull();

        // penting: set data provider dari list mutable supaya tombol Add bisa dipakai kapan saja
        grid.setItems(items);

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

        body.add(header, grid);
        body.setFlexGrow(1, grid);
        add(body);
        setSizeFull();
    }

    private void setListener() {
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

            // Ambil dari state kita sendiri (lebih aman daripada getListDataView)
            positionLevelService.saveAll(new java.util.ArrayList<>(items), currentUser.require());

            populateButton.click(); // refresh dari DB biar sinkron
        });

        addButton.addClickListener(e -> {
            if (!auth.canCreate) return;
            HrPositionLevel row = new HrPositionLevel();
            row.setPosition("");
            row.setPosition_description("");
            items.add(row);                        // ✅ tambahkan ke list mutable
            grid.getDataProvider().refreshAll();   // ✅ render baris baru
            // Optional: scroll/focus ke baris terakhir
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
    }

}
