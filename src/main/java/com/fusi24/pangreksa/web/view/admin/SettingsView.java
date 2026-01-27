package com.fusi24.pangreksa.web.view.admin;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.base.util.DatePickerUtil;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.FwSystem;
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.SystemService;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.HasSize;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.virtuallist.VirtualList;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Route("settings-page-access")
@PageTitle("Settings")
@Menu(order = 32, icon = "vaadin:cog", title = "Settings")
@RolesAllowed("PAGEACCESS")
//@PermitAll // When security is enabled, allow all authenticated users
public class SettingsView extends Main {
    private static final long serialVersionUID = 29L;
    private static final Logger log = LoggerFactory.getLogger(SettingsView.class);
    private final CurrentUser currentUser;
    private final CommonService commonService;
    private final SystemService systemService;
    private Authorization auth;

    public static final String VIEW_NAME = "Settings";

    private VerticalLayout body;
    private VirtualList<FwSystem> virtualList;

    Button saveButton;

    private HorizontalLayout toolbarLayoutMaster;

    private List<FwSystem> systemList;

    public SettingsView(CurrentUser currentUser, CommonService commonService, SystemService systemService) {
        this.currentUser = currentUser;
        this.commonService = commonService;
        this.systemService = systemService;

        this.auth = commonService.getAuthorization(
                currentUser.require(),
                (String) UI.getCurrent().getSession().getAttribute("responsibility"),
                this.serialVersionUID);

        log.debug("Page {}, Authorization: {} {} {} {}", VIEW_NAME, auth.canView, auth.canCreate, auth.canEdit, auth.canDelete);

        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        this.setHeightFull();

        add(new ViewToolbar(VIEW_NAME));
        createBody();

        setListener();
        setAuthorization();
    }

    private void setAuthorization(){
        if(!this.auth.canView){
            // User does not have permission to view this page
        }
    }

    private void createBody() {
        this.body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);

        body.setHeightFull();

        // Inisiasi toolbar Master
        toolbarLayoutMaster = new HorizontalLayout();
        toolbarLayoutMaster.setAlignItems(FlexComponent.Alignment.END);

        saveButton = new Button("Simpan");

        toolbarLayoutMaster.add(saveButton);
        toolbarLayoutMaster.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        toolbarLayoutMaster.setWidthFull();

        this.systemList =  systemService.findAllSystems();
        virtualList = new VirtualList<>();
        virtualList.setItems(systemList);
        virtualList.setRenderer(systemRenderer);

        body.add(toolbarLayoutMaster, virtualList);

        add(body);
    }

    private void setListener() {
        // Add listeners for UI components here
        // For example, you can add a click listener to a button
        // saveButton.addClickListener(event -> savePage());

        // SaveButton addclick listener to get all data from virtualList and print using log.debug
        saveButton.addClickListener(event -> {
            for (FwSystem system : systemList) {
                log.debug("Saving Key: {}, Type: {}, Value: {}", system.getKey(), system.getType(), system.getStringVal());
                systemService.saveSystem(system);
            }

            Notification.show("Settings berhasil tersimpan!");
        });
    }

    private ComponentRenderer<Component, FwSystem> systemRenderer = new ComponentRenderer<>(
            system -> {
                Text keyText = new Text(system.getKey());
                Component component;

                String type = system.getType().toString();
                switch (type) {
                    case "BOOL": {
                        Checkbox cb = new Checkbox();
                        cb.setValue(system.getBooleanVal() != null ? system.getBooleanVal() : false);
                        cb.addValueChangeListener(e -> system.setBooleanVal(e.getValue()));
                        component = cb;
                        break;
                    }
                    case "INT": {
                        IntegerField tf = new IntegerField();
                        tf.setStepButtonsVisible(true);
                        tf.setMin(0);
                        tf.setMax( system.getIntVal() > 100 ? 100000000 : 100);
                        tf.setValue(system.getIntVal() != null ? system.getIntVal() : 0);
                        tf.addValueChangeListener(e -> system.setIntVal(e.getValue() != null ? e.getValue() : null));
                        component = tf;
                        break;
                    }
                    case "DATE": {
                        DatePicker dp = new DatePicker();
                        dp.setI18n(DatePickerUtil.getIndonesianI18n());
                        dp.setValue(system.getDateVal() != null ? system.getDateVal() : null);
                        dp.addValueChangeListener(e -> system.setDateVal(e.getValue()));
                        component = dp;
                        break;
                    }
                    case "DATETIME": {
                        DateTimePicker dtp = new DateTimePicker();
                        dtp.setValue(system.getDatetimeVal() != null ? java.time.LocalDateTime.parse(system.getDatetimeVal().toString()) : null);
                        dtp.addValueChangeListener(e -> system.setDatetimeVal(e.getValue()));
                        component = dtp;
                        break;
                    }
                    default: {
                        TextField tf = new TextField();
                        tf.setValue(system.getStringVal() != null ? system.getStringVal() : "");
                        tf.addValueChangeListener(e -> system.setStringVal(e.getValue()));
                        component = tf;
                        break;
                    }
                }

                Text descText = new Text(system.getDescription());

                HorizontalLayout layout = new HorizontalLayout();
                layout.setAlignItems(FlexComponent.Alignment.CENTER);

                // Key column
                VerticalLayout keyCol = new VerticalLayout(keyText);
                keyCol.setPadding(false);
                keyCol.setSpacing(false);
                keyCol.setWidth("300px");

                // Component column
                VerticalLayout compCol = new VerticalLayout(component);
                compCol.setPadding(false);
                compCol.setSpacing(false);
                compCol.setWidth("500px");
                if (component instanceof HasSize) {
                    ((HasSize) component).setWidthFull();
                }

                // Description column
                VerticalLayout descCol = new VerticalLayout(descText);
                descCol.setPadding(false);
                descCol.setSpacing(false);
                descCol.setWidth("500px");

                layout.add(keyCol, compCol, descCol);
                layout.setWidthFull();

                return layout;
            });
}
