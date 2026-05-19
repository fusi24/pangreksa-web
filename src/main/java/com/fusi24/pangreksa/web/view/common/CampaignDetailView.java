package com.fusi24.pangreksa.web.view.common;

import com.pangreksa.service.model.entity.Campaign;
import com.pangreksa.service.service.CampaignService;
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
import com.fusi24.pangreksa.base.ui.TailwindUtility;
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
        addClassNames(TailwindUtility.Background.CONTRAST_5, TailwindUtility.Padding.MEDIUM);

        // Tombol Kembali
        Button btnBack = new Button("Kembali", VaadinIcon.ARROW_LEFT.create(), e -> UI.getCurrent().getPage().getHistory().back());
        btnBack.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        container.addClassNames(TailwindUtility.Background.BASE, TailwindUtility.BorderRadius.MEDIUM,
                TailwindUtility.BoxShadow.SMALL, TailwindUtility.Padding.LARGE, TailwindUtility.Margin.AUTO);
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
            img.addClassNames(TailwindUtility.BorderRadius.MEDIUM, TailwindUtility.Margin.Bottom.MEDIUM);
            container.add(img);
        }

        // 2. Judul & Kategori
        H2 title = new H2(campaign.getTitle());
        title.addClassNames(TailwindUtility.Margin.Bottom.XSMALL);

        Span category = new Span(campaign.getCategory());
        category.getElement().getThemeList().add("badge pill contrast");

        Span date = new Span("Periode: " + campaign.getStartDate().format(df) + " - " + campaign.getEndDate().format(df));
        date.addClassNames(TailwindUtility.FontSize.SMALL, TailwindUtility.TextColor.SECONDARY, TailwindUtility.Margin.Left.MEDIUM);

        container.add(new Div(title), new Div(category, date), new Hr());

        // 3. Deskripsi
        Paragraph desc = new Paragraph(campaign.getDescription());
        desc.addClassNames(TailwindUtility.FontSize.MEDIUM, TailwindUtility.LineHeight.MEDIUM, TailwindUtility.Margin.Vertical.MEDIUM);
        container.add(desc);

        // 4. Tombol Link Eksternal (Jika ada)
        if (campaign.getLinkUrl() != null && !campaign.getLinkUrl().isEmpty()) {
            // 1. Buat tombol standar (tanpa Anchor di dalamnya)
            Button btnLink = new Button("Buka Tautan Selengkapnya", VaadinIcon.EXTERNAL_LINK.create());

            // Styling
            btnLink.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            btnLink.setWidthFull();
            btnLink.getStyle().set("background-color", "#002d5d");
            btnLink.setIconAfterText(true);

            // 2. Logika Klik
            btnLink.addClickListener(e -> {
                // A. Update database terlebih dahulu
                campaignService.incrementClickCount(campaign.getId());

                // B. Buka link di tab baru lewat Java/JavaScript
                UI.getCurrent().getPage().open(campaign.getLinkUrl(), "_blank");
            });

            container.add(btnLink);
        }
    }
}