package com.fusi24.pangreksa.web.view.manager;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.base.util.DatePickerUtil;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.HrSalaryAllowance;
import com.fusi24.pangreksa.web.model.entity.HrSalaryBaseLevel;
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.PayrollService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.TextFieldVariant;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Route("komponen-gaji-list-page-access")
@PageTitle("Komponen Gaji")
@Menu(order = 23, icon = "vaadin:clipboard-check", title = "Komponen Gaji")
@RolesAllowed("KOMP_GAJI")
//@PermitAll // When security is enabled, allow all authenticated users
public class KomponenGajiView extends Main {
    private static final long serialVersionUID = 23L;
    private static final Logger log = LoggerFactory.getLogger(KomponenGajiView.class);
    private final CurrentUser currentUser;
    private final CommonService commonService;
    private PayrollService payrollService;
    private Authorization auth;

    public static final String VIEW_NAME = "Komponen Gaji";

    TabSheet tabsheet;
    private VerticalLayout body;

    // Base Level Salary
    Checkbox withInactiveBasicSalary;
    Button sblAddButton;
    Button sblSaveButton;
    Grid<HrSalaryBaseLevel> salaryBaseLevelGrid;
    boolean isBasicSalaryEdit = false;
    // Allowance
    Checkbox withInactiveAllowance;
    Button saAddButton;
    Button saSaveButton;
    Grid<HrSalaryAllowance> salaryAllowanceGrid;
    boolean isAllowanceEdit = false;

    public KomponenGajiView(CurrentUser currentUser, CommonService commonService, PayrollService payrollService) {
        this.currentUser = currentUser;
        this.commonService = commonService;
        this.payrollService = payrollService;

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
        if(!this.auth.canCreate){
            sblAddButton.setEnabled(false);
            sblSaveButton.setEnabled(false);

            saAddButton.setEnabled(false);
            saSaveButton.setEnabled(false);
        }
        if(!this.auth.canEdit){
            sblSaveButton.setEnabled(false);

            saSaveButton.setEnabled(false);
        }
        if(!this.auth.canView){
            withInactiveBasicSalary.setEnabled(false);

            withInactiveAllowance.setEnabled(false);
        }
    }

    private void createBody() {
        this.setHeightFull();

        this.body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);

        body.setHeightFull();

        withInactiveBasicSalary = new Checkbox(true);
        withInactiveBasicSalary.setLabel("With Inactive Level Code");

        withInactiveAllowance = new Checkbox(true);
        withInactiveAllowance.setLabel("With Inactive Allowance");


        this.tabsheet = new TabSheet();
        tabsheet.setWidthFull();
        tabsheet.setHeightFull();
        tabsheet.add("Basic Salary", createSalaryBaseLevelFunction());
        tabsheet.add("Allowances", createAlowanceFunction());

        body.add(tabsheet);

        populateSalaryBaseLevelGrid(withInactiveBasicSalary.getValue());
        populateAllowanceGrid(withInactiveAllowance.getValue());

        add(body);
    }

    private Component createSalaryBaseLevelFunction(){
        sblAddButton = new Button("Add");
        sblSaveButton = new Button("Save");

        HorizontalLayout leftLayout = new HorizontalLayout();
        leftLayout.setSpacing(true);
        leftLayout.add(withInactiveBasicSalary);
        leftLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        HorizontalLayout functionMenuLayout = new HorizontalLayout();
        functionMenuLayout.setWidthFull();
        functionMenuLayout.add(leftLayout);
        functionMenuLayout.add(sblSaveButton, sblAddButton);
        functionMenuLayout.setAlignItems(HorizontalLayout.Alignment.BASELINE);
        functionMenuLayout.expand(leftLayout); // leftLayout takes all available space, addButton stays right


        this.salaryBaseLevelGrid = new Grid<>(HrSalaryBaseLevel.class, false);
        salaryBaseLevelGrid.addClassNames("salary-base-level-grid");
        salaryBaseLevelGrid.setWidthFull();
        salaryBaseLevelGrid.setHeight("300px");

        // Editable Level Code
        salaryBaseLevelGrid.addColumn(new ComponentRenderer<>(item -> {
            TextField levelCodeField = new TextField();
            levelCodeField.setWidthFull();
            levelCodeField.setValue(item.getLevelCode() != null ? item.getLevelCode() : "");
            levelCodeField.addValueChangeListener(e -> {
                item.setLevelCode(e.getValue());
                this.isBasicSalaryEdit = true;
            });
            return levelCodeField;
        })).setHeader("Level Code").setWidth("50px");

        // Inside your grid setup:
        salaryBaseLevelGrid.addColumn(new ComponentRenderer<>(item -> {
            TextField salaryField = new TextField();
            salaryField.setWidthFull();
            salaryField.setValueChangeMode(ValueChangeMode.ON_BLUR);

            salaryField.addThemeVariants(TextFieldVariant.LUMO_ALIGN_RIGHT);
            salaryField.setAllowedCharPattern("[0-9]");
            // Prefix Rp
            salaryField.setPrefixComponent(new Span("Rp "));

            // Formatter for thousand + decimal separator (Indonesian style)
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("id", "ID"));
            symbols.setGroupingSeparator('.');
            symbols.setDecimalSeparator(',');
            DecimalFormat decimalFormat = new DecimalFormat("#,###.##", symbols);

            // Initial value
            if (item.getBaseSalary() != null) {
                salaryField.setValue(decimalFormat.format(item.getBaseSalary()));
            }

            // Listener for user input
            salaryField.addValueChangeListener(e -> {
                String value = e.getValue().replace(".", "").replace(",", ".");
                try {
                    BigDecimal parsed = new BigDecimal(value);
                    item.setBaseSalary(parsed);
                    this.isBasicSalaryEdit = true;
                    // Re-format to show separators again
                    salaryField.setValue(decimalFormat.format(parsed));
                } catch (NumberFormatException ex) {
                    // If invalid, reset
                    if (item.getBaseSalary() != null) {
                        salaryField.setValue(decimalFormat.format(item.getBaseSalary()));
                    } else {
                        salaryField.clear();
                    }
                }
            });

            return salaryField;
        })).setHeader("Amount").setAutoWidth(true).setTextAlign(ColumnTextAlign.END);
        // Editable Start Date
        salaryBaseLevelGrid.addColumn(new ComponentRenderer<>(item -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setSpacing(false);
            layout.getStyle().set("gap", "5px"); // small gap
            layout.setWidthFull();

            DatePicker startDate = new DatePicker();
            // Custom format: DD-MMM-YYYY
            startDate.setI18n(DatePickerUtil.getIndonesianI18n());
            startDate.setWidthFull();
            startDate.setValue(item.getStartDate());
            startDate.addValueChangeListener(e -> {
                item.setStartDate(e.getValue());
                this.isBasicSalaryEdit = true;
            });

            layout.add(startDate);
            return layout;
        })).setHeader("Start Date").setWidth("75px");

        // Editable End Date
        salaryBaseLevelGrid.addColumn(new ComponentRenderer<>(item -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setSpacing(false);
            layout.getStyle().set("gap", "5px");
            layout.setWidthFull();

            DatePicker endDate = new DatePicker();
            endDate.setI18n(DatePickerUtil.getIndonesianI18n());
            endDate.setWidthFull();
            endDate.setValue(item.getEndDate());
            endDate.addValueChangeListener(e -> {
                item.setEndDate(e.getValue());
                this.isBasicSalaryEdit = true;
            });

            layout.add(endDate);
            return layout;
        })).setHeader("End Date").setWidth("75px");

        // Delete button
        salaryBaseLevelGrid.addColumn(new ComponentRenderer<>(item -> {
            Button deleteButton = new Button(VaadinIcon.CLOSE.create());
            deleteButton.addClickListener(e -> {
                if (this.auth.canDelete) {
                    salaryBaseLevelGrid.getListDataView().removeItem(item);
                }
            });

            deleteButton.setEnabled(this.auth.canDelete);

            if(item.getId() == null)
                return deleteButton;
            else
                return null;
        })).setHeader("Actions").setFlexGrow(0).setAutoWidth(true);

        VerticalLayout salaryBaseLevelLayout = new VerticalLayout(functionMenuLayout, salaryBaseLevelGrid);
        salaryBaseLevelLayout.setSpacing(false);
        salaryBaseLevelLayout.setPadding(false);
        salaryBaseLevelLayout.setHeightFull();

        return salaryBaseLevelLayout;
    }

    private Component createAlowanceFunction(){
        saAddButton = new Button("Add");
        saSaveButton = new Button("Save");

        HorizontalLayout leftLayout = new HorizontalLayout();
        leftLayout.setSpacing(true);
        leftLayout.add(withInactiveAllowance);
        leftLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        HorizontalLayout functionMenuLayout = new HorizontalLayout();
        functionMenuLayout.setWidthFull();
        functionMenuLayout.add(leftLayout);
        functionMenuLayout.add(saSaveButton, saAddButton);
        functionMenuLayout.setAlignItems(HorizontalLayout.Alignment.BASELINE);
        functionMenuLayout.expand(leftLayout); // leftLayout takes all available space, addButton stays right


        this.salaryAllowanceGrid = new Grid<>(HrSalaryAllowance.class, false);
        salaryAllowanceGrid.addClassNames("salary-base-level-grid");
        salaryAllowanceGrid.setWidthFull();
        salaryAllowanceGrid.setHeight("300px");

        // Editable Level Code
        salaryAllowanceGrid.addColumn(new ComponentRenderer<>(item -> {
            TextField levelCodeField = new TextField();
            levelCodeField.setWidthFull();
            levelCodeField.setValue(item.getName() != null ? item.getName() : "");
            levelCodeField.addValueChangeListener(e -> {
                item.setName(e.getValue());
                this.isAllowanceEdit = true;
            });
            return levelCodeField;
        })).setHeader("Allowance Name").setWidth("50px");

        // Inside your grid setup:
        salaryAllowanceGrid.addColumn(new ComponentRenderer<>(item -> {
            TextField salaryField = new TextField();
            salaryField.setWidthFull();
            salaryField.setValueChangeMode(ValueChangeMode.ON_BLUR);

            salaryField.addThemeVariants(TextFieldVariant.LUMO_ALIGN_RIGHT);
            salaryField.setAllowedCharPattern("[0-9]");
            // Prefix Rp
            salaryField.setPrefixComponent(new Span("Rp "));

            // Formatter for thousand + decimal separator (Indonesian style)
            DecimalFormatSymbols symbols = new DecimalFormatSymbols(new Locale("id", "ID"));
            symbols.setGroupingSeparator('.');
            symbols.setDecimalSeparator(',');
            DecimalFormat decimalFormat = new DecimalFormat("#,###.##", symbols);

            // Initial value
            if (item.getAmount() != null) {
                salaryField.setValue(decimalFormat.format(item.getAmount()));
            }

            // Listener for user input
            salaryField.addValueChangeListener(e -> {
                String value = e.getValue().replace(".", "").replace(",", ".");
                try {
                    BigDecimal parsed = new BigDecimal(value);
                    item.setAmount(parsed);
                    this.isAllowanceEdit = true;
                    // Re-format to show separators again
                    salaryField.setValue(decimalFormat.format(parsed));
                } catch (NumberFormatException ex) {
                    // If invalid, reset
                    if (item.getAmount() != null) {
                        salaryField.setValue(decimalFormat.format(item.getAmount()));
                    } else {
                        salaryField.clear();
                    }
                }
            });

            return salaryField;
        })).setHeader("Amount").setAutoWidth(true).setTextAlign(ColumnTextAlign.END);
        // Editable Start Date
        salaryAllowanceGrid.addColumn(new ComponentRenderer<>(item -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setSpacing(false);
            layout.getStyle().set("gap", "5px"); // small gap
            layout.setWidthFull();

            DatePicker startDate = new DatePicker();
            // Custom format: DD-MMM-YYYY
            startDate.setI18n(DatePickerUtil.getIndonesianI18n());
            startDate.setWidthFull();
            startDate.setValue(item.getStartDate());
            startDate.addValueChangeListener(e -> {
                item.setStartDate(e.getValue());
                this.isAllowanceEdit = true;
            });

            layout.add(startDate);
            return layout;
        })).setHeader("Start Date").setWidth("75px");

        // Editable End Date
        salaryAllowanceGrid.addColumn(new ComponentRenderer<>(item -> {
            HorizontalLayout layout = new HorizontalLayout();
            layout.setSpacing(false);
            layout.getStyle().set("gap", "5px");
            layout.setWidthFull();

            DatePicker endDate = new DatePicker();
            endDate.setI18n(DatePickerUtil.getIndonesianI18n());
            endDate.setWidthFull();
            endDate.setValue(item.getEndDate());
            endDate.addValueChangeListener(e -> {
                item.setEndDate(e.getValue());
                this.isAllowanceEdit = true;
            });

            layout.add(endDate);
            return layout;
        })).setHeader("End Date").setWidth("75px");

        // Delete button
        salaryAllowanceGrid.addColumn(new ComponentRenderer<>(item -> {
            Button deleteButton = new Button(VaadinIcon.CLOSE.create());
            deleteButton.addClickListener(e -> {
                if (this.auth.canDelete) {
                    salaryAllowanceGrid.getListDataView().removeItem(item);
                }
            });

            deleteButton.setEnabled(this.auth.canDelete);

            if(item.getId() == null)
                return deleteButton;
            else
                return null;
        })).setHeader("Actions").setFlexGrow(0).setAutoWidth(true);

        VerticalLayout salaryAlowanceLayout = new VerticalLayout(functionMenuLayout, salaryAllowanceGrid);
        salaryAlowanceLayout.setSpacing(false);
        salaryAlowanceLayout.setPadding(false);
        salaryAlowanceLayout.setHeightFull();

        return salaryAlowanceLayout;
    }

    private  void populateSalaryBaseLevelGrid(boolean includeInactive){
        if(this.auth.canView) {
            List<HrSalaryBaseLevel> salaryBaseLevelList = payrollService.getAllSalaryBaseLevels(includeInactive);
            salaryBaseLevelGrid.setItems(salaryBaseLevelList);
        }
    }


    private  void populateAllowanceGrid(boolean includeInactive){
        if(this.auth.canView) {
            List<HrSalaryAllowance> salaryallowanceList = payrollService.getAllSalaryAllowances(includeInactive);
            salaryAllowanceGrid.setItems(salaryallowanceList);
        }
    }


    private void setListener() {
        // add button listener
        sblAddButton.addClickListener(e -> {
            HrSalaryBaseLevel newLevel = new HrSalaryBaseLevel();
            newLevel.setLevelCode("NEW LEVEL CODE");
            newLevel.setBaseSalary(BigDecimal.ZERO);
            newLevel.setStartDate(LocalDate.now());
            salaryBaseLevelGrid.getListDataView().addItem(newLevel);
            this.isBasicSalaryEdit = true;
        });

        // save button listener
        sblSaveButton.addClickListener(e -> {
            if(this.auth.canEdit && this.isBasicSalaryEdit) {

                List<HrSalaryBaseLevel> toSaveList = salaryBaseLevelGrid.getListDataView().getItems().toList();

                for (HrSalaryBaseLevel item : toSaveList) {
                    payrollService.saveSalaryBaseLevel(item, currentUser.require());
                }
                populateSalaryBaseLevelGrid(withInactiveBasicSalary.getValue());
                this.isBasicSalaryEdit = false;

                // Notification
                Notification.show("Data saved successfully");
            }
        });

        withInactiveBasicSalary.addValueChangeListener(e -> {
            populateSalaryBaseLevelGrid(e.getValue());
        });

        // add button listener
        saAddButton.addClickListener(e -> {
            HrSalaryAllowance newAllowance = new HrSalaryAllowance();
            newAllowance.setName("NEW ALLOWANCE");
            newAllowance.setAmount(BigDecimal.ZERO);
            newAllowance.setStartDate(LocalDate.now());
            salaryAllowanceGrid.getListDataView().addItem(newAllowance);
            this.isAllowanceEdit = true;
        });

        // save button listener
        saSaveButton.addClickListener(e -> {
            if(this.auth.canEdit && this.isAllowanceEdit) {

                List<HrSalaryAllowance> toSaveList = salaryAllowanceGrid.getListDataView().getItems().toList();

                for (HrSalaryAllowance item : toSaveList) {
                    payrollService.saveSalaryAllowance(item, currentUser.require());
                }
               populateAllowanceGrid(withInactiveAllowance.getValue());
                this.isAllowanceEdit = false;

                // Notification
                Notification.show("Data Allowance Saved Successfully");
            }
        });

        withInactiveBasicSalary.addValueChangeListener(e -> {
            populateAllowanceGrid(e.getValue());
        });
    }
}

