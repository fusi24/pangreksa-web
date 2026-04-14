package com.fusi24.pangreksa.web.view.admin;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.base.ui.notification.AppNotification;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.Campaign;
import com.fusi24.pangreksa.web.service.CampaignService;
import com.fusi24.pangreksa.web.service.CommonService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.IntegerField;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

@Route("management-campaign")
@PageTitle("Manajemen Campaign")
@RolesAllowed("CAMPAIGN")
public class CampaignManagementView extends Main implements com.vaadin.flow.router.HasUrlParameter<String> {

    private static final Logger log = LoggerFactory.getLogger(CampaignManagementView.class);

    private final CampaignService campaignService;
    private final CommonService commonService;
    private final CurrentUser currentUser;
    private Authorization auth;

    private final Binder<Campaign> binder = new Binder<>(Campaign.class);
    private Campaign currentCampaign;
    private AtomicReference<byte[]> uploadedImageBytes = new AtomicReference<>();

    private TextField title = new TextField("Judul Campaign");
    private TextArea description = new TextArea("Deskripsi");
    private TextField linkUrl = new TextField("Link Tautan (URL)");
    private DatePicker startDate = new DatePicker("Tanggal Mulai");
    private DatePicker endDate = new DatePicker("Tanggal Berakhir");
    private IntegerField priority = new IntegerField("Prioritas");
    private ComboBox<String> status = new ComboBox<>("Status Publikasi");
    private ComboBox<String> category = new ComboBox<>("Kategori");

    private Div photoPlaceholder;
    private Image photoPreview = new Image(); // Inisialisasi awal agar tidak null
    private H2 titleText = new H2("Tambah Campaign");
    public CampaignManagementView(CampaignService campaignService, CommonService commonService, CurrentUser currentUser) {
        this.campaignService = campaignService;
        this.commonService = commonService;
        this.currentUser = currentUser;

        this.currentCampaign = new Campaign();
        this.auth = commonService.getAuthorization(
                currentUser.require(),
                (String) UI.getCurrent().getSession().getAttribute("responsibility"),
                20L);

        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL, LumoUtility.Background.CONTRAST_5);

        add(new ViewToolbar("Manajemen Campaign"));
        createBody();

        // Bind data
        binder.bindInstanceFields(this);
    }

    @Override
    public void setParameter(com.vaadin.flow.router.BeforeEvent event,
                             @com.vaadin.flow.router.OptionalParameter String parameter) {
        if (parameter != null && !parameter.isEmpty()) {
            Long id = Long.parseLong(parameter);
            campaignService.getById(id).ifPresent(campaign -> {
                this.currentCampaign = campaign;
                binder.setBean(currentCampaign);

                // Update judul menjadi Ubah
                titleText.setText("Ubah Campaign");

                // Update status ComboBox agar sinkron dengan data DB
                status.setValue(campaign.isActive() ? "PUBLISH (AKTIF)" : "DRAFT");

                showExistingImage(campaign.getImagePath());
            });
        } else {
            // Skenario Tambah Baru
            this.currentCampaign = new Campaign();
            binder.setBean(currentCampaign);
            titleText.setText("Tambah Campaign");
            status.setValue("DRAFT");
        }
    }

    private void showExistingImage(String path) {
        if (path != null && !path.isEmpty()) {
            byte[] bytes = campaignService.getImagePathAsByteArray(path);
            if (bytes != null) {
                StreamResource res = new StreamResource("current-img", () -> new ByteArrayInputStream(bytes));
                photoPreview.setSrc(res);
                photoPreview.setVisible(true);
                photoPlaceholder.removeAll();
                photoPlaceholder.add(photoPreview);
            }
        }
    }

    private void createBody() {
        VerticalLayout card = new VerticalLayout();
        card.addClassNames(LumoUtility.Background.BASE, LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.BoxShadow.SMALL, LumoUtility.Padding.LARGE, LumoUtility.Margin.AUTO);
        card.setMaxWidth("1100px");

        // --- HEADER ---
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        titleText.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.Margin.NONE);
        H2 titleText = new H2(currentCampaign.getId() == null ? "Tambah Campaign" : "Ubah Campaign");
        titleText.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.Margin.NONE);

        Button btnSimpan = new Button("Simpan", VaadinIcon.CHECK.create(), e -> save());
        btnSimpan.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnSimpan.getStyle().set("background-color", "#002d5d");

        header.add(titleText, btnSimpan);

        // --- FORM LAYOUT ---
        category.setItems("Event", "Kebijakan", "Wellness", "Lainnya");

        // Konfigurasi Status Publikasi
        status.setItems("DRAFT", "PUBLISH (AKTIF)");
        status.setPlaceholder("Pilih status...");
        status.setHelperText("Pilih PUBLISH agar campaign dapat tayang sesuai jadwal.");

        // Set nilai awal berdasarkan data di database
        if (currentCampaign.isActive()) {
            status.setValue("PUBLISH (AKTIF)");
        } else {
            status.setValue("DRAFT");
        }

        FormLayout formLayout = new FormLayout();
        // Masukkan status ke dalam formLayout agar rapi sejajar dengan category/priority
        formLayout.add(title, category, status, priority, startDate, endDate, linkUrl, description);

        // Atur lebar field (2 kolom untuk title, link, dan deskripsi agar lebih luas)
        formLayout.setColspan(title, 2);
        formLayout.setColspan(linkUrl, 2);
        formLayout.setColspan(description, 2);

        // --- PHOTO SECTION ---
        photoPlaceholder = new Div();
        photoPlaceholder.addClassNames(LumoUtility.Background.CONTRAST_10, LumoUtility.Display.FLEX,
                LumoUtility.AlignItems.CENTER, LumoUtility.JustifyContent.CENTER, LumoUtility.BorderRadius.MEDIUM);
        photoPlaceholder.setWidth("100%");
        photoPlaceholder.setMaxWidth("600px"); // Preview jangan terlalu lebar
        photoPlaceholder.setHeight("200px"); // Tinggi preview lebih proporsional (3:1)

        Icon placeholderIcon = VaadinIcon.PICTURE.create();
        placeholderIcon.addClassNames(LumoUtility.TextColor.TERTIARY, LumoUtility.FontSize.XXLARGE);
        photoPlaceholder.add(placeholderIcon);

        photoPreview.setWidthFull();
        photoPreview.setHeightFull();
        photoPreview.getStyle().set("object-fit", "cover");
        photoPreview.setVisible(false);

        // Handler Upload
        var handler = UploadHandler.inMemory((metadata, data) -> {
            uploadedImageBytes.set(data);
            StreamResource res = new StreamResource(UUID.randomUUID().toString(), () -> new ByteArrayInputStream(data));

            UI.getCurrent().access(() -> {
                photoPreview.setSrc(res);
                photoPreview.setVisible(true);
                photoPlaceholder.removeAll();
                photoPlaceholder.add(photoPreview);
            });
        });

        Upload upload = new Upload(handler);
        upload.setAcceptedFileTypes("image/png", "image/jpeg");
        upload.setUploadButton(new Button("Pilih Gambar Banner", VaadinIcon.UPLOAD.create()));
        upload.setWidthFull();

        // --- PENYUSUNAN AKHIR ---
        // Tambahkan komponen ke card secara berurutan
        card.add(
                header,
                new Hr(), // Garis pemisah antara header dan form
                formLayout,
                new Span("Banner Preview"),
                photoPlaceholder,
                upload
        );

        add(card);
    }

    private void save() {
        if (binder.validate().isOk()) {
            try {
                // Mapping Status ComboBox ke Boolean isActive
                // Jika pilih PUBLISH, maka isActive = true
                currentCampaign.setActive("PUBLISH (AKTIF)".equals(status.getValue()));

                // Logika simpan gambar dan service tetap sama
                if (uploadedImageBytes.get() != null) {
                    String newPath = campaignService.saveImage(uploadedImageBytes.get());
                    currentCampaign.setImagePath(newPath);
                }

                var loginUser = commonService.getLoginUser(currentUser.require().getUserId().toString());
                campaignService.save(currentCampaign, loginUser.getId());

                AppNotification.success("Data campaign berhasil disimpan sebagai " + status.getValue());
                UI.getCurrent().navigate(CampaignListView.class);
            } catch (Exception ex) {
                AppNotification.error("Gagal simpan: " + ex.getMessage());
            }
        }
    }

    private void deleteOldFile(String oldPath) {
        try {
            String cleanPath = oldPath.startsWith("/") ? oldPath.substring(1) : oldPath;
            java.nio.file.Files.deleteIfExists(java.nio.file.Paths.get(cleanPath));
        } catch (Exception e) {
            log.error("Gagal menghapus file lama: {}", e.getMessage());
        }
    }
}