package com.fusi24.pangreksa.web.view.admin;

import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.*;
import com.fusi24.pangreksa.web.service.AdminService;
import com.fusi24.pangreksa.web.service.CommonService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.grid.Grid;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.StreamSupport;

import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.data.renderer.ComponentRenderer;

import com.vaadin.flow.component.notification.Notification;

@Route("responsibilities-page-access")
@PageTitle("Responsibilities")
@Menu(order = 4, icon = "vaadin:clipboard-check", title = "Responsibilities")
@RolesAllowed("RESPONSIBL")
//@PermitAll // When security is enabled, allow all authenticated users
public class ResponsibilitiesView extends Main {
    private static final long serialVersionUID = 4L;
    private static final Logger log = LoggerFactory.getLogger(ResponsibilitiesView.class);
    private final CurrentUser currentUser;
    private final AdminService adminService;
    private final CommonService commonService;
    private Authorization auth;

    public static final String VIEW_NAME = "Responsibilities";
    private VerticalLayout body;
    private Button populateButton;
    private Button saveButton;
    private Button addButton;

    ComboBox<FwResponsibilities> responsibilityDropdown;
    Grid<FwResponsibilitiesMenu> grid;
    List<FwPages> pagesList;

    private boolean isEdit = false;

    public ResponsibilitiesView(CurrentUser currentUser, AdminService adminService, CommonService commonService) {
        this.currentUser = currentUser;
        this.adminService = adminService;
        this.commonService = commonService;

        this.auth = commonService.getAuthorization(
                currentUser.require(),
                (String) UI.getCurrent().getSession().getAttribute("responsibility"),
                this.serialVersionUID);

        log.debug("Page {}, Authorization: {} {} {} {}", VIEW_NAME, auth.canView, auth.canCreate, auth.canEdit, auth.canDelete);

        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar(VIEW_NAME));
        add(createBody());
        add(addGrid());

        setListener();
        setAuthorization();
    }

    private void setAuthorization(){
        if(!this.auth.canView){
            this.populateButton.setEnabled(false);
        }
        if(!this.auth.canCreate){
            this.addButton.setEnabled(false);
            this.saveButton.setEnabled(false);
        }
        if(!this.auth.canEdit){
            this.saveButton.setEnabled(false);
        }
    }

    private Grid addGrid(){
        pagesList = adminService.findAllPages();

        this.grid = new Grid<>(FwResponsibilitiesMenu.class, false);

        // Sort Order editable
        grid.addColumn(new ComponentRenderer<>(menu -> {
            NumberField sortOrderField = new NumberField();
            sortOrderField.setValue(menu.getMenu() != null && menu.getMenu().getSortOrder() != null
                    ? menu.getMenu().getSortOrder().doubleValue() : 0.0);
            sortOrderField.setWidth("80px");
            sortOrderField.addValueChangeListener(e -> {
                if (menu.getMenu() != null) {
                    menu.getMenu().setSortOrder(e.getValue().intValue());
                    this.isEdit = true;
                    // Optionally persist change here
                }
            });
            return sortOrderField;
        })).setHeader("Sort Order").setFlexGrow(0).setAutoWidth(true);

        // Menu label editable
        grid.addColumn(new ComponentRenderer<>(menu -> {
            TextField labelField = new TextField();
            labelField.setValue(menu.getMenu() != null ? menu.getMenu().getLabel() : "");
            labelField.setWidth("250px");
            labelField.addValueChangeListener(e -> {
                if (menu.getMenu() != null) {
                    menu.getMenu().setLabel(e.getValue());
                    this.isEdit = true;
                    // Optionally persist change here
                }
            });
            return labelField;
        })).setHeader("Menu Label").setFlexGrow(1).setAutoWidth(true);

        // Pages Editable
        grid.addColumn(new ComponentRenderer<>(menu -> {
            ComboBox<FwPages> pagesDropdown = new ComboBox<>();
//            List<FwPages> pagesList = pagesRepository.findAll();
            pagesDropdown.setItems(pagesList);
            pagesDropdown.setItemLabelGenerator(FwPages::getDescription);
            pagesDropdown.setWidth("400px");
            if (menu.getMenu() != null && menu.getMenu().getPage() != null) {
                pagesDropdown.setValue(menu.getMenu().getPage());
            }
            pagesDropdown.addValueChangeListener(e -> {
                if (menu.getMenu() != null) {
                    menu.getMenu().setPage(e.getValue());
                    this.isEdit = true;
                    // Optionally persist change here
                }
            });
            return pagesDropdown;
        })).setHeader("Pages").setFlexGrow(1).setAutoWidth(true);

        // Can View editable
        grid.addColumn(new ComponentRenderer<>(menu -> {
            Checkbox viewCheckbox = new Checkbox(menu.getMenu() != null && Boolean.TRUE.equals(menu.getMenu().getCanView()));
            viewCheckbox.addValueChangeListener(e -> {
                if (menu.getMenu() != null) {
                    menu.getMenu().setCanView(e.getValue());
                    this.isEdit = true;
                    // Optionally persist change here
                }
            });
            return viewCheckbox;
        })).setHeader("Can View").setFlexGrow(0).setAutoWidth(true);

        // Can Create editable
        grid.addColumn(new ComponentRenderer<>(menu -> {
            Checkbox createCheckbox = new Checkbox(menu.getMenu() != null && Boolean.TRUE.equals(menu.getMenu().getCanCreate()));
            createCheckbox.addValueChangeListener(e -> {
                if (menu.getMenu() != null) {
                    menu.getMenu().setCanCreate(e.getValue());
                    this.isEdit = true;
                    // Optionally persist change here
                }
            });
            return createCheckbox;
        })).setHeader("Can Create").setFlexGrow(0).setAutoWidth(true);

        // Can Edit editable
        grid.addColumn(new ComponentRenderer<>(menu -> {
            Checkbox editCheckbox = new Checkbox(menu.getMenu() != null && Boolean.TRUE.equals(menu.getMenu().getCanEdit()));
            editCheckbox.addValueChangeListener(e -> {
                if (menu.getMenu() != null) {
                    menu.getMenu().setCanEdit(e.getValue());
                    this.isEdit = true;
                    // Optionally persist change here
                }
            });
            return editCheckbox;
        })).setHeader("Can Edit").setFlexGrow(0).setAutoWidth(true);

        // Can Delete editable
        grid.addColumn(new ComponentRenderer<>(menu -> {
            Checkbox deleteCheckbox = new Checkbox(menu.getMenu() != null && Boolean.TRUE.equals(menu.getMenu().getCanDelete()));
            deleteCheckbox.addValueChangeListener(e -> {
                if (menu.getMenu() != null) {
                    menu.getMenu().setCanDelete(e.getValue());
                    this.isEdit = true;
                    // Optionally persist change here
                }
            });
            return deleteCheckbox;
        })).setHeader("Can Delete").setFlexGrow(0).setAutoWidth(true);

        // Action column with delete button (icon only, no title)
        grid.addColumn(new ComponentRenderer<>(menu -> {
            Button deleteButton = new Button();
            deleteButton.setIcon(VaadinIcon.CLOSE.create());
            deleteButton.getElement().setAttribute("title", "Delete");
            deleteButton.addClickListener(e -> {
                grid.getListDataView().removeItem(menu);
                if (menu.getId() != null) {
                    adminService.deleteResponsibilityMenu(menu);
                }

                this.isEdit = true;
            });
            if(!this.auth.canDelete){
                deleteButton.setEnabled(false);
            }
            return deleteButton;
        })).setHeader("").setFlexGrow(0).setAutoWidth(false);

        return this.grid;
    }

    private VerticalLayout createBody(){
        this.body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);

        responsibilityDropdown = new ComboBox<>("Responsibility");
        populateButton = new Button("Populate");
        addButton = new Button("Add");
        saveButton = new Button("Save");

        responsibilityDropdown.setItemLabelGenerator(FwResponsibilities::getLabel);
        responsibilityDropdown.setItems(adminService.findActiveResponsibilities());

        responsibilityDropdown.getStyle().setWidth("300px");

        HorizontalLayout leftLayout = new HorizontalLayout(responsibilityDropdown, populateButton);
        leftLayout.setSpacing(true);
        leftLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        HorizontalLayout row = new HorizontalLayout();
        row.setWidthFull();
        row.add(leftLayout);
        row.add(saveButton, addButton);
        row.setAlignItems(HorizontalLayout.Alignment.BASELINE);
        row.expand(leftLayout); // leftLayout takes all available space, addButton stays right

        body.add(row);

        return body;
    }

    private void setListener(){
        responsibilityDropdown.addValueChangeListener( event -> {
            this.isEdit = false;
        });

        populateButton.addClickListener(event -> {
            if (!this.isEdit){
                List<FwResponsibilitiesMenu> fwResponsibilitiesMenu = adminService.findByResponsibilityMenu(responsibilityDropdown.getValue());
                log.debug("Selected Responsibility: {} get menus {}", responsibilityDropdown.getValue().getLabel(), fwResponsibilitiesMenu.size());
                grid.setItems(fwResponsibilitiesMenu);
                this.isEdit = false;
            } else {
                Notification.show("Please save your changes before populating the grid.");
            }
        });

        addButton.addClickListener(event -> {
            // Get Maximum number of sort order
            int maxSortOrder = StreamSupport.stream(this.grid.getListDataView().getItems().spliterator(), false)
                    .mapToInt(item -> item.getMenu() != null && item.getMenu().getSortOrder() != null
                            ? item.getMenu().getSortOrder()
                            : 0)
                    .max()
                    .orElse(0);

            // Add a new empty FwResponsibilitiesMenu to the grid
            FwResponsibilitiesMenu newRm = new FwResponsibilitiesMenu();
            FwMenus fwMenu = new FwMenus();
            fwMenu.setSortOrder(maxSortOrder + 1);
            fwMenu.setLabel("New Label");

            newRm.setMenu(fwMenu);
            newRm.setResponsibility(this.responsibilityDropdown.getValue());
            grid.getListDataView().addItem(newRm);
            this.isEdit = true;
        });

        saveButton.addClickListener(event -> {
            var user = currentUser.require();

            if (this.isEdit) {
                List<FwResponsibilitiesMenu> items = grid.getListDataView().getItems().toList();
                items.forEach(i -> {
                    i.setResponsibility(responsibilityDropdown.getValue());
                    adminService.saveResponsibilityMenu(i, user);
                });

                this.isEdit = false;
            }
        });
    }
}
