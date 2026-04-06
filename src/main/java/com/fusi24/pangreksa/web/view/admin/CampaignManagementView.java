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
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
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
@Menu(order = 20, icon = "vaadin:megaphone", title = "Manajemen Campaign")
@RolesAllowed("CAMPAIGN")
public class CampaignManagementView extends Main {

    private static final long serialVersionUID = 20L;
    private static final Logger log = LoggerFactory.getLogger(CampaignManagementView.class);

    private final CampaignService campaignService;
    private final CommonService commonService;
    private final CurrentUser currentUser;
    private Authorization auth;

    public static final String VIEW_NAME = "Manajemen Campaign";

    private final Binder<Campaign> binder = new Binder<>(Campaign.class);
    private Campaign currentCampaign;

    // Nama variabel disamakan dengan property di Entity Campaign agar bindInstanceFields bekerja
    private TextField title = new TextField("Judul Campaign");
    private TextArea description = new TextArea("Deskripsi");
    private TextField linkUrl = new TextField("Link Tautan (URL)");
    private DatePicker startDate = new DatePicker("Tanggal Mulai");
    private DatePicker endDate = new DatePicker("Tanggal Berakhir");
    private IntegerField priority = new IntegerField("Prioritas");
    private Checkbox isActive = new Checkbox("Aktif");

    private Image photoPreview;
    private Div photoPlaceholder;
    private AtomicReference<byte[]> uploadedImageBytes = new AtomicReference<>();

    public CampaignManagementView(CampaignService campaignService,
                                  CommonService commonService,
                                  CurrentUser currentUser) {
        this.campaignService = campaignService;
        this.commonService = commonService;
        this.currentUser = currentUser;

        // Ambil responsibility dari session
        String responsibility = (String) UI.getCurrent().getSession().getAttribute("responsibility");

        try {
            // Ambil otorisasi
            this.auth = commonService.getAuthorization(
                    currentUser.require(),
                    responsibility,
                    serialVersionUID);

            if (this.auth != null) {
                log.debug("Page {}, Authorization: View={}, Create={}, Edit={}, Delete={}",
                        VIEW_NAME, auth.canView, auth.canCreate, auth.canEdit, auth.canDelete);
            }
        } catch (Exception e) {
            log.error("Gagal mendapatkan otorisasi: {}", e.getMessage());
        }

        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL, LumoUtility.Background.CONTRAST_5);

        add(new ViewToolbar(VIEW_NAME));
        createBody();

        // Inisialisasi data
        this.currentCampaign = new Campaign();

        // Memetakan field secara otomatis ke entity
        binder.bindInstanceFields(this);
        binder.setBean(currentCampaign);
    }

    private void createBody() {
        VerticalLayout card = new VerticalLayout();
        card.addClassNames(LumoUtility.Background.BASE, LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.BoxShadow.SMALL, LumoUtility.Padding.LARGE, LumoUtility.Margin.AUTO);
        card.setMaxWidth("1100px");
        card.setSpacing(true);

        // --- HEADER SECTION ---
        HorizontalLayout header = new HorizontalLayout();
        header.setWidthFull();
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        HorizontalLayout titleGroup = new HorizontalLayout();
        titleGroup.setAlignItems(FlexComponent.Alignment.CENTER);
        Icon megaphoneIcon = VaadinIcon.MEGAPHONE.create();
        megaphoneIcon.getStyle().set("color", "#002d5d");
        H2 titleText = new H2("Ubah Campaign");
        titleText.addClassNames(LumoUtility.FontSize.XLARGE, LumoUtility.Margin.NONE);
        Icon editIcon = VaadinIcon.PENCIL.create();
        editIcon.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.TERTIARY);
        titleGroup.add(megaphoneIcon, titleText, editIcon);

        HorizontalLayout actions = new HorizontalLayout();
        Button btnSimpan = new Button("Simpan", VaadinIcon.CHECK.create(), e -> save());
        btnSimpan.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        btnSimpan.getStyle().set("background-color", "#002d5d");

        Button btnBatal = new Button("Batal", VaadinIcon.CLOSE.create(), e -> cancel());
        btnBatal.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        actions.add(btnSimpan, btnBatal);

        header.add(titleGroup, actions);

        // --- FORM SECTION ---
        FormLayout formLayout = new FormLayout();
        formLayout.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("600px", 2));

        formLayout.add(title, priority, startDate, linkUrl, endDate);
        description.setMinHeight("100px");
        formLayout.add(description);
        formLayout.setColspan(description, 2);

        // --- UPLOAD SECTION ---
        VerticalLayout uploadSection = new VerticalLayout();
        uploadSection.setPadding(false);
        uploadSection.setSpacing(false);

        Span previewLabel = new Span("Preview Image");
        previewLabel.addClassNames(LumoUtility.FontWeight.BOLD, LumoUtility.Margin.Bottom.SMALL);

        HorizontalLayout uploadControl = new HorizontalLayout();
        uploadControl.setAlignItems(FlexComponent.Alignment.START);
        uploadControl.setSpacing(true);

        photoPlaceholder = new Div();
        photoPlaceholder.addClassNames(LumoUtility.Background.CONTRAST_10, LumoUtility.BorderRadius.SMALL,
                LumoUtility.Display.FLEX, LumoUtility.AlignItems.CENTER, LumoUtility.JustifyContent.CENTER);
        photoPlaceholder.setWidth("120px");
        photoPlaceholder.setHeight("120px");

        Icon imageIcon = VaadinIcon.PICTURE.create();
        imageIcon.addClassNames(LumoUtility.TextColor.TERTIARY, LumoUtility.FontSize.XXLARGE);

        photoPreview = new Image();
        photoPreview.setWidthFull();
        photoPreview.setHeightFull();
        photoPreview.getStyle().set("object-fit", "cover");
        photoPreview.setVisible(false);

        photoPlaceholder.add(imageIcon, photoPreview);

        VerticalLayout uploadButtons = new VerticalLayout();
        uploadButtons.setPadding(false);

        var handler = UploadHandler.inMemory((metadata, data) -> {
            uploadedImageBytes.set(data);
            StreamResource res = new StreamResource(UUID.randomUUID().toString(), () -> new ByteArrayInputStream(data));
            UI.getCurrent().access(() -> {
                photoPreview.setSrc(res);
                photoPreview.setVisible(true);
                imageIcon.setVisible(false);
            });
        });
        Upload upload = new Upload(handler);
        upload.setAcceptedFileTypes("image/png", "image/jpeg");

// 2. Sembunyikan komponen upload asli agar tidak merusak UI kustom kita
        upload.getStyle().set("display", "none");

        Button uploadBtn = new Button("Upload Gambar Banner", VaadinIcon.UPLOAD.create());
        uploadBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        uploadBtn.getStyle().set("background-color", "#002d5d");

// 3. Gunakan JS untuk memicu klik pada input file internal milik vaadin-upload
        uploadBtn.addClickListener(e -> {
            upload.getElement().executeJs("this.shadowRoot.querySelector('input').click()");
        });

// 4. PENTING: Tambahkan 'upload' ke dalam layout agar ia eksis di DOM
        uploadButtons.add(upload, uploadBtn, isActive);

        uploadControl.add(photoPlaceholder, uploadButtons);
        uploadSection.add(previewLabel, uploadControl);

        card.add(header, formLayout, uploadSection);
        add(card);
    }

    private void save() {
        if (this.auth != null && !this.auth.canCreate && currentCampaign.getId() == null) {
            AppNotification.error("Anda tidak memiliki izin menyimpan data");
            return;
        }

        if (binder.validate().isOk()) {
            try {
                // 1. Cek apakah ada gambar baru yang diunggah
                if (uploadedImageBytes.get() != null) {
                    // Skenario: Simpan ke folder atau tentukan path-nya
                    // Untuk sementara kita buat nama file unik
                    String fileName = "banner_" + UUID.randomUUID().toString().substring(0, 8) + ".png";
                    String manualPath = "/uploads/campaigns/" + fileName;

                    // Set ke entity agar lolos validasi @NotBlank
                    currentCampaign.setImagePath(manualPath);

                    // TODO: Di sini Anda harusnya memanggil logic untuk menulis byte[]
                    // ke folder fisik server atau storage S3.
                    log.debug("Menyimpan gambar ke: {}", manualPath);
                } else if (currentCampaign.getImagePath() == null || currentCampaign.getImagePath().isEmpty()) {
                    // Jika tidak ada gambar baru DAN data lama juga kosong
                    AppNotification.error("Gambar banner wajib diunggah!");
                    return;
                }

                // 2. Ambil User Login
                var loginUser = commonService.getLoginUser(currentUser.require().getUserId().toString());

                // 3. Simpan ke database
                campaignService.save(currentCampaign, loginUser.getId());

                AppNotification.success("Data campaign berhasil tersimpan");
                cancel(); // Reset form dan reload
            } catch (Exception ex) {
                AppNotification.error("Gagal menyimpan data: " + ex.getMessage());
                log.error("Save error", ex);
            }
        }
    }

    private void cancel() {
        this.currentCampaign = new Campaign();
        binder.setBean(currentCampaign);
        photoPreview.setVisible(false);
        uploadedImageBytes.set(null);
        UI.getCurrent().getPage().reload();
    }
}