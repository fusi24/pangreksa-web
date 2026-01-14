package com.fusi24.pangreksa.web.view.admin;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.HrLeaveAbsenceTypes;
import com.fusi24.pangreksa.web.model.enumerate.LeaveAbsenceTypeEnum;
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.LeaveService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

@Route("leave-absence-management-access")
@PageTitle("Pengelolaan Cuti dan Izin Karyawan")
@Menu(order = 39, icon = "vaadin:calendar-user", title = "Pengelolaan Cuti dan Izin Karyawan")
@RolesAllowed("LEAVE_ABSC_MGT")
//@PermitAll // When security is enabled, allow all authenticated users
public class GlobalLeaveAbsenceManagementView extends Main {
    private static final long serialVersionUID = 39L;
    private static final Logger log = LoggerFactory.getLogger(GlobalLeaveAbsenceManagementView.class);
    private final CurrentUser currentUser;
    private final CommonService commonService;
    private final LeaveService leaveService;
    private Authorization auth;

    public static final String VIEW_NAME = "Pengelolaan Cuti dan Izin Karyawan";

    private VerticalLayout body;

    private boolean isGridEdit = false;
    private boolean isMenuEdit = false;

    Grid<HrLeaveAbsenceTypes> leaveAbsenceTypesGrid;
    List<HrLeaveAbsenceTypes> leaveAbsenceTypesList;

    private Button saveButton;
    private Button addButton;

    private List<HrLeaveAbsenceTypes> deletedItems = new ArrayList<>();

    public GlobalLeaveAbsenceManagementView(CurrentUser currentUser, CommonService commonService, LeaveService leaveService) {
        this.currentUser = currentUser;
        this.commonService = commonService;
        this.leaveService = leaveService;

        this.auth = commonService.getAuthorization(
                currentUser.require(),
                (String) UI.getCurrent().getSession().getAttribute("responsibility"),
                this.serialVersionUID);

        log.debug("Page {}, Authorization: {} {} {} {}", VIEW_NAME, auth.canView, auth.canCreate, auth.canEdit, auth.canDelete);

        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar(VIEW_NAME));
        createBody();

        setListener();
        setAuthorization();
    }

    private void setAuthorization(){
        if(!this.auth.canCreate){
            addButton.setEnabled(false);
            saveButton.setEnabled(false);
        }
    }

    private void createBody() {
        getStyle().setHeight("100%");

        this.body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);
        body.setHeightFull();

        addButton = new Button("Tambah");
        saveButton = new Button("Simpan");

        HorizontalLayout functionLeaveAbsenceManagement = new HorizontalLayout();
        functionLeaveAbsenceManagement.setWidthFull();
        functionLeaveAbsenceManagement.add(saveButton, addButton);
        functionLeaveAbsenceManagement.setAlignItems(HorizontalLayout.Alignment.BASELINE);
        //set to right align
        functionLeaveAbsenceManagement.getStyle().set("justify-content", "flex-end");

        VerticalLayout contentVerticalLayout = new VerticalLayout(functionLeaveAbsenceManagement, createGrid());
        contentVerticalLayout.setHeightFull();

        body.add(contentVerticalLayout);

        if(this.auth.canView){
            populateGrid();
        }

        add(body);
    }

    private Grid createGrid(){
        leaveAbsenceTypesGrid = null;
        leaveAbsenceTypesGrid = new Grid<>(HrLeaveAbsenceTypes.class, false);

        // Sort Order editable
        leaveAbsenceTypesGrid.addColumn(new ComponentRenderer<>(lat -> {
            NumberField sortOrderField = new NumberField();
            sortOrderField.setValue(lat != null && lat.getSortOrder() != null
                    ? lat.getSortOrder(): 0.0);
            sortOrderField.setWidth("80px");
            sortOrderField.addValueChangeListener(e -> {
                if (lat.getSortOrder() != null && e.getValue() != null) {
                    lat.setSortOrder(e.getValue().intValue());
                    this.isGridEdit = true;
                    // Optionally persist change here
                }
            });
            return sortOrderField;
        })).setHeader("Set Urutan").setFlexGrow(0).setAutoWidth(true);

        // Editable Type column
        leaveAbsenceTypesGrid.addColumn(new ComponentRenderer<>(lat -> {
            TextField labelField = new TextField();
            labelField.setValue(lat.getLeaveType());
            labelField.setWidthFull();
            labelField.setErrorMessage("Tipe wajib diisi");
            labelField.setManualValidation(true);
            labelField.setValueChangeMode(ValueChangeMode.EAGER);
            labelField.addValueChangeListener(e -> {
                boolean valid = StringUtils.isNotBlank(labelField.getValue());
                labelField.setInvalid(!valid);
                saveButton.setEnabled(valid);
                lat.setLeaveType(e.getValue());
                this.isGridEdit = true;
            });

            return labelField;
        })).setHeader("Tipe").setFlexGrow(0);

        // Editable Name/ Label column
        leaveAbsenceTypesGrid.addColumn(new ComponentRenderer<>(lat -> {
            TextField labelField = new TextField();
            labelField.setValue(lat.getLabel());
            labelField.setWidthFull();
            labelField.setErrorMessage("Name wajib diisi");
            labelField.setManualValidation(true);
            labelField.setValueChangeMode(ValueChangeMode.EAGER);
            labelField.addValueChangeListener(e -> {
                boolean valid = StringUtils.isNotBlank(labelField.getValue());
                labelField.setInvalid(!valid);
                saveButton.setEnabled(valid);
                lat.setLabel(e.getValue());
                this.isGridEdit = true;
            });
            return labelField;
        })).setHeader("Nama").setFlexGrow(1);

        // Editable Leave Absence Type Column
        leaveAbsenceTypesGrid.addColumn(new ComponentRenderer<>(lat -> {
            ComboBox<LeaveAbsenceTypeEnum> pagesDropdown = new ComboBox<>();
            pagesDropdown.setItems(LeaveAbsenceTypeEnum.values());
            pagesDropdown.setItemLabelGenerator(LeaveAbsenceTypeEnum::name);
            pagesDropdown.setWidth("180px");
            if (lat.getLeaveAbsenceType() != null) {
                pagesDropdown.setValue(lat.getLeaveAbsenceType());
            }
            pagesDropdown.addValueChangeListener(e -> {
                if (lat.getLeaveAbsenceType() != null) {
                    lat.setLeaveAbsenceType(e.getValue());
                    this.isGridEdit = true;
                    // Optionally persist change here
                }
            });
            return pagesDropdown;
        })).setHeader("Tipe Cuti").setFlexGrow(0).setWidth("200px");

        // Editable Description column
        leaveAbsenceTypesGrid.addColumn(new ComponentRenderer<>(lat -> {
            TextField labelField = new TextField();
            labelField.setValue(lat.getDescription());
            labelField.setWidthFull();
            labelField.setErrorMessage("Description wajib diisi");
            labelField.setManualValidation(true);
            labelField.setValueChangeMode(ValueChangeMode.EAGER);
            labelField.addValueChangeListener(e -> {
                boolean valid = StringUtils.isNotBlank(labelField.getValue());
                labelField.setInvalid(!valid);
                saveButton.setEnabled(valid);
                lat.setDescription(e.getValue());
                this.isGridEdit = true;
            });
            return labelField;
        })).setHeader("Deskripsi").setFlexGrow(1);

        // Editable Active column
        leaveAbsenceTypesGrid.addColumn(new ComponentRenderer<>(lat -> {
            Checkbox activeCheckbox = new Checkbox(Boolean.TRUE.equals(lat.getIsEnable()));
            activeCheckbox.addValueChangeListener(e -> {
                lat.setIsEnable(e.getValue());
                this.isGridEdit = true;
            });
            return activeCheckbox;
        })).setHeader("Enable").setFlexGrow(0);

        // Editable Max Allowed Days column
        leaveAbsenceTypesGrid.addColumn(new ComponentRenderer<>(lat -> {
            NumberField maxDaysField = new NumberField();
            maxDaysField.setStepButtonsVisible(true);
            maxDaysField.setMin(0);
            maxDaysField.setMax(50);
            maxDaysField.setValue(lat.getMaxAllowedDays() != null ? lat.getMaxAllowedDays().doubleValue() : 0.0);
            maxDaysField.setWidth("120px");
            maxDaysField.addValueChangeListener(e -> {
                lat.setMaxAllowedDays(e.getValue() != null ? e.getValue().intValue() : 0);
                this.isGridEdit = true;
            });
            return maxDaysField;
        })).setHeader("Maksimal Cuti").setFlexGrow(0).setAutoWidth(true);

        // Action column with delete button (icon only, no title)
        // Inside your grid's delete column
        leaveAbsenceTypesGrid.addColumn(new ComponentRenderer<>(lat -> {
            Button deleteButton = new Button();
            deleteButton.setIcon(VaadinIcon.CLOSE.create());
            deleteButton.getElement().setAttribute("title", "Hapus");
            deleteButton.addClickListener(e -> {
                ConfirmDialog confirmDialog = new ConfirmDialog();
                confirmDialog.setHeader("Confirm Deletion");
                confirmDialog.setText("Are you sure you want to delete '" + lat.getLabel() + "'?");
                confirmDialog.setCancelable(true);
                confirmDialog.addCancelListener(ev -> {});
                confirmDialog.setConfirmText("Hapus");
                confirmDialog.addConfirmListener(ev -> {
                    if (lat.getId() != null) {
                        // Mark for deletion (will be deleted on save)
                        deletedItems.add(lat);
                    }
                    // Remove from grid regardless
                    leaveAbsenceTypesGrid.getListDataView().removeItem(lat);
                    this.isGridEdit = true;
                });
                confirmDialog.open();
            });

            return this.auth.canDelete ? deleteButton : null;
        })).setHeader("").setFlexGrow(0).setAutoWidth(true);

        return this.leaveAbsenceTypesGrid;
    }

    private void populateGrid() {
        leaveAbsenceTypesList = leaveService.findAllLeaveAbsenceTypesList();
        leaveAbsenceTypesGrid.setItems(leaveAbsenceTypesList);
        leaveAbsenceTypesGrid.getColumns().forEach(col -> col.setAutoWidth(true));
        leaveAbsenceTypesGrid.setHeightFull();
    }

    private void setListener() {

        addButton.addClickListener(e -> {
            // Get Maximum number of sort order
            int maxSortOrder = StreamSupport.stream(this.leaveAbsenceTypesGrid.getListDataView().getItems().spliterator(), false)
                    .mapToInt(item -> item.getSortOrder() != null
                            ? item.getSortOrder()
                            : 0)
                    .max()
                    .orElse(0);

            HrLeaveAbsenceTypes newLeaveAbsenceType = HrLeaveAbsenceTypes.builder()
                    .leaveAbsenceType(LeaveAbsenceTypeEnum.UNPAID_LEAVE)
                    .leaveType("NEW")
                    .label("New Tipe Cuti")
                    .isEnable(Boolean.FALSE)
                    .description("n/a")
                    .maxAllowedDays(0)
                    .sortOrder(maxSortOrder + 1) // Increment max sort order
                    .build();
            leaveAbsenceTypesGrid.getListDataView().addItem(newLeaveAbsenceType);

            this.isGridEdit = true;
        });

        saveButton.addClickListener(e -> {
            var user = currentUser.require();

            // 🔴 FIRST: Delete marked items
            for (HrLeaveAbsenceTypes deleted : deletedItems) {
                try {
                    leaveService.deleteLeaveAbsenceType(deleted.getId(), user);
                    log.debug("Deleted Leave Absence Type: {}", deleted.getLabel());
                } catch (Exception ex) {
                    log.error("Failed to delete Leave Absence Type: " + deleted.getLabel(), ex);
                    Notification.show("Failed to delete: " + deleted.getLabel(), 5000, Notification.Position.MIDDLE);
                }
            }
            deletedItems.clear(); // Clear after processing

            // 🟢 Then: Save all remaining items (new + updated)
            if (this.isGridEdit) {
                List<HrLeaveAbsenceTypes> items = leaveAbsenceTypesGrid.getListDataView().getItems().toList();
                items.forEach(i -> {

                    // Format type to uppercase and replace space with underscore
                    if (i.getLeaveAbsenceType() != null) {
                        i.setLeaveAbsenceType(LeaveAbsenceTypeEnum.valueOf(i.getLeaveAbsenceType().name().toUpperCase().replace(" ", "_")));
                    }

                    // Format label to lowercase and first character should capitalized
                    if (i.getLabel() != null && !i.getLabel().isEmpty()) {
                        i.setLabel(i.getLabel().substring(0, 1).toUpperCase() + i.getLabel().substring(1).toLowerCase());
                    }

                    leaveService.saveLeaveAbsenceType(i, user);
                    log.debug("Saving Leave Absence Type: {} with Sort Order: {}", i.getLabel(), i.getSortOrder());
                });

                this.isGridEdit = false;

                Notification.show("Leave Absence Type List saved successfully.");

                if (this.auth.canView)
                    populateGrid();
            }
        });
    }
}
