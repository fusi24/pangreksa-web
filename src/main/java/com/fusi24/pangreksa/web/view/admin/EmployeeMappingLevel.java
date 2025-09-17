package com.fusi24.pangreksa.web.view.admin;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.dto.UserLevelRow;
import com.fusi24.pangreksa.web.model.entity.HrSalaryBaseLevel;
import com.fusi24.pangreksa.web.service.AdminService;
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.PersonService;
import com.fusi24.pangreksa.web.service.SalaryLevelService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.util.*;

@Route("employee-mapping-level")
@PageTitle("Mapping Employee Level")
@Menu(order = 33, icon = "vaadin:user", title = "Employee Level Mapping")
@RolesAllowed("USERS_MGT")
public class EmployeeMappingLevel extends Main {
    private static final long serialVersionUID = 33L;
    private static final Logger log = LoggerFactory.getLogger(EmployeeMappingLevel.class);

    private final CurrentUser currentUser;
    private final CommonService commonService;
    private final AdminService adminService;
    private final PersonService personService;
    private final SalaryLevelService salaryLevelService;

    public static final String VIEW_NAME = "Employee Level Mapping";

    private List<UserLevelRow> rows = new ArrayList<>();
    private Grid<UserLevelRow> grid;
    private Button populateButton;
    private Button saveButton;
    private com.vaadin.flow.component.textfield.TextField searchField;
    private VerticalLayout body;

    public EmployeeMappingLevel(CurrentUser currentUser,
                                CommonService commonService,
                                AdminService adminService,
                                PersonService personService,
                                SalaryLevelService salaryLevelService) {
        this.currentUser = currentUser;
        this.commonService = commonService;
        this.adminService = adminService;
        this.personService = personService;
        this.salaryLevelService = salaryLevelService;

        addClassNames(
                LumoUtility.BoxSizing.BORDER,
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Gap.SMALL
        );

        add(new ViewToolbar(VIEW_NAME));
        createBody();
        setListener();
    }

    private void createBody() {
        this.body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);

        searchField = new com.vaadin.flow.component.textfield.TextField("Search Filter");
        populateButton = new Button("Populate");
        saveButton = new Button("Save");

        HorizontalLayout leftLayout = new HorizontalLayout(searchField, populateButton);
        leftLayout.setSpacing(true);
        leftLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.add(leftLayout);
        row.add(saveButton);
        row.setAlignItems(HorizontalLayout.Alignment.BASELINE);
        row.expand(leftLayout);

        // === Grid ===
        grid = new Grid<>(UserLevelRow.class, false);

        // 1) Kolom User (Nickname)
        grid.addColumn(UserLevelRow::getDisplayName)
                .setHeader("User (Nickname)")
                .setAutoWidth(true)
                .setFlexGrow(1)
                .setSortable(true);

        // 2) Kolom Level (ComboBox)
        grid.addColumn(new ComponentRenderer<>(rowItem -> {
                    ComboBox<HrSalaryBaseLevel> cb = new ComboBox<>();
                    cb.setPlaceholder("Pilih level");
                    cb.setClearButtonVisible(true);
                    cb.setItemLabelGenerator(HrSalaryBaseLevel::getLevelCode);

                    Long companyId = rowItem.getCompanyId();
                    List<HrSalaryBaseLevel> options =
                            salaryLevelService.findActiveBaseLevels(LocalDate.now(), companyId);
                    cb.setItems(options);

                    // set nilai awal
                    if (rowItem.getSelectedBaseLevelId() != null) {
                        options.stream()
                                .filter(b -> Objects.equals(b.getId(), rowItem.getSelectedBaseLevelId()))
                                .findFirst()
                                .ifPresent(cb::setValue);
                    }

                    cb.addValueChangeListener(e -> {
                        HrSalaryBaseLevel v = e.getValue();
                        if (v != null) {
                            rowItem.setSelectedBaseLevelId(v.getId());
                            rowItem.setSelectedLevelCode(v.getLevelCode());
                            rowItem.setSelectedBaseSalary(v.getBaseSalary());
                        } else {
                            rowItem.setSelectedBaseLevelId(null);
                            rowItem.setSelectedLevelCode(null);
                            rowItem.setSelectedBaseSalary(null);
                        }
                        rowItem.setDirty(true);
                        grid.getDataProvider().refreshItem(rowItem);
                    });

                    return cb;
                })).setHeader("Level")
                .setAutoWidth(true)
                .setFlexGrow(0);

        // 3) Kolom Base Salary (read-only)
        grid.addColumn(rowItem -> {
                    if (rowItem.getSelectedBaseSalary() == null) return "";
                    return formatCurrency(rowItem.getSelectedBaseSalary());
                }).setHeader("Base Salary")
                .setAutoWidth(true)
                .setFlexGrow(0);

        body.add(row, grid);
        add(body);
    }

    private void setListener() {
        populateButton.addClickListener(e -> {
            String keyword = (searchField.getValue() == null || searchField.getValue().isBlank())
                    ? null : searchField.getValue().trim();

            rows = salaryLevelService.findUserLevelRows(keyword);
            grid.setItems(rows);
        });

        saveButton.addClickListener(e -> {
            var toSave = grid.getListDataView().getItems().toList();
            salaryLevelService.upsertMappings(toSave, currentUser.require());   // <-- tanpa actor
            populateButton.click();
        });
    }

    private String formatCurrency(BigDecimal v) {
        NumberFormat nf = NumberFormat.getNumberInstance(new Locale("id", "ID"));
        nf.setGroupingUsed(true);
        nf.setMinimumFractionDigits(0);
        nf.setMaximumFractionDigits(2);
        return nf.format(v);
    }
}
