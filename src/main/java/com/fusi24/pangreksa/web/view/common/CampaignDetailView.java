package com.fusi24.pangreksa.web.view.common;

import com.fusi24.pangreksa.web.model.entity.Campaign;
import com.fusi24.pangreksa.web.service.CampaignService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.PermitAll;

import java.io.ByteArrayInputStream;
import java.time.format.DateTimeFormatter;

@Route("campaign-detail")
@PageTitle("Detail Campaign")
@PermitAll // Semua user yang login bisa melihat ini
public class CampaignDetailView extends VerticalLayout implements HasUrlParameter<String> {

    private final CampaignService campaignService;
    private final Div container = new Div();
    private final DateTimeFormatter df = DateTimeFormatter.ofPattern("dd MMMM yyyy");

    public CampaignDetailView(CampaignService campaignService) {
        this.campaignService = campaignService;

        setSizeFull();
        setAlignItems(Alignment.CENTER);
        addClassNames(LumoUtility.Background.CONTRAST_5, LumoUtility.Padding.MEDIUM);

        // Tombol Kembali
        Button btnBack = new Button("Kembali", VaadinIcon.ARROW_LEFT.create(), e -> UI.getCurrent().getPage().getHistory().back());
        btnBack.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        container.addClassNames(LumoUtility.Background.BASE, LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.BoxShadow.SMALL, LumoUtility.Padding.LARGE, LumoUtility.Margin.AUTO);
        container.setMaxWidth("800px");
        container.setWidthFull();

        add(btnBack, container);
    }

    @Override
    public void setParameter(com.vaadin.flow.router.BeforeEvent event, String parameter) {
        if (parameter != null) {
            campaignService.getById(Long.parseLong(parameter)).ifPresentOrElse(
                    this::renderDetail,
                    () -> container.add(new H3("Campaign tidak ditemukan"))
            );
        }
    }

    private void renderDetail(Campaign campaign) {
        container.removeAll();

        // 1. Gambar Banner Besar
        byte[] bytes = campaignService.getImagePathAsByteArray(campaign.getImagePath());
        if (bytes != null) {
            StreamResource res = new StreamResource("detail-img", () -> new ByteArrayInputStream(bytes));
            Image img = new Image(res, campaign.getTitle());
            img.setWidthFull();
            img.addClassNames(LumoUtility.BorderRadius.MEDIUM, LumoUtility.Margin.Bottom.MEDIUM);
            container.add(img);
        }

        // 2. Judul & Kategori
        H2 title = new H2(campaign.getTitle());
        title.addClassNames(LumoUtility.Margin.Bottom.XSMALL);

        Span category = new Span(campaign.getCategory());
        category.getElement().getThemeList().add("badge pill contrast");

        Span date = new Span("Periode: " + campaign.getStartDate().format(df) + " - " + campaign.getEndDate().format(df));
        date.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY, LumoUtility.Margin.Left.MEDIUM);

        container.add(new Div(title), new Div(category, date), new Hr());

        // 3. Deskripsi
        Paragraph desc = new Paragraph(campaign.getDescription());
        desc.addClassNames(LumoUtility.FontSize.MEDIUM, LumoUtility.LineHeight.MEDIUM, LumoUtility.Margin.Vertical.MEDIUM);
        container.add(desc);

        // 4. Tombol Link Eksternal (Jika ada)
        if (campaign.getLinkUrl() != null && !campaign.getLinkUrl().isEmpty()) {
            // 1. Buat komponen ikon secara terpisah
            Icon icon = VaadinIcon.EXTERNAL_LINK.create();
            icon.getStyle().set("margin-left", "8px"); // Beri sedikit jarak dari teks

            // 2. Buat Anchor (Tautan)
            Anchor anchor = new Anchor(campaign.getLinkUrl(), "Buka Tautan Selengkapnya");
            anchor.setTarget("_blank");
            anchor.getStyle().set("color", "white"); // Pastikan teks tetap putih jika di dalam tombol biru
            anchor.getStyle().set("text-decoration", "none");

            // 3. Masukkan Anchor dan Icon ke dalam Button
            // Kita gunakan HorizontalLayout di dalam Button agar tata letaknya rapi
            Button btnLink = new Button(new HorizontalLayout(anchor, icon));
            btnLink.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btnLink.setWidthFull();
            btnLink.getStyle().set("background-color", "#002d5d");
            btnLink.getStyle().set("cursor", "pointer");

            container.add(btnLink);

            // Track Click
            btnLink.addClickListener(e -> campaignService.incrementClickCount(campaign.getId()));
        }
    }
}