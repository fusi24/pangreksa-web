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
import com.vaadin.flow.data.provider.ListDataProvider;
import java.io.ByteArrayInputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Route("campaign-list")
@PageTitle("Manajemen Campaign Internal")
@Menu(order = 19, icon = "vaadin:table", title = "Daftar Campaign")
@RolesAllowed("CAMPAIGN")
public class CampaignListView extends VerticalLayout {

    private final CampaignService campaignService;
    private Grid<Campaign> grid = new Grid<>(Campaign.class, false);
    private DateTimeFormatter df = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private ListDataProvider<Campaign> dataProvider;
    private TextField searchField = new TextField();
    private ComboBox<String> statusFilter = new ComboBox<>("Status");
    private ComboBox<String> categoryFilter = new ComboBox<>("Kategori");

    public CampaignListView(CampaignService campaignService) {
        this.campaignService = campaignService;
        setSizeFull();
        setPadding(true);
        setSpacing(false); // Mengurangi jarak default agar lebih rapat profesional

        add(new ViewToolbar("Manajemen Campaign Internal"));

        // Tambahkan deskripsi singkat di bawah toolbar seperti di mockup
        Span subTitle = new Span("Buat, edit, dan pantau performa campaign komunikasi untuk seluruh karyawan.");
        subTitle.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Bottom.MEDIUM);
        add(subTitle);

        add(createFilterBar());
        configureGrid();
        add(grid);

        updateList();
    }

    private HorizontalLayout createFilterBar() {
        // Search Field dengan ikon yang lebih pas
        searchField.setPlaceholder("Cari berdasarkan judul...");
        searchField.setPrefixComponent(VaadinIcon.SEARCH.create());
        searchField.setWidth("350px");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(com.vaadin.flow.data.value.ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> applyFilters());

        // Beri label pada ComboBox agar terlihat rapi
        statusFilter.setLabel("Status");
        statusFilter.setItems("Semua Status", "AKTIF", "TERJADWAL", "BERAKHIR", "DRAFT");
        statusFilter.setValue("Semua Status");
        statusFilter.addValueChangeListener(e -> applyFilters());

        categoryFilter.setLabel("Kategori");
        categoryFilter.setItems("Semua Kategori", "Event", "Kebijakan", "Wellness");
        categoryFilter.setValue("Semua Kategori");
        categoryFilter.addValueChangeListener(e -> applyFilters());

        Button btnClear = new Button(VaadinIcon.REFRESH.create(), e -> {
            searchField.clear();
            statusFilter.setValue("Semua Status");
            categoryFilter.setValue("Semua Kategori");
        });
        btnClear.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        btnClear.setTooltipText("Reset Filter");

        Button btnAdd = new Button("Tambah Campaign Baru", VaadinIcon.PLUS.create());
        btnAdd.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnAdd.getStyle().set("background-color", "#002d5d");
        btnAdd.addClickListener(e -> UI.getCurrent().navigate(CampaignManagementView.class));

        // Grouping filter ke kiri dan tombol tambah ke kanan
        HorizontalLayout filterGroup = new HorizontalLayout(searchField, statusFilter, categoryFilter, btnClear);
        filterGroup.setAlignItems(Alignment.END);

        HorizontalLayout layout = new HorizontalLayout(filterGroup, btnAdd);
        layout.setWidthFull();
        layout.setJustifyContentMode(JustifyContentMode.BETWEEN);
        layout.setAlignItems(Alignment.END);
        layout.addClassNames(LumoUtility.Margin.Bottom.MEDIUM);

        return layout;
    }

    private void applyFilters() {
        if (dataProvider == null) return;

        dataProvider.setFilter(campaign -> {
            // 1. Filter Judul
            boolean matchesText = true;
            if (searchField.getValue() != null && !searchField.getValue().isEmpty()) {
                matchesText = campaign.getTitle().toLowerCase()
                        .contains(searchField.getValue().toLowerCase());
            }

            // 2. Filter Kategori
            boolean matchesCategory = true;
            if (categoryFilter.getValue() != null && !categoryFilter.getValue().equals("Semua Kategori")) {
                matchesCategory = categoryFilter.getValue().equals(campaign.getCategory());
            }

            // 3. Filter Status (Menggunakan logika calculateStatus dari service)
            boolean matchesStatus = true;
            if (statusFilter.getValue() != null && !statusFilter.getValue().equals("Semua Status")) {
                String currentStatus = campaignService.calculateStatus(campaign);
                matchesStatus = statusFilter.getValue().equals(currentStatus);
            }

            return matchesText && matchesCategory && matchesStatus;
        });
    }

    private void updateList() {
        List<Campaign> allCampaigns = campaignService.getAllCampaigns();
        dataProvider = new ListDataProvider<>(allCampaigns);
        grid.setDataProvider(dataProvider);
    }

    private void configureGrid() {
        grid.setSelectionMode(Grid.SelectionMode.MULTI);
        grid.addClassName("campaign-grid");

        // 1. Thumbnail (Tampilan bulat/rounded kotak)
        grid.addComponentColumn(c -> {
            byte[] bytes = campaignService.getImagePathAsByteArray(c.getImagePath());
            Div imgContainer = new Div();
            imgContainer.setWidth("48px");
            imgContainer.setHeight("48px");
            imgContainer.addClassNames(LumoUtility.Background.CONTRAST_10, LumoUtility.BorderRadius.MEDIUM,
                    LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.JustifyContent.CENTER,
                    LumoUtility.Overflow.HIDDEN);

            if (bytes != null) {
                StreamResource res = new StreamResource("thumb", () -> new ByteArrayInputStream(bytes));
                Image img = new Image(res, "thumb");
                img.setWidthFull();
                img.setHeightFull();
                img.getStyle().set("object-fit", "cover");
                imgContainer.add(img);
            } else {
                imgContainer.add(VaadinIcon.PICTURE.create());
            }
            return imgContainer;
        }).setHeader("Thumbnail").setWidth("80px").setFlexGrow(0).setResizable(false);

        // 2. Data Columns dengan font weight bold untuk judul
        grid.addColumn(new ComponentRenderer<>(c -> {
            Span title = new Span(c.getTitle());
            title.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.TextColor.HEADER);
            return title;
        })).setHeader("Judul").setSortable(true).setFlexGrow(2);

        grid.addColumn(Campaign::getCategory).setHeader("Kategori").setAutoWidth(true);
        grid.addColumn(c -> c.getStartDate().format(df) + " - " + c.getEndDate().format(df))
                .setHeader("Periode Tayang").setAutoWidth(true);

        grid.addColumn(Campaign::getTargetAudience).setHeader("Target").setAutoWidth(true);

        // 3. Engagement (Gunakan Badge untuk angka agar menonjol)
        grid.addComponentColumn(c -> {
            Span s = new Span(c.getClickCount() + " / " + c.getViewCount());
            s.getElement().getThemeList().add("badge contrast small");
            return s;
        }).setHeader("Engagement").setAutoWidth(true);

        // 4. Status dengan Warna Lumo (Success, Error, Contrast)
        grid.addComponentColumn(c -> {
            String status = campaignService.calculateStatus(c);
            Span s = new Span(status);
            s.getElement().getThemeList().add("badge pill");

            // Pemberian warna berdasarkan status
            switch (status) {
                case "AKTIF" -> s.getElement().getThemeList().add("success");
                case "TERJADWAL" -> s.getElement().getThemeList().add("contrast");
                case "BERAKHIR" -> s.getElement().getThemeList().add("error");
                default -> { } // Draft atau lainnya tetap default
            }
            return s;
        }).setHeader("Status").setAutoWidth(true);

        // 5. Aksi yang lebih rapi
        grid.addComponentColumn(c -> {
            HorizontalLayout actions = new HorizontalLayout();

            Button edit = new Button(VaadinIcon.PENCIL.create(), e ->
                    getUI().ifPresent(ui -> ui.navigate(CampaignManagementView.class, c.getId().toString())));
            edit.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_SMALL);
            edit.setTooltipText("Ubah Data");

            Button delete = new Button(VaadinIcon.TRASH.create(), e -> confirmDelete(c));
            delete.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
            delete.setTooltipText("Hapus Data");

            actions.add(edit, delete);
            return actions;
        }).setHeader("Aksi").setFrozenToEnd(true).setWidth("100px").setFlexGrow(0);

        grid.setSizeFull();
        grid.addThemeVariants(com.vaadin.flow.component.grid.GridVariant.LUMO_ROW_STRIPES,
                com.vaadin.flow.component.grid.GridVariant.LUMO_NO_BORDER);
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

}