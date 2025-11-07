package com.fusi24.pangreksa.web.view.employee;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.entity.*;
import com.fusi24.pangreksa.web.model.enumerate.*;
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.PersonService;
import com.vaadin.flow.component.AttachEvent;
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
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Route("my-profile")
@PageTitle("My Profile")
@PermitAll
public class MyProfileView extends Main {

    // Services
    private final CurrentUser currentUser;
    private final CommonService commonService;
    private final PersonService personService;

    // Person
    private Long lastLoadedPersonId;
    private HrPerson personData;

    // Constants copied from KaryawanBaruFormView
    private final String RESP_1 = "400px";
    private final String RESP_2 = "700px";
    private final int COL_1 = 3;
    private final int COL_2 = 4;
    private final String MAX_WIDTH = "50%";
    private final String MAX_WIDTH_GRID = "40rem";
    private final String HEIGHT_GRID = "250px";
    private FormLayout contactFormLayout;
    private FormLayout educationFormLayout;
    private FormLayout documentFormLayout;

    // Toolbars & containers
    private VerticalLayout body;
    private HorizontalLayout toolbarLayoutMaster;
    private HorizontalLayout toolbarLayoutDetail;
    private FormLayout personFormLayout;
    private TabSheet tabSheet;
    HorizontalLayout addressesLayout = new HorizontalLayout();
    HorizontalLayout contactsLayout  = new HorizontalLayout();
    HorizontalLayout educationLayout = new HorizontalLayout();
    HorizontalLayout documentsLayout = new HorizontalLayout();

    Button saveButton;
    Button clearButton;

    Button clearButtonOnTab;
    Button addButtonOnTab;

    private Image photoPreview;
    private VerticalLayout avatarLayout;

    // ===== Person fields (READ-ONLY here) =====
    private TextField ktpNumber = new TextField("KTP Number");
    private TextField firstName = new TextField("First Name");
    private TextField middleName = new TextField("Middle Name");
    private TextField lastName = new TextField("Last Name");
    private TextField pob = new TextField("Place of Birth");
    private DatePicker dob = new DatePicker("Date of Birth");
    private TextField genderText = new TextField("Gender");
    private TextField nationalityText = new TextField("Nationality");
    private TextField religionText = new TextField("Religion");
    private TextField marriageText = new TextField("Marriage Status");

    // ===== Address fields & grid =====
    private TextArea fullAddress = new TextArea("Full Address");
    private Checkbox isDefaultAddress = new Checkbox("Default Address");
    private Grid<HrPersonAddress> gridAddress = new Grid<>(HrPersonAddress.class, false);
    private List<HrPersonAddress> addressList = new ArrayList<>();
    private HrPersonAddress addressData;

    // ===== Contact fields & grid =====
    private ComboBox<ContactTypeEnum> typeContact = new ComboBox<>("Contact Type");
    private TextField stringValue = new TextField("Value");
    private TextField description = new TextField("Description");
    private Grid<HrPersonContact> gridContacts = new Grid<>(HrPersonContact.class, false);
    private List<HrPersonContact> contactList = new ArrayList<>();
    private HrPersonContact contactData;

    // ===== Education fields & grid =====
    private ComboBox<EducationTypeEnum> typeEducation = new ComboBox<>("Education Type");
    private TextField institution = new TextField("Institution");
    private TextField program = new TextField("Program");
    private NumberField score = new NumberField("Score");
    private DatePicker startDate = new DatePicker("Start Date");
    private DatePicker finishDate = new DatePicker("Finish Date");
    private TextField certificateTitle = new TextField("Certificate Title");
    private DatePicker certificateExpiration = new DatePicker("Certificate Expiration");
    private Grid<HrPersonEducation> gridEducation = new Grid<>(HrPersonEducation.class, false);
    private List<HrPersonEducation> educationList = new ArrayList<>();
    private HrPersonEducation educationData;

    // ===== Document fields & grid =====
    private TextField nameDocoument = new TextField("Name");
    private TextField descDocument = new TextField("Description");
    private TextField notes = new TextField("Notes");
    private NumberField year = new NumberField("Year");
    private ComboBox<DocumentTypeEnum> typeDocument = new ComboBox<>("Document Type");
    private ComboBox<ContentTypeEnum> contentType = new ComboBox<>("Content Type");
    private NumberField size = new NumberField("Size (bytes)");
    private TextField filename = new TextField("Filename");
    private TextField path = new TextField("Path");
    private Grid<HrPersonDocument> gridDocument = new Grid<>(HrPersonDocument.class, false);
    private List<HrPersonDocument> documentList = new ArrayList<>();
    private HrPersonDocument documentData;

    public MyProfileView(CurrentUser currentUser, CommonService commonService, PersonService personService) {
        this.currentUser = currentUser;
        this.commonService = commonService;
        this.personService = personService;

        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar("My Profile"));
        createBody();
        setListeners();
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Long pid = resolveCurrentPersonId();
        if (pid == null) {
            Notification.show("Tidak menemukan personId untuk user saat ini.", 4000, Notification.Position.MIDDLE);
            return;
        }
        UI.getCurrent().getSession().setAttribute("personId", pid);
        UI.getCurrent().getSession().setAttribute("hrPersonId", pid);
        loadPerson(pid);
    }

    private void createBody() {
        this.body = new VerticalLayout();
        body.setPadding(false);

        // Master toolbar (Save hanya untuk Address/Contact/Education/Document)
        toolbarLayoutMaster = new HorizontalLayout();
        toolbarLayoutMaster.setAlignItems(FlexComponent.Alignment.END);
        saveButton = new Button("Save");
        clearButton = new Button("Reset");
        toolbarLayoutMaster.add(clearButton, saveButton);
        toolbarLayoutMaster.setJustifyContentMode(FlexComponent.JustifyContentMode.END);

        // Detail toolbar (per-tab)
        toolbarLayoutDetail = new HorizontalLayout();
        toolbarLayoutDetail.setAlignItems(FlexComponent.Alignment.END);
        clearButtonOnTab = new Button("Clear");
        addButtonOnTab = new Button("Add");
        toolbarLayoutDetail.add(clearButtonOnTab, addButtonOnTab);
        toolbarLayoutDetail.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        toolbarLayoutDetail.setMaxWidth("200px");

        // Avatar (preview only; no upload in self-profile)
        avatarLayout = new VerticalLayout();
        avatarLayout.setWidth("180px");
        avatarLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        avatarLayout.getStyle().set("background-color", "#f0f0f0");
        Span avatarLabel = new Span("Photo");
        photoPreview = new Image();
        photoPreview.setVisible(false);
        avatarLayout.add(avatarLabel, photoPreview);

        // Person form (read-only)
        createPersonForm();

        // Tabs
        tabSheet = new TabSheet();
        tabSheet.add("Addresses", addressesLayout);
        tabSheet.add("Contacts", contactsLayout);
        tabSheet.add("Educations", educationLayout);
        tabSheet.add("Documents", documentsLayout);
        tabSheet.getStyle().setWidth("100%");

        // Build each tab content
        createAddressForm();
        createContactsForm();
        createEducationForm();
        createDocumentForm();

        // Master + Detail layouts
        HorizontalLayout masterLayout = new HorizontalLayout(avatarLayout, personFormLayout, toolbarLayoutMaster);
        masterLayout.setWidthFull();
        masterLayout.setHeight("350px");
        masterLayout.setAlignItems(FlexComponent.Alignment.START);

        HorizontalLayout detailLayout = new HorizontalLayout(tabSheet, toolbarLayoutDetail);
        detailLayout.setWidthFull();
        detailLayout.setAlignItems(FlexComponent.Alignment.START);

        SplitLayout splitLayout = new SplitLayout(masterLayout, detailLayout);
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
        personFormLayout = new FormLayout();
        personFormLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 3),
                new FormLayout.ResponsiveStep("700px", 4)
        );

        // Read-only
        for (TextField tf : new TextField[]{ktpNumber, firstName, middleName, lastName, pob, genderText, nationalityText, religionText, marriageText}) {
            tf.setReadOnly(true);
        }
        dob.setReadOnly(true);

        personFormLayout.add(ktpNumber, firstName, middleName, lastName, genderText, pob, dob, nationalityText, religionText, marriageText);
        personFormLayout.getStyle().setMaxWidth(MAX_WIDTH);
    }

    private void createAddressForm() {
        fullAddress.getStyle().setMinWidth("400px");
        fullAddress.getStyle().setMinHeight("200px");

        // Layout form
        FormLayout addressFormLayout = new FormLayout();
        addressFormLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep(RESP_1, COL_1),
                new FormLayout.ResponsiveStep(RESP_2, COL_2)
        );

        addressFormLayout.add(
                fullAddress,
                isDefaultAddress
        );
        addressFormLayout.getStyle().setMaxWidth(MAX_WIDTH);

        // Grid
        gridAddress.addColumn(HrPersonAddress::getFullAddress).setHeader("Full Address").setAutoWidth(true);
        gridAddress.addColumn(a -> a.getIsDefault() != null && a.getIsDefault() ? "Yes" : "No").setHeader("Default").setAutoWidth(true);
        gridAddress.addComponentColumn(address -> {
            HorizontalLayout actions = new HorizontalLayout();
            Button deleteBtn = new Button("Delete", e -> {
                addressList.remove(address);
                gridAddress.setItems(addressList);
            });
            Button editBtn = new Button("Edit", e -> populateAddressFields(address));
            actions.add(deleteBtn, editBtn);
            return actions;
        }).setAutoWidth(true);

        gridAddress.setItems(addressList);
        gridAddress.getStyle().setMaxWidth(MAX_WIDTH_GRID);
        gridAddress.setHeight(HEIGHT_GRID);

        addressesLayout.setWidthFull();
        addressesLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.ROW, LumoUtility.Gap.MEDIUM);
        addressesLayout.add(gridAddress, addressFormLayout);
    }

    private void createContactsForm() {
        typeContact.setItems(ContactTypeEnum.values());

        contactFormLayout = new FormLayout();
        contactFormLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep(RESP_1, COL_1),
                new FormLayout.ResponsiveStep(RESP_2, COL_2)
        );

        contactFormLayout.add(typeContact, stringValue, description);
        contactFormLayout.getStyle().setMaxWidth(MAX_WIDTH);

        gridContacts.addColumn(c -> c.getType() != null ? c.getType().name() : "").setHeader("Type").setAutoWidth(true);
        gridContacts.addColumn(HrPersonContact::getStringValue).setHeader("Value").setAutoWidth(true);
        gridContacts.addColumn(HrPersonContact::getDescription).setHeader("Description").setAutoWidth(true);
        gridContacts.addComponentColumn(contact -> {
            HorizontalLayout actions = new HorizontalLayout();
            Button deleteBtn = new Button("Delete", e -> {
                contactList.remove(contact);
                gridContacts.setItems(contactList);
            });
            Button editBtn = new Button("Edit", e -> populateContactFields(contact));
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

        educationFormLayout = new FormLayout();
        educationFormLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep(RESP_1, COL_1),
                new FormLayout.ResponsiveStep(RESP_2, COL_2)
        );

        educationFormLayout.add(typeEducation, institution, program, score, startDate, finishDate, certificateTitle, certificateExpiration);
        educationFormLayout.getStyle().setMaxWidth(MAX_WIDTH);

        gridEducation.addColumn(e -> e.getType() != null ? e.getType().name() : "").setHeader("Type").setAutoWidth(true);
        gridEducation.addColumn(HrPersonEducation::getInstitution).setHeader("Institution").setAutoWidth(true);
        gridEducation.addColumn(HrPersonEducation::getProgram).setHeader("Program").setAutoWidth(true);
        gridEducation.addColumn(HrPersonEducation::getScore).setHeader("Score").setAutoWidth(true);
        gridEducation.addColumn(HrPersonEducation::getStartDate).setHeader("Start Date").setAutoWidth(true);
        gridEducation.addColumn(HrPersonEducation::getFinishDate).setHeader("Finish Date").setAutoWidth(true);

        gridEducation.addComponentColumn(education -> {
            HorizontalLayout actions = new HorizontalLayout();
            Button deleteBtn = new Button("Delete", e -> {
                educationList.remove(education);
                gridEducation.setItems(educationList);
            });
            Button editBtn = new Button("Edit", e -> populateEducationFields(education));
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
        typeDocument.setItems(DocumentTypeEnum.values());
        contentType.setItems(ContentTypeEnum.values());

        documentFormLayout = new FormLayout();
        documentFormLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep(RESP_1, COL_1),
                new FormLayout.ResponsiveStep(RESP_2, COL_2)
        );

        documentFormLayout.add(nameDocoument, descDocument, notes, year, typeDocument, contentType, size, filename, path);
        documentFormLayout.getStyle().setMaxWidth(MAX_WIDTH);

        gridDocument.addColumn(HrPersonDocument::getName).setHeader("Name").setAutoWidth(true);
        gridDocument.addColumn(HrPersonDocument::getDescription).setHeader("Description").setAutoWidth(true);
        gridDocument.addColumn(HrPersonDocument::getYear).setHeader("Year").setAutoWidth(true);
        gridDocument.addColumn(d -> d.getType() != null ? d.getType().name() : "").setHeader("Doc Type").setAutoWidth(true);
        gridDocument.addComponentColumn(document -> {
            HorizontalLayout actions = new HorizontalLayout();
            Button deleteBtn = new Button("Delete", e -> {
                documentList.remove(document);
                gridDocument.setItems(documentList);
            });
            Button editBtn = new Button("Edit", e -> populateDocumentFields(document));
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

    private void setListeners() {
        clearButton.addClickListener(e -> {
            clearForm(true, true, true, true, true);
            clearGrid(true, true, true, true);
        });

        clearButtonOnTab.addClickListener(e -> {
            int tabNo = tabSheet.getSelectedIndex();
            switch (tabNo) {
                case 0 -> clearForm(false, true, false, false, false); // Address
                case 1 -> clearForm(false, false, true, false, false); // Contact
                case 2 -> clearForm(false, false, false, true, false); // Education
                case 3 -> clearForm(false, false, false, false, true); // Document
            }
        });

        addButtonOnTab.addClickListener(e -> {
            int tabNo = tabSheet.getSelectedIndex();
            switch (tabNo) {
                case 0 -> {
                    HrPersonAddress address = this.addressData != null ? this.addressData : new HrPersonAddress();
                    address.setFullAddress(fullAddress.getValue());
                    address.setIsDefault(Boolean.TRUE.equals(isDefaultAddress.getValue()));

                    addressList.add(address);
                    gridAddress.setItems(addressList);
                    clearForm(false, true, false, false, false);
                    this.addressData = null;
                }
                case 1 -> {
                    HrPersonContact contact = this.contactData != null ? this.contactData : new HrPersonContact();
                    contact.setType(typeContact.getValue());
                    contact.setStringValue(stringValue.getValue());
                    contact.setDescription(description.getValue());

                    contactList.add(contact);
                    gridContacts.setItems(contactList);
                    clearForm(false, false, true, false, false);
                    this.contactData = null;
                }
                case 2 -> {
                    HrPersonEducation education = this.educationData != null ? this.educationData : new HrPersonEducation();
                    education.setType(typeEducation.getValue());
                    education.setInstitution(institution.getValue());
                    education.setProgram(program.getValue());
                    education.setScore(score.getValue() != null ? BigDecimal.valueOf(score.getValue()) : null);
                    education.setStartDate(startDate.getValue());
                    education.setFinishDate(finishDate.getValue());
                    education.setCertificateTitle(certificateTitle.getValue());
                    education.setCertificateExpiration(certificateExpiration.getValue());

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

                    documentList.add(document);
                    gridDocument.setItems(documentList);
                    clearForm(false, false, false, false, true);
                    this.documentData = null;
                }
            }
        });

        saveButton.addClickListener(e -> saveLists());
    }

    private void saveLists() {
        try {
            if (lastLoadedPersonId == null) {
                Notification.show("Person belum dimuat.", 4000, Notification.Position.MIDDLE);
                return;
            }
            // Lock service to this person & persist lists
            personService.getPerson(lastLoadedPersonId);
            personService.saveAllInformation(addressList, contactList, educationList, documentList);

            // Reload fresh from DB
            addressList = personService.getPersonAddresses();
            contactList = personService.getPersonContacts();
            educationList = personService.getPersonEducations();
            documentList = personService.getPersonDocuments();

            gridAddress.setItems(addressList);
            gridContacts.setItems(contactList);
            gridEducation.setItems(educationList);
            gridDocument.setItems(documentList);

            if (gridAddress.getDataProvider()!=null) gridAddress.getDataProvider().refreshAll();
            if (gridContacts.getDataProvider()!=null) gridContacts.getDataProvider().refreshAll();
            if (gridEducation.getDataProvider()!=null) gridEducation.getDataProvider().refreshAll();
            if (gridDocument.getDataProvider()!=null) gridDocument.getDataProvider().refreshAll();

            Notification.show("Perubahan disimpan.", 2500, Notification.Position.BOTTOM_END);
        } catch (Exception ex) {
            Notification.show("Gagal menyimpan: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
        }
    }

    private void loadPerson(Long personId) {
        this.lastLoadedPersonId = personId;
        try {
            this.personData = personService.getPerson(personId);

            // Fill read-only fields
            set(tf(ktpNumber), s(personData, "getKtpNumber"));
            set(tf(firstName), s(personData, "getFirstName"));
            set(tf(middleName), s(personData, "getMiddleName"));
            set(tf(lastName), s(personData, "getLastName"));
            set(tf(pob), s(personData, "getBirthPlace"));
            dob.setValue(ld(personData, "getBirthDate"));
            set(tf(genderText), s(personData, "getGender"));
            set(tf(nationalityText), s(personData, "getNationality"));
            set(tf(religionText), s(personData, "getReligion"));
            set(tf(marriageText), s(personData, "getMarriageStatus"));

            // Lists
            addressList = personService.getPersonAddresses();
            contactList = personService.getPersonContacts();
            educationList = personService.getPersonEducations();
            documentList = personService.getPersonDocuments();

            gridAddress.setItems(addressList);
            gridContacts.setItems(contactList);
            gridEducation.setItems(educationList);
            gridDocument.setItems(documentList);

            if (gridAddress.getDataProvider()!=null) gridAddress.getDataProvider().refreshAll();
            if (gridContacts.getDataProvider()!=null) gridContacts.getDataProvider().refreshAll();
            if (gridEducation.getDataProvider()!=null) gridEducation.getDataProvider().refreshAll();
            if (gridDocument.getDataProvider()!=null) gridDocument.getDataProvider().refreshAll();

        } catch (Exception ex) {
            Notification.show("Gagal memuat profil: " + (ex.getMessage()==null?"Unknown":ex.getMessage()),
                    4500, Notification.Position.MIDDLE);
        }
    }

    // ===== Populate helpers =====
    private void populateAddressFields(HrPersonAddress a) {
        this.addressData = a;
        fullAddress.setValue(a.getFullAddress() != null ? a.getFullAddress() : "");
        isDefaultAddress.setValue(Boolean.TRUE.equals(a.getIsDefault()));
    }
    private void populateContactFields(HrPersonContact c) {
        this.contactData = c;
        typeContact.setValue(c.getType());
        stringValue.setValue(c.getStringValue() != null ? c.getStringValue() : "");
        description.setValue(c.getDescription() != null ? c.getDescription() : "");
    }
    private void populateEducationFields(HrPersonEducation e) {
        this.educationData = e;
        typeEducation.setValue(e.getType());
        institution.setValue(e.getInstitution() != null ? e.getInstitution() : "");
        program.setValue(e.getProgram() != null ? e.getProgram() : "");
        score.setValue(e.getScore() != null ? e.getScore().doubleValue() : null);
        startDate.setValue(e.getStartDate());
        finishDate.setValue(e.getFinishDate());
        certificateTitle.setValue(e.getCertificateTitle() != null ? e.getCertificateTitle() : "");
        certificateExpiration.setValue(e.getCertificateExpiration());
    }
    private void populateDocumentFields(HrPersonDocument d) {
        this.documentData = d;
        nameDocoument.setValue(d.getName() != null ? d.getName() : "");
        descDocument.setValue(d.getDescription() != null ? d.getDescription() : "");
        notes.setValue(d.getNotes() != null ? d.getNotes() : "");
        year.setValue(d.getYear() != null ? d.getYear().doubleValue() : null);
        typeDocument.setValue(d.getType());
        contentType.setValue(d.getContentType());
        size.setValue(d.getSize() != null ? d.getSize().doubleValue() : null);
        filename.setValue(d.getFilename() != null ? d.getFilename() : "");
        path.setValue(d.getPath() != null ? d.getPath() : "");
    }

    private void clearForm(boolean all, boolean address, boolean contact, boolean education, boolean document) {
        if (all || address) {
            fullAddress.clear();
            isDefaultAddress.clear();
            addressData = null;
        }
        if (all || contact) {
            typeContact.clear();
            stringValue.clear();
            description.clear();
            contactData = null;
        }
        if (all || education) {
            typeEducation.clear();
            institution.clear();
            program.clear();
            score.clear();
            startDate.clear();
            finishDate.clear();
            certificateTitle.clear();
            certificateExpiration.clear();
            educationData = null;
        }
        if (all || document) {
            nameDocoument.clear();
            descDocument.clear();
            notes.clear();
            year.clear();
            typeDocument.clear();
            contentType.clear();
            size.clear();
            filename.clear();
            path.clear();
            documentData = null;
        }
    }

    private void clearGrid(boolean address, boolean contact, boolean education, boolean document) {
        if (address) gridAddress.setItems(new ArrayList<>());
        if (contact) gridContacts.setItems(new ArrayList<>());
        if (education) gridEducation.setItems(new ArrayList<>());
        if (document) gridDocument.setItems(new ArrayList<>());
    }

    // ===== Utils =====
    private static String s(Object bean, String getter) {
        try {
            if (bean == null) return "";
            Method m = bean.getClass().getMethod(getter);
            Object v = m.invoke(bean);
            return v == null ? "" : String.valueOf(v);
        } catch (Exception ignore) {
            return "";
        }
    }
    private static void set(TextField tf, String val) { tf.setValue(val == null ? "" : val); }
    private static TextField tf(TextField tf) { return tf; }
    private static LocalDate ld(Object bean, String getter) {
        try {
            if (bean == null) return null;
            Method m = bean.getClass().getMethod(getter);
            Object v = m.invoke(bean);
            if (v instanceof LocalDate ld) return ld;
            if (v instanceof java.util.Date d) return new java.sql.Date(d.getTime()).toLocalDate();
            return null;
        } catch (Exception ignore) { return null; }
    }

    // Robust resolver: session -> FwAppUser getters/nested -> optional personService methods
    private Long resolveCurrentPersonId() {
        try {
            Object sess = UI.getCurrent().getSession().getAttribute("personId");
            if (sess instanceof Number n) return n.longValue();
            if (sess instanceof String s) try { return Long.parseLong(s); } catch (Exception ignore) {}
            Object sessHr = UI.getCurrent().getSession().getAttribute("hrPersonId");
            if (sessHr instanceof Number n2) return n2.longValue();
            if (sessHr instanceof String s2) try { return Long.parseLong(s2); } catch (Exception ignore) {}

            var user = currentUser.require();
            var appUser = commonService.getLoginUser(user.getUserId().toString());
            Long id = tryLongGetter(appUser, "getHrPersonId"); if (id != null) return id;
            id = tryLongGetter(appUser, "getPersonId"); if (id != null) return id;
            id = tryLongGetter(appUser, "getEmployeeId"); if (id != null) return id;
            id = tryNestedId(appUser, "getHrPerson", "getId"); if (id != null) return id;
            id = tryNestedId(appUser, "getPerson", "getId"); if (id != null) return id;

            Long viaService = tryServiceResolve(user.getUserId().toString());
            if (viaService != null) return viaService;
        } catch (Exception ignore) {}
        return null;
    }
    private Long tryLongGetter(Object target, String methodName) {
        try {
            Method m = target.getClass().getMethod(methodName);
            Object v = m.invoke(target);
            if (v instanceof Number n) return n.longValue();
        } catch (Exception ignore) {}
        return null;
    }
    private Long tryNestedId(Object target, String outerGetter, String innerGetter) {
        try {
            Method outer = target.getClass().getMethod(outerGetter);
            Object obj = outer.invoke(target);
            if (obj != null) {
                Method inner = obj.getClass().getMethod(innerGetter);
                Object v = inner.invoke(obj);
                if (v instanceof Number n) return n.longValue();
            }
        } catch (Exception ignore) {}
        return null;
    }
    private Long tryServiceResolve(String uid) {
        try {
            for (String name : new String[]{"findPersonIdByUserId","getPersonIdByUserId","findHrPersonIdByUserId","getHrPersonIdByUserId","getPersonIdByAccountId"}) {
                try {
                    Method m = personService.getClass().getMethod(name, String.class);
                    Object v = m.invoke(personService, uid);
                    if (v instanceof Number n) return n.longValue();
                } catch (NoSuchMethodException ignore) {}
            }
        } catch (Exception ignore) {}
        return null;
    }
}
