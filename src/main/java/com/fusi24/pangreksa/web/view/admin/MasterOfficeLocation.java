package com.fusi24.pangreksa.web.view.admin;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.base.util.GeometryUtil;
import com.fusi24.pangreksa.web.model.entity.HrOfficeLocation;
import com.fusi24.pangreksa.web.repo.HrOfficeLocationRepo;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.lang3.StringUtils;
import org.locationtech.jts.geom.Point;
import org.springframework.beans.factory.annotation.Autowired;
import software.xdev.vaadin.maps.leaflet.MapContainer;
import software.xdev.vaadin.maps.leaflet.basictypes.LLatLng;
import software.xdev.vaadin.maps.leaflet.layer.other.LGeoJSONLayer;
import software.xdev.vaadin.maps.leaflet.layer.raster.LTileLayer;
import software.xdev.vaadin.maps.leaflet.map.LMap;
import software.xdev.vaadin.maps.leaflet.registry.LComponentManagementRegistry;
import software.xdev.vaadin.maps.leaflet.registry.LDefaultComponentManagementRegistry;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Route("master-office-location-page-access")
@PageTitle("Master Office Location")
@Menu(order = 4, icon = "vaadin:clipboard-check", title = "Master Office Location")
@RolesAllowed("USERS_MGT")
public class MasterOfficeLocation extends Main {

    public static final String VIEW_NAME = "Master Office Location";

    private final HrOfficeLocationRepo repo;
    private final Grid<HrOfficeLocation> grid = new Grid<>(HrOfficeLocation.class);

    private final Button addButton = new Button("Tambah Office Location");
    private final TextField searchField = new TextField("Cari");

    // Main editor dialog
    private final Dialog editorDialog = new Dialog();
    private final TextField nameField = new TextField("Name");
    private final TextField descriptionField = new TextField("Description");
    private final Checkbox isActiveField = new Checkbox("Active");
    private final NumberField bufferField = new NumberField("Buffer (meters)");
    private final Button editGeometryBtn = new Button("Edit Geometry (GeoJSON)");
    private Button deleteButton;

    // Map components
    private LMap map;
    private MapContainer mapContainer;
    private LComponentManagementRegistry reg;

    private HorizontalLayout dialogLayout;
    private VerticalLayout dialogContent;

    // GeoJSON editor dialog
    private final Dialog geoJsonDialog = new Dialog();
    private final TextArea geoJsonArea = new TextArea("GeoJSON");

    private final Binder<HrOfficeLocation> binder = new Binder<>(HrOfficeLocation.class);
    private HrOfficeLocation currentEntity;

    @Autowired
    public MasterOfficeLocation(HrOfficeLocationRepo hrOfficeLocationRepo) {
        this.repo = hrOfficeLocationRepo;

        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        setupEditorDialog();
        configureGrid();
        setupGeoJsonDialog();

        addToolbar();
        add(grid);
    }

    private void addToolbar() {
        searchField.setPlaceholder("Cari berdasarkan name atau description...");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> refreshGrid());

        addButton.addClickListener(e -> openEditor(new HrOfficeLocation()));

        HorizontalLayout toolbar = new HorizontalLayout(searchField, addButton);
        toolbar.setWidthFull();
        searchField.setWidth("300px");
        toolbar.setVerticalComponentAlignment(FlexComponent.Alignment.END, addButton);

        add(new ViewToolbar(VIEW_NAME));
        add(toolbar);
    }

    private void configureGrid() {
        grid.removeAllColumns();
        grid.addColumn(HrOfficeLocation::getName).setHeader("Name");
        grid.addColumn(HrOfficeLocation::getDescription).setHeader("Description");
        grid.addColumn(HrOfficeLocation::getIsActive).setHeader("Active");
        grid.asSingleSelect().addValueChangeListener(event ->
                {
                    if(!event.getHasValue().isEmpty()) {
                        openEditor(event.getValue());
                    }
                }
        );
        refreshGrid();
    }

    private void setupEditorDialog() {
        FormLayout form = new FormLayout();
        form.add(nameField, descriptionField, isActiveField, bufferField);
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        editGeometryBtn.addClickListener(e -> openGeoJsonEditor());

        dialogLayout = new HorizontalLayout();
        dialogLayout.setWidthFull();
        dialogContent = new VerticalLayout(form, editGeometryBtn);

        // Initialize map registry and container
        reg = new LDefaultComponentManagementRegistry(dialogLayout);
        mapContainer = new MapContainer(reg);
        mapContainer.setHeight("600px");
        mapContainer.setWidthFull();

        map = mapContainer.getlMap();
        map.addLayer(LTileLayer.createDefaultForOpenStreetMapTileServer(reg));
        map.setView(new LLatLng(reg, -6.2088, 106.8456), 17); // Jakarta: lat, lng

        dialogLayout.add(mapContainer, dialogContent);

        dialogContent.setPadding(false);
        dialogContent.setSpacing(true);
        dialogContent.setWidth("50%");

        Button saveButton = new Button("Save", e -> saveAndClose());
        Button cancelButton = new Button("Cancel", e -> editorDialog.close());
        deleteButton = new Button("Delete", e -> deleteAndClose());
        deleteButton.getStyle().set("margin-inline-start", "auto");

        HorizontalLayout buttonBar = new HorizontalLayout(saveButton, cancelButton, deleteButton);
        buttonBar.setWidthFull();

        editorDialog.setHeaderTitle("Office Location");
        editorDialog.add(dialogLayout, buttonBar);
        editorDialog.setWidth("90%");
        editorDialog.setHeight("85%");

        // Bind form fields
        binder.bind(nameField, HrOfficeLocation::getName, HrOfficeLocation::setName);
        binder.bind(descriptionField, HrOfficeLocation::getDescription, HrOfficeLocation::setDescription);
        binder.bind(isActiveField, HrOfficeLocation::getIsActive, HrOfficeLocation::setIsActive);
        binder.bind(bufferField, HrOfficeLocation::getBuffer, HrOfficeLocation::setBuffer);
    }

    private void setupGeoJsonDialog() {
        geoJsonArea.setWidthFull();
        geoJsonArea.setHeight("200px");
        geoJsonArea.setPlaceholder("{ \"type\": \"Point\", \"coordinates\": [106.8456, -6.2088] }");

        Button applyBtn = new Button("Apply", e -> applyGeoJson());
        Button cancelBtn = new Button("Cancel", e -> geoJsonDialog.close());

        HorizontalLayout btnBar = new HorizontalLayout(applyBtn, cancelBtn);
        btnBar.setWidthFull();

        geoJsonDialog.setHeaderTitle("Edit Geometry (GeoJSON)");
        geoJsonDialog.add(geoJsonArea, btnBar);
        geoJsonDialog.setWidth("500px");
    }

    private void openEditor(HrOfficeLocation entity) {
        currentEntity = entity;
        binder.readBean(currentEntity);
        updateMapFromEntity();
        editorDialog.open();
        deleteButton.setVisible(currentEntity.getId() != null);
    }

    private void openGeoJsonEditor() {
        if (currentEntity == null) return;

        String geoJson = GeometryUtil.geometryToGeoJson(currentEntity.getGeometry());
        geoJsonArea.setValue(geoJson != null ? geoJson : "");
        geoJsonDialog.open();
    }

    private void applyGeoJson() {
        String input = geoJsonArea.getValue();
        try {
            if (input == null || input.trim().isEmpty()) {
                currentEntity.setGeometry(null);
                Notification.show("Geometry cleared.", 3000, Notification.Position.MIDDLE);
            } else {
                var geom = GeometryUtil.geoJsonToGeometry(input.trim());
                if (geom == null) {
                    throw new IllegalArgumentException("Parsed geometry is null.");
                }
                currentEntity.setGeometry(geom);
                Notification.show("Geometry updated successfully.", 3000, Notification.Position.MIDDLE);
            }
            updateMapFromEntity();
            geoJsonDialog.close();
        } catch (Exception ex) {
            Notification.show("Invalid GeoJSON: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    }

    private void updateMapFromEntity() {
        // Clear all layers and re-add base tile
        // Initialize map registry and container
        dialogLayout.removeAll();

        reg = new LDefaultComponentManagementRegistry(dialogLayout);
        mapContainer = new MapContainer(reg);
        mapContainer.setHeight("600px");
        mapContainer.setWidthFull();

        dialogLayout.add(mapContainer, dialogContent);

        map = mapContainer.getlMap();
        map.addLayer(LTileLayer.createDefaultForOpenStreetMapTileServer(reg));

        if (currentEntity != null && currentEntity.getGeometry() != null) {
            String geoJson = GeometryUtil.geometryToGeoJson(currentEntity.getGeometry());
            Point center = currentEntity.getGeometry().getCentroid();
            if (geoJson != null && !geoJson.trim().isEmpty()) {
                try {
                    map.addLayer(new LGeoJSONLayer(reg, geoJson));
                    map.setView(new LLatLng(reg, center.getY(), center.getX()), 17);
                } catch (Exception e) {
                    Notification.show("Warning: Could not render geometry on map.", 3000, Notification.Position.MIDDLE);
                }
            }
        } else {
            map.setView(new LLatLng(reg, -6.2088, 106.8456), 17); // Jakarta: lat, lng
        }
    }

    private void saveAndClose() {
        if (binder.writeBeanIfValid(currentEntity)) {
            repo.save(currentEntity);
            refreshGrid();
            editorDialog.close();
        } else {
            Notification.show("Please fill required fields correctly.", 3000, Notification.Position.MIDDLE);
        }
    }

    private void deleteAndClose() {
        if (currentEntity != null && currentEntity.getId() != null) {
            repo.delete(currentEntity);
            refreshGrid();
            editorDialog.close();
        }
    }

    private void refreshGrid() {
        String term = searchField.getValue();
        if(StringUtils.isNotBlank(term)) {
            grid.setItems(StreamSupport.stream(repo.findAll().spliterator(), false)
                    .filter(d -> d.getName().toLowerCase().contains(term.toLowerCase()) ||
                            d.getDescription().toLowerCase().contains(term.toLowerCase()))
                    .collect(Collectors.toList()));
        } else {
            grid.setItems(StreamSupport.stream(repo.findAll().spliterator(), false)
                    .collect(Collectors.toList()));
        }

    }
}