package com.fusi24.pangreksa.web.view.common;

import com.fusi24.pangreksa.web.model.entity.Campaign;
import com.fusi24.pangreksa.web.service.CampaignService;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.List;

public class CampaignCarousel extends Div {

    public CampaignCarousel(CampaignService campaignService) {
        addClassNames(LumoUtility.Width.FULL, LumoUtility.Margin.Bottom.MEDIUM);

        // Mengambil data campaign yang sedang aktif saja
        List<Campaign> activeCampaigns = campaignService.getActiveCampaigns();

        System.out.println("Jumlah Campaign Aktif: " + activeCampaigns.size());

        if (activeCampaigns.isEmpty()) {
            setVisible(false); // Sembunyikan jika tidak ada promo
            return;
        }

        // Container utama untuk scroll horizontal
        HorizontalLayout carouselContainer = new HorizontalLayout();
        carouselContainer.getStyle()
                .set("overflow-x", "auto")
                .set("scroll-snap-type", "x mandatory")
                .set("scrollbar-width", "none") // Sembunyikan scrollbar di Firefox
                .set("-ms-overflow-style", "none"); // Sembunyikan scrollbar di IE/Edge

        carouselContainer.addClassNames(LumoUtility.Width.FULL, LumoUtility.Gap.SMALL);

        for (Campaign campaign : activeCampaigns) {
            // ... di dalam loop for (Campaign campaign : activeCampaigns)
            Div slide = new Div();
            slide.getStyle()
                    .set("min-width", "100%")
                    .set("scroll-snap-align", "start")
                    .set("aspect-ratio", "3 / 1");

// GUNAKAN STREAM RESOURCE SEPERTI DI KARYAWAN BARU
            byte[] imageBytes = campaignService.getImagePathAsByteArray(campaign.getImagePath());

            if (imageBytes != null) {
                StreamResource resource = new StreamResource("banner-" + campaign.getId(),
                        () -> new java.io.ByteArrayInputStream(imageBytes));

                Image banner = new Image(resource, campaign.getTitle());
                banner.addClassNames(LumoUtility.Width.FULL, LumoUtility.Height.FULL,
                        LumoUtility.BorderRadius.MEDIUM, LumoUtility.BoxShadow.SMALL);
                banner.getStyle().set("object-fit", "cover");

                if (campaign.getLinkUrl() != null && !campaign.getLinkUrl().isEmpty()) {
                    Anchor anchor = new Anchor(campaign.getLinkUrl(), banner);
                    anchor.setTarget("_blank");
                    slide.add(anchor);
                } else {
                    slide.add(banner);
                }
            } else {
                // Jika file tidak ditemukan di disk, tampilkan placeholder atau abaikan
                slide.add(new Span("Gambar tidak ditemukan di disk: " + campaign.getImagePath()));
            }

            carouselContainer.add(slide);
        }

        add(carouselContainer);

        // Tambahkan CSS khusus untuk menyembunyikan scrollbar di Chrome/Safari
        getElement().executeJs(
                "const style = document.createElement('style');" +
                        "style.innerHTML = '::-webkit-scrollbar { display: none; }';" +
                        "this.appendChild(style);"
        );
    }
}