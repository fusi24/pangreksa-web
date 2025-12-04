package com.fusi24.pangreksa.web.view.admin;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.base.util.ConfirmationDialogUtil;
import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.repo.HrCompanyRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
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

import java.util.List;
import java.util.stream.Collectors;

@Route("master-company-page-access")
@PageTitle("Master Company")
@Menu(order = 4, icon = "vaadin:building", title = "Master Company")
@RolesAllowed("USERS_MGT")
public class MasterCompanyView extends Main {

    public static final String VIEW_NAME = "Master Company";

    private final HrCompanyRepository companyRepo;

    private final Grid<HrCompany> grid = new Grid<>(HrCompany.class);
    private final TextField searchField = new TextField("Search");
    private final Button addButton = new Button("Add Company");

    private final Dialog dialog = new Dialog();
    private final FormLayout form = new FormLayout();
    private final Binder<HrCompany> binder = new Binder<>(HrCompany.class);

    private final ComboBox<HrCompany> parentField = new ComboBox<>("Parent Company");
    private final TextField nameField = new TextField("Name");
    private final TextField shortNameField = new TextField("Short Name");
    private final TextField registrationNumberField = new TextField("Registration Number");
    private final DatePicker establishmentDateField = new DatePicker("Establishment Date");
    private final TextField phoneField = new TextField("Phone");
    private final TextField emailField = new TextField("Email");
    private final TextField websiteField = new TextField("Website");
    private final Checkbox isActiveField = new Checkbox("Active");
    private final Checkbox isHrManagedField = new Checkbox("HR Managed");
    private final TextArea notesField = new TextArea("Notes");

    private HrCompany currentCompany;

    @Autowired
    public MasterCompanyView(HrCompanyRepository companyRepo) {
        this.companyRepo = companyRepo;

        addClassNames(
                LumoUtility.BoxSizing.BORDER,
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Gap.SMALL
        );

        configureGrid();
        configureForm();
        addToolbar();

        add(grid, dialog);
        refreshGrid();
    }

    private void configureGrid() {
        grid.removeAllColumns();

        grid.addColumn(company -> company.getParent() != null ? company.getParent().getName() : "")
                .setHeader("Parent Company");
        grid.addColumn(HrCompany::getName).setHeader("Name");
        grid.addColumn(HrCompany::getShortName).setHeader("Short Name");
        grid.addColumn(HrCompany::getRegistrationNumber).setHeader("Registration Number");
        grid.addColumn(HrCompany::getPhone).setHeader("Phone");
        grid.addColumn(HrCompany::getEmail).setHeader("Email");
        grid.addColumn(HrCompany::getWebsite).setHeader("Website");
        grid.addColumn(c -> BooleanUtils.toString(c.getIsActive(), "Active", "Inactive"))
                .setHeader("Active");
        grid.addColumn(c -> BooleanUtils.toString(c.getIsHrManaged(), "Yes", "No"))
                .setHeader("HR Managed");

        grid.addComponentColumn(company -> {
            Button edit = new Button("Edit");
            edit.addClickListener(e -> openEditDialog(company));
            return edit;
        }).setHeader("Edit");

        grid.addComponentColumn(company -> {
            Button delete = new Button("Delete");
            delete.addClickListener(e -> deleteCompany(company));
            return delete;
        }).setHeader("Delete");
    }

    private void configureForm() {
        // Parent company dropdown
        parentField.setItemLabelGenerator(c -> c != null ? c.getName() : "");
        parentField.setPlaceholder("Select Parent Company (Optional)");

        // Name
        nameField.setRequired(true);
        nameField.setMaxLength(100);

        // Short name
        shortNameField.setMaxLength(20);
        shortNameField.setAutocapitalize(Autocapitalize.CHARACTERS);

        // Registration number
        registrationNumberField.setMaxLength(50);

        // Phone
        phoneField.setMaxLength(30);

        // Email
        emailField.setMaxLength(50);

        // Website
        websiteField.setMaxLength(100);

        // Notes
        notesField.setPlaceholder("Additional notes...");
        notesField.setMinLength(0);
        notesField.setMaxLength(2000);
        notesField.setHeight("120px");

        form.add(
                parentField,
                nameField,
                shortNameField,
                registrationNumberField,
                establishmentDateField,
                phoneField,
                emailField,
                websiteField,
                isActiveField,
                isHrManagedField,
                notesField
        );

        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
                new FormLayout.ResponsiveStep("600px", 2, FormLayout.ResponsiveStep.LabelsPosition.TOP)
        );

        // Binder
        binder.forField(parentField)
                .bind(HrCompany::getParent, HrCompany::setParent);

        binder.forField(nameField)
                .asRequired("Name is required")
                .bind(HrCompany::getName, HrCompany::setName);

        binder.forField(shortNameField)
                .bind(HrCompany::getShortName, HrCompany::setShortName);

        binder.forField(registrationNumberField)
                .bind(HrCompany::getRegistrationNumber, HrCompany::setRegistrationNumber);

        binder.forField(establishmentDateField)
                .bind(HrCompany::getEstablishmentDate, HrCompany::setEstablishmentDate);

        binder.forField(phoneField)
                .bind(HrCompany::getPhone, HrCompany::setPhone);

        binder.forField(emailField)
                .bind(HrCompany::getEmail, HrCompany::setEmail);

        binder.forField(websiteField)
                .bind(HrCompany::getWebsite, HrCompany::setWebsite);

        binder.forField(isActiveField)
                .bind(HrCompany::getIsActive, HrCompany::setIsActive);

        binder.forField(isHrManagedField)
                .bind(HrCompany::getIsHrManaged, HrCompany::setIsHrManaged);

        binder.forField(notesField)
                .bind(HrCompany::getNotes, HrCompany::setNotes);

        dialog.add(new H3("Company Details"), form);

        Button saveButton = new Button("Save", e -> saveCompany());
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelButton, saveButton);
    }

    private void addToolbar() {
        searchField.setPlaceholder("Search by name, short name, reg. number, phone, email, or website...");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> refreshGrid());

        addButton.addClickListener(e -> openNewDialog());

        HorizontalLayout toolbar = new HorizontalLayout(searchField, addButton);
        toolbar.setWidthFull();
        searchField.setWidth("350px");
        toolbar.setVerticalComponentAlignment(FlexComponent.Alignment.END, addButton);

        add(new ViewToolbar(VIEW_NAME));
        add(toolbar);
    }

    private void refreshGrid() {
        String term = searchField.getValue();
        List<HrCompany> all = companyRepo.findAll();

        if (term == null || term.isBlank()) {
            grid.setItems(all);
        } else {
            String lower = term.toLowerCase();
            grid.setItems(
                    all.stream()
                            .filter(c ->
                                    (c.getName() != null && c.getName().toLowerCase().contains(lower)) ||
                                            (c.getShortName() != null && c.getShortName().toLowerCase().contains(lower)) ||
                                            (c.getRegistrationNumber() != null && c.getRegistrationNumber().toLowerCase().contains(lower)) ||
                                            (c.getPhone() != null && c.getPhone().toLowerCase().contains(lower)) ||
                                            (c.getEmail() != null && c.getEmail().toLowerCase().contains(lower)) ||
                                            (c.getWebsite() != null && c.getWebsite().toLowerCase().contains(lower))
                            )
                            .collect(Collectors.toList())
            );
        }
    }

    private void openNewDialog() {
        currentCompany = new HrCompany();
        // default: aktif & HR managed (sesuai default di entity)
        currentCompany.setIsActive(true);
        currentCompany.setIsHrManaged(true);

        parentField.setItems(companyRepo.findAll());
        binder.readBean(currentCompany);
        dialog.open();
    }

    private void openEditDialog(HrCompany company) {
        currentCompany = company;

        List<HrCompany> all = companyRepo.findAll();
        List<HrCompany> parentOptions = all.stream()
                .filter(c -> !c.getId().equals(company.getId()))
                .collect(Collectors.toList());
        parentField.setItems(parentOptions);

        binder.readBean(company);
        dialog.open();
    }

    private void saveCompany() {
        try {
            binder.writeBean(currentCompany);

            // Contoh validasi sederhana: Name wajib unik (opsional, bisa Anda hilangkan kalau tidak perlu)
            boolean duplicateName = companyRepo.findAll().stream()
                    .anyMatch(c ->
                            c.getId() != null &&
                                    !c.getId().equals(currentCompany.getId()) &&
                                    c.getName() != null &&
                                    c.getName().equalsIgnoreCase(currentCompany.getName())
                    );
            if (duplicateName) {
                Notification.show(
                        "Company name must be unique. Another company already uses this name.",
                        5000,
                        Notification.Position.MIDDLE
                );
                return;
            }

            companyRepo.save(currentCompany);
            dialog.close();
            refreshGrid();
            Notification.show("Saved successfully!");
        } catch (Exception e) {
            Notification.show("Error saving company: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void deleteCompany(HrCompany company) {
        String header = "Delete Company " + company.getName() + "?";
        String message = "Are you sure you want to permanently delete this record? This action cannot be undone.";

        ConfirmationDialogUtil.showConfirmation(
                header,
                message,
                "Delete",
                event -> {
                    try {
                        companyRepo.delete(company);
                        refreshGrid();
                        Notification.show("Deleted successfully!");
                    } catch (Exception ex) {
                        Notification.show("Deletion failed: " + ex.getMessage(),
                                5000, Notification.Position.MIDDLE);
                        ex.printStackTrace();
                    }
                }
        );
    }
}
