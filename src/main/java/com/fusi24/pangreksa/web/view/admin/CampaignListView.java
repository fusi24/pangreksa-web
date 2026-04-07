package com.fusi24.pangreksa.web.view.admin;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.base.ui.notification.AppNotification;
import com.fusi24.pangreksa.web.model.entity.Campaign;
import com.fusi24.pangreksa.web.service.CampaignService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;

import java.io.ByteArrayInputStream;
import java.time.format.DateTimeFormatter;

@Route("campaign-list")
@PageTitle("Manajemen Campaign Internal")
@Menu(order = 19, icon = "vaadin:table", title = "Daftar Campaign")
@RolesAllowed("CAMPAIGN")
public class CampaignListView extends VerticalLayout {

    private final CampaignService campaignService;
    private Grid<Campaign> grid = new Grid<>(Campaign.class, false);
    private DateTimeFormatter df = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public CampaignListView(CampaignService campaignService) {
        this.campaignService = campaignService;
        setSizeFull();
        setPadding(true);

        add(new ViewToolbar("Manajemen Campaign Internal"));
        add(createFilterBar());
        configureGrid();
        add(grid);

        updateList();
    }

    private HorizontalLayout createFilterBar() {
        TextField search = new TextField();
        search.setPlaceholder("Cari berdasarkan judul...");
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setWidth("300px");

        ComboBox<String> statusFilter = new ComboBox<>("", "Semua Status", "AKTIF", "TERJADWAL", "BERAKHIR", "DRAFT");
        statusFilter.setValue("Semua Status");

        ComboBox<String> categoryFilter = new ComboBox<>("", "Semua Kategori", "Event", "Kebijakan", "Wellness");
        categoryFilter.setValue("Semua Kategori");

        Button btnFilter = new Button("Terapkan Filter");

        Button btnAdd = new Button("Tambah Campaign Baru", VaadinIcon.PLUS.create());
        btnAdd.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnAdd.getStyle().set("background-color", "#002d5d");
        btnAdd.addClickListener(e -> UI.getCurrent().navigate(CampaignManagementView.class));

        HorizontalLayout layout = new HorizontalLayout(search, statusFilter, categoryFilter, btnFilter, btnAdd);
        layout.setVerticalComponentAlignment(Alignment.END, btnAdd);
        layout.setWidthFull();
        layout.setFlexGrow(1, search);
        return layout;
    }

    private void configureGrid() {
        grid.setSelectionMode(Grid.SelectionMode.MULTI);

        // 1. Thumbnail Column
        grid.addComponentColumn(c -> {
            byte[] bytes = campaignService.getImagePathAsByteArray(c.getImagePath());
            if (bytes != null) {
                StreamResource res = new StreamResource("thumb", () -> new ByteArrayInputStream(bytes));
                Image img = new Image(res, "thumb");
                img.setWidth("40px");
                img.setHeight("40px");
                img.addClassName(LumoUtility.BorderRadius.SMALL);
                return img;
            }
            return VaadinIcon.PICTURE.create();
        }).setHeader("Thumbnail").setWidth("80px").setFlexGrow(0);

        // 2. Data Columns
        grid.addColumn(Campaign::getTitle).setHeader("Judul Campaign").setSortable(true);
        grid.addColumn(Campaign::getCategory).setHeader("Kategori");
        grid.addColumn(c -> c.getStartDate().format(df) + " - " + c.getEndDate().format(df)).setHeader("Periode Tayang");
        grid.addColumn(Campaign::getTargetAudience).setHeader("Target Audience");

        // 3. Engagement
        grid.addColumn(c -> c.getClickCount() + " / " + c.getViewCount()).setHeader("Engagement (Klik/Lihat)");

        // 4. Status Otomatis
        grid.addComponentColumn(c -> {
            String status = campaignService.calculateStatus(c);
            Span s = new Span(status);
            s.getElement().getThemeList().add("badge pill"); // Gunakan style standar Vaadin
            return s;
        }).setHeader("Status");

        // 5. Aksi
        // ... di dalam method configureGrid()
        grid.addComponentColumn(c -> {
            HorizontalLayout actions = new HorizontalLayout();
            actions.setSpacing(true);

            Button edit = new Button(VaadinIcon.PENCIL.create(), e -> {
                // Navigasi yang benar untuk HasUrlParameter
                getUI().ifPresent(ui -> ui.navigate(CampaignManagementView.class, c.getId().toString()));
            });
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

            // Tombol Hapus
            Button delete = new Button(VaadinIcon.TRASH.create(), e -> {
                confirmDelete(c);
            });
            delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            delete.setTooltipText("Hapus Campaign");

            actions.add(edit, delete);
            return actions;
        }).setHeader("Aksi").setFlexGrow(0).setWidth("120px");




        grid.setSizeFull();
        grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES);
    }

    private void confirmDelete(Campaign campaign) {
        com.vaadin.flow.component.confirmdialog.ConfirmDialog dialog = new com.vaadin.flow.component.confirmdialog.ConfirmDialog();
        dialog.setHeader("Hapus Campaign");
        dialog.setText("Apakah Anda yakin ingin menghapus campaign '" + campaign.getTitle() + "'? File gambar juga akan dihapus dari server.");

        dialog.setCancelable(true);
        dialog.setCancelText("Batal");

        dialog.setConfirmText("Hapus");
        dialog.setConfirmButtonTheme("error primary");

        dialog.addConfirmListener(e -> {
            try {
                // Memanggil service yang sudah kita buat (menghapus file & DB)
                campaignService.deleteCampaign(campaign);

                AppNotification.success("Campaign berhasil dihapus");
                updateList(); // Refresh Grid
            } catch (Exception ex) {
                AppNotification.error("Gagal menghapus data: " + ex.getMessage());
            }
        });

        dialog.open();
    }

    private void updateList() {
        grid.setItems(campaignService.getAllCampaigns());
    }
}