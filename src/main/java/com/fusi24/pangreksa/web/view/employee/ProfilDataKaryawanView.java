package com.fusi24.pangreksa.web.view.employee;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.*;
import com.fusi24.pangreksa.web.repo.HrDepartmentRepo;
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.CompanyService;
import com.fusi24.pangreksa.web.service.PersonService;
import com.fusi24.pangreksa.web.service.SystemService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.ocpsoft.prettytime.PrettyTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Route("profil-data-karyawan-page-access")
@PageTitle("Profil Data Karyawan")
@Menu(order = 14, icon = "vaadin:users", title = "Profil Data Karyawan")
@RolesAllowed("PROFIL_KAR")
//@PermitAll // When security is enabled, allow all authenticated users
public class ProfilDataKaryawanView extends Main {
    private static final long serialVersionUID = 14L;
    private static final Logger log = LoggerFactory.getLogger(ProfilDataKaryawanView.class);
    private final CurrentUser currentUser;
    private final CommonService commonService;
    private final PersonService personService;
    private final CompanyService companyService;
    private final SystemService systemService;
    private Authorization auth;
    private FwAppUser currentAppUser;

    public static final String VIEW_NAME = "Profil Data Karyawan";
    public static final String ROUTE_EDIT = "karyawan-baru-form-page-access/";

    private int MAX_RESULT = 5;
    private int MIN_SEARCH_LENGTH = 5;

    private VerticalLayout body;
    private Grid<HrPersonPosition> gridEmployees;
    private Grid<HrPerson> gridUnassignedPersons;
    private Button populateButton;
    private Button addPersonButton;
    private TextField searchField;

    private TabSheet tabsheet;
    Grid<HrOrgStructure> orgStructureGrid;
    Grid<HrPosition> positionGrid;

    @Autowired
    private HrDepartmentRepo hrDepartmentRepo;

    public ProfilDataKaryawanView(CurrentUser currentUser, CommonService commonService, PersonService personService,
                                  CompanyService companyService, SystemService systemService) {
        this.currentUser = currentUser;
        this.commonService = commonService;
        this.personService = personService;
        this.companyService = companyService;
        this.systemService = systemService;

        this.auth = commonService.getAuthorization(
                currentUser.require(),
                (String) UI.getCurrent().getSession().getAttribute("responsibility"),
                this.serialVersionUID);

        this.currentAppUser = commonService.getLoginUser(currentUser.require().getUserId().toString());

        this.MAX_RESULT = systemService.getMaxSearchResult();
        this.MIN_SEARCH_LENGTH = systemService.getMinSearchLength();

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
            this.populateButton.setEnabled(false);
        }
    }

    private void createBody() {
        this.setHeightFull();

        this.body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);

        body.setHeightFull();

        addPersonButton = new Button("Add Person");

        searchField = new TextField();
        searchField.setClearButtonVisible(true);
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setWidth("270px");
        populateButton = new Button("Populate");

        // Create left and right layouts
        HorizontalLayout leftLayout = new HorizontalLayout();
        leftLayout.add(searchField, populateButton);
        leftLayout.setAlignItems(Alignment.END);
        leftLayout.setSpacing(true);

        HorizontalLayout rightLayout = new HorizontalLayout(addPersonButton);
        rightLayout.setSpacing(true);

        // Main toolbar layout
        HorizontalLayout toolbarLayout = new HorizontalLayout();
        toolbarLayout.setWidthFull();
        toolbarLayout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        toolbarLayout.setAlignItems(Alignment.BASELINE);
        toolbarLayout.add(leftLayout, rightLayout);
        toolbarLayout.addClassNames(LumoUtility.Padding.Top.NONE, LumoUtility.Padding.Right.NONE, LumoUtility.Padding.Bottom.MEDIUM, LumoUtility.Padding.Left.NONE);

        body.add(toolbarLayout);

        createTabSheet();

        add(body);
    }

    private void createTabSheet() {
        tabsheet = new TabSheet();

        PrettyTime prettyTime = new PrettyTime();

        VerticalLayout tabA = new VerticalLayout();
        tabA.setHeightFull();
        tabsheet.add("Employees", tabA);
        VerticalLayout tabB = new VerticalLayout();
        tabB.setHeightFull();
        tabsheet.add("Unassigned Persons", tabB);
//        tabsheet.getStyle().setFlexGrow("1").setWidth("100%");
        tabsheet.setHeightFull();
        tabsheet.setWidthFull();

        gridUnassignedPersons = new Grid<>(HrPerson.class, false);
        gridUnassignedPersons.setSelectionMode(Grid.SelectionMode.SINGLE);
        gridUnassignedPersons.addColumn(HrPerson::getFirstName).setHeader("First Name").setSortable(true);
        gridUnassignedPersons.addColumn(HrPerson::getLastName).setHeader("Last Name").setSortable(true);
        gridUnassignedPersons.addColumn(person ->
                person.getCreatedAt() != null ? prettyTime.format(person.getCreatedAt()) : ""
        ).setHeader("Created Date").setSortable(false);
        // Action column with delete button (icon only, no title)
        // Action column with edit & assign button
        // Action column with edit & assign button
        // Action column with edit & assign button
        gridUnassignedPersons.addColumn(new ComponentRenderer<>(person -> {
            HorizontalLayout actionLayout = new HorizontalLayout();

            // Edit button
            Button editButton = new Button();
            editButton.setIcon(VaadinIcon.EDIT.create());
            editButton.getElement().setAttribute("title", "Edit Person");
            editButton.addClickListener(e -> {
                UI.getCurrent().navigate(ROUTE_EDIT + person.getId());
            });
            if (!this.auth.canEdit) {
                editButton.setEnabled(false);
            }

            // Assign to Company button
            Button assignButton = new Button();
            assignButton.setIcon(VaadinIcon.WORKPLACE.create());
            assignButton.getElement().setAttribute("title", "Assign to Company");

            assignButton.addClickListener(e -> {
                Dialog dialog = new Dialog();
                dialog.setHeaderTitle("Assign to Company");

                VerticalLayout dialogLayout = new VerticalLayout();
                dialogLayout.setPadding(false);
                dialogLayout.setSpacing(true);

                ComboBox<HrOrgStructure> orgStructureDropdown = new ComboBox<>("Org. Structure");
                ComboBox<HrPosition> positionDropdown = new ComboBox<>("Position");
                DatePicker startDatePicker = new DatePicker("Start Date");
                DatePicker endDatePicker = new DatePicker("End Date");
                Checkbox isPrimaryCheckbox = new Checkbox("Is Primary");
                Checkbox isActingCheckbox = new Checkbox("Is Acting");
                ComboBox<HrPerson> requestedByDropdown = new ComboBox<>("Requested By");

                // === Requested By (lazy search, mirip view lain) ===
                requestedByDropdown.setItemLabelGenerator(p ->
                        p.getFirstName() + " " + (p.getLastName() != null ? p.getLastName() : "")
                );
                requestedByDropdown.setPlaceholder("Type to search requester");
                requestedByDropdown.setClearButtonVisible(true);
                requestedByDropdown.setWidth("400px");
                requestedByDropdown.setPageSize(20);
                requestedByDropdown.setItems(query -> {
                    String filter = query.getFilter().orElse("").trim();
                    int offset = query.getOffset();
                    int limit = query.getLimit();

                    log.debug("Searching persons with filter: {}", filter);
                    java.util.List<HrPerson> persons = personService.findPersonByKeyword(filter);
                    return persons.stream()
                            .skip(offset)
                            .limit(limit);
                });

                // === Org. Structure ===
                orgStructureDropdown.setItems(
                        companyService.getAllOrgStructuresInCompany(this.currentAppUser.getCompany())
                );
                orgStructureDropdown.setItemLabelGenerator(HrOrgStructure::getName);
                orgStructureDropdown.setWidth("400px");

                // === Position (di-enable hanya kalau org dipilih) ===
                positionDropdown.setItemLabelGenerator(HrPosition::getName);
                positionDropdown.setWidth("400px");
                positionDropdown.setEnabled(false);

                // Saat Org. Structure dipilih -> load posisi yang ada di unit tsb
                orgStructureDropdown.addValueChangeListener(event2 -> {
                    positionDropdown.clear();
                    positionDropdown.setItems(java.util.Collections.emptyList());
                    positionDropdown.setEnabled(false);

                    HrOrgStructure selectedOrg = event2.getValue();
                    if (selectedOrg != null) {
                        java.util.List<HrPosition> positions =
                                companyService.getAllPositionsInOrganization(
                                        this.currentAppUser.getCompany(), selectedOrg
                                );

                        log.debug("Assign dialog: found {} positions for org {}",
                                positions.size(), selectedOrg.getName());

                        if (!positions.isEmpty()) {
                            positionDropdown.setItems(positions);
                            positionDropdown.setEnabled(true);
                        }
                    }
                });

                // === Tombol Save / Cancel ===
                HorizontalLayout buttonLayout = new HorizontalLayout();
                Button cancelButton = new Button("Cancel", ev -> dialog.close());
                Button saveButton = new Button("Save");

                if (!this.auth.canEdit) {
                    saveButton.setEnabled(false);
                }

                saveButton.addClickListener(ev -> {
                    if (orgStructureDropdown.isEmpty() || positionDropdown.isEmpty()) {
                        Notification.show("Org Structure and Position are required.");
                        return;
                    }

                    log.debug("Saving Org Structure {} and Position {} to Person {}",
                            orgStructureDropdown.getValue(), positionDropdown.getValue(), person.getFirstName());

                    HrPersonPosition personPosition = HrPersonPosition.builder()
                            .person(person)
                            .position(positionDropdown.getValue())
                            .startDate(startDatePicker.getValue())
                            .endDate(endDatePicker.getValue())
                            .isActing(isActingCheckbox.getValue())
                            .isPrimary(isPrimaryCheckbox.getValue())
                            .requestedBy(requestedByDropdown.getValue())
                            .company(this.currentAppUser.getCompany())
                            .build();

                    personService.savePersonPosition(personPosition, currentUser.require());

                    dialog.close();
                    // refresh grid karyawan supaya langsung kelihatan
                    populateEmployees();
                });

                buttonLayout.add(cancelButton, saveButton);

                dialogLayout.add(
                        orgStructureDropdown,
                        positionDropdown,
                        new HorizontalLayout(startDatePicker, endDatePicker),
                        new HorizontalLayout(isPrimaryCheckbox, isActingCheckbox),
                        requestedByDropdown,
                        buttonLayout
                );

                dialog.add(dialogLayout);
                dialog.open();
            });


            if (!this.auth.canEdit) {
                assignButton.setEnabled(false);
            }

            actionLayout.add(editButton, assignButton);
            return actionLayout;
        })).setHeader("").setAutoWidth(true);



        tabA.add(createEmployeesGridFunction());
        tabB.add(gridUnassignedPersons);


        populateOrgStructureGrid();

        body.add(tabsheet);
    }

    private Component createEmployeesGridFunction(){
        HorizontalLayout leftLayout = new HorizontalLayout();
        leftLayout.setSpacing(true);
//        leftLayout.add(searchField, populateButton);
        leftLayout.setAlignItems(Alignment.BASELINE);

        HorizontalLayout functionMenuLayout = new HorizontalLayout();
        functionMenuLayout.setWidthFull();
        functionMenuLayout.add(leftLayout);
//        functionMenuLayout.add(saSaveButton, saAddButton);
        functionMenuLayout.setAlignItems(Alignment.BASELINE);
        functionMenuLayout.expand(leftLayout); // leftLayout takes all available space, addButton stays right

        String WIDTH_ = "250px";

        orgStructureGrid = new Grid<>(HrOrgStructure.class, false);
        orgStructureGrid.addClassNames("org-structure-grid");
        orgStructureGrid.setWidth(WIDTH_);
        orgStructureGrid.setHeightFull();
        orgStructureGrid.setSelectionMode(Grid.SelectionMode.SINGLE);

        // Perubahan: Menambahkan kolom Code dan Type yang ada di MasterOrgStructureView
        orgStructureGrid.addColumn(HrOrgStructure::getName).setHeader("Organization Name"); // Tetap ada
//        orgStructureGrid.addColumn(HrOrgStructure::getCode).setHeader("Code");             // Tambahan
//        orgStructureGrid.addColumn(org -> org.getType() != null ? org.getType().name() : "").setHeader("Type"); // Tambahan


        positionGrid = new Grid<>(HrPosition.class, false);
        positionGrid.addClassNames("position-grid");
        positionGrid.setWidth(WIDTH_);
        positionGrid.setHeightFull();
        positionGrid.setSelectionMode(Grid.SelectionMode.SINGLE);
        // add columns
        positionGrid.addColumn(HrPosition::getName).setHeader("Position");

        gridEmployees = new Grid<>(HrPersonPosition.class, false);
        gridEmployees.setWidthFull();
        gridEmployees.setHeightFull();
        gridEmployees.setSelectionMode(Grid.SelectionMode.NONE);
        // Name
        gridEmployees.addColumn(pos -> {
            HrPerson person = pos.getPerson();
            return person != null ? person.getFirstName() : "";
        }).setHeader("First Name").setSortable(true);
        gridEmployees.addColumn(pos -> {
            HrPerson person = pos.getPerson();
            return person != null ? person.getLastName() : "";
        }).setHeader("Last Name").setSortable(true);
        // Position
        gridEmployees.addColumn(pos -> {
            HrPosition position = pos.getPosition();
            return position != null ? position.getName() : "";
        }).setHeader("Position").setSortable(true);
        gridEmployees.addColumn(pos -> {
            HrPosition position = pos.getPosition();
            return position != null ? position.getOrgStructure().getName() : "";
        }).setHeader("Org Structure").setSortable(true);
        gridEmployees.addColumn(pos -> {
            HrDepartment dept = pos.getDepartment();
            return Optional.ofNullable(dept).map(HrDepartment::getName).orElse("");
        }).setHeader("Department").setSortable(true);
        // Start Date
        gridEmployees.addColumn(HrPersonPosition::getStartDate).setHeader("Start Date").setSortable(true);
        // Action column with delete button (icon only, no title)
        gridEmployees.addColumn(new ComponentRenderer<>(personPosition -> {
            HorizontalLayout actionLayout = new HorizontalLayout();

            // Edit button
            Button editButton = new Button();
            editButton.setIcon(VaadinIcon.EDIT.create());
            editButton.getElement().setAttribute("title", "Edit Person");
            editButton.addClickListener(e -> {
                UI.getCurrent().navigate(ROUTE_EDIT + personPosition.getPerson().getId());
            });
            if (!this.auth.canEdit) {
                editButton.setEnabled(false);
            }

            actionLayout.add(editButton);
            return actionLayout;
        })).setHeader("").setAutoWidth(true);

        HorizontalLayout horizontalbodyLayout = new HorizontalLayout(
                orgStructureGrid,
                positionGrid,
                gridEmployees);
        horizontalbodyLayout.setHeightFull();
        horizontalbodyLayout.setWidthFull();

        VerticalLayout allowancePackageLayout = new VerticalLayout(functionMenuLayout, horizontalbodyLayout);
        allowancePackageLayout.setSpacing(false);
        allowancePackageLayout.setPadding(false);
        allowancePackageLayout.setHeightFull();

        return allowancePackageLayout;
    }

    private void populateOrgStructureGrid(){
        if(this.auth.canView) {
            List<HrOrgStructure> orgStructureList = companyService.getAllOrgStructuresInCompany(this.currentAppUser.getCompany());
            orgStructureGrid.setItems(orgStructureList);
        }
    }

    private void populatePositionGrid(HrOrgStructure orgStructure){
        if(this.auth.canView) {
            List<HrPosition> positionList = companyService.getAllPositionsInOrganization(this.currentAppUser.getCompany(), orgStructure);
            positionGrid.setItems(positionList);
        }
    }


    public void populateEmployees() {
        personService.workingWithCompany(this.currentAppUser.getCompany());
        List<HrPersonPosition> personPositionList;

        if (!searchField.getValue().isEmpty()) {
            if (searchField.getValue().length() < this.MIN_SEARCH_LENGTH) {
                String msg = "Please enter at least " + this.MIN_SEARCH_LENGTH + " characters to search.";
                Notification.show(msg);
                searchField.setErrorMessage(msg);
                return;
            }
            personPositionList = personService.getPersonHasPositionInCompanyByKeyword (currentAppUser.getCompany(), searchField.getValue(), 0, this.MAX_RESULT);
        } else if (this.orgStructureGrid.getSelectedItems().size() == 1 && this.positionGrid.getSelectedItems().size() == 1) {
            personPositionList = personService.getPersonHasPositionInCompanyByPosition(currentAppUser.getCompany(), positionGrid.getSelectedItems().iterator().next());
        } else if (this.orgStructureGrid.getSelectedItems().size() == 1) {
            personPositionList = personService.getPersonHasPositionInCompanyByOrgStructure(currentAppUser.getCompany(), orgStructureGrid.getSelectedItems().iterator().next());
        } else { // this.orgStructureGrid.getSelectedItems().size() == 0)
            personPositionList = personService.getPersonHasPositionInCompany();
        }

        gridEmployees.setItems(Collections.emptyList());
        if (!personPositionList.isEmpty())
            gridEmployees.setItems(personPositionList);
    }

    public void populateUnassignedPersons() {
        List<HrPerson> personList = personService.findUnassignedPersons();
        personList.sort((a, b) -> {
            if (a.getCreatedAt() == null && b.getCreatedAt() == null) return 0;
            if (a.getCreatedAt() == null) return 1;
            if (b.getCreatedAt() == null) return -1;
            return b.getCreatedAt().compareTo(a.getCreatedAt());
        });
        gridUnassignedPersons.setItems(Collections.emptyList());
        if (!personList.isEmpty())
            gridUnassignedPersons.setItems(personList);
    }

    private void setListener() {
        populateButton.addClickListener( e -> {
            try {
                populateButton.setEnabled(false);

                populateEmployees();
                populateUnassignedPersons();

                populateButton.setEnabled(true);
            } catch (Exception err) {
                log.error("Error populating person data", err);
                populateButton.setEnabled(true);
            }
        });

        addPersonButton.addClickListener(e -> {
            if (this.auth.canCreate) {
                UI.getCurrent().navigate(ROUTE_EDIT);
            } else {
                Notification.show("You do not have permission to add a new person.");
            }
        });

        orgStructureGrid.addSelectionListener(e -> {
            e.getFirstSelectedItem().ifPresent( item -> {
                positionGrid.deselectAll();
                e.getFirstSelectedItem().ifPresent(this::populatePositionGrid);

                populateEmployees();
            });
        });

        positionGrid.addSelectionListener(e -> {
            e.getFirstSelectedItem().ifPresentOrElse(item -> {
                // ✅ If item selected
            }, () -> {
                // ❌ Else (no item selected)
            });

            populateEmployees();
        });
    }
}