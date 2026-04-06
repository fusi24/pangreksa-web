package com.fusi24.pangreksa.web.view.common;

import com.fusi24.pangreksa.web.model.entity.Campaign;
import com.fusi24.pangreksa.web.service.CampaignService;
import com.vaadin.flow.component.Html;
import com.vaadin.flow.component.Tag;
import com.vaadin.flow.component.dependency.JsModule;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.util.List;

public class CampaignCarousel extends Div {

    public CampaignCarousel(CampaignService campaignService) {
        addClassNames(LumoUtility.Width.FULL, LumoUtility.Margin.Bottom.MEDIUM);

        // Mengambil data campaign yang sedang aktif saja
        List<Campaign> activeCampaigns = campaignService.getActiveCampaigns();

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
            Div slide = new Div();
            slide.getStyle()
                    .set("min-width", "100%") // Memaksa satu slide per tampilan
                    .set("scroll-snap-align", "start")
                    .set("aspect-ratio", "3 / 1"); // Rasio 3:1 sesuai request

            Image banner = new Image(campaign.getImagePath(), campaign.getTitle());
            banner.addClassNames(LumoUtility.Width.FULL, LumoUtility.Height.FULL,
                    LumoUtility.BorderRadius.MEDIUM, LumoUtility.BoxShadow.SMALL);
            banner.getStyle().set("object-fit", "cover");

            // Jika ada link, bungkus dengan Anchor agar bisa diklik
            if (campaign.getLinkUrl() != null && !campaign.getLinkUrl().isEmpty()) {
                Anchor anchor = new Anchor(campaign.getLinkUrl(), banner);
                anchor.setTarget("_blank"); // Buka di tab baru
                slide.add(anchor);
            } else {
                slide.add(banner);
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