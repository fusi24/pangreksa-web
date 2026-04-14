package com.fusi24.pangreksa.web.view.common;

import com.fusi24.pangreksa.web.model.entity.Campaign;
import com.fusi24.pangreksa.web.service.CampaignService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

public class CampaignCarousel extends Div {

    public CampaignCarousel(CampaignService campaignService) {
        addClassNames(LumoUtility.Width.FULL, LumoUtility.Margin.Bottom.MEDIUM);

        List<Campaign> activeCampaigns = campaignService.getActiveCampaigns();
        if (activeCampaigns.isEmpty()) {
            setVisible(false);
            return;
        }

        // Kita beri ID khusus agar bisa ditembak oleh JavaScript
        String carouselId = "carousel-" + UUID.randomUUID().toString().substring(0, 8);

        HorizontalLayout container = new HorizontalLayout();
        container.setId(carouselId);
        container.setSpacing(false);
        container.setPadding(false);
        container.setWidthFull();

        // CSS untuk Scroll & Snap
        container.getStyle()
                .set("overflow-x", "auto")
                .set("scroll-snap-type", "x mandatory")
                .set("scroll-behavior", "smooth") // Agar geserannya halus
                .set("scrollbar-width", "none");

        for (Campaign campaign : activeCampaigns) {
            Div slide = new Div();
            slide.addClassNames("carousel-slide");
            slide.getStyle()
                    .set("flex", "0 0 100%") // Memaksa 1 slide per layar
                    .set("scroll-snap-align", "start")
                    .set("aspect-ratio", "3 / 1");

            byte[] imageBytes = campaignService.getImagePathAsByteArray(campaign.getImagePath());
            if (imageBytes != null) {
                StreamResource res = new StreamResource("img-" + campaign.getId(), () -> new ByteArrayInputStream(imageBytes));
                Image img = new Image(res, campaign.getTitle());
                img.addClassNames(LumoUtility.Width.FULL, LumoUtility.Height.FULL, LumoUtility.BorderRadius.MEDIUM);
                img.getStyle().set("object-fit", "cover");

                // 1. Tambahkan Tracking View: Catat setiap kali banner muncul di dashboard
                campaignService.incrementViewCount(campaign.getId());

// 2. Ubah kursor menjadi pointer agar user tahu gambar bisa diklik
                img.getStyle().set("cursor", "pointer");

// 3. Tambahkan Click Listener untuk navigasi ke halaman Detail
                img.addClickListener(e -> {
                    campaignService.incrementClickCount(campaign.getId());
                    UI.getCurrent().navigate(CampaignDetailView.class, campaign.getId().toString());
                });

// 4. Masukkan gambar langsung ke slide (tanpa Anchor luar)
                slide.add(img);
            }
            container.add(slide);
        }

        add(container);

        // JAVASCRIPT: Logika Auto-Slide (Setiap 5 Detik)
        if (activeCampaigns.size() > 1) {
            getElement().executeJs(
                    "const container = document.getElementById($0);" +
                            "let index = 0;" +
                            "const slides = container.querySelectorAll('.carousel-slide');" +
                            "const total = slides.length;" +

                            "setInterval(() => {" +
                            "  index = (index + 1) % total;" +
                            "  const x = container.offsetWidth * index;" +
                            "  container.scrollTo({ left: x, behavior: 'smooth' });" +
                            "}, 5000);", // 5000ms = 5 detik
                    carouselId
            );
        }

        // CSS tambahan untuk menyembunyikan scrollbar Chrome
        getElement().executeJs(
                "const style = document.createElement('style');" +
                        "style.innerHTML = '#'+$0+'::-webkit-scrollbar { display: none; }';" +
                        "this.appendChild(style);",
                carouselId
        );
    }
}