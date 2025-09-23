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
import com.vaadin.flow.router.*;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.streams.InMemoryUploadHandler;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.theme.lumo.LumoUtility;
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

    public KaryawanBaruFormView(CurrentUser currentUser, CommonService commonService, PersonService personService) {
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

    private void setAuthorization(){
        if(!this.auth.canView){
            // User does not have permission to view this page
        }

        if(!this.auth.canCreate){
            demoButton.setEnabled(false);
            saveButton.setEnabled(false);
            saveButtonOnTab.setEnabled(false);
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

        // Add all fields to the form layout
        personFormLayout.add(
                ktpNumber,
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
        demoButton.addClickListener( e -> {
            if(!this.auth.canCreate){
                return;
            }
            populateDemoDate();
        });

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

                    HrPersonAddress address = this.addressData != null ? this.addressData : new HrPersonAddress();
                    address.setFullAddress(fullAddress.getValue());
                    address.setIsDefault(isDefaultAddress.getValue());
                    // address.setPerson(person);

                    addressList.add(address);
                    gridAddress.setItems(addressList);

                    clearForm(false, true, false, false, false);
                    this.addressData = null;
                }
                case 1 -> {
                    HrPersonContact contact = this.contactData != null ? this.contactData : new HrPersonContact();
                    contact.setDesignation(designation.getValue());
                    contact.setRelationship(relationship.getValue());
                    contact.setStringValue(stringValue.getValue());
                    contact.setType(typeContact.getValue());
                    contact.setDescription(description.getValue());
                    contact.setIsDefault(isDefaultContact.getValue());
                    // contact.setPerson(person);

                    contactList.add(contact);
                    gridContacts.setItems(contactList);
                    clearForm(false, false, true, false, false);
                    this.contactData = null;
                }
                case 2 -> {
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
        });

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

    private void save() {
        // Create HrPerson
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

        // Upload file if available
        if (uploadedImageBytes.get() != null) {
            // Delete
            if (person.getPhotoFilename() != null && !person.getPhotoFilename().isEmpty()) {
                personService.deletePhoto(person.getPhotoFilename());
            }
            // Upload
            String filename = "photo_" + UUID.randomUUID() + ".png";
            personService.uploadPhoto(uploadedImageBytes.get(), filename);
            person.setPhotoFilename(filename);
        }

        var user = currentUser.require();
        FwAppUser appUser = commonService.getLoginUser(user.getUserId().toString());

        personService.workingWithPerson(person, appUser);
        personService.savePerson();

        personService.saveAllInformation(
                addressList,
                contactList,
                educationList,
                documentList
        );

        Notification.show("Data saved successfully");

        clearForm(true,true, true, true, true);
        clearGrid(true, true, true, true);
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

            log.debug("loaded person: {} {} {} {} {}", this.personData.getFirstName(), addressList.size(), contactList.size(), educationList.size(), documentList.size());
            demoButton.setEnabled(false);
        } else {
            clearForm(true, true, true, true, true);
            clearGrid(true, true, true, true);
        }
    }
}
