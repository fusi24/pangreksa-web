package com.fusi24.pangreksa.web.view.admin;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.*;
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.CompanyService;
import com.fusi24.pangreksa.web.service.PersonService;
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
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
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

@Route("global-person-data-page-access")
@PageTitle("Global Person Data")
@Menu(order = 40, icon = "vaadin:user", title = "Global Person Data")
@RolesAllowed("GLOBAL_PERSON")
//@PermitAll // When security is enabled, allow all authenticated users
public class GlobalPersonDataView extends Main {
    private static final long serialVersionUID = 40L;
    private static final Logger log = LoggerFactory.getLogger(GlobalPersonDataView.class);
    private final CurrentUser currentUser;
    private final CommonService commonService;
    private final PersonService personService;
    private final CompanyService companyService;
    private Authorization auth;

    public static final String VIEW_NAME = "Global Person Data";
    public static final String ROUTE_EDIT = "global-person-data-page-access/";

    private VerticalLayout body;
    private Grid<HrPersonPosition> gridEmployees;
    private Grid<HrPerson> gridUnassignedPersons;
    private Button populateButton;
    private Button addPersonButton;

    private ComboBox<HrCompany> companyDropdown;
    private TabSheet tabsheet;

    public GlobalPersonDataView(CurrentUser currentUser, CommonService commonService, PersonService personService, CompanyService companyService) {
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
        setSizeFull();
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
        body.setSizeFull();
        companyDropdown = new ComboBox<>("Perusahaan");
        companyDropdown.setItems(companyService.getallCompanies());
        companyDropdown.setItemLabelGenerator(HrCompany::getName);
        companyDropdown.getStyle().setWidth("350px");

        populateButton = new Button("Muat Data");
        addPersonButton = new Button("Tambah Karyawan");

        // Create left and right layouts
        HorizontalLayout leftLayout = new HorizontalLayout(companyDropdown, populateButton);
        leftLayout.setAlignItems(FlexComponent.Alignment.END);
        leftLayout.setSpacing(true);

        HorizontalLayout rightLayout = new HorizontalLayout(addPersonButton);
        rightLayout.setSpacing(true);

        // Main toolbar layout
        HorizontalLayout toolbarLayout = new HorizontalLayout();
        toolbarLayout.setWidthFull();
        toolbarLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        toolbarLayout.setAlignItems(FlexComponent.Alignment.BASELINE);
        toolbarLayout.add(leftLayout, rightLayout);
        toolbarLayout.addClassNames(LumoUtility.Padding.Top.NONE, LumoUtility.Padding.Right.NONE, LumoUtility.Padding.Bottom.MEDIUM, LumoUtility.Padding.Left.NONE);

        body.add(toolbarLayout);

        createTabSheet();

        add(body);
    }

    private void createTabSheet() {
        tabsheet = new TabSheet();
        tabsheet.setSizeFull();

        PrettyTime prettyTime = new PrettyTime();

        VerticalLayout tabA = new VerticalLayout();
        tabA.setSizeFull();
        tabA.setPadding(false);
        tabA.setSpacing(false);

        VerticalLayout tabB = new VerticalLayout();
        tabB.setSizeFull();
        tabB.setPadding(false);
        tabB.setSpacing(false);

        tabsheet.add("Karyawan", tabA);
        tabsheet.add("Person Belum Ditugaskan", tabB);

        gridEmployees = new Grid<>(HrPersonPosition.class, false);
        gridEmployees.setSizeFull();

        gridEmployees.addColumn(pos -> {
            HrPerson p = pos.getPerson();
            return p != null ? p.getFirstName() : "";
        }).setHeader("Nama Awal");

        gridEmployees.addColumn(pos -> {
            HrPerson p = pos.getPerson();
            return p != null ? p.getLastName() : "";
        }).setHeader("Nama Akhir");

        gridEmployees.addColumn(pos -> {
            HrPosition position = pos.getPosition();
            return position != null ? position.getName() : "";
        }).setHeader("Posisi");

        gridEmployees.addColumn(HrPersonPosition::getStartDate)
                .setHeader("Tanggal Mulai");


        gridUnassignedPersons = new Grid<>(HrPerson.class, false);
        gridUnassignedPersons.setSizeFull();

        gridUnassignedPersons.addColumn(HrPerson::getFirstName)
                .setHeader("Nama Awal")
                .setAutoWidth(true)
                .setFlexGrow(0);

        gridUnassignedPersons.addColumn(HrPerson::getLastName)
                .setHeader("Nama Akhir")
                .setAutoWidth(true)
                .setFlexGrow(0);

        gridUnassignedPersons.addColumn(person ->
                person.getCreatedAt() != null
                        ? new PrettyTime().format(person.getCreatedAt())
                        : ""
        ).setHeader("Tanggal Dibuat")
                .setAutoWidth(true)
                .setFlexGrow(1);


        tabA.add(gridEmployees);
        tabA.setFlexGrow(1, gridEmployees);

        tabB.add(gridUnassignedPersons);
        tabB.setFlexGrow(1, gridUnassignedPersons);



        body.add(tabsheet);
        body.setFlexGrow(1, tabsheet);
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
                Notification.show("Anda tidak memiliki izin untuk menambahkan person baru.");
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

