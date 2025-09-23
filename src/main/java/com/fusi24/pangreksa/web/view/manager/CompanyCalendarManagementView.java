package com.fusi24.pangreksa.web.view.manager;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.HrCompanyCalendar;
import com.fusi24.pangreksa.web.model.enumerate.CalendarTypeEnum;
import com.fusi24.pangreksa.web.service.CalendarService;
import com.fusi24.pangreksa.web.service.CommonService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

@Route("company-calendar-mgt-page-access")
@PageTitle("Company Calendar Management")
@Menu(order = 20, icon = "vaadin:clipboard-check", title = "Company Calendar Management")
@RolesAllowed("KAL_FORM")
//@PermitAll // When security is enabled, allow all authenticated users
public class CompanyCalendarManagementView extends Main {
    private static final long serialVersionUID = 20L;
    private static final Logger log = LoggerFactory.getLogger(CompanyCalendarManagementView.class);
    private final CurrentUser currentUser;
    private final CommonService commonService;
    private final CalendarService calendarService;
    private Authorization auth;

    public static final String VIEW_NAME = "Company Calendar Management";

    private VerticalLayout body;

    private ComboBox<Integer> yearDropdown;
    private Button populateButton;
    private Button saveButton;
    private Button addButton;

    Grid<HrCompanyCalendar> companyCalendarGrid;

    private boolean isEdit = false;

    public CompanyCalendarManagementView(CurrentUser currentUser, CommonService commonService, CalendarService calendarService) {
        this.currentUser = currentUser;
        this.commonService = commonService;
        this.calendarService = calendarService;

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
            populateButton.setEnabled(false);
        }
        if(!this.auth.canCreate){
            addButton.setEnabled(false);
            saveButton.setEnabled(false);
        }
    }

    private void createBody() {
        this.setHeightFull();

        this.body = new VerticalLayout();
        this.body.setHeightFull();

        body.setPadding(false);
        body.setSpacing(false);

        yearDropdown = new ComboBox<>("Year");
        // put 5 years back and next 5 years
        int currentYear = LocalDate.now().getYear();
        yearDropdown.setItems(currentYear - 5, currentYear - 4, currentYear - 3, currentYear - 2, currentYear - 1,
                currentYear, currentYear + 1);
        yearDropdown.setItemLabelGenerator(String::valueOf);
        yearDropdown.setValue(currentYear);

        populateButton = new Button("Populate");
        saveButton = new Button("Save");
        addButton = new Button("Add");

        HorizontalLayout leftLayout = new HorizontalLayout(yearDropdown, populateButton);
        leftLayout.setSpacing(true);
        leftLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        HorizontalLayout functionMenuListGrid = new HorizontalLayout();
        functionMenuListGrid.setWidthFull();
        functionMenuListGrid.add(leftLayout);
        functionMenuListGrid.add(saveButton, addButton);
        functionMenuListGrid.setAlignItems(HorizontalLayout.Alignment.BASELINE);
        functionMenuListGrid.expand(leftLayout); // leftLayout takes all available space, addButton stays right

        body.add(functionMenuListGrid);
        body.add(createGrid());

        add(body);
    }

    private Grid<HrCompanyCalendar> createGrid() {
        companyCalendarGrid = new Grid<>(HrCompanyCalendar.class, false);
        companyCalendarGrid.setSizeFull(); // responsive full width

        // Editable Start Date
        companyCalendarGrid.addColumn(new ComponentRenderer<>(item -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setSpacing(false);
            layout.getStyle().set("gap", "5px"); // small gap
            layout.setWidthFull();

            DatePicker startDate = new DatePicker();
            startDate.setWidthFull();
            startDate.setValue(item.getStartDate());
            startDate.addValueChangeListener(e -> {
                item.setStartDate(e.getValue());
                this.isEdit = true;
            });

            layout.add(startDate);
            return layout;
        })).setHeader("Start Date").setWidth("75px");

        // Editable End Date
        companyCalendarGrid.addColumn(new ComponentRenderer<>(item -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setSpacing(false);
            layout.getStyle().set("gap", "5px");
            layout.setWidthFull();

            DatePicker endDate = new DatePicker();
            endDate.setWidthFull();
            endDate.setValue(item.getEndDate());
            endDate.addValueChangeListener(e -> {
                item.setEndDate(e.getValue());
                this.isEdit = true;
            });

            layout.add(endDate);
            return layout;
        })).setHeader("End Date").setWidth("75px");

        // Editable Calendar Type
        companyCalendarGrid.addColumn(new ComponentRenderer<>(item -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setSpacing(false);
            layout.getStyle().set("gap", "5px");
            layout.setWidthFull();

            ComboBox<CalendarTypeEnum> typeBox = new ComboBox<>();
            typeBox.setItems(CalendarTypeEnum.values());
            typeBox.setWidthFull();
            typeBox.setValue(item.getCalendarType());
            typeBox.addValueChangeListener(e -> {
                item.setCalendarType(e.getValue());
                this.isEdit = true;
            });

            layout.add(typeBox);
            return layout;
        })).setHeader("Calendar Type").setFlexGrow(1).setAutoWidth(false);

        // Editable Label
        companyCalendarGrid.addColumn(new ComponentRenderer<>(item -> {
            TextField labelField = new TextField();
            labelField.setWidthFull();
            labelField.setValue(item.getLabel() != null ? item.getLabel() : "");
            labelField.addValueChangeListener(e -> {
                item.setLabel(e.getValue());
                this.isEdit = true;
            });
            return labelField;
        })).setHeader("Label").setFlexGrow(2).setAutoWidth(false);

        // Editable Year
        companyCalendarGrid.addColumn(new ComponentRenderer<>(item -> {
            NumberField yearField = new NumberField();
            yearField.setWidthFull();
            yearField.setValue(item.getYear() != null ? item.getYear().doubleValue() : 0.0);
            yearField.setStep(1);
            yearField.addValueChangeListener(e -> {
                item.setYear(e.getValue().intValue());
                this.isEdit = true;
            });
            return yearField;
        })).setHeader("Year").setWidth("50px");

        // Editable Based On
        companyCalendarGrid.addColumn(new ComponentRenderer<>(item -> {
            TextField basedOnField = new TextField();
            basedOnField.setWidthFull();
            basedOnField.setValue(item.getBasedOn() != null ? item.getBasedOn() : "");
            basedOnField.addValueChangeListener(e -> {
                item.setBasedOn(e.getValue());
                this.isEdit = true;
            });
            return basedOnField;
        })).setHeader("Based On").setFlexGrow(3).setAutoWidth(false);

        // Editable Document File
        companyCalendarGrid.addColumn(new ComponentRenderer<>(item -> {
            TextField docField = new TextField();
            docField.setWidthFull();
            docField.setValue(item.getDocumentFile() != null ? item.getDocumentFile() : "");
            docField.addValueChangeListener(e -> {
                item.setDocumentFile(e.getValue());
                this.isEdit = true;
            });
            return docField;
        })).setHeader("Document File").setFlexGrow(2).setAutoWidth(false);

        // Delete button
        companyCalendarGrid.addColumn(new ComponentRenderer<>(item -> {
            Button deleteButton = new Button(VaadinIcon.CLOSE.create());
            deleteButton.addClickListener(e -> {
                if (this.auth.canDelete) {
                    companyCalendarGrid.getListDataView().removeItem(item);
                    calendarService.deleteCompanyCalendar(item);
                }
            });
            deleteButton.setEnabled(this.auth.canDelete);
            return deleteButton;
        })).setHeader("Actions").setFlexGrow(0).setAutoWidth(true);

        return companyCalendarGrid;
    }

    private void populateGrid() {
        // This method should fetch data from the service and populate the grid
        // For example:
        int selectedYear = yearDropdown.getValue();
        companyCalendarGrid.setItems(calendarService.getCompanyCalendarsByYear(selectedYear));
    }

    private void setListener() {
        populateButton.addClickListener( e -> {
            if(this.auth.canView) {
                populateGrid();
            }
        });

        addButton.addClickListener( e ->{
           if(this.auth.canCreate){
               int yearValue = yearDropdown.getValue();
               // add a new empty row to the grid
                HrCompanyCalendar newCalendar = HrCompanyCalendar.builder()
                          .startDate(LocalDate.now().withYear(yearValue))
                          .endDate(LocalDate.now().withYear(yearValue))
                            .isActive(true)
                          .calendarType(CalendarTypeEnum.COMPANY_HOLIDAY)
                          .label("New Label")
                          .year(yearValue)
                          .basedOn("n/a")
                          .documentFile("")
                          .build();
                companyCalendarGrid.getListDataView().addItem(newCalendar);
           }
        });

        saveButton.addClickListener( e -> {
            if(this.auth.canEdit && this.isEdit == true) {
                saveButton.setEnabled(false);
                var user = currentUser.require();

                // Save all changes made in the grid
                companyCalendarGrid.getListDataView().getItems().forEach(item -> {
                    calendarService.saveCompanyCalendar(item, user);
                    log.debug("Saving calendar: {}", item.getLabel());
                });
                // Optionally, refresh the grid after saving
                populateGrid();

                // Notify user of success
                Notification.show("Successfully saved company calendar data.");
                saveButton.setEnabled(true);
                this.isEdit = false;
            }
        });
    }
}

