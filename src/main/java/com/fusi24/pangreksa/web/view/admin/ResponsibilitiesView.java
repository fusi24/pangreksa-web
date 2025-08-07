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
import com.vaadin.flow.component.tabs.TabSheet;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
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
    private Button saveMenuButton;
    private Button addMenuButton;
    private Button saveRespButton;
    private Button addRespButton;

    ComboBox<FwResponsibilities> responsibilityDropdown;
    Grid<FwResponsibilitiesMenu> menuGrid;
    Grid<FwResponsibilities> respGrid;
    List<FwPages> pagesList;

    private boolean isMenuEdit = false;
    private boolean isRespEdit = false;

    TabSheet tabsheet;

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

        setListener();
        setAuthorization();
    }

    private void setAuthorization(){
        if(!this.auth.canView){
            this.populateButton.setEnabled(false);
        }
        if(!this.auth.canCreate){
            this.addMenuButton.setEnabled(false);
            this.saveMenuButton.setEnabled(false);
        }
        if(!this.auth.canEdit){
            this.saveMenuButton.setEnabled(false);
        }
    }

    private Grid addMenuListGrid(){
        pagesList = adminService.findAllPages();

        this.menuGrid = new Grid<>(FwResponsibilitiesMenu.class, false);

        // Sort Order editable
        menuGrid.addColumn(new ComponentRenderer<>(menu -> {
            NumberField sortOrderField = new NumberField();
            sortOrderField.setValue(menu.getMenu() != null && menu.getMenu().getSortOrder() != null
                    ? menu.getMenu().getSortOrder().doubleValue() : 0.0);
            sortOrderField.setWidth("80px");
            sortOrderField.addValueChangeListener(e -> {
                if (menu.getMenu() != null) {
                    menu.getMenu().setSortOrder(e.getValue().intValue());
                    this.isMenuEdit = true;
                    // Optionally persist change here
                }
            });
            return sortOrderField;
        })).setHeader("Sort Order").setFlexGrow(0).setAutoWidth(true);

        // Menu label editable
        menuGrid.addColumn(new ComponentRenderer<>(menu -> {
            TextField labelField = new TextField();
            labelField.setValue(menu.getMenu() != null ? menu.getMenu().getLabel() : "");
            labelField.setWidth("250px");
            labelField.addValueChangeListener(e -> {
                if (menu.getMenu() != null) {
                    menu.getMenu().setLabel(e.getValue());
                    this.isMenuEdit = true;
                    // Optionally persist change here
                }
            });
            return labelField;
        })).setHeader("Menu Label").setFlexGrow(1).setAutoWidth(true);

        // Pages Editable
        menuGrid.addColumn(new ComponentRenderer<>(menu -> {
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
                    this.isMenuEdit = true;
                    // Optionally persist change here
                }
            });
            return pagesDropdown;
        })).setHeader("Pages").setFlexGrow(1).setWidth("400px");

        // Can View editable
        menuGrid.addColumn(new ComponentRenderer<>(menu -> {
            Checkbox viewCheckbox = new Checkbox(menu.getMenu() != null && Boolean.TRUE.equals(menu.getMenu().getCanView()));
            viewCheckbox.addValueChangeListener(e -> {
                if (menu.getMenu() != null) {
                    menu.getMenu().setCanView(e.getValue());
                    this.isMenuEdit = true;
                    // Optionally persist change here
                }
            });
            return viewCheckbox;
        })).setHeader("Can View").setFlexGrow(0).setAutoWidth(true);

        // Can Create editable
        menuGrid.addColumn(new ComponentRenderer<>(menu -> {
            Checkbox createCheckbox = new Checkbox(menu.getMenu() != null && Boolean.TRUE.equals(menu.getMenu().getCanCreate()));
            createCheckbox.addValueChangeListener(e -> {
                if (menu.getMenu() != null) {
                    menu.getMenu().setCanCreate(e.getValue());
                    this.isMenuEdit = true;
                    // Optionally persist change here
                }
            });
            return createCheckbox;
        })).setHeader("Can Create").setFlexGrow(0).setAutoWidth(true);

        // Can Edit editable
        menuGrid.addColumn(new ComponentRenderer<>(menu -> {
            Checkbox editCheckbox = new Checkbox(menu.getMenu() != null && Boolean.TRUE.equals(menu.getMenu().getCanEdit()));
            editCheckbox.addValueChangeListener(e -> {
                if (menu.getMenu() != null) {
                    menu.getMenu().setCanEdit(e.getValue());
                    this.isMenuEdit = true;
                    // Optionally persist change here
                }
            });
            return editCheckbox;
        })).setHeader("Can Edit").setFlexGrow(0).setAutoWidth(true);

        // Can Delete editable
        menuGrid.addColumn(new ComponentRenderer<>(menu -> {
            Checkbox deleteCheckbox = new Checkbox(menu.getMenu() != null && Boolean.TRUE.equals(menu.getMenu().getCanDelete()));
            deleteCheckbox.addValueChangeListener(e -> {
                if (menu.getMenu() != null) {
                    menu.getMenu().setCanDelete(e.getValue());
                    this.isMenuEdit = true;
                    // Optionally persist change here
                }
            });
            return deleteCheckbox;
        })).setHeader("Can Delete").setFlexGrow(0).setAutoWidth(true);

        // Action column with delete button (icon only, no title)
        menuGrid.addColumn(new ComponentRenderer<>(menu -> {
            Button deleteButton = new Button();
            deleteButton.setIcon(VaadinIcon.CLOSE.create());
            deleteButton.getElement().setAttribute("title", "Delete");
            deleteButton.addClickListener(e -> {
                menuGrid.getListDataView().removeItem(menu);
                if (menu.getId() != null) {
                    adminService.deleteResponsibilityMenu(menu);
                }

                this.isMenuEdit = true;
            });
            if(!this.auth.canDelete){
                deleteButton.setEnabled(false);
            }
            return deleteButton;
        })).setHeader("").setFlexGrow(0).setAutoWidth(false);

        return this.menuGrid;
    }

    private Grid addResponsibilitiesListGrid()  {
        this.respGrid = new Grid<>(FwResponsibilities.class, false);

        // Sort Order editable
        respGrid.addColumn(new ComponentRenderer<>(resp -> {
            NumberField sortOrderField = new NumberField();
            sortOrderField.setValue(resp != null && resp.getSortOrder() != null
                    ? resp.getSortOrder().doubleValue() : 0.0);
            sortOrderField.setWidth("80px");
            sortOrderField.addValueChangeListener(e -> {
                if (resp.getSortOrder() != null) {
                    resp.setSortOrder(e.getValue().intValue());
                    this.isRespEdit = true;
                    // Optionally persist change here
                }
            });
            return sortOrderField;
        })).setHeader("Sort Order").setFlexGrow(0).setAutoWidth(true);

        // Editable Label column
        respGrid.addColumn(new ComponentRenderer<>(resp -> {
            TextField labelField = new TextField();
            labelField.setValue(resp.getLabel());
            labelField.setWidthFull();
            labelField.addValueChangeListener(e -> {
                resp.setLabel(e.getValue());
                this.isRespEdit = true;
            });
            return labelField;
        })).setHeader("Responsibility").setFlexGrow(1);

        // Editable Description column
        respGrid.addColumn(new ComponentRenderer<>(resp -> {
            TextField descField = new TextField();
            descField.setValue(resp.getDescription());
            descField.setWidthFull();
            descField.addValueChangeListener(e -> {
                resp.setDescription(e.getValue());
                this.isRespEdit = true;
            });
            return descField;
        })).setHeader("Description").setFlexGrow(2);

        // Editable Active column
        respGrid.addColumn(new ComponentRenderer<>(resp -> {
            Checkbox activeCheckbox = new Checkbox(Boolean.TRUE.equals(resp.getIsActive()));
            activeCheckbox.addValueChangeListener(e -> {
                resp.setIsActive(e.getValue());
                this.isRespEdit = true;
            });
            return activeCheckbox;
        })).setHeader("active").setFlexGrow(2);

        // Action column with delete button (icon only, no title)
        respGrid.addColumn(new ComponentRenderer<>(resp -> {
            Button deleteButton = new Button();
            deleteButton.setIcon(VaadinIcon.CLOSE.create());
            deleteButton.getElement().setAttribute("title", "Delete");
            deleteButton.addClickListener(e -> {
                respGrid.getListDataView().removeItem(resp);
                this.isMenuEdit = true;
            });

            if (resp.getId() == null)
                return deleteButton;
            return null;
        })).setHeader("").setFlexGrow(0).setAutoWidth(false);

        respGrid.setItems(adminService.findAllResponsibilities());

//        respGrid.addItemClickListener(event -> {
//            FwResponsibilities selectedResponsibility = event.getItem();
//            responsibilityDropdown.setValue(selectedResponsibility);
//            this.isRespEdit = false;
//        });

        return respGrid;
    }

    private VerticalLayout createBody(){
        this.body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);

        responsibilityDropdown = new ComboBox<>("Responsibility");
        populateButton = new Button("Populate");
        addMenuButton = new Button("Add");
        saveMenuButton = new Button("Save");
        addRespButton = new Button("Add");
        saveRespButton = new Button("Save");

        responsibilityDropdown.setItemLabelGenerator(FwResponsibilities::getLabel);
        responsibilityDropdown.setItems(adminService.findActiveResponsibilities());

        responsibilityDropdown.getStyle().setWidth("300px");

        HorizontalLayout leftLayout = new HorizontalLayout(responsibilityDropdown, populateButton);
        leftLayout.setSpacing(true);
        leftLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        HorizontalLayout functionMenuListGrid = new HorizontalLayout();
        functionMenuListGrid.setWidthFull();
        functionMenuListGrid.add(leftLayout);
        functionMenuListGrid.add(saveMenuButton, addMenuButton);
        functionMenuListGrid.setAlignItems(HorizontalLayout.Alignment.BASELINE);
        functionMenuListGrid.expand(leftLayout); // leftLayout takes all available space, addButton stays right

        HorizontalLayout functionResponsibilitiesListGrid = new HorizontalLayout();
        functionResponsibilitiesListGrid.setWidthFull();
        functionResponsibilitiesListGrid.add(saveRespButton, addRespButton);
        functionResponsibilitiesListGrid.setAlignItems(HorizontalLayout.Alignment.BASELINE);
        //set to right align
        functionResponsibilitiesListGrid.getStyle().set("justify-content", "flex-end");


        this.tabsheet = new TabSheet();
        tabsheet.setWidthFull();
        tabsheet.add("Menu Management", new VerticalLayout(functionMenuListGrid, addMenuListGrid()));
        tabsheet.add("Responsibility List", new VerticalLayout(functionResponsibilitiesListGrid, addResponsibilitiesListGrid()));

        body.add(tabsheet);

        return body;
    }

    private void setListener(){
        responsibilityDropdown.addValueChangeListener( event -> {
            this.isMenuEdit = false;
        });

        populateButton.addClickListener(event -> {
            if (!this.isMenuEdit && !responsibilityDropdown.isEmpty()){
                List<FwResponsibilitiesMenu> fwResponsibilitiesMenu = adminService.findByResponsibilityMenu(responsibilityDropdown.getValue());
                log.debug("Selected Responsibility: {} get menus {}", responsibilityDropdown.getValue().getLabel(), fwResponsibilitiesMenu.size());
                // Ensure the list is mutable to allow addItem/removeItem
                menuGrid.setItems(new ArrayList<>(fwResponsibilitiesMenu));
                this.isMenuEdit = false;
            } else {
                Notification.show("Please select responsibility before populating the grid.");
            }
        });

        addMenuButton.addClickListener(event -> {
            // Get Maximum number of sort order
            int maxSortOrder = StreamSupport.stream(this.menuGrid.getListDataView().getItems().spliterator(), false)
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
            menuGrid.getListDataView().addItem(newRm);

            this.isMenuEdit = true;
        });

        saveMenuButton.addClickListener(event -> {
            var user = currentUser.require();

            if (this.isMenuEdit) {
                List<FwResponsibilitiesMenu> items = menuGrid.getListDataView().getItems().toList();
                items.forEach(i -> {
                    i.setResponsibility(responsibilityDropdown.getValue());
                    adminService.saveResponsibilityMenu(i, user);
                });

                this.isMenuEdit = false;

                Notification.show("Changes saved successfully.");
            }
        });

        addRespButton.addClickListener(event -> {
            // Get Maximum number of sort order
            int maxSortOrder = StreamSupport.stream(this.respGrid.getListDataView().getItems().spliterator(), false)
                    .mapToInt(item -> item != null && item.getSortOrder() != null
                            ? item.getSortOrder()
                            : 0)
                    .max()
                    .orElse(0);

            // Add a new empty FwResponsibilitiesMenu to the grid
            FwResponsibilities resp = new FwResponsibilities();
            resp.setSortOrder(maxSortOrder + 1);
            resp.setLabel("New Responsibilities");
            resp.setIsActive(false);
            resp.setDescription("New Responsibilities Description");
            respGrid.getListDataView().addItem(resp);

            this.isRespEdit = true;
        });

        saveRespButton.addClickListener(event -> {
            var user = currentUser.require();

            if (this.isRespEdit) {
                List<FwResponsibilities> items = respGrid.getListDataView().getItems().toList();
                items.forEach(i -> {
                    adminService.saveResponsibility(i, user);
                    log.debug("Saving Responsibility: {} with Sort Order: {}", i.getLabel(), i.getSortOrder());
                });

                this.isRespEdit = false;

                Notification.show("Responsibility List saved successfully.");

                this.responsibilityDropdown.setItems(adminService.findActiveResponsibilities());
                this.menuGrid.setItems(Collections.emptyList());
            }
        });
    }
}
