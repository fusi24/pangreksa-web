package com.fusi24.pangreksa.web.view.employee;

import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrPersonPosition;
import com.fusi24.pangreksa.web.model.entity.HrPosition;
import com.fusi24.pangreksa.web.service.CompanyService;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.combobox.ComboBox;
import com.fusi24.pangreksa.web.model.entity.HrPerson;
import com.fusi24.pangreksa.web.service.PersonService;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.button.Button;
import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.service.CommonService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.ocpsoft.prettytime.PrettyTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;

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
    private Authorization auth;

    public static final String VIEW_NAME = "Profil Data Karyawan";
    public static final String ROUTE_EDIT = "karyawan-baru-form-page-access/";

    private VerticalLayout body;
    private Grid<HrPersonPosition> gridEmployees;
    private Grid<HrPerson> gridUnassignedPersons;
    private Button populateButton;
    private Button addPersonButton;

    private ComboBox<HrCompany> companyDropdown;
    private TabSheet tabsheet;

    public ProfilDataKaryawanView(CurrentUser currentUser, CommonService commonService, PersonService personService, CompanyService companyService) {
        this.currentUser = currentUser;
        this.commonService = commonService;
        this.personService = personService;
        this.companyService = companyService;

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
            this.populateButton.setEnabled(false);
        }
    }

    private void createBody() {
        this.body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);

        companyDropdown = new ComboBox<>("Company");
        companyDropdown.setItems(companyService.getallCompanies());
        companyDropdown.setItemLabelGenerator(HrCompany::getName);
        companyDropdown.getStyle().setWidth("350px");

        populateButton = new Button("Populate");
        addPersonButton = new Button("Add Person");

        // Create left and right layouts
        HorizontalLayout leftLayout = new HorizontalLayout(companyDropdown, populateButton);
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
        tabsheet.add("Employees", tabA);
        VerticalLayout tabB = new VerticalLayout();
        tabsheet.add("Unassigned Persons", tabB);
        tabsheet.getStyle().setFlexGrow("1").setWidth("100%");

        gridEmployees = new Grid<>(HrPersonPosition.class, false);
        gridEmployees.setSelectionMode(Grid.SelectionMode.SINGLE);
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

        gridUnassignedPersons = new Grid<>(HrPerson.class, false);
        gridUnassignedPersons.setSelectionMode(Grid.SelectionMode.SINGLE);
        gridUnassignedPersons.addColumn(HrPerson::getFirstName).setHeader("First Name").setSortable(true);
        gridUnassignedPersons.addColumn(HrPerson::getLastName).setHeader("Last Name").setSortable(true);
        gridUnassignedPersons.addColumn(person ->
                person.getCreatedAt() != null ? prettyTime.format(person.getCreatedAt()) : ""
                ).setHeader("Created Date").setSortable(false);
        // Action column with delete button (icon only, no title)
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
                // Assign person to company
            });
            if(!this.auth.canEdit){
                assignButton.setEnabled(false);
            }

            actionLayout.add(editButton, assignButton);
            return actionLayout;
        })).setHeader("").setAutoWidth(true);

        tabA.add(gridEmployees);
        tabB.add(gridUnassignedPersons);


        body.add(tabsheet);
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
    }

    public void populateEmployees() {
        personService.workingWithCompany(companyDropdown.getValue());
        List<HrPersonPosition> personPositionList = personService.getPersonHasPositionInCompany();

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
}
