package com.fusi24.pangreksa.web.view.common;

import com.fusi24.pangreksa.web.model.entity.Campaign;
import com.fusi24.pangreksa.web.service.CampaignService;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.io.ByteArrayInputStream;
import java.util.List;

public class CampaignCarousel extends Div {

    public CampaignCarousel(CampaignService campaignService) {
        addClassNames(LumoUtility.Width.FULL, LumoUtility.Margin.Bottom.MEDIUM);

        List<Campaign> activeCampaigns = campaignService.getActiveCampaigns();

        if (activeCampaigns.isEmpty()) {
            setVisible(false);
            return;
        }

        HorizontalLayout carouselContainer = new HorizontalLayout();
        carouselContainer.getStyle()
                .set("overflow-x", "auto")
                .set("scroll-snap-type", "x mandatory")
                .set("scrollbar-width", "none")
                .set("-ms-overflow-style", "none");

        carouselContainer.addClassNames(LumoUtility.Width.FULL, LumoUtility.Gap.SMALL);

        for (Campaign campaign : activeCampaigns) {
            // 1. Definisikan 'slide' di dalam loop
            Div slide = new Div();
            slide.getStyle()
                    .set("min-width", "100%")
                    .set("scroll-snap-align", "start")
                    .set("aspect-ratio", "3 / 1");

            // Tracking View
            campaignService.incrementViewCount(campaign.getId());

            // 2. Ambil byte gambar dan definisikan 'imageBytes'
            byte[] imageBytes = campaignService.getImagePathAsByteArray(campaign.getImagePath());

            if (imageBytes != null) {
                StreamResource resource = new StreamResource("banner-" + campaign.getId(),
                        () -> new ByteArrayInputStream(imageBytes));

                Image banner = new Image(resource, campaign.getTitle());
                banner.addClassNames(LumoUtility.Width.FULL, LumoUtility.Height.FULL,
                        LumoUtility.BorderRadius.MEDIUM, LumoUtility.BoxShadow.SMALL);
                banner.getStyle().set("object-fit", "cover");

                if (campaign.getLinkUrl() != null && !campaign.getLinkUrl().isEmpty()) {
                    Anchor anchor = new Anchor(campaign.getLinkUrl(), banner);
                    anchor.setTarget("_blank");

                    // Tracking Click
                    anchor.getElement().addEventListener("click", e -> {
                        campaignService.incrementClickCount(campaign.getId());
                    });

                    slide.add(anchor);
                } else {
                    slide.add(banner);
                }
            } else {
                // Tampilkan pesan jika gambar gagal dimuat
                Span errorMsg = new Span("Gambar tidak tersedia");
                errorMsg.addClassNames(LumoUtility.TextColor.ERROR, LumoUtility.FontSize.SMALL);
                slide.add(errorMsg);
            }

            carouselContainer.add(slide);
        }

        add(carouselContainer);

        // CSS untuk menyembunyikan scrollbar di Chrome/Safari
        getElement().executeJs(
                "const style = document.createElement('style');" +
                        "style.innerHTML = '::-webkit-scrollbar { display: none; }';" +
                        "this.appendChild(style);"
        );
    }
}