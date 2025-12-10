package com.fusi24.pangreksa.web.view.admin;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.base.util.ConfirmationDialogUtil;
import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrCompanyBranch;
import com.fusi24.pangreksa.web.repo.HrCompanyRepository;
import com.fusi24.pangreksa.web.service.CompanyBranchService;
import com.vaadin.flow.component.Component;
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
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.TabSheet;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Route("master-company-page-access")
@PageTitle("Master Company")
@Menu(order = 4, icon = "vaadin:building", title = "Master Company")
@RolesAllowed("USERS_MGT")
public class MasterCompanyView extends Main {

    public static final String VIEW_NAME = "Master Company";

    private final HrCompanyRepository companyRepo;
    private final CompanyBranchService branchService;

    // ===== TAB 1: COMPANY =====
    private final Grid<HrCompany> companyGrid = new Grid<>(HrCompany.class);
    private final TextField companySearchField = new TextField("Search");
    private final Button addCompanyButton = new Button("Add Company");

    private final Dialog companyDialog = new Dialog();
    private final FormLayout companyForm = new FormLayout();
    private final Binder<HrCompany> companyBinder = new Binder<>(HrCompany.class);

    private final ComboBox<HrCompany> parentField = new ComboBox<>("Parent Company");
    private final TextField nameField = new TextField("Name");
    private final TextField shortNameField = new TextField("Short Name");
    private final TextField registrationNumberField = new TextField("Registration Number");
    private final com.vaadin.flow.component.datepicker.DatePicker establishmentDateField =
            new com.vaadin.flow.component.datepicker.DatePicker("Establishment Date");
    private final TextField phoneField = new TextField("Phone");
    private final TextField emailField = new TextField("Email");
    private final TextField websiteField = new TextField("Website");
    private final Checkbox isActiveField = new Checkbox("Active");
    private final Checkbox isHrManagedField = new Checkbox("HR Managed");
    private final TextArea notesField = new TextArea("Notes");

    private HrCompany currentCompany;

    // ===== TAB 2: BRANCH =====
    private final Grid<HrCompanyBranch> branchGrid = new Grid<>(HrCompanyBranch.class, false);
    private final TextField branchSearchField = new TextField("Search Branch");
    private final Button addBranchButton = new Button("Add Branch");

    private final Dialog branchDialog = new Dialog();
    private final FormLayout branchForm = new FormLayout();
    private final Binder<HrCompanyBranch> branchBinder = new Binder<>(HrCompanyBranch.class);

    private final ComboBox<HrCompany> branchCompanyField = new ComboBox<>("Perusahaan");
    private final TextField branchCodeField = new TextField("Kode Cabang");
    private final TextField branchNameField = new TextField("Nama Cabang");
    private final TextArea branchAddressField = new TextArea("Alamat");
    private final TextField branchCityField = new TextField("Kota");
    private final TextField branchProvinceField = new TextField("Provinsi");
    private final ComboBox<String> branchTimezoneField = new ComboBox<>("Zona Waktu");
    private final TextField branchLatitudeField = new TextField("Latitude");
    private final TextField branchLongitudeField = new TextField("Longitude");

    private HrCompanyBranch currentBranch;

    @Autowired
    public MasterCompanyView(HrCompanyRepository companyRepo,
                             CompanyBranchService branchService) {
        this.companyRepo = companyRepo;
        this.branchService = branchService;

        addClassNames(
                LumoUtility.BoxSizing.BORDER,
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Gap.SMALL
        );

        add(new ViewToolbar(VIEW_NAME));

        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();
        tabs.setHeightFull();

        tabs.add("Perusahaan", createCompanyTab());
        tabs.add("Cabang Perusahaan", createBranchTab());

        add(tabs);

        refreshCompanyGrid();
        refreshBranchGrid();
    }

    // =========================
    // TAB COMPANY UI
    // =========================
    private Component createCompanyTab() {
        configureCompanyGrid();
        configureCompanyForm();
        Component toolbar = buildCompanyToolbar();

        VerticalLayout layout = new VerticalLayout(toolbar, companyGrid, companyDialog);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setWidthFull();
        return layout;
    }

    private void configureCompanyGrid() {
        companyGrid.removeAllColumns();

        companyGrid.addColumn(company -> company.getParent() != null ? company.getParent().getName() : "")
                .setHeader("Parent Company");
        companyGrid.addColumn(HrCompany::getName).setHeader("Name");
        companyGrid.addColumn(HrCompany::getShortName).setHeader("Short Name");
        companyGrid.addColumn(HrCompany::getRegistrationNumber).setHeader("Registration Number");
        companyGrid.addColumn(HrCompany::getPhone).setHeader("Phone");
        companyGrid.addColumn(HrCompany::getEmail).setHeader("Email");
        companyGrid.addColumn(HrCompany::getWebsite).setHeader("Website");
        companyGrid.addColumn(c -> BooleanUtils.toString(c.getIsActive(), "Active", "Inactive"))
                .setHeader("Active");
        companyGrid.addColumn(c -> BooleanUtils.toString(c.getIsHrManaged(), "Yes", "No"))
                .setHeader("HR Managed");

        companyGrid.addComponentColumn(company -> {
            Button edit = new Button("Edit");
            edit.addClickListener(e -> openEditCompanyDialog(company));
            return edit;
        }).setHeader("Edit");

        companyGrid.addComponentColumn(company -> {
            Button delete = new Button("Delete");
            delete.addClickListener(e -> deleteCompany(company));
            return delete;
        }).setHeader("Delete");
    }

    private void configureCompanyForm() {
        parentField.setItemLabelGenerator(c -> c != null ? c.getName() : "");
        parentField.setPlaceholder("Select Parent Company (Optional)");

        nameField.setRequired(true);
        nameField.setMaxLength(100);

        shortNameField.setMaxLength(20);
        shortNameField.setAutocapitalize(Autocapitalize.CHARACTERS);

        registrationNumberField.setMaxLength(50);
        phoneField.setMaxLength(30);
        emailField.setMaxLength(50);
        websiteField.setMaxLength(100);

        notesField.setPlaceholder("Additional notes...");
        notesField.setMinLength(0);
        notesField.setMaxLength(2000);
        notesField.setHeight("120px");

        companyForm.removeAll();
        companyForm.add(
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

        companyForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
                new FormLayout.ResponsiveStep("600px", 2, FormLayout.ResponsiveStep.LabelsPosition.TOP)
        );

        companyBinder.forField(parentField)
                .bind(HrCompany::getParent, HrCompany::setParent);

        companyBinder.forField(nameField)
                .asRequired("Name is required")
                .bind(HrCompany::getName, HrCompany::setName);

        companyBinder.forField(shortNameField)
                .bind(HrCompany::getShortName, HrCompany::setShortName);

        companyBinder.forField(registrationNumberField)
                .bind(HrCompany::getRegistrationNumber, HrCompany::setRegistrationNumber);

        companyBinder.forField(establishmentDateField)
                .bind(HrCompany::getEstablishmentDate, HrCompany::setEstablishmentDate);

        companyBinder.forField(phoneField)
                .bind(HrCompany::getPhone, HrCompany::setPhone);

        companyBinder.forField(emailField)
                .bind(HrCompany::getEmail, HrCompany::setEmail);

        companyBinder.forField(websiteField)
                .bind(HrCompany::getWebsite, HrCompany::setWebsite);

        companyBinder.forField(isActiveField)
                .bind(HrCompany::getIsActive, HrCompany::setIsActive);

        companyBinder.forField(isHrManagedField)
                .bind(HrCompany::getIsHrManaged, HrCompany::setIsHrManaged);

        companyBinder.forField(notesField)
                .bind(HrCompany::getNotes, HrCompany::setNotes);

        companyDialog.removeAll();
        companyDialog.add(new H3("Company Details"), companyForm);

        Button saveButton = new Button("Save", e -> saveCompany());
        Button cancelButton = new Button("Cancel", e -> companyDialog.close());
        companyDialog.getFooter().removeAll();
        companyDialog.getFooter().add(cancelButton, saveButton);
    }

    private Component buildCompanyToolbar() {
        companySearchField.setPlaceholder("Search by name, short name, reg. number, phone, email, or website...");
        companySearchField.setClearButtonVisible(true);
        companySearchField.setValueChangeMode(ValueChangeMode.EAGER);
        companySearchField.addValueChangeListener(e -> refreshCompanyGrid());

        addCompanyButton.addClickListener(e -> openNewCompanyDialog());

        HorizontalLayout toolbar = new HorizontalLayout(companySearchField, addCompanyButton);
        toolbar.setWidthFull();
        companySearchField.setWidth("350px");
        toolbar.setVerticalComponentAlignment(FlexComponent.Alignment.END, addCompanyButton);
        return toolbar;
    }

    private void refreshCompanyGrid() {
        String term = companySearchField.getValue();
        List<HrCompany> all = companyRepo.findAll();

        if (term == null || term.isBlank()) {
            companyGrid.setItems(all);
        } else {
            String lower = term.toLowerCase();
            companyGrid.setItems(
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

    private void openNewCompanyDialog() {
        currentCompany = new HrCompany();
        currentCompany.setIsActive(true);
        currentCompany.setIsHrManaged(true);

        parentField.setItems(companyRepo.findAll());
        companyBinder.readBean(currentCompany);
        companyDialog.open();
    }

    private void openEditCompanyDialog(HrCompany company) {
        currentCompany = company;

        List<HrCompany> all = companyRepo.findAll();
        List<HrCompany> parentOptions = all.stream()
                .filter(c -> !c.getId().equals(company.getId()))
                .collect(Collectors.toList());

        parentField.setItems(parentOptions);
        companyBinder.readBean(company);
        companyDialog.open();
    }

    private void saveCompany() {
        try {
            companyBinder.writeBean(currentCompany);

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
            companyDialog.close();
            refreshCompanyGrid();
            refreshBranchGrid();
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
                        refreshCompanyGrid();
                        refreshBranchGrid();
                        Notification.show("Deleted successfully!");
                    } catch (Exception ex) {
                        Notification.show("Deletion failed: " + ex.getMessage(),
                                5000, Notification.Position.MIDDLE);
                        ex.printStackTrace();
                    }
                }
        );
    }

    // =========================
    // TAB BRANCH UI
    // =========================
    private Component createBranchTab() {
        configureBranchGrid();
        configureBranchForm();
        Component toolbar = buildBranchToolbar();

        VerticalLayout layout = new VerticalLayout(toolbar, branchGrid, branchDialog);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setWidthFull();
        return layout;
    }

    private void configureBranchGrid() {
        branchGrid.removeAllColumns();

        branchGrid.addColumn(b -> b.getCompany() != null ? b.getCompany().getName() : "")
                .setHeader("Perusahaan");
        branchGrid.addColumn(HrCompanyBranch::getBranchCode).setHeader("Kode Cabang");
        branchGrid.addColumn(HrCompanyBranch::getBranchName).setHeader("Nama Cabang");
        branchGrid.addColumn(HrCompanyBranch::getBranchAddressCity).setHeader("Kota");
        branchGrid.addColumn(HrCompanyBranch::getBranchAddressProvince).setHeader("Provinsi");
        branchGrid.addColumn(HrCompanyBranch::getBranchTimezone).setHeader("Zona Waktu");

        branchGrid.addColumn(b -> {
            if (b.getBranchLatitude() == null || b.getBranchLongitude() == null) return "";
            return b.getBranchLatitude() + ", " + b.getBranchLongitude();
        }).setHeader("Koordinat");

        branchGrid.addComponentColumn(branch -> {
            Button edit = new Button("Edit");
            edit.addClickListener(e -> openEditBranchDialog(branch));
            return edit;
        }).setHeader("Edit");

        branchGrid.addComponentColumn(branch -> {
            Button delete = new Button("Delete");
            delete.addClickListener(e -> deleteBranch(branch));
            return delete;
        }).setHeader("Delete");
    }

    private Component buildBranchToolbar() {
        branchSearchField.setPlaceholder("Search by code, name, city, province...");
        branchSearchField.setClearButtonVisible(true);
        branchSearchField.setValueChangeMode(ValueChangeMode.EAGER);
        branchSearchField.addValueChangeListener(e -> refreshBranchGrid());

        addBranchButton.addClickListener(e -> openNewBranchDialog());

        HorizontalLayout toolbar = new HorizontalLayout(branchSearchField, addBranchButton);
        toolbar.setWidthFull();
        branchSearchField.setWidth("350px");
        toolbar.setVerticalComponentAlignment(FlexComponent.Alignment.END, addBranchButton);
        return toolbar;
    }

    private void configureBranchForm() {
        branchCompanyField.setItems(companyRepo.findAll());
        branchCompanyField.setItemLabelGenerator(HrCompany::getName);
        branchCompanyField.setRequired(true);

        branchCodeField.setRequired(true);
        branchCodeField.setMaxLength(30);

        branchNameField.setRequired(true);
        branchNameField.setMaxLength(150);

        branchAddressField.setMaxLength(2000);
        branchAddressField.setHeight("120px");

        branchCityField.setMaxLength(100);
        branchProvinceField.setMaxLength(100);

        branchTimezoneField.setItems(
                "Asia/Jakarta",
                "Asia/Makassar",
                "Asia/Jayapura"
        );
        branchTimezoneField.setHelperText("Gunakan format IANA timezone.");

        branchLatitudeField.setPlaceholder("-6.200000");
        branchLongitudeField.setPlaceholder("106.816666");

        branchForm.removeAll();
        branchForm.add(
                branchCompanyField,
                branchCodeField,
                branchNameField,
                branchAddressField,
                branchCityField,
                branchProvinceField,
                branchTimezoneField,
                branchLatitudeField,
                branchLongitudeField
        );

        branchForm.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
                new FormLayout.ResponsiveStep("600px", 2, FormLayout.ResponsiveStep.LabelsPosition.TOP)
        );

        branchBinder.forField(branchCompanyField)
                .asRequired("Perusahaan wajib diisi")
                .bind(HrCompanyBranch::getCompany, HrCompanyBranch::setCompany);

        branchBinder.forField(branchCodeField)
                .asRequired("Kode cabang wajib diisi")
                .bind(HrCompanyBranch::getBranchCode, HrCompanyBranch::setBranchCode);

        branchBinder.forField(branchNameField)
                .asRequired("Nama cabang wajib diisi")
                .bind(HrCompanyBranch::getBranchName, HrCompanyBranch::setBranchName);

        branchBinder.forField(branchAddressField)
                .bind(HrCompanyBranch::getBranchAddress, HrCompanyBranch::setBranchAddress);

        branchBinder.forField(branchCityField)
                .bind(HrCompanyBranch::getBranchAddressCity, HrCompanyBranch::setBranchAddressCity);

        branchBinder.forField(branchProvinceField)
                .bind(HrCompanyBranch::getBranchAddressProvince, HrCompanyBranch::setBranchAddressProvince);

        branchBinder.forField(branchTimezoneField)
                .bind(HrCompanyBranch::getBranchTimezone, HrCompanyBranch::setBranchTimezone);

        // latitude/longitude parse manual saat save

        branchDialog.removeAll();
        branchDialog.add(new H3("Branch Details"), branchForm);

        Button saveButton = new Button("Save", e -> saveBranch());
        Button cancelButton = new Button("Cancel", e -> branchDialog.close());
        branchDialog.getFooter().removeAll();
        branchDialog.getFooter().add(cancelButton, saveButton);
    }

    private void refreshBranchGrid() {
        String term = branchSearchField.getValue();
        List<HrCompanyBranch> all = branchService.findAll();

        if (term == null || term.isBlank()) {
            branchGrid.setItems(all);
        } else {
            String lower = term.toLowerCase();
            branchGrid.setItems(
                    all.stream()
                            .filter(b ->
                                    (b.getBranchCode() != null && b.getBranchCode().toLowerCase().contains(lower)) ||
                                            (b.getBranchName() != null && b.getBranchName().toLowerCase().contains(lower)) ||
                                            (b.getBranchAddressCity() != null && b.getBranchAddressCity().toLowerCase().contains(lower)) ||
                                            (b.getBranchAddressProvince() != null && b.getBranchAddressProvince().toLowerCase().contains(lower))
                            )
                            .collect(Collectors.toList())
            );
        }
    }

    private void openNewBranchDialog() {
        currentBranch = new HrCompanyBranch();

        branchCompanyField.setItems(companyRepo.findAll());
        branchTimezoneField.setValue("Asia/Jakarta");

        branchLatitudeField.clear();
        branchLongitudeField.clear();

        branchBinder.readBean(currentBranch);
        branchDialog.open();
    }

    private void openEditBranchDialog(HrCompanyBranch branch) {
        currentBranch = branch;

        branchCompanyField.setItems(companyRepo.findAll());

        branchLatitudeField.setValue(branch.getBranchLatitude() != null ? branch.getBranchLatitude().toPlainString() : "");
        branchLongitudeField.setValue(branch.getBranchLongitude() != null ? branch.getBranchLongitude().toPlainString() : "");

        branchBinder.readBean(branch);
        branchDialog.open();
    }

    private void saveBranch() {
        try {
            branchBinder.writeBean(currentBranch);

            // Parse lat/long
            currentBranch.setBranchLatitude(parseDecimalOrNull(branchLatitudeField.getValue()));
            currentBranch.setBranchLongitude(parseDecimalOrNull(branchLongitudeField.getValue()));

            // Validasi ringan range lat/long
            if (currentBranch.getBranchLatitude() != null) {
                double lat = currentBranch.getBranchLatitude().doubleValue();
                if (lat < -90 || lat > 90) {
                    Notification.show("Latitude harus di antara -90 sampai 90.",
                            4000, Notification.Position.MIDDLE);
                    return;
                }
            }
            if (currentBranch.getBranchLongitude() != null) {
                double lon = currentBranch.getBranchLongitude().doubleValue();
                if (lon < -180 || lon > 180) {
                    Notification.show("Longitude harus di antara -180 sampai 180.",
                            4000, Notification.Position.MIDDLE);
                    return;
                }
            }

            // Unique code per company
            if (currentBranch.getCompany() != null && currentBranch.getBranchCode() != null) {
                boolean duplicate = branchService.isDuplicateCode(currentBranch.getCompany(), currentBranch);
                if (duplicate) {
                    Notification.show("Kode cabang sudah dipakai di perusahaan yang sama.",
                            4000, Notification.Position.MIDDLE);
                    return;
                }
            }

            branchService.save(currentBranch);
            branchDialog.close();
            refreshBranchGrid();
            Notification.show("Branch saved successfully!");
        } catch (Exception e) {
            Notification.show("Error saving branch: " + e.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void deleteBranch(HrCompanyBranch branch) {
        String header = "Delete Branch " + branch.getBranchName() + "?";
        String message = "Are you sure you want to permanently delete this branch?";

        ConfirmationDialogUtil.showConfirmation(
                header,
                message,
                "Delete",
                event -> {
                    try {
                        branchService.delete(branch);
                        refreshBranchGrid();
                        Notification.show("Deleted successfully!");
                    } catch (Exception ex) {
                        Notification.show("Deletion failed: " + ex.getMessage(),
                                5000, Notification.Position.MIDDLE);
                        ex.printStackTrace();
                    }
                }
        );
    }

    private BigDecimal parseDecimalOrNull(String s) {
        if (s == null) return null;
        String v = s.trim();
        if (v.isEmpty()) return null;
        try {
            return new BigDecimal(v);
        } catch (Exception e) {
            return null;
        }
    }
}
