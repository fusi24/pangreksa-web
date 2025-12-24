package com.fusi24.pangreksa.web.view.employee;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.base.util.DatePickerUtil;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.*;
import com.fusi24.pangreksa.web.model.enumerate.*;
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.PersonService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import org.springframework.beans.factory.annotation.Autowired;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.streams.InMemoryUploadHandler;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.fusi24.pangreksa.web.service.PersonPtkpService;

import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import com.fusi24.pangreksa.web.service.PersonTanggunganService;

@Route("karyawan-baru-form-page-access")
@PageTitle("Karyawan Baru Form")
@Menu(order = 15, icon = "vaadin:user-card", title = "Karyawan Baru Form")
@RolesAllowed("KAR_BARU")
//@PermitAll // When security is enabled, allow all authenticated users
public class KaryawanBaruFormView extends Main implements HasUrlParameter<Long> {
    private static final long serialVersionUID = 15L;
    private static final Logger log = LoggerFactory.getLogger(KaryawanBaruFormView.class);
    private final CurrentUser currentUser;
    private final CommonService commonService;
    private final PersonService personService;
    private Authorization auth;

    private PersonPtkpService personPtkpService;
    private PersonTanggunganService personTanggunganService;


    public static final String VIEW_NAME = "Karyawan Baru Form";

    private VerticalLayout body;
    private TabSheet tabSheet;
    private HorizontalLayout toolbarLayoutMaster;
    private HorizontalLayout toolbarLayoutDetail;
    private FormLayout personFormLayout;
    private FormLayout contactFormLayout;
    private FormLayout educationFormLayout;
    private FormLayout documentFormLayout;

    HorizontalLayout addressesLayout = new HorizontalLayout();
    HorizontalLayout contactsLayout = new HorizontalLayout();
    HorizontalLayout educationLayout = new HorizontalLayout();
    HorizontalLayout documentsLayout = new HorizontalLayout();

    Button demoButton;
    Button saveButton;
    Button clearButton;

    Button clearButtonOnTab;
    Button saveButtonOnTab;

    private Image photoPreview;
    private VerticalLayout avatarLayout;
    // To store byte[] of uploaded image
    AtomicReference<byte[]> uploadedImageBytes = new AtomicReference<>();


    // Person Fields
    private TextField firstName = new TextField("First Name");
    private TextField middleName = new TextField("Middle Name");
    private TextField lastName = new TextField("Last Name");
    private TextField pob = new TextField("Place of Birth");
    private DatePicker dob = new DatePicker("Date of Birth");
    private ComboBox<GenderEnum> gender = new ComboBox<>("Gender");
    private ComboBox<NationalityEnum> nationality = new ComboBox<>("Nationality");
    private ComboBox<ReligionEnum> religion = new ComboBox<>("Religion");
    private ComboBox<MarriageEnum> marriage = new ComboBox<>("Marriage Status");
    private TextField ktpNumber = new TextField("KTP Number");

    // Address Fields
    private TextArea fullAddress = new TextArea("Full Address");
    private Checkbox isDefaultAddress = new Checkbox("Default Address");

    // Contacts Fields
    private TextField designation = new TextField("Designation");
    private TextField relationship = new TextField("Relationship");
    private TextField stringValue = new TextField("Value");
    private ComboBox<ContactTypeEnum> typeContact = new ComboBox<>("Contact Type");
    private TextField description = new TextField("Description");
    private Checkbox isDefaultContact = new Checkbox("Default Contact");

    // Education Fields
    private TextField institution = new TextField("Institution");
    private TextField program = new TextField("Program");
    private NumberField score = new NumberField("Score");
    private DatePicker startDate = new DatePicker("Start Date");
    private DatePicker finishDate = new DatePicker("Finish Date");
    private TextField certificateTitle = new TextField("Certificate Title");
    private DatePicker certificateExpiration = new DatePicker("Certificate Expiration");
    private ComboBox<EducationTypeEnum> typeEducation = new ComboBox<>("Education Type");

    // Document Fields
    private TextField nameDocoument = new TextField("Document Name");
    private TextField descDocument = new TextField("Description");
    private TextField notes = new TextField("Notes");
    private NumberField year = new NumberField("Year");
    private ComboBox<DocumentTypeEnum> typeDocument = new ComboBox<>("Document Type");
    private ComboBox<ContentTypeEnum> contentType = new ComboBox<>("Content Type");
    private NumberField size = new NumberField("Size (bytes)");
    private TextField filename = new TextField("Filename");
    private TextField path = new TextField("Path");

    // ================= TANGGUNGAN =================
    HorizontalLayout tanggunganLayout = new HorizontalLayout();

    private TextField tgName = new TextField("Nama");
    private ComboBox<String> tgRelation = new ComboBox<>("Hubungan");
    private DatePicker tgDob = new DatePicker("Tanggal Lahir");
    private ComboBox<GenderEnum> tgGender = new ComboBox<>("Jenis Kelamin");
    private Checkbox tgStillDependent = new Checkbox("Masih Tanggungan");

    private Grid<HrPersonTanggungan> gridTanggungan;
    private List<HrPersonTanggungan> tanggunganList = new ArrayList<>();
    private HrPersonTanggungan tanggunganData;
    private Checkbox jointIncomeField = new Checkbox("Penghasilan Suami & Istri Digabung (K/I)");



    // Grids
    private Grid<HrPersonAddress> gridAddress;
    private Grid<HrPersonContact> gridContacts;
    private Grid<HrPersonDocument> gridDocument;
    private Grid<HrPersonEducation> gridEducation;

    private List<HrPersonAddress> addressList = new ArrayList<>();
    private List<HrPersonContact> contactList = new ArrayList<>();
    private List<HrPersonEducation> educationList = new ArrayList<>();
    private List<HrPersonDocument> documentList = new ArrayList<>();

    private HrPerson personData;
    private HrPersonAddress addressData;
    private HrPersonContact contactData;
    private HrPersonEducation educationData;
    private HrPersonDocument documentData;

    public KaryawanBaruFormView(CurrentUser currentUser,
                                CommonService commonService,
                                PersonService personService,
                                PersonTanggunganService personTanggunganService,
                                PersonPtkpService personPtkpService) {

        this.currentUser = currentUser;
        this.commonService = commonService;
        this.personService = personService;
        this.personTanggunganService = personTanggunganService;
        this.personPtkpService = personPtkpService;

        this.auth = commonService.getAuthorization(
                currentUser.require(),
                (String) UI.getCurrent().getSession().getAttribute("responsibility"),
                this.serialVersionUID);

        log.debug("Page {}, Authorization: {} {} {} {}", VIEW_NAME, auth.canView, auth.canCreate, auth.canEdit, auth.canDelete);

        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar(VIEW_NAME));
        createBody();

        saveButton.addClickListener(e -> {
            if (this.auth != null && !this.auth.canCreate) {
                Notification.show(
                        "Anda tidak memiliki izin menyimpan data",
                        3000,
                        Notification.Position.MIDDLE
                );
                return;
            }
            save();
        });
        // ================= LISTENERS =================

// CLEAR GLOBAL
        clearButton.addClickListener(e -> {
            clearForm(true, true, true, true, true);
            clearGrid(true, true, true, true);
        });

// CLEAR PER TAB
        clearButtonOnTab.addClickListener(e -> {
            int tabNo = tabSheet.getSelectedIndex();
            switch (tabNo) {
                case 0 -> clearForm(false, true, false, false, false); // Address
                case 1 -> clearForm(false, false, true, false, false); // Contact
                case 2 -> clearForm(false, false, false, true, false); // Education
                case 3 -> clearForm(false, false, false, false, true); // Document
                case 4 -> clearTanggunganForm();                       // Tanggungan
            }
        });

// ADD PER TAB
        saveButtonOnTab.addClickListener(e -> {

            if (this.auth != null && !this.auth.canCreate) {
                Notification.show(
                        "Anda tidak memiliki izin untuk menambah data.",
                        3000,
                        Notification.Position.MIDDLE
                );
                return;
            }

            int tabNo = tabSheet.getSelectedIndex();

            switch (tabNo) {
                case 0 -> { /* ADDRESS (sudah benar, biarkan) */ }
                case 1 -> {
                    boolean valid = true;

                    if (typeContact.getValue() == null) valid = false;
                    if (stringValue.isEmpty()) valid = false;
                    if (designation.isEmpty()) valid = false;

                    if (typeContact.getValue() == ContactTypeEnum.EMERGENCY) {
                        if (relationship.isEmpty()) valid = false;
                        if (description.isEmpty()) valid = false;
                    }

                    if (!valid) {
                        Notification.show(
                                "Field wajib belum lengkap (Relationship & Description wajib untuk Emergency Contact)",
                                3000,
                                Notification.Position.MIDDLE
                        );
                        return;
                    }

                }
                case 2 -> { /* EDUCATION (sudah benar, biarkan) */ }
                case 3 -> { /* DOCUMENT (sudah benar, biarkan) */ }

                case 4 -> { // === TANGGUNGAN (IN-MEMORY) ===

                    if (tgName.isEmpty()
                            || tgRelation.isEmpty()
                            || tgDob.isEmpty()
                            || tgGender.isEmpty()) {
                        Notification.show("Semua field tanggungan wajib diisi");
                        return;
                    }

                    HrPersonTanggungan t =
                            tanggunganData != null ? tanggunganData : new HrPersonTanggungan();

                    t.setName(tgName.getValue());
                    t.setRelation(tgRelation.getValue());
                    t.setDob(tgDob.getValue());
                    t.setGender(tgGender.getValue());
                    t.setStillDependent(tgStillDependent.getValue());

                    // ❌ JANGAN save DB
                    // ❌ JANGAN setPerson

                    tanggunganList.add(t);
                    gridTanggungan.setItems(tanggunganList);

                    clearTanggunganForm();
                    tanggunganData = null;
                }
            }

        });



        setAuthorization();
    }

    private void setAuthorization(){
        if(!this.auth.canView){
            // User does not have permission to view this page
        }

        if(!this.auth.canCreate){
            demoButton.setEnabled(false);
            /* saveButton.setEnabled(false); // kept enabled per request *///
            /* saveButtonOnTab.setEnabled(false); // kept enabled per request */ // disabled by permission previously; kept enabled per request
        }
    }

    private void createBody() {
        this.body = new VerticalLayout();
        body.setPadding(false);

        // Inisiasi toolbar Master
        toolbarLayoutMaster = new HorizontalLayout();
        toolbarLayoutMaster.setAlignItems(FlexComponent.Alignment.END);

        demoButton = new Button("Add Dummy");
        saveButton = new Button("Save");
        clearButton = new Button("Reset");

        toolbarLayoutMaster.add(demoButton, clearButton, saveButton);
        toolbarLayoutMaster.setJustifyContentMode(JustifyContentMode.END);

        // Inisiasi toolbar Detail
        toolbarLayoutDetail = new HorizontalLayout();
        toolbarLayoutDetail.setAlignItems(FlexComponent.Alignment.END);

        clearButtonOnTab = new Button("Clear");
        saveButtonOnTab = new Button("Add");

        toolbarLayoutDetail.add(clearButtonOnTab, saveButtonOnTab);
        toolbarLayoutDetail.setJustifyContentMode(JustifyContentMode.END);
        toolbarLayoutDetail.setMaxWidth("200px");

        // Photo
        avatarLayout = new VerticalLayout();
        avatarLayout.setWidth(CIRCLE_PX);
        avatarLayout.setHeight(CIRCLE_PX);
        avatarLayout.setMargin(true);
        avatarLayout.setWidth("180px");
        //aligment center
        avatarLayout.setJustifyContentMode(JustifyContentMode.CENTER);
        avatarLayout.getStyle().set("background-color", "#f0f0f0");
        avatarLayout.getStyle().set("border-radius", "10%"); // Makes it perfectly round

        createPersonForm();

        this.tabSheet = new TabSheet();

        tabSheet.add("Addresses", addressesLayout);
        tabSheet.add("Contacts", contactsLayout);
        tabSheet.add("Educations", educationLayout);
        tabSheet.add("Documents", documentsLayout);
        tabSheet.add("Tanggungan", tanggunganLayout);


        tabSheet.getStyle().setWidth("100%");

        createAddressForm();
        createContactsForm();
        createEducationForm();
        createDocumentForm();
        createTanggunganForm();

        HorizontalLayout masterLayout = new HorizontalLayout(createAvatarUploadLayout(), personFormLayout, toolbarLayoutMaster);
        masterLayout.setWidthFull();
        masterLayout.setHeight("350px");
        masterLayout.setAlignItems(FlexComponent.Alignment.START);

        HorizontalLayout detailLayout =  new HorizontalLayout(tabSheet, toolbarLayoutDetail);
        detailLayout.setWidthFull();
        detailLayout.setAlignItems(FlexComponent.Alignment.START);

        SplitLayout splitLayout = new SplitLayout(
                masterLayout,
                detailLayout
        );

        splitLayout.setOrientation(SplitLayout.Orientation.VERTICAL);
        splitLayout.setSizeFull();

        body.add(splitLayout);
        body.addClassNames(LumoUtility.Gap.MEDIUM);
        body.setHeightFull();
        body.getStyle().setFlexGrow("1");

        this.setHeightFull();
        add(body);
    }

    private void createPersonForm() {
        // Setup enum combo boxes
        gender.setItems(GenderEnum.values());
        nationality.setItems(NationalityEnum.values());
        religion.setItems(ReligionEnum.values());
        marriage.setItems(MarriageEnum.values());
        dob.setI18n(DatePickerUtil.getIndonesianI18n());

        // Create the FormLayout
        personFormLayout = new FormLayout();
        personFormLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 3),
                new FormLayout.ResponsiveStep("700px", 4)
        );

        // Add all fields to the form layout

        // ===== UI Validations =====
        // Required indicators
        firstName.setRequiredIndicatorVisible(true);
        lastName.setRequiredIndicatorVisible(true);
        ktpNumber.setRequiredIndicatorVisible(true);
        dob.setRequiredIndicatorVisible(true);
        gender.setRequiredIndicatorVisible(true);
        nationality.setRequiredIndicatorVisible(true);
        religion.setRequiredIndicatorVisible(true);
        marriage.setRequiredIndicatorVisible(true);
        pob.setRequiredIndicatorVisible(true);

        // KTP/NIK: only numbers, exactly 16 digits (no spaces)
        ktpNumber.setClearButtonVisible(true);
        ktpNumber.setValueChangeMode(ValueChangeMode.EAGER);
        ktpNumber.setAllowedCharPattern("\\d");
        ktpNumber.setPattern("\\d*");
        ktpNumber.setMaxLength(16);
        ktpNumber.setMinLength(16);
        ktpNumber.setHelperText("Masukkan 16 digit angka tanpa spasi");
        ktpNumber.setErrorMessage("NIK harus 16 digit angka tanpa spasi");
        ktpNumber.addValueChangeListener(e -> {
            String v = e.getValue();
            if (v != null) {
                String cleaned = v.replaceAll("[^\\d]", "");
                if (!cleaned.equals(v)) {
                    ktpNumber.setValue(cleaned);
                }
                ktpNumber.setInvalid(cleaned.length() == 0 ? false : cleaned.length() != 16);
            }
        });

        // Names: only letters & spaces
        firstName.setAllowedCharPattern("[\\p{L}\\s]");
        firstName.setMaxLength(50);
        firstName.setHelperText("Hanya huruf & spasi, wajib diisi");
        firstName.addValueChangeListener(e -> {
            String v = e.getValue() != null ? e.getValue().trim() : "";
            boolean ok = v.matches("^[\\p{L} ]+$");
            firstName.setInvalid(v.isEmpty() ? false : !ok);
        });

        middleName.setAllowedCharPattern("[\\p{L}\\s]");
        middleName.setMaxLength(50);
        middleName.setHelperText("Hanya huruf & spasi (opsional)");
        middleName.addValueChangeListener(e -> {
            String v = e.getValue() != null ? e.getValue().trim() : "";
            boolean ok = v.isEmpty() || v.matches("^[\\p{L} ]+$");
            middleName.setInvalid(!ok);
        });

        lastName.setAllowedCharPattern("[\\p{L}\\s]");
        lastName.setMaxLength(50);
        lastName.setHelperText("Hanya huruf & spasi, wajib diisi");
        lastName.addValueChangeListener(e -> {
            String v = e.getValue() != null ? e.getValue().trim() : "";
            boolean ok = v.matches("^[\\p{L} ]+$");
            lastName.setInvalid(v.isEmpty() ? false : !ok);
        });

        // Place of Birth: letters & spaces, min 2 chars
        pob.setAllowedCharPattern("[\\p{L}\\s]");
        pob.setMaxLength(100);
        pob.setHelperText("Hanya huruf & spasi, wajib diisi"); // --- UPDATE HELPER TEXT ---
        pob.addValueChangeListener(e -> {
            String v = e.getValue() != null ? e.getValue().trim() : "";
            // --- UPDATE VALIDASI (TIDAK BOLEH KOSONG) ---
            boolean ok = v.matches("^[\\p{L} ]{2,}$");
            pob.setInvalid(v.isEmpty() ? false : !ok); // Biarkan validasi save() menangani jika kosong
        });

        // Date of Birth: set max to today
        dob.setMax(LocalDate.now());
        personFormLayout.add(
                ktpNumber,
                firstName,
                middleName,
                lastName,
                gender,
                pob,
                dob,
                nationality,
                religion,
                marriage,
                jointIncomeField   // ⬅️ TAMBAHKAN DI SINI
        );

        // ===== PTKP K/I VISIBILITY =====
        jointIncomeField.setVisible(false);

        marriage.addValueChangeListener(e -> {
            boolean married = e.getValue() != null
                    && !e.getValue().name().equalsIgnoreCase("SINGLE")
                    && !e.getValue().name().equalsIgnoreCase("TK");

            jointIncomeField.setVisible(married);

            if (!married) {
                jointIncomeField.setValue(false);
            }
        });

    }

    private static final String CIRCLE_PX = "200px";

    private VerticalLayout  createAvatarUploadLayout() {
        VerticalLayout photoLayout = new VerticalLayout();
        photoLayout.setMinWidth("250px");
        photoLayout.setWidth("300px");
        photoLayout.setSpacing(false);
//        photoLayout.setHeightFull();
        // set photoLayout bacground to gray
//        photoLayout.getStyle().set("background-color", "#f0f0f0");

        this.photoPreview = new Image();
        photoPreview.setVisible(false);

        InMemoryUploadHandler inMemoryHandler = UploadHandler.inMemory(
                (metadata, data) -> {
                    // Get other information about the file.
                    String fileName = metadata.fileName();
                    String mimeType = metadata.contentType();
                    long contentLength = metadata.contentLength();

                    log.debug("got filename: {}, mimeType: {}, contentLength: {}", fileName, mimeType, contentLength);

                    photoPreview.removeAll();

                    // Create image from byte array
                    StreamResource imageResource = new StreamResource(
                            UUID.randomUUID() + ".png", // Use PNG or your real format
                            () -> new ByteArrayInputStream(data)
                    );

//                    // Update image source with the uploaded file data
//                    UI ui = UI.getCurrent();
//                    ui.access(() -> {
                    photoPreview.removeAll();
                    photoPreview = new Image(imageResource, fileName);
                    photoPreviewCosmetics();

                    this.avatarLayout.removeAll();
                    avatarLayout.add(photoPreview);

                    // Store to uploadedImageBytes
                    uploadedImageBytes.set(data);
//                    });

                    // Do something with the file data...
                    // processFile(data, fileName);
                });

        Upload upload = new Upload(inMemoryHandler);
        upload.setWidthFull();
        upload.setAcceptedFileTypes(
                "image/png",
                "image/jpeg",
                "image/jpg",
                "image/gif",
                "image/webp",
                "image/bmp",
                "image/svg+xml",
                "image/tiff"
        );
        upload.setMaxFiles(1);
        upload.setMaxFileSize(1 * 1024 * 1024); // 1 MB

        photoLayout.add(avatarLayout, upload);
        return photoLayout;
    }

    private void photoPreviewCosmetics() {
        photoPreview.setWidth(CIRCLE_PX);
        photoPreview.setHeight(CIRCLE_PX);
        photoPreview.getStyle().set("border-radius", "10%"); // Makes it perfectly round
        photoPreview.getStyle().set("object-fit", "cover"); // Ensures it fills the circle properly
        photoPreview.getStyle().set("box-shadow", "0 4px 8px rgba(0, 0, 0, 0.2)");
        photoPreview.getStyle().set("margin-left", "-25px");
    }

    private final  String RESP_1 = "400px";
    private final  String RESP_2 = "700px";
    private final int COL_1 = 3;
    private final int COL_2 = 4;
    private final String MAX_WIDTH = "50%";
    private final String MAX_WIDTH_GRID = "40rem";
    private final String HEIGHT_GRID = "250px";

    private void createAddressForm() {
        fullAddress.getStyle().setMinWidth("400px");
        fullAddress.getStyle().setMinHeight("200px");

        fullAddress.setRequiredIndicatorVisible(true);
// Create address form layout
        FormLayout addressFormLayout = new FormLayout();
        addressFormLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep(RESP_1, COL_1),
                new FormLayout.ResponsiveStep(RESP_2, COL_2)
        );

        // Add address fields to the form layout
        addressFormLayout.add(
                fullAddress,
                isDefaultAddress
        );
        addressFormLayout.getStyle().setMaxWidth(MAX_WIDTH);

        this.gridAddress = new Grid<>(HrPersonAddress.class, false);
        // Column 1: Address name and full address (multi-line)
        gridAddress.addComponentColumn(address -> {
            VerticalLayout layout = new VerticalLayout();
            layout.setSpacing(false);
            layout.setPadding(false);
            layout.setMargin(false);
            layout.setAlignItems(FlexComponent.Alignment.START);

            layout.add(new Span(address.getFullAddress()));

            return layout;
        }).setHeader("Address").setAutoWidth(true);
        // Column 2: Default (Yes/No)
        gridAddress.addColumn(address -> address.getIsDefault() ? "Yes" : "No")
                .setHeader("Default")
                .setAutoWidth(true);

        // Action column: Delete and Edit buttons
        gridAddress.addComponentColumn(address -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            Button deleteBtn = new Button(VaadinIcon.CLOSE_CIRCLE_O.create());
            deleteBtn.getElement().setAttribute("theme", "icon error");
            deleteBtn.addClickListener(e -> {
                addressList.remove(address);
                gridAddress.setItems(addressList);
                if (address.getId() != null)
                    personService.deleteAddress(address);
            });

            Button editBtn = new Button(VaadinIcon.PENCIL.create());
            editBtn.getElement().setAttribute("theme", "icon primary");
            editBtn.addClickListener(e -> {
                populateAddressFields(address);
            });

            actions.add(deleteBtn, editBtn);
            return actions;
        }).setAutoWidth(true);

        gridAddress.setItems(addressList);
        gridAddress.getStyle().setMaxWidth(MAX_WIDTH_GRID);
        gridAddress.setHeight(HEIGHT_GRID);

        addressesLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.ROW, LumoUtility.Gap.MEDIUM);
        addressesLayout.add(gridAddress, addressFormLayout);
    }

    private void createContactsForm() {
        typeContact.setItems(ContactTypeEnum.values());

        typeContact.addValueChangeListener(e -> {
            ContactTypeEnum type = e.getValue();

            boolean isEmergency = type == ContactTypeEnum.EMERGENCY;

            // Relationship & Description hanya mandatory jika EMERGENCY
            relationship.setRequiredIndicatorVisible(isEmergency);
            description.setRequiredIndicatorVisible(isEmergency);

            if (!isEmergency) {
                relationship.setInvalid(false);
                description.setInvalid(false);
            }
        });


        // --- TAMBAHKAN INDIKATOR WAJIB ---
        typeContact.setRequiredIndicatorVisible(true);
        stringValue.setRequiredIndicatorVisible(true);
        designation.setRequiredIndicatorVisible(true);
        relationship.setRequiredIndicatorVisible(false);
        description.setRequiredIndicatorVisible(false);
        // ---------------------------------

        // ===== Added contact type dynamic validation =====
        if (typeContact != null && stringValue != null) {
            Runnable applyContactTypeRules = () -> {
                Object t = typeContact.getValue();

                // Reset validasi
                stringValue.setInvalid(false);
                stringValue.setAllowedCharPattern(null);

                if (t == ContactTypeEnum.EMAIL) {
                    stringValue.setMaxLength(160);
                    stringValue.setHelperText("Email valid, contoh: nama@domain.com");

                } else if (t == ContactTypeEnum.NUMBER || t == ContactTypeEnum.EMERGENCY) {
                    stringValue.setAllowedCharPattern("\\d"); // Hanya digit
                    stringValue.setMaxLength(18); // Batas 18
                    stringValue.setHelperText("Hanya angka, maks. 18 digit");

                } else {
                    stringValue.setAllowedCharPattern(null);
                    stringValue.setMaxLength(255); // Default
                    stringValue.setHelperText(null);
                }
            };

            // Listener untuk mengubah aturan saat Tipe Kontak diganti
            typeContact.addValueChangeListener(e -> applyContactTypeRules.run());

            // Listener untuk validasi input 'Value' secara real-time
            stringValue.addValueChangeListener(e -> {
                String v = e.getValue();
                if (v == null || v.isBlank()) {
                    stringValue.setInvalid(false); // Jangan tampilkan error jika kosong (biarkan tombol Add yg validasi)
                    return;
                }

                ContactTypeEnum t = typeContact.getValue();
                boolean invalid = false;

                if (t == ContactTypeEnum.EMAIL) {
                    invalid = !isEmailValid(v.trim());
                } else if (t == ContactTypeEnum.NUMBER || t == ContactTypeEnum.EMERGENCY) {
                    invalid = !isPhone18Digits(v); // Cek format angka 18 digit
                }

                stringValue.setInvalid(invalid);
            });

            applyContactTypeRules.run();
        }


        description.getStyle().setMinWidth("200px");

        // Create FormLayout
        contactFormLayout = new FormLayout();
        contactFormLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep(RESP_1, COL_1),
                new FormLayout.ResponsiveStep(RESP_2, COL_2)
        );

        contactFormLayout.add(
                typeContact,
                stringValue,
                designation,
                relationship,
                description,
                isDefaultContact
        );

        contactFormLayout.getStyle().setMaxWidth(MAX_WIDTH);


        this.gridContacts = new Grid<>(HrPersonContact.class, false);
        // Column 1: Type
        gridContacts.addColumn(contact -> contact.getType().toString())
                .setHeader("Type")
                .setAutoWidth(true);
        // Column 2: multi-line
        gridContacts.addComponentColumn(contact -> {
            VerticalLayout layout = new VerticalLayout();
            layout.setSpacing(false);
            layout.setPadding(false);
            layout.setMargin(false);
            layout.setAlignItems(FlexComponent.Alignment.START);

            layout.add(new Span(contact.getStringValue()));
            layout.add(new Span(contact.getDesignation()));
            layout.add(new Span(contact.getRelationship()));

            return layout;
        }).setHeader("Contact").setAutoWidth(true);
        // Add action column
        gridContacts.addComponentColumn(contact -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            Button deleteBtn = new Button(VaadinIcon.CLOSE_CIRCLE_O.create());
            deleteBtn.getElement().setAttribute("theme", "icon error");
            deleteBtn.addClickListener(e -> {
                contactList.remove(contact);
                gridContacts.setItems(contactList);

                if (contact.getId() != null)
                    personService.deleteContact(contact);
            });

            Button editBtn = new Button(VaadinIcon.PENCIL.create());
            editBtn.getElement().setAttribute("theme", "icon primary");
            editBtn.addClickListener(e -> {
                populateContactFields(contact);
            });

            actions.add(deleteBtn, editBtn);
            return actions;
        }).setHeader("Actions").setAutoWidth(true);

        gridContacts.setItems(contactList);
        gridContacts.getStyle().setMaxWidth(MAX_WIDTH_GRID);
        gridContacts.setHeight(HEIGHT_GRID);

        contactsLayout.setWidthFull();
        contactsLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.ROW, LumoUtility.Gap.MEDIUM);
        contactsLayout.add(gridContacts, contactFormLayout);
    }

    private void createEducationForm() {
        typeEducation.setItems(EducationTypeEnum.values());

        score.setStep(0.10);
        score.setMin(0);
        score.setMax(10.99);

        institution.setRequiredIndicatorVisible(true);
        program.setRequiredIndicatorVisible(true);
        typeEducation.setRequiredIndicatorVisible(true);

        // --- TAMBAHKAN INI ---
        score.setRequiredIndicatorVisible(true);
        startDate.setRequiredIndicatorVisible(true);
        finishDate.setRequiredIndicatorVisible(true);
        certificateTitle.setRequiredIndicatorVisible(true);
        // ---------------------

        // Create FormLayout
        educationFormLayout = new FormLayout();
        educationFormLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep(RESP_1, COL_1),
                new FormLayout.ResponsiveStep(RESP_2, COL_2)
        );

        educationFormLayout.add(
                institution,
                program,
                score,
                startDate,
                finishDate,
                certificateTitle,
                certificateExpiration,
                typeEducation
        );
        educationFormLayout.getStyle().setMaxWidth(MAX_WIDTH);

        this.gridEducation = new Grid<>(HrPersonEducation.class, false);
        // Column 1: Type
        gridEducation.addColumn(education -> education.getType().toString())
                .setHeader("Type")
                .setAutoWidth(true);
        // Column 2: multi-line
        gridEducation.addComponentColumn(education -> {
            VerticalLayout layout = new VerticalLayout();
            layout.setSpacing(false);
            layout.setPadding(false);
            layout.setMargin(false);
            layout.setAlignItems(FlexComponent.Alignment.START);

            layout.add(new Span(education.getInstitution()));
            layout.add(new Span(education.getProgram()));
            layout.add(new Span(education.getScore() != null ? education.getScore().toString() : "No Score"));
            String start = education.getStartDate() != null ? education.getStartDate().toString() : "Not Specified";
            String finish = education.getFinishDate() != null ? education.getFinishDate().toString() : "Not Specified";
            layout.add(new Span("Duration: " + start + " to " + finish));


            return layout;
        }).setHeader("Education").setAutoWidth(true);
        // Column 3: multi-line
        gridEducation.addComponentColumn(education -> {
            VerticalLayout layout = new VerticalLayout();
            layout.setSpacing(false);
            layout.setPadding(false);
            layout.setMargin(false);
            layout.setAlignItems(FlexComponent.Alignment.START);

            layout.add(new Span(education.getCertificateTitle()));
            layout.add(new Span(education.getCertificateExpiration() != null ? education.getCertificateExpiration().toString() : "No Expiration"));

            return layout;
        }).setHeader("Certificate").setAutoWidth(true);
        // Add action column
        gridEducation.addComponentColumn(education -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            Button deleteBtn = new Button(VaadinIcon.CLOSE_CIRCLE_O.create());
            deleteBtn.getElement().setAttribute("theme", "icon error");
            deleteBtn.addClickListener(e -> {
                educationList.remove(education);
                gridEducation.setItems(educationList);

                if (education.getId() != null)
                    personService.deleteEducation(education);
            });

            Button editBtn = new Button(VaadinIcon.PENCIL.create());
            editBtn.getElement().setAttribute("theme", "icon primary");
            editBtn.addClickListener(e -> {
                populateEducationFields(education);
            });

            actions.add(deleteBtn, editBtn);
            return actions;
        }).setHeader("Actions").setAutoWidth(true);

        gridEducation.setItems(educationList);
        gridEducation.getStyle().setMaxWidth(MAX_WIDTH_GRID);
        gridEducation.setHeight(HEIGHT_GRID);

        educationLayout.setWidthFull();
        educationLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.ROW, LumoUtility.Gap.MEDIUM);
        educationLayout.add(gridEducation, educationFormLayout);
    }

    private void createDocumentForm() {
        // ComboBox enum
        typeDocument.setItems(DocumentTypeEnum.values());
        contentType.setItems(ContentTypeEnum.values());

        // Field configuration
        year.setMin(1950);
        year.setMax(2045);
        year.setStep(1);

        size.setMin(0);
        size.setStep(1024); // KB step

        // --- TAMBAHKAN INI (UNTUK SEMUA FIELD) ---
        typeDocument.setRequiredIndicatorVisible(true);
        nameDocoument.setRequiredIndicatorVisible(true);
        descDocument.setRequiredIndicatorVisible(true);
        notes.setRequiredIndicatorVisible(true);
        year.setRequiredIndicatorVisible(true);
        contentType.setRequiredIndicatorVisible(true);
        size.setRequiredIndicatorVisible(true);
        filename.setRequiredIndicatorVisible(true);
        path.setRequiredIndicatorVisible(true);
        // ---------------------------------------


        // Build FormLayout
        documentFormLayout = new FormLayout();
        documentFormLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep(RESP_1, COL_1),
                new FormLayout.ResponsiveStep(RESP_2, COL_2)
        );

        documentFormLayout.add(
                typeDocument,
                nameDocoument,
                descDocument,
                notes,
                year,
                contentType,
                size,
                filename,
                path
        );
        documentFormLayout.getStyle().setMaxWidth(MAX_WIDTH);

        this.gridDocument = new Grid<>(HrPersonDocument.class, false);
        // Column 1: Type
        gridDocument.addColumn(doc -> doc.getType().toString())
                .setHeader("Type")
                .setAutoWidth(true);
        // Column 2: name
        gridDocument.addColumn(HrPersonDocument::getName)
                .setHeader("Name")
                .setAutoWidth(true);
        // Column 3: Type
        gridDocument.addColumn(HrPersonDocument::getFilename)
                .setHeader("Filename")
                .setAutoWidth(true);

        // Add action column
        gridDocument.addComponentColumn(document -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            Button deleteBtn = new Button(VaadinIcon.CLOSE_CIRCLE_O.create());
            deleteBtn.getElement().setAttribute("theme", "icon error");
            deleteBtn.addClickListener(e -> {
                documentList.remove(document);
                gridDocument.setItems(documentList);

                if (document.getId() != null)
                    personService.deleteDocument(document);
            });

            Button editBtn = new Button(VaadinIcon.PENCIL.create());
            editBtn.getElement().setAttribute("theme", "icon primary");
            editBtn.addClickListener(e -> {
                populateDocumentFields(document);
            });

            actions.add(deleteBtn, editBtn);
            return actions;
        }).setHeader("Actions").setAutoWidth(true);

        gridDocument.setItems(documentList);
        gridDocument.getStyle().setMaxWidth(MAX_WIDTH_GRID);
        gridDocument.setHeight(HEIGHT_GRID);

        documentsLayout.setWidthFull();
        documentsLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.ROW, LumoUtility.Gap.MEDIUM);
        documentsLayout.add(gridDocument, documentFormLayout);
    }

    private void createTanggunganForm() {
        tgRelation.setItems("Suami", "Istri", "Anak Kandung");
        tgGender.setItems(GenderEnum.values());
        tgDob.setI18n(DatePickerUtil.getIndonesianI18n());
        tgStillDependent.setEnabled(false);

        tgDob.addValueChangeListener(e -> updateDependentStatus());
        tgRelation.addValueChangeListener(e -> updateDependentStatus());

        FormLayout tanggunganForm = new FormLayout(
                tgName, tgRelation, tgDob, tgGender, tgStillDependent
        );

        gridTanggungan = new Grid<>(HrPersonTanggungan.class, false);
        gridTanggungan.addColumn(HrPersonTanggungan::getName).setHeader("Nama");
        gridTanggungan.addColumn(HrPersonTanggungan::getRelation).setHeader("Hubungan");
        gridTanggungan.addColumn(HrPersonTanggungan::getDob).setHeader("Tanggal Lahir");
        gridTanggungan.addColumn(t -> t.getGender().name()).setHeader("Gender");
        gridTanggungan.addColumn(t -> t.getStillDependent() ? "Ya" : "Tidak")
                .setHeader("Masih Tanggungan");

        gridTanggungan.addComponentColumn(t -> {
            Button edit = new Button(VaadinIcon.PENCIL.create(), e -> loadTanggungan(t));
            Button del = new Button(VaadinIcon.TRASH.create(), e -> {
                if (t.getId() != null) {
                    personTanggunganService.delete(t);
                }
                tanggunganList.remove(t);
                gridTanggungan.setItems(tanggunganList);
            });
            return new HorizontalLayout(edit, del);
        }).setHeader("Action");

        gridTanggungan.setItems(tanggunganList);
        gridTanggungan.setHeight("250px");

        tanggunganLayout.add(gridTanggungan, tanggunganForm);
    }
    private void updateDependentStatus() {
        if (tgDob.getValue() == null) {
            tgStillDependent.setValue(false);
            return;
        }

        int age = java.time.Period.between(tgDob.getValue(), LocalDate.now()).getYears();
        boolean still = age <= 21;

        if ("Suami".equals(tgRelation.getValue()) || "Istri".equals(tgRelation.getValue())) {
            still = false;
        }

        tgStillDependent.setValue(still);
    }

    // Helper to populate address fields for editing
    private void populateAddressFields(HrPersonAddress address) {
        this.addressData = address;
        this.addressList.remove(address);
        this.gridAddress.setItems(addressList);

        fullAddress.setValue(address.getFullAddress() != null ? address.getFullAddress() : "");
        isDefaultAddress.setValue(address.getIsDefault() != null ? address.getIsDefault() : false);
    }

    // Helper to populate contact fields for editing
    private void populateContactFields(HrPersonContact contact) {
        this.contactData = contact;
        this.contactList.remove(contact);
        this.gridContacts.setItems(contactList);

        designation.setValue(contact.getDesignation() != null ? contact.getDesignation() : "");
        relationship.setValue(contact.getRelationship() != null ? contact.getRelationship() : "");
        stringValue.setValue(contact.getStringValue() != null ? contact.getStringValue() : "");
        typeContact.setValue(contact.getType());
        description.setValue(contact.getDescription() != null ? contact.getDescription() : "");
        isDefaultContact.setValue(contact.getIsDefault() != null ? contact.getIsDefault() : false);
    }

    // Helper to populate education fields for editing
    private void populateEducationFields(HrPersonEducation education) {
        this.educationData = education;
        this.educationList.remove(education);
        this.gridEducation.setItems(educationList);

        institution.setValue(education.getInstitution() != null ? education.getInstitution() : "");
        program.setValue(education.getProgram() != null ? education.getProgram() : "");
        score.setValue(education.getScore() != null ? education.getScore().doubleValue() : null);
        startDate.setValue(education.getStartDate());
        finishDate.setValue(education.getFinishDate());
        certificateTitle.setValue(education.getCertificateTitle() != null ? education.getCertificateTitle() : "");
        certificateExpiration.setValue(education.getCertificateExpiration());
        typeEducation.setValue(education.getType());
    }

    // Helper to populate document fields for editing
    private void populateDocumentFields(HrPersonDocument document) {
        this.documentData = document;
        this.documentList.remove(document);
        this.gridDocument.setItems(documentList);

        nameDocoument.setValue(document.getName() != null ? document.getName() : "");
        descDocument.setValue(document.getDescription() != null ? document.getDescription() : "");
        notes.setValue(document.getNotes() != null ? document.getNotes() : "");
        year.setValue(document.getYear() != null ? document.getYear().doubleValue() : null);
        typeDocument.setValue(document.getType());
        contentType.setValue(document.getContentType());
        size.setValue(document.getSize() != null ? document.getSize().doubleValue() : null);
        filename.setValue(document.getFilename() != null ? document.getFilename() : "");
        path.setValue(document.getPath() != null ? document.getPath() : "");
    }


    private void loadTanggungan(HrPersonTanggungan t) {
        this.tanggunganData = t;

        tgName.setValue(t.getName());
        tgRelation.setValue(t.getRelation());
        tgDob.setValue(t.getDob());
        tgGender.setValue(t.getGender());
        tgStillDependent.setValue(
                t.getStillDependent() != null ? t.getStillDependent() : false
        );
    }
    private void clearTanggunganForm() {
        tgName.clear();
        tgRelation.clear();
        tgDob.clear();
        tgGender.clear();
        tgStillDependent.clear();
        tanggunganData = null;
    }

    private void populateDemoDate() {
        try {
            HttpClient client = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create("https://randomuser.me/api/"))
                    .build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

            ObjectMapper mapper = new ObjectMapper();
            JsonNode root = mapper.readTree(response.body());
            JsonNode user = root.path("results").get(0);

            // Person Fields
            firstName.setValue(user.path("name").path("first").asText(""));
            middleName.setValue(""); // Not available
            lastName.setValue(user.path("name").path("last").asText(""));
            pob.setValue(user.path("location").path("city").asText(""));
            dob.setValue(LocalDate.parse(user.path("dob").path("date").asText("").substring(0, 10)));
            gender.setValue(GenderEnum.valueOf(user.path("gender").asText("").toUpperCase()));
            nationality.setValue(NationalityEnum.OTHER); // Dummy value
            religion.setValue(ReligionEnum.ISLAM); // Dummy value
            marriage.setValue(MarriageEnum.YES); // Dummy value
            ktpNumber.setValue(String.format("%08d%08d",
                    new java.util.Random().nextInt(100_000_000),
                    new java.util.Random().nextInt(100_000_000))); // dummy value

            // Get photo URL and download image
            String photo_url = user.path("picture").path("large").asText("");
            // get image byte[] from photo_url and put on uploadedImageBytes
            HttpResponse<byte[]> photoResponse = client.send(
                    HttpRequest.newBuilder().uri(URI.create(photo_url)).build(),
                    HttpResponse.BodyHandlers.ofByteArray()
            );
            uploadedImageBytes.set(photoResponse.body());

            // Update photo preview
            photoPreview.removeAll();
            StreamResource imageResource = new StreamResource(
                    UUID.randomUUID() + ".png", // Use PNG or your real format
                    () -> new ByteArrayInputStream(uploadedImageBytes.get())
            );

            photoPreview = new Image(imageResource, "Random User Photo");
            photoPreviewCosmetics();
            this.avatarLayout.removeAll();
            avatarLayout.add(photoPreview);

            // Address Fields
            JsonNode loc = user.path("location");
            String address = loc.path("street").path("number").asInt() + " " +
                    loc.path("street").path("name").asText("") + ", " +
                    loc.path("city").asText("") + ", " +
                    loc.path("state").asText("") + ", " +
                    loc.path("country").asText("") + ", " +
                    loc.path("postcode").asText("");
            fullAddress.setValue(address);
            isDefaultAddress.setValue(true);

            // Contacts Fields
            designation.setValue("Emergency Contact");
            relationship.setValue("Friend");
            stringValue.setValue(user.path("phone").asText(""));
            typeContact.setValue(ContactTypeEnum.NUMBER);
            description.setValue("Imported from randomuser.me");
            isDefaultContact.setValue(true);

            // Education Fields
            institution.setValue("Random University");
            program.setValue("Computer Science");
            score.setValue(3.5);
            startDate.setValue(LocalDate.now().minusYears(4));
            finishDate.setValue(LocalDate.now());
            certificateTitle.setValue("Bachelor Degree");
            certificateExpiration.setValue(null);
            typeEducation.setValue(EducationTypeEnum.ACADEMIC);

            // Document Fields
            nameDocoument.setValue("ID Card");
            descDocument.setValue("Generated by randomuser.me");
            notes.setValue("Sample document");
            year.setValue(2024d);
            typeDocument.setValue(DocumentTypeEnum.KTP);
            contentType.setValue(ContentTypeEnum.PHYSICAL_STORAGE);
            size.setValue(102400d);
            filename.setValue("idcard.pdf");
            path.setValue("/documents/idcard.pdf");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== Contact validation helpers (UPDATED) =====
    /**
     * Validasi email generik (tidak terbatas TLD)
     */
    private static boolean isEmailValid(String s) {
        if (s == null) return false;
        if (s.length() > 160) return false; // Tetap batasi panjang
        // Regex email umum
        String emailRegex = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return s.matches(emailRegex);
    }

    /**
     * Validasi nomor telepon/angka (maks 18 digit)
     */
    private static boolean isPhone18Digits(String s) {
        // hanya angka, panjang 1..18
        return s != null && s.matches("^\\d{1,18}$");
    }

    private void save() {

        // ⬅️ BUAT DI SINI, AMAN
        FwAppUser appUser = commonService.getLoginUser(
                currentUser.require().getUserId().toString()
        );

        HrPerson person = this.personData != null ? this.personData : new HrPerson();

        person.setFirstName(firstName.getValue());
        person.setMiddleName(middleName.getValue());
        person.setLastName(lastName.getValue());
        person.setPob(pob.getValue());
        person.setDob(dob.getValue());
        person.setGender(gender.getValue());
        person.setNationality(nationality.getValue());
        person.setReligion(religion.getValue());
        person.setMarriage(marriage.getValue());
        person.setKtpNumber(ktpNumber.getValue());

        // upload photo dll...

        personService.workingWithPerson(person, appUser);
        personService.savePerson();
        this.personData = person;


        personService.saveAllInformation(
                addressList,
                contactList,
                educationList,
                documentList
        );

        personService.saveTanggungan(
                person,
                tanggunganList
        );
        // ===== PTKP =====
        List<HrPersonTanggungan> dbTanggungan =
                personTanggunganService.findByPerson(person);

        personPtkpService.generateAndSavePtkp(
                person,
                person.getMarriage(),
                dbTanggungan,
                Boolean.TRUE.equals(jointIncomeField.getValue())
        );

        Notification.show("Data saved successfully");
    }


    public void clearForm(boolean person, boolean address, boolean contact, boolean education, boolean document) {
        // Person Fields
        if  (person) {
            firstName.setValue("");
            middleName.setValue("");
            lastName.setValue("");
            pob.setValue("");
            dob.setValue(null);
            gender.setValue(null);
            nationality.setValue(null);
            religion.setValue(null);
            marriage.setValue(null);
            ktpNumber.setValue("");

            uploadedImageBytes.set(null);
            if (photoPreview != null) {
                photoPreview.setVisible(false);
                this.avatarLayout.removeAll();
            }
        }

        if (address) {
            // Address Fields
            fullAddress.setValue("");
            fullAddress.getStyle().setMinWidth("400px");
            isDefaultAddress.setValue(false);
        }

        if (contact) {
            // Contacts Fields
            designation.setValue("");
            relationship.setValue("");

            stringValue.setValue("");
            typeContact.setValue(null);
            description.setValue("");
            isDefaultContact.setValue(false);
        }

        if (education) {
            // Education Fields
            institution.setValue("");
            program.setValue("");
            score.setValue(null);
            startDate.setValue(null);
            finishDate.setValue(null);
            certificateTitle.setValue("");
            certificateExpiration.setValue(null);
            typeEducation.setValue(null);
        }

        // Document Fields
        if (document) {
            nameDocoument.setValue("");
            descDocument.setValue("");
            notes.setValue("");
            year.setValue(null);
            typeDocument.setValue(null);
            contentType.setValue(null);
            size.setValue(null);
            filename.setValue("");
            path.setValue("");
        }
    }

    public void clearGrid(boolean address, boolean contact, boolean education, boolean document) {
        if (address) {
            addressList.clear();
            gridAddress.setItems(addressList);
        }
        if (contact) {
            contactList.clear();
            gridContacts.setItems(contactList);
        }
        if (education) {
            educationList.clear();
            gridEducation.setItems(educationList);
        }
        if (document) {
            documentList.clear();
            gridDocument.setItems(documentList);
        }
    }

    @Override
    public void setParameter(BeforeEvent beforeEvent, @OptionalParameter Long iddb) {
        if(!this.auth.canEdit) {
            Notification.show("You do not have permission to edit this person.");
        }

        if (iddb != null && this.auth.canEdit) {
            log.debug("Loading person with ID: {}", iddb);

            this.personData = personService.getPerson(iddb);
            this.addressList = personService.getPersonAddresses();
            this.contactList = personService.getPersonContacts();
            this.educationList = personService.getPersonEducations();
            this.documentList = personService.getPersonDocuments();

            // Populate Person Data
            firstName.setValue(personData.getFirstName() != null ? personData.getFirstName() : "");
            middleName.setValue(personData.getMiddleName() != null ? personData.getMiddleName() : "");
            lastName.setValue(personData.getLastName() != null ? personData.getLastName() : "");
            pob.setValue(personData.getPob() != null ? personData.getPob() : "");
            dob.setValue(personData.getDob() != null ? personData.getDob() : null);
            gender.setValue(personData.getGender());
            nationality.setValue(personData.getNationality());
            religion.setValue(personData.getReligion());
            marriage.setValue(personData.getMarriage());
            ktpNumber.setValue(personData.getKtpNumber() != null ? personData.getKtpNumber() : "");

            // Load photo if available
            if (personData.getPhotoFilename() != null && !personData.getPhotoFilename().isEmpty()) {
                log.debug("Getting photo from path: {}", personData.getPhotoFilename());

                // Load image file from photoPath as in memory byte array
                byte[] imageBytes = personService.getPhotoAsByteArray(personData.getPhotoFilename());
                this.photoPreview = new Image(new StreamResource(
                        personData.getPhotoFilename(),
                        () -> new ByteArrayInputStream(imageBytes)
                ), "Photo for " + personData.getFirstName());
                photoPreviewCosmetics();
                this.avatarLayout.removeAll();
                this.avatarLayout.add(photoPreview);
            }


            // Populate the form fields with the loaded data
            this.gridAddress.setItems(this.addressList);
            this.gridContacts.setItems(this.contactList);
            this.gridEducation.setItems(this.educationList);
            this.gridDocument.setItems(this.documentList);
            this.tanggunganList =
                    personTanggunganService.findByPerson(this.personData);
            this.gridTanggungan.setItems(this.tanggunganList);

            log.debug("loaded person: {} {} {} {} {}", this.personData.getFirstName(), addressList.size(), contactList.size(), educationList.size(), documentList.size());
            demoButton.setEnabled(false);
        } else {
            clearForm(true, true, true, true, true);
            clearGrid(true, true, true, true);
        }
    }
}
