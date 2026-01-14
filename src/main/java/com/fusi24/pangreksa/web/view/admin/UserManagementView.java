package com.fusi24.pangreksa.web.view.admin;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.FwAppUser;
import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrPerson;
import com.fusi24.pangreksa.web.service.AdminService;
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.PersonService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.PasswordField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Route("user-management-page-access")
@PageTitle("User Management")
@Menu(order = 33, icon = "vaadin:user", title = "User Management")
@RolesAllowed("USERS_MGT")
//@PermitAll // When security is enabled, allow all authenticated users
public class UserManagementView extends Main {
    private static final long serialVersionUID = 33L;
    private static final Logger log = LoggerFactory.getLogger(UserManagementView.class);
    private final CurrentUser currentUser;
    private final CommonService commonService;
    private final AdminService adminService;
    private final PersonService personService;
    private Authorization auth;

    public static final String VIEW_NAME = "User Management";
    public static final String ROUTE_EDIT = "user-creation-form-access/";

    private List<FwAppUser> appUserList;
    private Grid<FwAppUser> gridUsers;
    private Button addButton;
    private Button populateButton;
    private Button saveButton;
    private TextField searchField;


    private VerticalLayout body;

    public UserManagementView(CurrentUser currentUser, CommonService commonService, AdminService adminService, PersonService personService) {
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
        setHeightFull();
        add(new ViewToolbar(VIEW_NAME));
        createBody();

        setListener();
        setAuthorization();
    }

    private void setAuthorization(){
        if(!this.auth.canView){
            populateButton.setEnabled(false);
        }
        if(!this.auth.canCreate){
            addButton.setEnabled(false);
            saveButton.setEnabled(false);
        }
    }

    private void createBody() {
        this.body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);
        body.setSizeFull();
        searchField = new TextField("Search Filter");
        addButton = new Button("Tambah User");
        populateButton = new Button("Muat Data");
        saveButton = new Button("Simpan");

        HorizontalLayout leftLayout = new HorizontalLayout(searchField, populateButton);
        leftLayout.setSpacing(true);
        leftLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.add(leftLayout);
        row.add(saveButton, addButton);
        row.setAlignItems(HorizontalLayout.Alignment.BASELINE);
        row.expand(leftLayout); // leftLayout takes all available space, addButton stays right

        gridUsers = new Grid<>(FwAppUser.class, false);

        // Username editable (inline TextField)
        gridUsers.addColumn(new ComponentRenderer<>(user -> {
            TextField usernameField = new TextField();
            usernameField.setValue(user.getUsername() != null ? user.getUsername() : "");
            usernameField.addValueChangeListener(e -> {
                user.setUsername(e.getValue());
                // Optionally set an isEdit flag here
            });
            return usernameField;
        })).setHeader("Username").setSortable(true);
        gridUsers.setSizeFull();

        // Email editable (inline TextField)
        gridUsers.addColumn(new ComponentRenderer<>(user -> {
            EmailField emailField = new EmailField();
            emailField.setValue(user.getEmail() != null ? user.getEmail() : "");
            emailField.addValueChangeListener(e -> {
                user.setEmail(e.getValue());
                // Optionally set an isEdit flag here
            });
            return emailField;
        })).setHeader("Email").setSortable(true);

        // Person column: inline editable ComboBox with search
        gridUsers.addColumn(new ComponentRenderer<>(user -> {
            ComboBox<HrPerson> personCombo = new ComboBox<>();
            personCombo.setItemLabelGenerator(p -> p.getFirstName() + " " + (p.getLastName() != null ? p.getLastName() : ""));
            personCombo.setPlaceholder("Unassigned");
            personCombo.setClearButtonVisible(true);

            personCombo.setItems(query -> {
                String filter = query.getFilter().orElse("");
                int offset = query.getOffset();
                int limit = query.getLimit(); // not used in this example, but can be used for pagination
                log.debug("Searching persons with filter: {}", filter);
                return personService.findPersonByKeyword(filter).stream();
            } );

            if (user.getPerson() != null) {
                personCombo.setValue(user.getPerson());
            }

            personCombo.addValueChangeListener(e -> {
                if (e.getValue() != null) {
                    user.setPerson(e.getValue());
                }
            });

            return personCombo;
        })).setHeader("Karyawan").setSortable(true);

        // Company column: inline editable ComboBox with search
        gridUsers.addColumn(new ComponentRenderer<>(user -> {
            ComboBox<HrCompany> companyCombo = new ComboBox<>();
            companyCombo.setItemLabelGenerator(HrCompany::getName);
            companyCombo.setPlaceholder("Pilih perusahaan");
            companyCombo.setClearButtonVisible(true);

            companyCombo.setItems(query -> {
                String filter = query.getFilter().orElse("");
                int offset = query.getOffset();
                int limit = query.getLimit(); // not used in this example, but can be used for pagination
                log.debug("Searching Company with filter: {}", filter);
                return personService.findCompanyByKeyword(filter).stream();
            } );

            if (user.getCompany() != null) {
                companyCombo.setValue(user.getCompany());
            }

            companyCombo.addValueChangeListener(e -> {
                if (e.getValue() != null) {
                    user.setCompany(e.getValue());
                }
            });

            return companyCombo;
        })).setHeader("Perusahaan").setSortable(true);

        // Username editable
        gridUsers.addColumn(new ComponentRenderer<>(user -> {
            TextField nicknameField = new TextField();
            nicknameField.setValue(user.getNickname());
            nicknameField.addValueChangeListener(e -> {
                user.setNickname(e.getValue());
                // Optionally set an isEdit flag here
            });
            return nicknameField;
        })).setHeader("Nama").setSortable(true);
        // isActive editable
        gridUsers.addColumn(new ComponentRenderer<>(user -> {
            Checkbox activeCheckbox = new Checkbox(Boolean.TRUE.equals(user.getIsActive()));
            activeCheckbox.addValueChangeListener(e -> {
                user.setIsActive(e.getValue());
                // Optionally set an isEdit flag here
            });
            return activeCheckbox;
        })).setHeader("Aktif").setSortable(true);

        gridUsers.addColumn(FwAppUser::getLastLogin).setHeader("Last Login").setSortable(true).setFlexGrow(0).setAutoWidth(true);
        // Action column with delete button (icon only, no title)
        gridUsers.addColumn(new ComponentRenderer<>(user -> {
            // Edit button
            Button editButton = new Button();
            editButton.setIcon(VaadinIcon.USER.create());
            editButton.getElement().setAttribute("title", "Edit Karyawan");
            editButton.addClickListener(e -> {
                UI.getCurrent().navigate(ROUTE_EDIT + user.getId());
            });
            if (!this.auth.canEdit) {
                editButton.setEnabled(false);
            }

            // Edit Password button
            Button editPasswordButton = new Button();
            editPasswordButton.setIcon(VaadinIcon.PASSWORD.create());
            editPasswordButton.getElement().setAttribute("title", "Edit Password");

            if(!this.auth.canEdit){
                editPasswordButton.setEnabled(false);
            }

            editPasswordButton.addClickListener(e -> {
                Dialog dialog = new Dialog();

                VerticalLayout dialogLayout = new VerticalLayout();
                PasswordField passwordField = new PasswordField("Password");
                PasswordField confirmPasswordField = new PasswordField("Confirm Password");
                confirmPasswordField.setRevealButtonVisible(false);
                passwordField.setRevealButtonVisible(false);
                passwordField.setRequiredIndicatorVisible(true);
                confirmPasswordField.setRequiredIndicatorVisible(true);


                HorizontalLayout buttonLayout = new HorizontalLayout();
                Button cancelButton = new Button("Cancel", event -> dialog.close());
                Button saveButton = new Button("Simpan");

                if(!this.auth.canEdit){
                    saveButton.setEnabled(false);
                }

                saveButton.addClickListener(event -> {
                    // Validasi Password
                    String password = passwordField.getValue() != null ? passwordField.getValue().trim() : "";
                    String confirmPassword = confirmPasswordField.getValue() != null ? confirmPasswordField.getValue().trim() : "";

                    // Reset invalid state
                    passwordField.setInvalid(false);
                    confirmPasswordField.setInvalid(false);

                    if (password.isEmpty()) {
                        passwordField.setInvalid(true);
                        passwordField.setErrorMessage("Password belum diisi");
                        return;
                    }

                    if (password.length() < 6) {
                        passwordField.setInvalid(true);
                        passwordField.setErrorMessage("Password minimal 6 karakter");
                        return;
                    }

                    if (!password.matches(".*\\d.*")) {
                        passwordField.setInvalid(true);
                        passwordField.setErrorMessage("Password harus mengandung angka (0–9)");
                        return;
                    }

                    // Hanya izinkan huruf, angka, dan simbol @ # $ % (simbol opsional)
                    if (!password.matches("^[A-Za-z0-9@#$%]+$")) {
                        passwordField.setInvalid(true);
                        passwordField.setErrorMessage("Password hanya boleh berisi huruf, angka, atau simbol @ # $ %");
                        return;
                    }

                    if (!password.equals(confirmPassword)) {
                        passwordField.setInvalid(true);
                        confirmPasswordField.setInvalid(true);
                        String msg = "Password dan Konfirmasi tidak sama";
                        passwordField.setErrorMessage(msg);
                        confirmPasswordField.setErrorMessage(msg);
                        return;
                    }

                    // Jika lolos validasi: simpan ke entitas
                    user.setPassword(password);
                    user.setPasswordHash(password);

                    // Tutup dialog setelah sukses
                    dialog.close();
                });

                buttonLayout.add(cancelButton, saveButton);
                dialogLayout.add(passwordField, confirmPasswordField, buttonLayout);
                dialog.add(dialogLayout);
                dialog.open();
            });

            if(!this.auth.canDelete){
                editPasswordButton.setEnabled(false);
            }
            HorizontalLayout actionLayout = new HorizontalLayout(editPasswordButton, editButton);
            return actionLayout;
        })).setHeader("").setFlexGrow(1).setAutoWidth(false);


        body.add(row, gridUsers);
        body.setFlexGrow(1, gridUsers);

        add(body);
    }

    private void setListener() {
        // Add listeners for UI components here
        // For example, you can add a click listener to a button
        // saveButton.addClickListener(event -> savePage());
        populateButton.addClickListener( e-> {
            if ( searchField.getValue() != null && !searchField.getValue().isEmpty() ) {
                appUserList = adminService.findAllAppUsersByKeyword(searchField.getValue());
            } else {
                appUserList = adminService.findAllAppUsers();
            }
            gridUsers.setItems(appUserList);
        });

        saveButton.addClickListener( e->{
            // getAll FwAppUser from grid
            List<FwAppUser> usersToSave = gridUsers.getListDataView().getItems().toList();

            FwAppUser hasNoPassword = usersToSave.stream().filter(p -> p.getPassword() == null && p.getPasswordHash() == null).findFirst().orElse(null);
            if(hasNoPassword != null) {
                Notification.show("Password wajib diisi for User: "+hasNoPassword.getUsername(), 500, Notification.Position.MIDDLE);
                return;
            }

            // log all user
            usersToSave.forEach(user -> {
                log.debug("Saving user: {} - {} - {}", user.getUsername(), user.getPerson(), user.getCompany());
                adminService.saveAppUser(user, currentUser.require());
            });

            populateButton.click();
        });

        addButton.addClickListener(event -> {
            FwAppUser newUser = new FwAppUser();
            newUser.setIsActive(false);
            newUser.setUsername("ChangeMe");
            newUser.setNickname("NicknameMe");

            gridUsers.getListDataView().addItem(newUser);
        });
    }
}