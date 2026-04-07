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
    private Checkbox isActive = new Checkbox("Aktif");
    private ComboBox<String> category = new ComboBox<>("Kategori");

    private Div photoPlaceholder;
    private Image photoPreview = new Image(); // Inisialisasi awal agar tidak null

    public CampaignManagementView(CampaignService campaignService, CommonService commonService, CurrentUser currentUser) {
        this.campaignService = campaignService;
        this.commonService = commonService;
        this.currentUser = currentUser;

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
            try {
                Long id = Long.parseLong(parameter);
                campaignService.getById(id).ifPresentOrElse(campaign -> {
                    this.currentCampaign = campaign;
                    binder.setBean(currentCampaign); // Mengisi data ke form
                    showExistingImage(campaign.getImagePath());
                }, () -> {
                    AppNotification.error("Data tidak ditemukan");
                    UI.getCurrent().navigate(CampaignListView.class);
                });
            } catch (NumberFormatException e) {
                log.error("Invalid ID format: {}", parameter);
            }
        } else {
            this.currentCampaign = new Campaign();
            binder.setBean(currentCampaign);
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
        card.addClassNames(LumoUtility.Background.BASE, LumoUtility.BorderRadius.MEDIUM, LumoUtility.BoxShadow.SMALL, LumoUtility.Padding.LARGE, LumoUtility.Margin.AUTO);
        card.setMaxWidth("1100px");

        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        H2 titleText = new H2("Ubah Campaign");
        titleText.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.Margin.NONE);

        Button btnSimpan = new Button("Simpan", VaadinIcon.CHECK.create(), e -> save());
        btnSimpan.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnSimpan.getStyle().set("background-color", "#002d5d");

        header.add(titleText, btnSimpan);

        category.setItems("Event", "Kebijakan", "Wellness", "Lainnya");
        FormLayout formLayout = new FormLayout();
        formLayout.add(title, category, priority, startDate, linkUrl, endDate, description);
        formLayout.setColspan(description, 2);

        photoPlaceholder = new Div();
        photoPlaceholder.addClassNames(LumoUtility.Background.CONTRAST_10, LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.JustifyContent.CENTER);
        photoPlaceholder.setWidth("300px");
        photoPlaceholder.setHeight("100px");

        Icon placeholderIcon = VaadinIcon.PICTURE.create();
        placeholderIcon.addClassNames(LumoUtility.TextColor.TERTIARY, LumoUtility.FontSize.XXLARGE);
        photoPlaceholder.add(placeholderIcon);

        // Styling awal preview (tersembunyi)[cite: 7]
        photoPreview.setWidthFull();
        photoPreview.setHeightFull();
        photoPreview.getStyle().set("object-fit", "cover");
        photoPreview.setVisible(false);

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
        upload.setUploadButton(new Button("Pilih Gambar", VaadinIcon.UPLOAD.create()));

        card.add(header, formLayout, new Span("Preview Image"), photoPlaceholder, upload, isActive);
        add(card);
    }

    private void save() {
        if (binder.validate().isOk()) {
            try {
                if (uploadedImageBytes.get() != null) {
                    // Hapus file lama jika ada (Skenario Edit)[cite: 7]
                    if (currentCampaign.getImagePath() != null) {
                        deleteOldFile(currentCampaign.getImagePath());
                    }
                    String newPath = campaignService.saveImage(uploadedImageBytes.get());
                    currentCampaign.setImagePath(newPath);
                }

                var loginUser = commonService.getLoginUser(currentUser.require().getUserId().toString());
                campaignService.save(currentCampaign, loginUser.getId());

                AppNotification.success("Data campaign berhasil diperbarui");
                UI.getCurrent().navigate(CampaignListView.class);
            } catch (Exception ex) {
                AppNotification.error("Gagal menyimpan: " + ex.getMessage());
                log.error("Save error", ex);
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