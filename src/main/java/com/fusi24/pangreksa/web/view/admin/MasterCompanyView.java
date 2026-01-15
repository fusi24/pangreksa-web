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
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.Autocapitalize;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.server.VaadinSession;
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
@PageTitle("Master perusahaan")
@Menu(order = 4, icon = "vaadin:building", title = "Master perusahaan")
@RolesAllowed("USERS_MGT")
public class MasterCompanyView extends Main {

    public static final String VIEW_NAME = "Master perusahaan";

    private final HrCompanyRepository companyRepo;
    private final CompanyBranchService branchService;

    // ===== TAB 1: COMPANY =====
    private final Grid<HrCompany> companyGrid = new Grid<>(HrCompany.class);
    private final TextField companySearchField = new TextField("Cari");
    private final Button addCompanyButton = new Button("Tambah perusahaan");

    private final Dialog companyDialog = new Dialog();
    private final FormLayout companyForm = new FormLayout();
    private final Binder<HrCompany> companyBinder = new Binder<>(HrCompany.class);

    private final ComboBox<HrCompany> parentField = new ComboBox<>("Induk perusahaan");
    private final TextField nameField = new TextField("Nama");
    private final TextField shortNameField = new TextField("Singkatan");
    private final TextField registrationNumberField = new TextField("Nomor Registrasi");
    private final com.vaadin.flow.component.datepicker.DatePicker establishmentDateField =
            new com.vaadin.flow.component.datepicker.DatePicker("Tanggal Berdiri");
    private final TextField phoneField = new TextField("Phone");
    private final TextField emailField = new TextField("Email");
    private final TextField websiteField = new TextField("Website");
    private final Checkbox isActiveField = new Checkbox("Aktif");
    private final Checkbox isHrManagedField = new Checkbox("Dikelola HR");
    private final TextArea notesField = new TextArea("Catatan");

    private HrCompany currentCompany;

    // ===== TAB 2: BRANCH =====
    private final Grid<HrCompanyBranch> branchGrid = new Grid<>(HrCompanyBranch.class, false);
    private final TextField branchSearchField = new TextField("Cari Cabang");
    private final Button addBranchButton = new Button("Tambah Branch");

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

    // ===== TAB 3: PINDAH CABANG =====
    private final ComboBox<HrCompany> fromCompanyField = new ComboBox<>("Dari Perusahaan");
    private final ComboBox<HrCompanyBranch> fromBranchField = new ComboBox<>("Dari Cabang");
    private final ComboBox<HrCompany> toCompanyField = new ComboBox<>("Ke Perusahaan");
    private final ComboBox<HrCompanyBranch> toBranchField = new ComboBox<>("Ke Cabang");
    private final Button switchBranchButton = new Button("Simpan Cabang Aktif");

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
        setSizeFull();
        add(new ViewToolbar(VIEW_NAME));

        TabSheet tabs = new TabSheet();
        tabs.setWidthFull();
        tabs.setHeightFull();

        tabs.add("Perusahaan", createCompanyTab());
        tabs.add("Cabang Perusahaan", createBranchTab());
        tabs.add("Pindah Cabang", createSwitchBranchTab());

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

        companyGrid.setSizeFull();

        VerticalLayout layout = new VerticalLayout(toolbar, companyGrid);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setSizeFull();
        layout.setFlexGrow(1, companyGrid);

        return layout;
    }


    private void configureCompanyGrid() {
        companyGrid.removeAllColumns();

        companyGrid.addColumn(HrCompany::getName)
                .setHeader("Nama")
                .setAutoWidth(true);

        companyGrid.addColumn(HrCompany::getShortName)
                .setHeader("Singkatan");

        companyGrid.addColumn(c -> c.getParent() != null ? c.getParent().getName() : "-")
                .setHeader("Induk");

        companyGrid.addColumn(c -> BooleanUtils.toString(c.getIsActive(), "Active", "Inactive"))
                .setHeader("Status");

        companyGrid.addComponentColumn(this::createCompanyActions)
                .setHeader("Aksi")
                .setAutoWidth(true);
    }

    private HorizontalLayout createCompanyActions(HrCompany company) {
        Button detail = new Button("Detail");
        detail.addClickListener(e -> openCompanyDetailDialog(company));

        Button edit = new Button("Ubah");
        edit.addClickListener(e -> openEditCompanyDialog(company));

        Button delete = new Button("Hapus");
        delete.addClickListener(e -> deleteCompany(company));

        return new HorizontalLayout(detail, edit, delete);
    }

    private HorizontalLayout createBranchActions(HrCompanyBranch branch) {
        Button detail = new Button("Detail");
        detail.addClickListener(e -> openBranchDetailDialog(branch));

        Button edit = new Button("Ubah");
        edit.addClickListener(e -> openEditBranchDialog(branch));

        Button delete = new Button("Hapus");
        delete.addClickListener(e -> deleteBranch(branch));

        return new HorizontalLayout(detail, edit, delete);
    }

    private void openBranchDetailDialog(HrCompanyBranch b) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Branch Detail");
        dialog.setWidth("500px");

        FormLayout layout = new FormLayout();
        layout.add(
                ro("Perusahaan", b.getCompany() != null ? b.getCompany().getName() : "-"),
                ro("Branch Code", b.getBranchCode()),
                ro("Branch Name", b.getBranchName()),
                ro("Alamat", b.getBranchAddress()),
                ro("Kota", b.getBranchAddressCity()),
                ro("Provinsi", b.getBranchAddressProvince()),
                ro("Zona Waktu", b.getBranchTimezone()),
                ro("Latitude", b.getBranchLatitude() != null ? b.getBranchLatitude().toPlainString() : "-"),
                ro("Longitude", b.getBranchLongitude() != null ? b.getBranchLongitude().toPlainString() : "-")
        );

        Button close = new Button("Close", e -> dialog.close());
        dialog.getFooter().add(close);

        dialog.add(layout);
        dialog.open();
    }


    private void openCompanyDetailDialog(HrCompany c) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Perusahaan Detail");
        dialog.setWidth("500px");

        FormLayout layout = new FormLayout();
        layout.add(
                ro("Nama", c.getName()),
                ro("Singkatan", c.getShortName()),
                ro("Induk perusahaan", c.getParent() != null ? c.getParent().getName() : "-"),
                ro("Nomor Registrasi", c.getRegistrationNumber()),
                ro("Tanggal Berdiri",
                        c.getEstablishmentDate() != null ? c.getEstablishmentDate().toString() : "-"),
                ro("Phone", c.getPhone()),
                ro("Email", c.getEmail()),
                ro("Website", c.getWebsite()),
                ro("Dikelola HR", BooleanUtils.toString(c.getIsHrManaged(), "Yes", "No")),
                ro("Status", BooleanUtils.toString(c.getIsActive(), "Active", "Inactive")),
                ro("Catatan", c.getNotes())
        );

        Button close = new Button("Close", e -> dialog.close());
        dialog.getFooter().add(close);

        dialog.add(layout);
        dialog.open();
    }

    private void configureBranchGrid() {
        branchGrid.removeAllColumns();

        branchGrid.addColumn(b -> b.getCompany() != null ? b.getCompany().getName() : "-")
                .setHeader("Perusahaan");

        branchGrid.addColumn(HrCompanyBranch::getBranchCode)
                .setHeader("Kode");

        branchGrid.addColumn(HrCompanyBranch::getBranchName)
                .setHeader("Branch Name");

        branchGrid.addColumn(HrCompanyBranch::getBranchAddressCity)
                .setHeader("Kota");

        branchGrid.addColumn(HrCompanyBranch::getBranchTimezone)
                .setHeader("Zona Waktu");

        branchGrid.addComponentColumn(this::createBranchActions)
                .setHeader("Aksi");
    }

    private TextField ro(String label, String value) {
        TextField f = new TextField(label);
        f.setValue(value != null ? value : "-");
        f.setReadOnly(true);
        f.setWidthFull();
        return f;
    }


    private void configureCompanyForm() {
        parentField.setItemLabelGenerator(c -> c != null ? c.getName() : "");
        parentField.setPlaceholder("Pilih Induk perusahaan (Optional)");

        nameField.setRequired(true);
        nameField.setMaxLength(100);

        companyBinder.forField(registrationNumberField)
                .withValidator(value -> value == null || value.matches("\\d+"),
                        "Registration number wajib angka")
                .bind(HrCompany::getRegistrationNumber, HrCompany::setRegistrationNumber);

        companyBinder.forField(phoneField)
                .withValidator(value -> value == null || value.matches("\\d+"),
                        "Nomor telepon wajib angka")
                .bind(HrCompany::getPhone, HrCompany::setPhone);

        companyBinder.forField(emailField)
                .withValidator(value -> value == null || value.contains("@"),
                        "Email harus mengandung '@'")
                .bind(HrCompany::getEmail, HrCompany::setEmail);



        shortNameField.setMaxLength(20);
        shortNameField.setAutocapitalize(Autocapitalize.CHARACTERS);

        registrationNumberField.setMaxLength(50);
        phoneField.setMaxLength(30);
        emailField.setMaxLength(50);
        websiteField.setMaxLength(100);

        notesField.setPlaceholder("Tambahitional notes...");
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
                .asRequired("Name wajib diisi")
                .bind(HrCompany::getName, HrCompany::setName);

        companyBinder.forField(shortNameField)
                .bind(HrCompany::getShortName, HrCompany::setShortName);

        companyBinder.forField(establishmentDateField)
                .bind(HrCompany::getEstablishmentDate, HrCompany::setEstablishmentDate);

        companyBinder.forField(websiteField)
                .bind(HrCompany::getWebsite, HrCompany::setWebsite);

        companyBinder.forField(isActiveField)
                .bind(HrCompany::getIsActive, HrCompany::setIsActive);

        companyBinder.forField(isHrManagedField)
                .bind(HrCompany::getIsHrManaged, HrCompany::setIsHrManaged);

        companyBinder.forField(notesField)
                .bind(HrCompany::getNotes, HrCompany::setNotes);

        companyDialog.removeAll();
        companyDialog.add(new H3("Perusahaan Details"), companyForm);

        Button saveButton = new Button("Simpan", e -> saveCompany());
        Button cancelButton = new Button("Batal", e -> companyDialog.close());
        companyDialog.getFooter().removeAll();
        companyDialog.getFooter().add(cancelButton, saveButton);
    }

    private Component buildCompanyToolbar() {
        companySearchField.setPlaceholder("Cari berdasarkan name, Singkatan, reg. number, phone, email, or website...");
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
                        "Perusahaan name must be unique. Another company already uses this name.",
                        5000,
                        Notification.Position.MIDDLE
                );
                return;
            }

            companyRepo.save(currentCompany);
            companyDialog.close();
            refreshCompanyGrid();
            refreshBranchGrid();
            Notification.show("Data berhasil disimpan.");
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
                "Hapus",
                event -> {
                    try {
                        companyRepo.delete(company);
                        refreshCompanyGrid();
                        refreshBranchGrid();
                        Notification.show("Data berhasi dihapus.");
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

        branchGrid.setSizeFull();

        VerticalLayout layout = new VerticalLayout(toolbar, branchGrid);
        layout.setSizeFull();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setFlexGrow(1, branchGrid);

        return layout;
    }



    private Component buildBranchToolbar() {
        branchSearchField.setPlaceholder("Cari berdasarkan code, name, city, province...");
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

        branchDialog.removeAll();
        branchDialog.add(new H3("Branch Details"), branchForm);

        Button saveButton = new Button("Simpan", e -> saveBranch());
        Button cancelButton = new Button("Batal", e -> branchDialog.close());
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
            Notification.show("Branch berhasil tersimpan!");
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
                "Hapus",
                event -> {
                    try {
                        branchService.delete(branch);
                        refreshBranchGrid();
                        Notification.show("Data berhasi dihapus.");
                    } catch (Exception ex) {
                        Notification.show("Deletion failed: " + ex.getMessage(),
                                5000, Notification.Position.MIDDLE);
                        ex.printStackTrace();
                    }
                }
        );
    }

    // =========================
    // TAB PINDAH CABANG UI
    // =========================
    private Component createSwitchBranchTab() {
        // Isi data perusahaan
        List<HrCompany> companies = companyRepo.findAll();
        fromCompanyField.setItems(companies);
        fromCompanyField.setItemLabelGenerator(c -> c != null ? c.getName() : "");

        toCompanyField.setItems(companies);
        toCompanyField.setItemLabelGenerator(c -> c != null ? c.getName() : "");

        // Konfigurasi combo cabang
        fromBranchField.setItemLabelGenerator(b -> {
            if (b == null) return "";
            String code = b.getBranchCode() != null ? b.getBranchCode() : "";
            String name = b.getBranchName() != null ? b.getBranchName() : "";
            return code.isBlank() ? name : code + " - " + name;
        });
        toBranchField.setItemLabelGenerator(b -> {
            if (b == null) return "";
            String code = b.getBranchCode() != null ? b.getBranchCode() : "";
            String name = b.getBranchName() != null ? b.getBranchName() : "";
            return code.isBlank() ? name : code + " - " + name;
        });

        fromBranchField.setEnabled(false);
        toBranchField.setEnabled(false);

        fromCompanyField.addValueChangeListener(e -> {
            HrCompany company = e.getValue();
            if (company == null) {
                fromBranchField.clear();
                fromBranchField.setItems();
                fromBranchField.setEnabled(false);
            } else {
                List<HrCompanyBranch> branches = branchService.findByCompany(company);
                fromBranchField.setItems(branches);
                fromBranchField.setEnabled(true);
            }
        });

        toCompanyField.addValueChangeListener(e -> {
            HrCompany company = e.getValue();
            if (company == null) {
                toBranchField.clear();
                toBranchField.setItems();
                toBranchField.setEnabled(false);
            } else {
                List<HrCompanyBranch> branches = branchService.findByCompany(company);
                toBranchField.setItems(branches);
                toBranchField.setEnabled(true);
            }
        });

        // Pre-select "from" berdasarkan cabang aktif di session (jika ada)
        // Pre-select "from" berdasarkan cabang aktif di session (jika ada)
        Object activeBranchIdObj = VaadinSession.getCurrent().getAttribute("ACTIVE_BRANCH_ID");
        Long activeBranchId = null;
        if (activeBranchIdObj instanceof Number) {
            activeBranchId = ((Number) activeBranchIdObj).longValue();
        } else if (activeBranchIdObj instanceof String) {
            try {
                activeBranchId = Long.parseLong((String) activeBranchIdObj);
            } catch (NumberFormatException ignore) {
                // abaikan
            }
        }

        if (activeBranchId != null) {
            final Long branchId = activeBranchId; // ⬅️ ini yang dipakai di lambda

            List<HrCompanyBranch> allBranches = branchService.findAll();
            HrCompanyBranch activeBranch = allBranches.stream()
                    .filter(b -> b.getId() != null && b.getId().longValue() == branchId)
                    .findFirst()
                    .orElse(null);

            if (activeBranch != null) {
                HrCompany company = activeBranch.getCompany();
                if (company != null) {
                    fromCompanyField.setValue(company);
                    List<HrCompanyBranch> branches = branchService.findByCompany(company);
                    fromBranchField.setItems(branches);
                    fromBranchField.setEnabled(true);
                    fromBranchField.setValue(activeBranch);
                }
            }
        }


        switchBranchButton.addClickListener(e -> {
            if (toCompanyField.isEmpty() || toBranchField.isEmpty()) {
                Notification.show("Perusahaan dan cabang tujuan wajib dipilih.",
                        4000, Notification.Position.MIDDLE);
                return;
            }

            HrCompanyBranch targetBranch = toBranchField.getValue();
            if (targetBranch == null || targetBranch.getId() == null) {
                Notification.show("Cabang tujuan tidak valid.",
                        4000, Notification.Position.MIDDLE);
                return;
            }

            // Simpan cabang aktif di session.
            VaadinSession.getCurrent().setAttribute("ACTIVE_BRANCH_ID", targetBranch.getId());

            // TODO: di sini nanti bisa panggil service untuk sync ke hr_person_position.branch_id
            // misalnya: userBranchService.setActiveBranchForCurrentUser(targetBranch);

            Notification.show(
                    "Cabang aktif dipindah ke: " + targetBranch.getBranchName(),
                    4000,
                    Notification.Position.MIDDLE
            );
        });

        FormLayout form = new FormLayout();
        form.setWidth("500px");
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
                new FormLayout.ResponsiveStep("600px", 2, FormLayout.ResponsiveStep.LabelsPosition.TOP)
        );

        form.add(
                fromCompanyField,
                fromBranchField,
                toCompanyField,
                toBranchField
        );

        HorizontalLayout buttons = new HorizontalLayout(switchBranchButton);
        buttons.setWidthFull();
        buttons.setJustifyContentMode(FlexComponent.JustifyContentMode.START);

        VerticalLayout layout = new VerticalLayout(form, buttons);
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.setWidthFull();

        return layout;
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
