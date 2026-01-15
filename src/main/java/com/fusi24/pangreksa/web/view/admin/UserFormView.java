package com.fusi24.pangreksa.web.view.admin;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.*;
import com.fusi24.pangreksa.web.service.AdminService;
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.PersonService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Route("user-creation-form-access")
@PageTitle("User Form")
@Menu(order = 34, icon = "vaadin:user-card", title = "User Form")
@RolesAllowed("USER_CRT")
//@PermitAll // When security is enabled, allow all authenticated users
public class UserFormView extends Main implements HasUrlParameter<Long> {
    private static final long serialVersionUID = 34L;
    private static final Logger log = LoggerFactory.getLogger(UserFormView.class);
    private final CurrentUser currentUser;
    private final CommonService commonService;
    private final AdminService adminService;
    private final PersonService personService;
    private Authorization auth;

    public static final String VIEW_NAME = "User Form";

    private VerticalLayout body;
    private TabSheet tabSheet;

    private FormLayout apUserFormLayout;
    private HorizontalLayout responsibilityLayout = new HorizontalLayout();

    private HorizontalLayout toolbarLayoutMaster;
    private HorizontalLayout toolbarLayoutDetail;

    Button saveButton;
    Button clearButton;

    Button clearButtonOnTab;
    Button saveButtonOnTab;

    private final  String RESP_1 = "400px";
    private final  String RESP_2 = "700px";
    private final int COL_1 = 3;
    private final int COL_2 = 4;
    private final String MAX_WIDTH = "50%";
    private final String MAX_WIDTH_GRID = "40rem";
    private final String HEIGHT_GRID = "250px";

    private FwAppUser appUser;

    // variable for User
    private final TextField username = new TextField("Username");
    private final PasswordField password = new PasswordField("Password");
    private final PasswordField passwordConfirmation = new PasswordField("Password Confirmation");
    private final EmailField email = new EmailField("Email");
    private final TextField nickname = new TextField("Nickname");
    private final Checkbox isActive = new Checkbox("Active");
    private ComboBox<HrPerson> personCombo = new ComboBox<>();
    private ComboBox<HrCompany> companyCombo = new ComboBox<>();

    //variables for Responsibilities
    ComboBox<FwResponsibilities> responsibilityDropdown;

    // Grids
    private Grid<FwAppuserResp> gridAppUserResp;

    //List
    List<FwAppuserResp> appUserRespList = new ArrayList<>();;

    public UserFormView(CurrentUser currentUser, CommonService commonService, AdminService adminService, PersonService personService) {
        this.currentUser = currentUser;
        this.commonService = commonService;
        this.adminService = adminService;
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
            responsibilityDropdown.setEnabled(false);
            personCombo.setEnabled(false);
            companyCombo.setEnabled(false);
        }
        if(!this.auth.canCreate){
            saveButton.setEnabled(false);
            saveButtonOnTab.setEnabled(false);
        }
        if(!this.auth.canEdit){
            username.setReadOnly(true);
            password.setReadOnly(false);
            passwordConfirmation.setReadOnly(false);
            email.setReadOnly(false);
            nickname.setReadOnly(false);
            isActive.setReadOnly(false);
            personCombo.setReadOnly(false);
            companyCombo.setReadOnly(false);
        }
    }

    private void createBody() {
        this.body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);

        // Inisiasi toolbar Master
        toolbarLayoutMaster = new HorizontalLayout();
        toolbarLayoutMaster.setAlignItems(FlexComponent.Alignment.END);

        saveButton = new Button("Simpan");
        clearButton = new Button("Reset");

        toolbarLayoutMaster.add(clearButton, saveButton);
        toolbarLayoutMaster.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        toolbarLayoutMaster.setWidthFull();

        createAppUserForm();

        // Inisiasi toolbar Detail
        toolbarLayoutDetail = new HorizontalLayout();
        toolbarLayoutDetail.setAlignItems(FlexComponent.Alignment.END);

        clearButtonOnTab = new Button("Clear");
        saveButtonOnTab = new Button("Tambah");

        toolbarLayoutDetail.add(clearButtonOnTab, saveButtonOnTab);
        toolbarLayoutDetail.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        toolbarLayoutDetail.setMaxWidth("200px");

        this.tabSheet = new TabSheet();
        tabSheet.getStyle().setWidth("100%");

        tabSheet.add("Hak Akses", responsibilityLayout);

        HorizontalLayout masterLayout = new HorizontalLayout(apUserFormLayout,toolbarLayoutMaster);
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

        createResponsibilityForm();

        body.add(splitLayout);
        body.addClassNames(LumoUtility.Gap.MEDIUM);
        body.setHeightFull();
        body.getStyle().setFlexGrow("1");

        this.setHeightFull();
        add(body);
    }

    private void createAppUserForm() {
        passwordConfirmation.setRevealButtonVisible(false);

        // Person ComboBox
        this.personCombo = new ComboBox<>("Person");
        personCombo.addClassName("no-dropdown-icon");
        personCombo.setItemLabelGenerator(p -> p.getFirstName() + " " + (p.getLastName() != null ? p.getLastName() : ""));
        personCombo.setPlaceholder("Tipe untuk dicari");
        personCombo.setClearButtonVisible(true);

        personCombo.setItems(query -> {
            String filter = query.getFilter().orElse("");
            int offset = query.getOffset();
            int limit = query.getLimit(); // not used in this example, but can be used for pagination
            log.debug("Searching persons with filter: {}", filter);
            return personService.findPersonByKeyword(filter).stream();
        } );
        // Company ComboBox
        this.companyCombo = new ComboBox<>("Perusahaan");
        companyCombo.addClassName("no-dropdown-icon");
        companyCombo.setItemLabelGenerator(HrCompany::getName);
        companyCombo.setPlaceholder("Tipe untuk dicari");
        companyCombo.setClearButtonVisible(true);

        companyCombo.setItems(query -> {
            String filter = query.getFilter().orElse("");
            int offset = query.getOffset();
            int limit = query.getLimit(); // not used in this example, but can be used for pagination
            log.debug("Searching Company with filter: {}", filter);
            return personService.findCompanyByKeyword(filter).stream();
        } );

        // Create the FormLayout
        apUserFormLayout = new FormLayout();
        apUserFormLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("500px", 3)
        );

        // Add all fields to the form layout
        apUserFormLayout.add(
                username,
                password,
                passwordConfirmation,
                nickname,
                email,
                isActive,
                personCombo,
                companyCombo
        );
    }

    private void createResponsibilityForm() {
        this.responsibilityDropdown = new ComboBox<>("Responsibility");
        responsibilityDropdown.setItemLabelGenerator(FwResponsibilities::getLabel);
        responsibilityDropdown.setClearButtonVisible(true);

        responsibilityDropdown.setItems(adminService.findActiveResponsibilities());

        // Create address form layout
        FormLayout responsibilityFromLayout = new FormLayout();
        responsibilityFromLayout.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep(RESP_1, COL_1),
                new FormLayout.ResponsiveStep(RESP_2, COL_2)
        );

        // Add address fields to the form layout
        responsibilityFromLayout.add(
                responsibilityDropdown
        );
        responsibilityFromLayout.getStyle().setMaxWidth(MAX_WIDTH);

        this.gridAppUserResp = new Grid<>(FwAppuserResp.class, false);
        // add column responsibility label
        gridAppUserResp.addColumn(appUserResp -> appUserResp.getResponsibility().getLabel())
                .setHeader("Hak Akses")
                .setSortable(true)
                .setKey("responsibility");
        // Add action column
        gridAppUserResp.addComponentColumn(appUserResp -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            Button deleteBtn = new Button(VaadinIcon.CLOSE_CIRCLE_O.create());
            deleteBtn.addClickListener(e -> {
                appUserRespList.remove(appUserResp);
                gridAppUserResp.setItems(appUserRespList);

                if (appUserResp.getId() != null)
                    adminService.deleteAppUserResp(appUserResp);
            });

            if(!this.auth.canDelete) {
                deleteBtn.setEnabled(false);
            }

            actions.add(deleteBtn);
            return actions;
        }).setHeader("Aksi").setAutoWidth(true);


        gridAppUserResp.setItems(appUserRespList);
        gridAppUserResp.getStyle().setMaxWidth(MAX_WIDTH_GRID);
        gridAppUserResp.setHeight(HEIGHT_GRID);

        responsibilityLayout.addClassNames(LumoUtility.Display.FLEX, LumoUtility.FlexDirection.ROW, LumoUtility.Gap.MEDIUM);
        responsibilityLayout.add(gridAppUserResp, responsibilityFromLayout);
    }

    private void clearForm(boolean form, boolean responsibilities) {
        if (form){
            username.clear();
            password.clear();
            passwordConfirmation.clear();
            email.clear();
            nickname.clear();
            isActive.setValue(false);
            personCombo.clear();
            companyCombo.clear();
        }
        if (responsibilities) {
            responsibilityDropdown.clear();
        }
    }

    private void clearGrid(boolean form, boolean responsibilities) {
        if (form) {
            this.appUser = new FwAppUser();
            clearForm(true, false);
        }
        if (responsibilities) {
            appUserRespList = new ArrayList<>();
            gridAppUserResp.setItems(appUserRespList);
        }
    }

    @Override
    public void setParameter(BeforeEvent beforeEvent, @OptionalParameter Long iddb) {
        if(!this.auth.canEdit) {
            Notification.show("You do not have permission to edit this person.");
        }

        if (iddb != null && this.auth.canEdit) {
            log.debug("Loading person with ID: {}", iddb);

            this.appUser = adminService.findAppUserById(iddb);

            // Populate appUser Fields
            this.username.setValue(this.appUser.getUsername());
//            this.password.setValue(this.appUser.getPassword());
//            this.passwordConfirmation.setValue(this.appUser.getPassword());
            this.email.setValue(this.appUser.getEmail());
            this.nickname.setValue(this.appUser.getNickname());
            this.isActive.setValue(this.appUser.getIsActive() != null ? this.appUser.getIsActive() : false);
            this.personCombo.setValue(this.appUser.getPerson() != null ? this.appUser.getPerson() : null);
            this.companyCombo.setValue(this.appUser.getCompany() != null ? this.appUser.getCompany() : null);

            // Load responsibilities
            this.appUserRespList = adminService.findAppUserRespByUser(this.appUser);


            // Populate the form fields with the loaded data
            this.gridAppUserResp.setItems(this.appUserRespList);

            log.debug("loaded User: {} {}", this.appUser.getUserId(), this.appUserRespList.size());
        } else {
            clearForm(true, true);
            clearGrid(true, true);
        }
    }

    private void setListener() {
        saveButton.addClickListener(e -> {
            this.appUser = this.appUser != null ? this.appUser : new FwAppUser();

            if (this.appUser.getPasswordHash() == null) {
                if (passwordConfirmation.isEmpty()){
                    Notification.show("You need to set a password");
                    return;
                } else if (username.isEmpty() || password.isEmpty() || passwordConfirmation.isEmpty()) {
                    Notification.show("Username, Password and Password Confirmation are required");
                    return;
                } else if ( !password.getValue().equals(passwordConfirmation.getValue()) ) {
                    Notification.show("Password and Password Confirmation do not match");
                    return;
                }
            }

            // Save the user
            appUser.setUsername(username.getValue());
            // Update password for existing user only if password field is not empty
            if(this.appUser.getPasswordHash() != null && !passwordConfirmation.isEmpty()) {
                appUser.setPassword(password.getValue());
                appUser.setPasswordHash(password.getValue());
            } else
            // Update password for new user
            {
                appUser.setPassword(passwordConfirmation.getValue());
                appUser.setPasswordHash(passwordConfirmation.getValue());
            }
            appUser.setEmail(email.getValue());
            appUser.setNickname(nickname.getValue());
            appUser.setIsActive(isActive.getValue());
            appUser.setPerson(personCombo.getValue());
            appUser.setCompany(companyCombo.getValue());

            this.appUser = adminService.saveAppUser(appUser, currentUser.require());
            this.appUserRespList = gridAppUserResp.getListDataView().getItems().toList();

            // Save responsibilities
            for (FwAppuserResp resp : appUserRespList) {
                resp.setAppuser(this.appUser);
                adminService.saveAppUserResp(resp, currentUser.require());
            }

            Notification.show("User and Responsibilities saved successfully");
            clearGrid(true, true);
            clearForm(true, true);
        });

        clearButton.addClickListener(e -> {
            clearGrid(true, true);
            clearForm(true, true);
        });

        clearButtonOnTab.addClickListener( e -> {
            int tabNo = tabSheet.getSelectedIndex();
            switch (tabNo) {
                case 0 -> clearForm(false, true); // Responsibilities
            }
        });

        saveButtonOnTab.addClickListener( e -> {
            int tabNo = tabSheet.getSelectedIndex();
            switch (tabNo) {
                case 0 -> {

                    //find in gridAppUserResp, if there is already a responsibility with the same id, do not add it again
                    if (appUserRespList.stream().anyMatch(resp -> resp.getResponsibility().getId().equals(responsibilityDropdown.getValue().getId()))) {
                        Notification.show("Responsibility already exists in the list");
                        return;
                    }

                    FwAppuserResp appUserResp = new FwAppuserResp();
                    appUserResp.setResponsibility(responsibilityDropdown.getValue());

                    appUserRespList.add(appUserResp);
                    gridAppUserResp.setItems(appUserRespList);

                    clearForm(false, true);
                }
            }
        });
    }
}
