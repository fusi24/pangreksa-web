package com.fusi24.pangreksa.web.view.admin;

import com.fusi24.pangreksa.web.model.entity.HrDepartment;
import com.fusi24.pangreksa.web.repo.HrDepartmentRepo;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
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
    // For simplicity, isActive as checkbox could be added, but we'll skip for brevity

    private HrDepartment currentDepartment;

    @Autowired
    public MasterDepartmentView(HrDepartmentRepo departmentRepo) {
        this.departmentRepo = departmentRepo;
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
        form.add(codeField, nameField, descriptionField);
        binder.bindInstanceFields(this);
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
            departmentRepo.save(currentDepartment);
            dialog.close();
            refreshGrid();
            com.vaadin.flow.component.notification.Notification.show("Saved successfully!");
        } catch (Exception e) {
            com.vaadin.flow.component.notification.Notification.show("Error saving department: " + e.getMessage());
        }
    }

    private void deleteDepartment(HrDepartment department) {
        departmentRepo.delete(department);
        refreshGrid();
        com.vaadin.flow.component.notification.Notification.show("Deleted successfully!");
    }
}
