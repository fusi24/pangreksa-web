package com.fusi24.pangreksa.web.view.manager;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
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
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
// Vaadin Components
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.textfield.IntegerField;

// Java
import java.math.BigDecimal;
import com.fusi24.pangreksa.web.model.entity.MasterPtkp;
import com.fusi24.pangreksa.web.service.MasterPtkpService;

import java.util.List;

@Route("bpjs-dan-pajak-list-page-access")
@PageTitle("BPJS & Pajak")
@Menu(order = 26, icon = "vaadin:clipboard-check", title = "BPJS & Pajak")
@RolesAllowed("BPJS_PAJAK")
//@PermitAll // When security is enabled, allow all authenticated users
public class BpjsPajakView extends Main {
    private static final long serialVersionUID = 1092L;
    private static final Logger log = LoggerFactory.getLogger(BpjsPajakView.class);
    private final CurrentUser currentUser;
    private final CommonService commonService;
    private final SystemService systemService;
    private Authorization auth;

    public static final String VIEW_NAME = "BPJS & Pajak";

    private VerticalLayout body;

    private VirtualList<FwSystem> virtualList;

    Button saveButton;

    private HorizontalLayout toolbarLayoutMaster;


    private List<FwSystem> systemList;

    private List<String> idStrings = List.of(
            "1efc0c30-ab18-4f85-93e7-144c44165071",
            "ba8979a0-ac03-420e-a418-49b9d9915d8a",
            "f56c31fb-f1c0-4660-adc5-1a458fccb96d",
            "3ae1184a-b6b8-412c-9ee8-13e478796aa4",
            "8a06244d-ebe9-4a4a-a71c-e947759dcec6",
            "3d1426fb-4503-4085-abc4-51c9469aa1c9",
            "e53eb8ac-a133-481a-a0ad-1cb892f0b740"
    );

    private Grid<MasterPtkp> ptkpGrid;
    private List<MasterPtkp> ptkpList;
    private final MasterPtkpService masterPtkpService;

    public BpjsPajakView(CurrentUser currentUser,
                         CommonService commonService,
                         SystemService systemService,
                         MasterPtkpService masterPtkpService) {

        this.currentUser = currentUser;   // ✅ FIX UTAMA
        this.commonService = commonService;
        this.systemService = systemService;
        this.masterPtkpService = masterPtkpService;

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
            // User does not have permission to view this page
        }
    }


    private Component createPtkpTable() {

        ptkpGrid = new Grid<>(MasterPtkp.class, false);

        ptkpGrid.addColumn(MasterPtkp::getKodePtkp)
                .setHeader("Kode PTKP");

        ptkpGrid.addColumn(MasterPtkp::getGolongan)
                .setHeader("Golongan");

        ptkpGrid.addColumn(MasterPtkp::getJumlahTanggungan)
                .setHeader("Jumlah Tanggungan");

        ptkpGrid.addColumn(MasterPtkp::getNominal)
                .setHeader("Nominal (Rp)");

        ptkpGrid.addColumn(new ComponentRenderer<>(ptkp -> {
            Button edit = new Button("Edit", ev -> openPtkpDialog(ptkp));
            Button del = new Button("Delete");

            edit.setEnabled(true);
            del.setEnabled(true);

            del.addClickListener(e -> {
                masterPtkpService.delete(ptkp);
                refreshPtkpGrid();
            });

            return new HorizontalLayout(edit, del);
        })).setHeader("Action");

        refreshPtkpGrid();
        ptkpGrid.setWidthFull();

        return ptkpGrid;
    }


    private void refreshPtkpGrid() {
        ptkpList = masterPtkpService.findAll();
        ptkpGrid.setItems(ptkpList);
    }

    private void openPtkpDialog(MasterPtkp existing) {

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existing == null ? "Tambah PTKP" : "Edit PTKP");

        TextField kode = new TextField("Kode PTKP");
        TextField gol = new TextField("Golongan");
        IntegerField tanggungan = new IntegerField("Jumlah Tanggungan");
        tanggungan.setMin(0);
        tanggungan.setMax(3);
        NumberField nominal = new NumberField("Nominal");

        if (existing != null) {
            kode.setValue(existing.getKodePtkp());
            gol.setValue(existing.getGolongan());
            tanggungan.setValue(existing.getJumlahTanggungan());
            nominal.setValue(existing.getNominal().doubleValue());
        }

        Button save = new Button("Save", evt -> {

            MasterPtkp m = (existing == null ? new MasterPtkp() : existing);

            m.setKodePtkp(kode.getValue());
            m.setGolongan(gol.getValue());
            m.setJumlahTanggungan(tanggungan.getValue());
            m.setNominal(BigDecimal.valueOf(nominal.getValue()));

            masterPtkpService.save(m);

            refreshPtkpGrid();
            dialog.close();
        });

        Button cancel = new Button("Cancel", evt -> dialog.close());

        dialog.add(kode, gol, tanggungan, nominal);
        dialog.getFooter().add(cancel, save);

        dialog.open();
    }



    private void createBody() {

        this.body = new VerticalLayout();
        body.setPadding(true);
        body.setSpacing(true);
        body.setWidthFull();

        // ====== TOOLBAR ATAS ======
        toolbarLayoutMaster = new HorizontalLayout();
        toolbarLayoutMaster.setAlignItems(FlexComponent.Alignment.CENTER);
        toolbarLayoutMaster.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        toolbarLayoutMaster.setWidthFull();
        toolbarLayoutMaster.setSpacing(true);

        saveButton = new Button("Save");
        Button btnAddPtkp = new Button("Add Master PTKP");

        btnAddPtkp.addClickListener(e -> openPtkpDialog(null));

        toolbarLayoutMaster.add(saveButton, btnAddPtkp);

        // ====== JUDUL MASTER PTKP ======
        H3 ptkpTitle = new H3("Master PTKP");

        // ====== GRID PTKP ======
        Component ptkpTable = createPtkpTable();

        // ====== SYSTEM LIST (VirtualList) ======
        this.systemList = systemService.findSystemsByIds(this.idStrings);
        virtualList = new VirtualList<>();
        virtualList.setItems(systemList);
        virtualList.setRenderer(systemRenderer);

        VerticalLayout systemSection = new VerticalLayout();
        systemSection.setPadding(true);
        systemSection.setSpacing(true);

        H3 systemTitle = new H3("Pengaturan Sistem");
        systemSection.add(systemTitle, virtualList);

        // ====== TAMBAHKAN KE BODY ======
        body.add(
                toolbarLayoutMaster,
                ptkpTitle,
                ptkpTable,
                systemSection
        );

        add(body);
    }

    private void setListener() {
        // SaveButton addclick listener to get all data from virtualList and print using log.debug
        saveButton.addClickListener(event -> {
            for (FwSystem system : systemList) {
                log.debug("Saving Key: {}, Type: {}, Value: {}", system.getKey(), system.getType(), system.getStringVal());
                systemService.saveSystem(system);
            }

            Notification.show("Settings saved successfully!");
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

