package com.fusi24.pangreksa.web.view.employee;

import com.vaadin.flow.data.value.ValueChangeMode;
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
import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
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
import com.vaadin.flow.server.streams.InMemoryUploadHandler;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicReference;



@Route("my-profile")
@PageTitle("Profil Karyawan Saya")
@PermitAll
// When security is enabled, allow all authenticated users
public class MyProfileView extends Main {
    private static final long serialVersionUID = 15L;
    private static final Logger log = LoggerFactory.getLogger(KaryawanBaruFormView.class);
    private final CurrentUser currentUser;
    private final CommonService commonService;
    private final PersonService personService;
    private Authorization auth;

    public static final String VIEW_NAME = "Profil Karyawan Saya";

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

    public MyProfileView(CurrentUser currentUser, CommonService commonService, PersonService personService) {
        this.currentUser = currentUser;
        this.commonService = commonService;
        this.personService = personService;

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
    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);

        // KTP/NIK tidak boleh diubah lewat UI profil saya
        ktpNumber.setReadOnly(true);
        firstName.setReadOnly(true);
        middleName.setReadOnly(true);
        lastName.setReadOnly(true);
        gender.setReadOnly(true);
        pob.setReadOnly(true);
        dob.setReadOnly(true);
        nationality.setReadOnly(true);
        religion.setReadOnly(true);
        marriage.setReadOnly(true);
        Long pid = resolveCurrentPersonId();
        if (pid == null) {
            Notification.show("Tidak menemukan personId untuk user saat ini.", 4000, Notification.Position.MIDDLE);
            return;
        }
        // Load person and related lists
        this.personData = personService.getPerson(pid);
        this.addressList = personService.getPersonAddresses();
        this.contactList = personService.getPersonContacts();
        this.educationList = personService.getPersonEducations();
        this.documentList = personService.getPersonDocuments();

        // Populate Person fields
        firstName.setValue(personData.getFirstName() != null ? personData.getFirstName() : "");
        middleName.setValue(personData.getMiddleName() != null ? personData.getMiddleName() : "");
        lastName.setValue(personData.getLastName() != null ? personData.getLastName() : "");
        ktpNumber.setValue(personData.getKtpNumber() != null ? personData.getKtpNumber() : "");
        pob.setValue(personData.getPob() != null ? personData.getPob() : "");
        dob.setValue(personData.getDob());
        gender.setValue(personData.getGender());
        nationality.setValue(personData.getNationality());
        religion.setValue(personData.getReligion());
        marriage.setValue(personData.getMarriage());

        // Load photo if exists
        if (personData.getPhotoFilename() != null) {
            try {
                byte[] bytes = personService.getPhotoAsByteArray(personData.getPhotoFilename());
                if (bytes != null && bytes.length > 0) {
                    this.uploadedImageBytes.set(bytes);
                    String mime = mimeFromFilename(personData.getPhotoFilename());
                    String __dataUrl = "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(bytes);
                    photoPreview.setSrc(__dataUrl);
                    photoPreview.setAlt(personData.getPhotoFilename() != null ? personData.getPhotoFilename() : "profile-photo");
                    photoPreview.setVisible(true);
                    photoPreviewCosmetics();
                    if (this.avatarLayout != null) { this.avatarLayout.removeAll(); this.avatarLayout.add(photoPreview); }
                }
            } catch (Exception ignore) {}
        }

        // Populate Grids
        gridAddress.setItems(this.addressList);
        gridContacts.setItems(this.contactList);
        gridEducation.setItems(this.educationList);
        gridDocument.setItems(this.documentList);

        updateSaveButtonState();
    }


    private void setAuthorization(){
        if(!this.auth.canView){
            // User does not have permission to view this page
        }

        if(!this.auth.canCreate){            saveButton.setEnabled(false);
            saveButtonOnTab.setEnabled(false);
        }
    }

    private void createBody() {
        this.body = new VerticalLayout();
        body.setPadding(false);

        // Inisiasi toolbar Master
        toolbarLayoutMaster = new HorizontalLayout();
        toolbarLayoutMaster.setAlignItems(FlexComponent.Alignment.END);

        saveButton = new Button("Save");
        clearButton = new Button("Reset");

        toolbarLayoutMaster.add( clearButton, saveButton);
        saveButton.setEnabled(false);

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

        tabSheet.getStyle().setWidth("100%");

        createAddressForm();
        createContactsForm();
        createEducationForm();
        createDocumentForm();

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


        // Validasi UI untuk NIK (KTP Number)
        ktpNumber.setClearButtonVisible(true);
        ktpNumber.setValueChangeMode(ValueChangeMode.EAGER);
        ktpNumber.setMaxLength(16);
        ktpNumber.setMinLength(16);
        ktpNumber.setPattern("\\d*"); // hanya digit
        ktpNumber.setAllowedCharPattern("\\d");
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



        // Update save button state initially and on changes
        Runnable refreshState = () -> UI.getCurrent().access(this::updateSaveButtonState);
        firstName.addValueChangeListener(e -> updateSaveButtonState());
        lastName.addValueChangeListener(e -> updateSaveButtonState());
        ktpNumber.addValueChangeListener(e -> updateSaveButtonState());
        dob.addValueChangeListener(e -> updateSaveButtonState());
        gender.addValueChangeListener(e -> updateSaveButtonState());
        nationality.addValueChangeListener(e -> updateSaveButtonState());
        religion.addValueChangeListener(e -> updateSaveButtonState());
        marriage.addValueChangeListener(e -> updateSaveButtonState());
        // ===== UI Validations =====
        // Required indicators
        // ===== Required indicators (hanya 2 field wajib) =====
        firstName.setRequiredIndicatorVisible(true);
        ktpNumber.setRequiredIndicatorVisible(true);

        // lainnya TIDAK wajib
        lastName.setRequiredIndicatorVisible(false);
        dob.setRequiredIndicatorVisible(false);
        gender.setRequiredIndicatorVisible(false);
        nationality.setRequiredIndicatorVisible(false);
        religion.setRequiredIndicatorVisible(false);
        marriage.setRequiredIndicatorVisible(false);

        // Names: only letters and spaces
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
        pob.setHelperText("Hanya huruf & spasi");
        pob.addValueChangeListener(e -> {
            String v = e.getValue() != null ? e.getValue().trim() : "";
            boolean ok = v.isEmpty() ? true : v.matches("^[\\p{L} ]{2,}$");
            pob.setInvalid(!ok);
        });

        // Date of Birth: set max to today
        dob.setMax(java.time.LocalDate.now());

        // Add all fields to the form layout
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
                marriage
        );
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

                    // photoPreview has no removeAll(); skip

                    // Create image from byte array (Base64)
                    String __mime = (mimeType != null && mimeType.startsWith("image/")) ? mimeType : "image/png";
                    String __imgDataUrl = "data:" + __mime + ";base64," + Base64.getEncoder().encodeToString(data);
                    photoPreview.setSrc(__imgDataUrl);


//                    // Update image source with the uploaded file data
//                    UI ui = UI.getCurrent();
//                    ui.access(() -> {
                    // photoPreview has no removeAll(); skip
                    String __imgDataUrl2 = "data:" + __mime + ";base64," + Base64.getEncoder().encodeToString(data);
                    photoPreview = new Image();
                    photoPreview.setSrc(__imgDataUrl2);
                    photoPreview.setAlt(fileName);
                    photoPreview.setVisible(true);

                    photoPreviewCosmetics();

                    this.avatarLayout.removeAll();
                    avatarLayout.add(photoPreview);

                    // Store to uploadedImageBytes
                    uploadedImageBytes.set(data);
//                    });

                    // Do something with the file data
                    // // processFile(data, fileName);
                });

        Upload upload = new Upload(inMemoryHandler);
        upload.setVisible(false);
        upload.setEnabled(false);
        upload.getElement().setProperty("title", "Upload foto dinonaktifkan");
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

        // Field constraints for contacts
        stringValue.setMaxLength(160);
        stringValue.setClearButtonVisible(true);
        stringValue.setValueChangeMode(ValueChangeMode.EAGER);
        relationship.setValueChangeMode(ValueChangeMode.EAGER);
        designation.setValueChangeMode(ValueChangeMode.EAGER);

        typeContact.addValueChangeListener(e -> {
            // reset errors
            stringValue.setInvalid(false);
            relationship.setInvalid(false);
            if (e.getValue() == ContactTypeEnum.EMAIL) {
                stringValue.setHelperText("Format: nama@email.com, maks 160 karakter");
            } else if (e.getValue() == ContactTypeEnum.NUMBER) {
                stringValue.setHelperText("Hanya angka, maks 15 digit");
            } else {
                stringValue.setHelperText(null);
            }
        });
        stringValue.addValueChangeListener(e -> { validateCurrentContactInputs(); updateSaveButtonState(); });
        relationship.addValueChangeListener(e -> { validateCurrentContactInputs(); updateSaveButtonState(); });
        designation.addValueChangeListener(e -> { validateCurrentContactInputs(); updateSaveButtonState(); });
        isDefaultContact.addValueChangeListener(e -> updateSaveButtonState());


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
                updateSaveButtonState();

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

        // Create FormLayout
        educationFormLayout = new FormLayout();
        educationFormLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep(RESP_1, COL_1),
                new FormLayout.ResponsiveStep(RESP_2, COL_2)
        );

        institution.setRequiredIndicatorVisible(true);
        institution.setValueChangeMode(ValueChangeMode.EAGER);
        institution.addValueChangeListener(e -> { try { institution.setInvalid(false); institution.setErrorMessage(null); } catch (Exception ignore) {} });
        program.setRequiredIndicatorVisible(true);
        program.setValueChangeMode(ValueChangeMode.EAGER);
        program.addValueChangeListener(e -> { try { program.setInvalid(false); program.setErrorMessage(null); } catch (Exception ignore) {} });
        typeEducation.setRequiredIndicatorVisible(true);
        typeEducation.addValueChangeListener(e -> { try { typeEducation.setInvalid(false); typeEducation.setErrorMessage(null); } catch (Exception ignore) {} });
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

    private void setListener() {
        saveButton.addClickListener(e -> {
            if (!this.auth.canCreate) {
                return;
            }
            save();
        });

        clearButton.addClickListener( e -> {
            clearForm(true, true, true, true, true);
            clearGrid(true,true, true, true);
        });

        clearButtonOnTab.addClickListener( e -> {
            int tabNo = tabSheet.getSelectedIndex();
            switch (tabNo) {
                case 0 -> clearForm(false, true, false, false, false); // Addresses
                case 1 -> clearForm(false, false, true, false, false); // Contacts
                case 2 -> clearForm(false, false, false, true, false); // Educations
                case 3 -> clearForm(false, false, false, false, true); // Documents
            }
        });

        saveButtonOnTab.addClickListener( e -> {
                    int tabNo = tabSheet.getSelectedIndex();
                    switch (tabNo) {
                        case 0 -> {
                            // Validate Address form before adding
                            String __addr = (fullAddress != null && fullAddress.getValue() != null) ? fullAddress.getValue().trim() : "";
                            if (__addr.isEmpty()) {
                                try { fullAddress.setInvalid(true); fullAddress.setErrorMessage("Alamat wajib diisi"); fullAddress.focus(); } catch (Exception ignore) {}
                                Notification.show("Alamat wajib diisi", 3000, Notification.Position.MIDDLE);
                                return;
                            }


                            HrPersonAddress address = this.addressData != null ? this.addressData : new HrPersonAddress();
                            address.setFullAddress(fullAddress.getValue());
                            address.setIsDefault(isDefaultAddress.getValue());
                            // address.setPerson(person);

                            address.setPerson(personData);
                            addressList.add(address);
                            gridAddress.setItems(addressList);

                            clearForm(false, true, false, false, false);
                            this.addressData = null;
                        }
                        case 1 -> {
                            // Validate inputs before adding to grid
                            if (!validateCurrentContactInputs()) {
                                Notification.show("Perbaiki input kontak terlebih dahulu.", 3000, Notification.Position.MIDDLE);
                                return;
                            }

                            HrPersonContact contact = this.contactData != null ? this.contactData : new HrPersonContact();
                            contact.setDesignation(designation.getValue());
                            contact.setRelationship(relationship.getValue());
                            contact.setStringValue(stringValue.getValue());
                            contact.setType(typeContact.getValue());
                            contact.setDescription(description.getValue());
                            contact.setIsDefault(isDefaultContact.getValue());
                            // contact.setPerson(person);

                            contact.setPerson(personData);
                            contactList.add(contact);
                            gridContacts.setItems(contactList);
                            clearForm(false, false, true, false, false);
                            this.contactData = null;
                            updateSaveButtonState();
                        }
                        case 2 -> {
                            // Validate required Education fields before adding
                            String __inst = institution != null ? (institution.getValue() != null ? institution.getValue().trim() : "") : "";
                            String __prog = program != null ? (program.getValue() != null ? program.getValue().trim() : "") : "";
                            var __type = (typeEducation != null) ? typeEducation.getValue() : null;
                            boolean __ok = true;
                            if (__inst.isEmpty()) {
                                __ok = false;
                                try { institution.setInvalid(true); institution.setErrorMessage("Institution wajib diisi"); institution.focus(); } catch (Exception ignore) {}
                            } else { try { institution.setInvalid(false); institution.setErrorMessage(null); } catch (Exception ignore) {} }
                            if (__prog.isEmpty()) {
                                __ok = false;
                                try { program.setInvalid(true); program.setErrorMessage("Program wajib diisi"); if (__ok) program.focus(); } catch (Exception ignore) {}
                            } else { try { program.setInvalid(false); program.setErrorMessage(null); } catch (Exception ignore) {} }
                            if (__type == null) {
                                __ok = false;
                                try { typeEducation.setInvalid(true); typeEducation.setErrorMessage("Education Type wajib dipilih"); if (__ok) typeEducation.focus(); } catch (Exception ignore) {}
                            } else { try { typeEducation.setInvalid(false); typeEducation.setErrorMessage(null); } catch (Exception ignore) {} }
                            if (!__ok) {
                                Notification.show("Lengkapi: Institution, Program, dan Education Type.", 3000, Notification.Position.MIDDLE);
                                return;
                            }

                            HrPersonEducation education = this.educationData != null ? this.educationData : new HrPersonEducation();
                            education.setInstitution(institution.getValue());
                            education.setProgram(program.getValue());
                            education.setScore(score.getValue() != null ? BigDecimal.valueOf(score.getValue()) : null);
                            education.setStartDate(startDate.getValue());
                            education.setFinishDate(finishDate.getValue());
                            education.setCertificateTitle(certificateTitle.getValue());
                            education.setCertificateExpiration(certificateExpiration.getValue());
                            education.setType(typeEducation.getValue());
                            // education.setPerson(person);

                            education.setPerson(personData);
                            educationList.add(education);
                            gridEducation.setItems(educationList);
                            clearForm(false, false, false, true, false);
                            this.educationData = null;
                        }
                        case 3 -> {
                            HrPersonDocument document = this.documentData != null ? this.documentData : new HrPersonDocument();
                            document.setName(nameDocoument.getValue());
                            document.setDescription(descDocument.getValue());
                            document.setNotes(notes.getValue());
                            document.setYear(year.getValue() != null ? year.getValue().intValue() : null);
                            document.setType(typeDocument.getValue());
                            document.setContentType(contentType.getValue());
                            document.setSize(size.getValue() != null ? size.getValue().longValue() : null);
                            document.setFilename(filename.getValue());
                            document.setPath(path.getValue());
                            // document.setPerson(person);

                            documentList.add(document);
                            gridDocument.setItems(documentList);
                            clearForm(false, false, false, false, true);
                            this.documentData = null;
                        }
                    }

                    save();
                }
        );

    }

    // === Helpers added for My Profile view ===
    private Long resolveCurrentPersonId() {
        try {
            Object sess = UI.getCurrent().getSession().getAttribute("personId");
            if (sess instanceof Number) return ((Number) sess).longValue();
            if (sess instanceof String s1) { try { return Long.parseLong(s1); } catch (Exception ignore) {} }
            Object sessHr = UI.getCurrent().getSession().getAttribute("hrPersonId");
            if (sessHr instanceof Number) return ((Number) sessHr).longValue();
            if (sessHr instanceof String s2) { try { return Long.parseLong(s2); } catch (Exception ignore) {} }

            var user = currentUser.require();
            var appUser = commonService.getLoginUser(user.getUserId().toString());
            Long id = tryLongGetter(appUser, "getHrPersonId"); if (id != null) return id;
            id = tryLongGetter(appUser, "getPersonId"); if (id != null) return id;
            id = tryNestedId(appUser, "getHrPerson", "getId"); if (id != null) return id;
            id = tryNestedId(appUser, "getPerson", "getId"); if (id != null) return id;
        } catch (Exception ignore) {}
        return null;
    }
    private Long tryLongGetter(Object target, String methodName) {
        if (target == null) return null;
        try {
            java.lang.reflect.Method m = target.getClass().getMethod(methodName);
            Object v = m.invoke(target);
            if (v instanceof Number) return ((Number) v).longValue();
        } catch (Exception ignore) {}
        return null;
    }
    private Long tryNestedId(Object target, String outerGetter, String innerGetter) {
        if (target == null) return null;
        try {
            java.lang.reflect.Method outer = target.getClass().getMethod(outerGetter);
            Object obj = outer.invoke(target);
            if (obj != null) {
                java.lang.reflect.Method inner = obj.getClass().getMethod(innerGetter);
                Object v = inner.invoke(obj);
                if (v instanceof Number) return ((Number) v).longValue();
            }
        } catch (Exception ignore) {}
        return null;
    }
    private void updateSaveButtonState() {
        boolean firstOk = firstName != null && firstName.getValue() != null && !firstName.getValue().trim().isEmpty();
        boolean nikOk = ktpNumber != null && ktpNumber.getValue() != null && ktpNumber.getValue().matches("^\\d{16}$");
        if (saveButton != null) saveButton.setEnabled(firstOk && nikOk);
        if (saveButtonOnTab != null) saveButtonOnTab.setEnabled(true);
    }



    private boolean validateCurrentContactInputs() {
        var type = (typeContact != null) ? typeContact.getValue() : null;
        var val  = (stringValue != null) ? stringValue.getValue() : null;
        if (type == null || val == null || val.trim().isEmpty()) return false;
        String v = val.trim();
        try {
            // Treat EMAIL distinctly; others numeric-ish
            if (type == ContactTypeEnum.EMAIL || (type.toString() != null && type.toString().equalsIgnoreCase("EMAIL"))) {
                return v.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
            } else {
                return v.replaceAll("[^\\d]", "").length() >= 8;
            }
        } catch (Exception ignore) { }
        return true;
    }

    private void clearForm(boolean person, boolean address, boolean contact, boolean education, boolean document) {
        if (person) {
            try { firstName.clear(); } catch (Exception ignore) {}
            try { middleName.clear(); } catch (Exception ignore) {}
            try { lastName.clear(); } catch (Exception ignore) {}
            try { pob.clear(); } catch (Exception ignore) {}
            try { dob.clear(); } catch (Exception ignore) {}
            try { gender.clear(); } catch (Exception ignore) {}
            try { nationality.clear(); } catch (Exception ignore) {}
            try { religion.clear(); } catch (Exception ignore) {}
            try { marriage.clear(); } catch (Exception ignore) {}
        }
        if (address) {
            try { fullAddress.clear(); } catch (Exception ignore) {}
            try { isDefaultAddress.setValue(false); } catch (Exception ignore) {}
        }
        if (contact) {
            try { designation.clear(); } catch (Exception ignore) {}
            try { relationship.clear(); } catch (Exception ignore) {}
            try { stringValue.clear(); } catch (Exception ignore) {}
            try { typeContact.clear(); } catch (Exception ignore) {}
            try { description.clear(); } catch (Exception ignore) {}
            try { isDefaultContact.setValue(false); } catch (Exception ignore) {}
        }
        if (education) {
            try { institution.clear(); } catch (Exception ignore) {}
            try { program.clear(); } catch (Exception ignore) {}
            try { score.clear(); } catch (Exception ignore) {}
            try { startDate.clear(); } catch (Exception ignore) {}
            try { finishDate.clear(); } catch (Exception ignore) {}
            try { certificateTitle.clear(); } catch (Exception ignore) {}
            try { certificateExpiration.clear(); } catch (Exception ignore) {}
            try { typeEducation.clear(); } catch (Exception ignore) {}
        }
        if (document) {
            try { nameDocoument.clear(); } catch (Exception ignore) {}
            try { descDocument.clear(); } catch (Exception ignore) {}
            try { notes.clear(); } catch (Exception ignore) {}
            try { year.clear(); } catch (Exception ignore) {}
            try { typeDocument.clear(); } catch (Exception ignore) {}
            try { contentType.clear(); } catch (Exception ignore) {}
            try { size.clear(); } catch (Exception ignore) {}
            try { filename.clear(); } catch (Exception ignore) {}
            try { path.clear(); } catch (Exception ignore) {}
        }
    }
    private void clearGrid(boolean addresses, boolean contacts, boolean educations, boolean documents) {
        if (addresses) { try { addressList.clear(); gridAddress.setItems(addressList); } catch (Exception ignore) {} }
        if (contacts) { try { contactList.clear(); gridContacts.setItems(contactList); } catch (Exception ignore) {} }
        if (educations) { try { educationList.clear(); gridEducation.setItems(educationList); } catch (Exception ignore) {} }
        if (documents) { try { documentList.clear(); gridDocument.setItems(documentList); } catch (Exception ignore) {} }
    }

    private void save() {
        try {
//            if (!(this.auth.canCreate || this.auth.canUpdate)) {
//                Notification.show("Anda tidak punya izin menyimpan.", 2500, Notification.Position.MIDDLE);
//                return;
//            }

            // Ensure personData exists and fill from form
            if (personData == null) personData = new HrPerson();
            personData.setFirstName(firstName.getValue());
            personData.setMiddleName(middleName.getValue());
            personData.setLastName(lastName.getValue());
            personData.setKtpNumber(ktpNumber.getValue());
            personData.setPob(pob.getValue());
            personData.setDob(dob.getValue());
            personData.setGender(gender.getValue());
            personData.setNationality(nationality.getValue());
            personData.setReligion(religion.getValue());
            personData.setMarriage(marriage.getValue());

            // Get current app user for service context
            var user = currentUser.require();
            var appUser = commonService.getLoginUser(user.getUserId().toString());

            // Match PersonService signatures:
            // workingWithPerson(HrPerson, FwAppUser)
            personService.workingWithPerson(personData, appUser);
            // savePerson() with no args
            personService.savePerson();
            // saveAllInformation(Lists only, no person arg)
            personService.saveAllInformation(addressList, contactList, educationList, documentList);

            Notification.show("Profil berhasil disimpan.", 2500, Notification.Position.BOTTOM_CENTER);
            updateSaveButtonState();
        } catch (Exception ex) {
            Notification.show("Gagal menyimpan profil: " + ex.getMessage(), 3000, Notification.Position.MIDDLE);
        }
    }

    private String mimeFromFilename(String name) {
        if (name == null) return "image/png";
        String f = name.toLowerCase();
        if (f.endsWith(".jpg") || f.endsWith(".jpeg")) return "image/jpeg";
        if (f.endsWith(".gif")) return "image/gif";
        if (f.endsWith(".webp")) return "image/webp";
        if (f.endsWith(".bmp")) return "image/bmp";
        return "image/png";

    }

    private void reloadAddresses() {
        try {
            this.addressList = personService.getPersonAddresses();
            gridAddress.setItems(addressList);
        } catch (Exception ex) {
            String __msg = "unknown";
            if (ex != null && ex.getMessage() != null) __msg = ex.getMessage();
            Notification.show("Gagal memuat alamat: " + __msg, 2500, Notification.Position.MIDDLE);
        }
    }

    private void reloadContacts() {
        try {
            this.contactList = personService.getPersonContacts();
            gridContacts.setItems(contactList);
        } catch (Exception ex) {
            String __msg = "unknown";
            if (ex != null && ex.getMessage() != null) __msg = ex.getMessage();
            Notification.show("Gagal memuat kontak: " + __msg, 2500, Notification.Position.MIDDLE);
        }
    }

    private void reloadEducations() {
        try {
            this.educationList = personService.getPersonEducations();
            gridEducation.setItems(educationList);
        } catch (Exception ex) {
            String __msg = "unknown";
            if (ex != null && ex.getMessage() != null) __msg = ex.getMessage();
            Notification.show("Gagal memuat pendidikan: " + __msg, 2500, Notification.Position.MIDDLE);
        }
    }

}