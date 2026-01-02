package com.fusi24.pangreksa.web.view.admin;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.base.util.ConfirmationDialogUtil;
import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrOrgStructure;
import com.fusi24.pangreksa.web.model.enumerate.OrgStructureEnum;
import com.fusi24.pangreksa.web.repo.HrCompanyRepository;
import com.fusi24.pangreksa.web.repo.HrOrgStructureRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.Autocapitalize;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Route("master-org-structure-page-access")
@PageTitle("Master Organization Structure")
@Menu(order = 5, icon = "vaadin:organization", title = "Master Organization Structure")
@RolesAllowed("USERS_MGT")
public class MasterOrgStructureView extends Main {
    public static final String VIEW_NAME = "Master Organization Structure";
    private final HrOrgStructureRepository orgStructureRepo;
    private final HrCompanyRepository companyRepo;

    private final Grid<HrOrgStructure> grid = new Grid<>(HrOrgStructure.class);
    private final TextField searchField = new TextField("Search");
    private final Button addButton = new Button("Add Organization Structure");

    private final Dialog dialog = new Dialog();
    private final FormLayout form = new FormLayout();
    private final Binder<HrOrgStructure> binder = new Binder<>(HrOrgStructure.class);

    private final ComboBox<HrCompany> companyField = new ComboBox<>("Company");
    private final ComboBox<HrOrgStructure> parentField = new ComboBox<>("Parent Structure");
    private final TextField codeField = new TextField("Code");
    private final TextField nameField = new TextField("Name");
    private final ComboBox<OrgStructureEnum> typeField = new ComboBox<>("Type");
    private final TextArea descriptionField = new TextArea("Description");
    private final Checkbox isActiveField = new Checkbox();

    private HrOrgStructure currentOrgStructure;

    @Autowired
    public MasterOrgStructureView(HrOrgStructureRepository orgStructureRepo, HrCompanyRepository companyRepo) {
        this.orgStructureRepo = orgStructureRepo;
        this.companyRepo = companyRepo;

        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);
        setHeightFull();

        configureGrid();
        configureForm();
        addToolbar();
        add(grid, dialog);
        refreshGrid();

    }

    private void configureGrid() {
        grid.removeAllColumns();
        grid.addColumn(org -> org.getCompany() != null ? org.getCompany().getName() : "").setHeader("Company");
        grid.addColumn(org -> org.getParent() != null ? org.getParent().getName() : "").setHeader("Parent");
        grid.addColumn(HrOrgStructure::getName).setHeader("Name");
        grid.addColumn(HrOrgStructure::getCode).setHeader("Code");
        grid.addColumn(org -> org.getType() != null ? org.getType().name() : "").setHeader("Type");
        grid.addColumn(org -> BooleanUtils.toString(org.getIsActive(), "Active", "Inactive")).setHeader("Active");

        grid.addComponentColumn(org -> {
            Button edit = new Button("Edit");
            edit.addClickListener(e -> openEditDialog(org));
            return edit;
        }).setHeader("Edit");

        grid.addComponentColumn(org -> {
            Button delete = new Button("Delete");
            delete.addClickListener(e -> deleteOrgStructure(org));
            return delete;
        }).setHeader("Delete");
    }

    private void configureForm() {
        // Configure company dropdown
        companyField.setItems(companyRepo.findAll());
        companyField.setItemLabelGenerator(company -> company != null ? company.getName() : "");
        companyField.setRequired(true);

        // Configure parent structure dropdown
        parentField.setItems(orgStructureRepo.findAllWithAssociations());
        parentField.setItemLabelGenerator(parent -> parent != null ? parent.getName() : "");
        parentField.setPlaceholder("Select Parent Structure (Optional)");

        // Configure type dropdown
        typeField.setItems(OrgStructureEnum.values());
        typeField.setItemLabelGenerator(type -> type != null ? type.name() : "");
        typeField.setRequired(true);

        // Configure description field
        descriptionField.setPlaceholder("Enter description...");
        descriptionField.setMinLength(0);
        descriptionField.setMaxLength(1000);
        descriptionField.setHeight("100px");

        // Configure code field
        codeField.setMaxLength(50);
        codeField.setAutocapitalize(Autocapitalize.CHARACTERS);

        // Configure name field
        nameField.setMaxLength(100);
        nameField.setRequired(true);

        // Configure active checkbox
        isActiveField.setLabel("Active");

        form.add(companyField, parentField, codeField, nameField, typeField, descriptionField, isActiveField);
        form.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
            new FormLayout.ResponsiveStep("600px", 2, FormLayout.ResponsiveStep.LabelsPosition.TOP)
        );

        // Bind fields to entity
        binder.forField(companyField)
                .asRequired("Company is required")
                .bind(HrOrgStructure::getCompany, HrOrgStructure::setCompany);
        
        binder.forField(parentField)
                .bind(HrOrgStructure::getParent, HrOrgStructure::setParent);
        
        binder.forField(codeField)
                .asRequired("Code is required")
                .bind(HrOrgStructure::getCode, HrOrgStructure::setCode);
        
        binder.forField(nameField)
                .asRequired("Name is required")
                .bind(HrOrgStructure::getName, HrOrgStructure::setName);
        
        binder.forField(typeField)
                .asRequired("Type is required")
                .bind(HrOrgStructure::getType, HrOrgStructure::setType);
        
        binder.forField(descriptionField)
                .bind(HrOrgStructure::getDescription, HrOrgStructure::setDescription);
        
        binder.forField(isActiveField)
                .bind(HrOrgStructure::getIsActive, HrOrgStructure::setIsActive);

        dialog.add(new H3("Organization Structure Details"), form);

        Button saveButton = new Button("Save", e -> saveOrgStructure());
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelButton, saveButton);
    }

    private void addToolbar() {
        searchField.setPlaceholder("Search by code, name, or type...");
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
            grid.setItems(orgStructureRepo.findAllWithAssociations());
        } else {
            grid.setItems(orgStructureRepo.findAllWithAssociations().stream()
                    .filter(org -> 
                        (org.getCode() != null && org.getCode().toLowerCase().contains(term.toLowerCase())) ||
                        (org.getName() != null && org.getName().toLowerCase().contains(term.toLowerCase())) ||
                        (org.getType() != null && org.getType().name().toLowerCase().contains(term.toLowerCase())) ||
                        (org.getDescription() != null && org.getDescription().toLowerCase().contains(term.toLowerCase()))
                    )
                    .collect(Collectors.toList()));
        }
    }

    private void openNewDialog() {
        currentOrgStructure = new HrOrgStructure();
        // Refresh parent dropdown items
        parentField.setItems(orgStructureRepo.findAllWithAssociations());
        binder.readBean(currentOrgStructure);
        dialog.open();
    }

    private void openEditDialog(HrOrgStructure orgStructure) {
        currentOrgStructure = orgStructure;
        // Refresh parent dropdown items (excluding current item to avoid circular reference)
        List<HrOrgStructure> allStructures = orgStructureRepo.findAllWithAssociations();
        List<HrOrgStructure> parentOptions = allStructures.stream()
                .filter(struct -> !struct.getId().equals(orgStructure.getId()))
                .collect(Collectors.toList());
        parentField.setItems(parentOptions);
        binder.readBean(orgStructure);
        dialog.open();
    }

    private void saveOrgStructure() {
        try {
            binder.writeBean(currentOrgStructure);

            // Check for duplicate code
            boolean isDuplicateCode = orgStructureRepo.existsByCodeAndIdNot(
                    currentOrgStructure.getCode(),
                    currentOrgStructure.getId()
            );

            if (isDuplicateCode) {
                Notification.show("Organization structure code must be unique. Another structure already uses this code.",
                        5000, Notification.Position.MIDDLE);
                return;
            }

            // Check for duplicate name within same company
            boolean isDuplicateName = orgStructureRepo.existsByNameAndCompanyIdAndIdNot(
                    currentOrgStructure.getName(),
                    currentOrgStructure.getCompany().getId(),
                    currentOrgStructure.getId()
            );

            if (isDuplicateName) {
                Notification.show("Organization structure name must be unique within the company. Another structure already uses this name.",
                        5000, Notification.Position.MIDDLE);
                return;
            }

            // Check for circular reference if parent is set
            if (currentOrgStructure.getParent() != null) {
                if (hasCircularReference(currentOrgStructure, currentOrgStructure.getParent())) {
                    Notification.show("Cannot create circular reference. The selected parent creates a circular hierarchy.",
                            5000, Notification.Position.MIDDLE);
                    return;
                }
            }

            orgStructureRepo.save(currentOrgStructure);
            dialog.close();
            refreshGrid();
            Notification.show("Saved successfully!");
        } catch (Exception e) {
            Notification.show("Error saving organization structure: " + e.getMessage());
        }
    }

    private boolean hasCircularReference(HrOrgStructure child, HrOrgStructure potentialParent) {
        HrOrgStructure current = potentialParent;
        while (current != null) {
            if (current.getId().equals(child.getId())) {
                return true; // Circular reference detected
            }
            current = current.getParent();
        }
        return false;
    }

    private void deleteOrgStructure(HrOrgStructure orgStructure) {
        // Check if this structure has children
        List<HrOrgStructure> children = orgStructureRepo.findByParentId(orgStructure.getId());
        if (!children.isEmpty()) {
            Notification.show("Cannot delete organization structure. It has " + children.size() + 
                    " child structure(s). Please remove or reassign children first.",
                    5000, Notification.Position.MIDDLE);
            return;
        }

        String header = "Delete Organization Structure for " + orgStructure.getName() + "?";
        String message = "Are you sure you want to permanently delete this record? This action cannot be undone.";
        ConfirmationDialogUtil.showConfirmation(
                header,
                message,
                "Delete",
                event -> {
                    try {
                        orgStructureRepo.delete(orgStructure);
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