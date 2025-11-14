package com.fusi24.pangreksa.web.view.admin;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.base.util.ConfirmationDialogUtil;
import com.fusi24.pangreksa.web.model.entity.HrDepartment;
import com.fusi24.pangreksa.web.repo.HrDepartmentRepo;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.Autocapitalize;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;


@Route("master-dept-page-access")
@PageTitle("Master Department")
@Menu(order = 4, icon = "vaadin:clipboard-check", title = "Master Department")
@RolesAllowed("USERS_MGT")
public class MasterDepartmentView extends Main {
    public static final String VIEW_NAME = "Master Department";
    private final HrDepartmentRepo departmentRepo;

    private final Grid<HrDepartment> grid = new Grid<>(HrDepartment.class);
    private final TextField searchField = new TextField("Search");
    private final Button addButton = new Button("Add Department");

    private final Dialog dialog = new Dialog();
    private final FormLayout form = new FormLayout();
    private final Binder<HrDepartment> binder = new Binder<>(HrDepartment.class);

    private final TextField codeField = new TextField("Code");
    private final TextField nameField = new TextField("Name");
    private final TextField descriptionField = new TextField("Description");
    private final Checkbox isActiveField = new Checkbox();
    // For simplicity, isActive as checkbox could be added, but we'll skip for brevity

    private HrDepartment currentDepartment;

    @Autowired
    public MasterDepartmentView(HrDepartmentRepo departmentRepo) {
        this.departmentRepo = departmentRepo;

        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        configureGrid();
        configureForm();
        addToolbar();
        add(grid, dialog);
        refreshGrid();
    }

    private void configureGrid() {
        grid.setColumns("code", "name", "description", "isActive");
        grid.getColumnByKey("isActive").setHeader("Active");

        grid.addComponentColumn(dept -> {
            Button edit = new Button("Edit");
            edit.addClickListener(e -> openEditDialog(dept));
            return edit;
        }).setHeader("Edit");

        grid.addComponentColumn(dept -> {
            Button delete = new Button("Delete");
            delete.addClickListener(e -> deleteDepartment(dept));
            return delete;
        }).setHeader("Delete");
    }

    private void configureForm() {
        isActiveField.setLabel("Aktif");
        codeField.setMaxLength(3);
        codeField.setAutocapitalize(Autocapitalize.CHARACTERS);
        form.add(codeField, nameField, descriptionField, isActiveField);

        // Explicitly bind each field
        binder.forField(codeField)
                .asRequired("Code is required")
                .bind(HrDepartment::getCode, HrDepartment::setCode);
        binder.forField(nameField)
                .asRequired("Name is required")
                .bind(HrDepartment::getName, HrDepartment::setName);
        binder.forField(descriptionField)
                .bind(HrDepartment::getDescription, HrDepartment::setDescription);
        binder.forField(isActiveField)
                .bind(HrDepartment::getIsActive, HrDepartment::setIsActive);

        dialog.add(new H3("Department Details"), form);

        Button saveButton = new Button("Save", e -> saveDepartment());
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelButton, saveButton);
    }

    private void addToolbar() {
        searchField.setPlaceholder("Search by code or name...");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> refreshGrid());

        addButton.addClickListener(e -> openNewDialog());

        HorizontalLayout toolbar = new HorizontalLayout(searchField, addButton);
        toolbar.setWidthFull();
        searchField.setWidth("300px");
        toolbar.setVerticalComponentAlignment(FlexComponent.Alignment.END, addButton);

        add(new ViewToolbar(VIEW_NAME));
        add(toolbar);
    }

    private void refreshGrid() {
        String term = searchField.getValue();
        if (term == null || term.isEmpty()) {
            grid.setItems((Collection<HrDepartment>) departmentRepo.findAll());
        } else {
            grid.setItems(StreamSupport.stream(departmentRepo.findAll().spliterator(), false)
                    .filter(d -> d.getCode().toLowerCase().contains(term.toLowerCase()) ||
                            d.getName().toLowerCase().contains(term.toLowerCase()))
                    .collect(Collectors.toList()));
        }
    }

    private void openNewDialog() {
        currentDepartment = new HrDepartment();
        binder.readBean(currentDepartment);
        dialog.open();
    }

    private void openEditDialog(HrDepartment department) {
        currentDepartment = department;
        binder.readBean(department);
        dialog.open();
    }

    private void saveDepartment() {
        try {
            binder.writeBean(currentDepartment);

            // Check for duplicate code
            boolean isDuplicateCode = departmentRepo.existsByCodeAndIdNot(
                    currentDepartment.getCode(),
                    currentDepartment.getId()
            );

            if (isDuplicateCode) {
                Notification.show("Department code must be unique. Another department already uses this code.",
                        5000, Notification.Position.MIDDLE);
                return;
            }

            // Optional: Also check for duplicate name, if needed
            boolean isDuplicateName = departmentRepo.existsByNameAndIdNot(currentDepartment.getName(), currentDepartment.getId());
            if (isDuplicateName) {
                Notification.show("Department Name must be unique. Another department already uses this Name.",
                        5000, Notification.Position.MIDDLE);
                return;
            }

            departmentRepo.save(currentDepartment);
            dialog.close();
            refreshGrid();
            Notification.show("Saved successfully!");
        } catch (Exception e) {
            Notification.show("Error saving department: " + e.getMessage());
        }
    }

    private void deleteDepartment(HrDepartment department) {
        String header = "Delete Departement for " + department.getCode() + "?";
        String message = "Are you sure you want to permanently delete this record? This action cannot be undone.";
        ConfirmationDialogUtil.showConfirmation(
                header,
                message,
                "Delete", // The text on the confirm button
                // The action to perform on confirmation
                event -> {
                    try {
                        departmentRepo.delete(department);
                        refreshGrid();
                        Notification.show("Deleted successfully!");
                    } catch (Exception ex) {
                        Notification.show("Deletion failed: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                        ex.printStackTrace();
                    }
                }
        );
    }
}
