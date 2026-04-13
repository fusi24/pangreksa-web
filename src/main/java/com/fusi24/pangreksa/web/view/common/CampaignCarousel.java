package com.fusi24.pangreksa.web.view.common;

import com.fusi24.pangreksa.web.model.entity.Campaign;
import com.fusi24.pangreksa.web.service.CampaignService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.theme.lumo.LumoUtility;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.UUID;

/**
 * Komponen Carousel untuk menampilkan Campaign aktif di dashboard Pangreksa.
 * Dilengkapi dengan navigasi panah, indikator titik (dots), dan auto-slide.
 */
public class CampaignCarousel extends Div {

    public CampaignCarousel(CampaignService campaignService) {
        // Setup Container Utama
        addClassNames(LumoUtility.Width.FULL, LumoUtility.Margin.Bottom.MEDIUM, LumoUtility.Position.RELATIVE);
        getStyle().set("overflow", "hidden");

        List<Campaign> activeCampaigns = campaignService.getActiveCampaigns();
        if (activeCampaigns.isEmpty()) {
            setVisible(false);
            return;
        }

        String carouselId = "carousel-" + UUID.randomUUID().toString().substring(0, 8);

        // 1. Kontainer Slides (Scroll Area)
        HorizontalLayout container = new HorizontalLayout();
        container.setId(carouselId);
        container.setSpacing(false);
        container.setPadding(false);
        container.setWidthFull();
        container.getStyle()
                .set("overflow-x", "auto")
                .set("scroll-snap-type", "x mandatory")
                .set("scroll-behavior", "smooth")
                .set("scrollbar-width", "none");

        // 2. Kontainer Indikator Dots
        HorizontalLayout dotsContainer = new HorizontalLayout();
        dotsContainer.addClassNames(LumoUtility.Position.ABSOLUTE, LumoUtility.Display.FLEX, LumoUtility.JustifyContent.CENTER);
        dotsContainer.getStyle()
                .set("bottom", "10px")
                .set("left", "0")
                .set("right", "0")
                .set("z-index", "10");

        for (int i = 0; i < activeCampaigns.size(); i++) {
            Campaign campaign = activeCampaigns.get(i);

            // Membuat Slide
            Div slide = new Div();
            slide.addClassNames("carousel-slide");
            slide.getStyle()
                    .set("flex", "0 0 100%")
                    .set("scroll-snap-align", "start")
                    .set("aspect-ratio", "3 / 1");

            byte[] imageBytes = campaignService.getImagePathAsByteArray(campaign.getImagePath());
            if (imageBytes != null) {
                StreamResource res = new StreamResource("img-" + campaign.getId(), () -> new ByteArrayInputStream(imageBytes));
                Image img = new Image(res, campaign.getTitle());
                img.addClassNames(LumoUtility.Width.FULL, LumoUtility.Height.FULL, LumoUtility.BorderRadius.MEDIUM);
                img.getStyle().set("object-fit", "cover").set("cursor", "pointer");

                img.addClickListener(e -> {
                    campaignService.incrementClickCount(campaign.getId());
                    // Menggunakan Class tujuan dan parameter ID
                    UI.getCurrent().navigate(CampaignDetailView.class, campaign.getId().toString());
                });

                campaignService.incrementViewCount(campaign.getId());
                slide.add(img);
            }
            container.add(slide);

            // Membuat Dot (Indikator)
            Div dot = new Div();
            dot.addClassNames("carousel-dot"); // Class ini digunakan oleh JavaScript selector
            dot.getStyle()
                    .set("width", "10px")
                    .set("height", "10px")
                    .set("border-radius", "50%")
                    .set("background-color", i == 0 ? "white" : "rgba(255,255,255,0.5)")
                    .set("margin", "0 5px")
                    .set("transition", "all 0.3s ease")
                    .set("cursor", "pointer");

            final int index = i;
            dot.addClickListener(e -> getElement().executeJs(
                    "const c = document.getElementById($0); c.scrollTo({left: c.offsetWidth * $1, behavior: 'smooth'});",
                    carouselId, index
            ));

            dotsContainer.add(dot);
        }

        // 3. Tombol Navigasi Kiri & Kanan
        Button btnPrev = new Button(VaadinIcon.CHEVRON_LEFT.create());
        Button btnNext = new Button(VaadinIcon.CHEVRON_RIGHT.create());

        styleNavButton(btnPrev, true);
        styleNavButton(btnNext, false);

        btnPrev.addClickListener(e -> getElement().executeJs(
                "const c = document.getElementById($0); const idx = Math.round(c.scrollLeft / c.offsetWidth); " +
                        "c.scrollTo({left: c.offsetWidth * (idx - 1), behavior: 'smooth'});", carouselId));

        btnNext.addClickListener(e -> getElement().executeJs(
                "const c = document.getElementById($0); const idx = Math.round(c.scrollLeft / c.offsetWidth); " +
                        "c.scrollTo({left: c.offsetWidth * (idx + 1), behavior: 'smooth'});", carouselId));

        add(container, btnPrev, btnNext, dotsContainer);

        // 4. JAVASCRIPT: Logika Auto-slide & Sinkronisasi Dots
        if (activeCampaigns.size() > 1) {
            getElement().executeJs(
                    "const container = document.getElementById($0);" +
                            "let index = 0;" +

                            "const updateDots = (activeIdx) => {" +
                            "  const dots = this.querySelectorAll('.carousel-dot');" +
                            "  dots.forEach((dot, i) => {" +
                            "    dot.style.backgroundColor = (i === activeIdx) ? 'white' : 'rgba(255,255,255,0.5)';" +
                            "    dot.style.transform = (i === activeIdx) ? 'scale(1.2)' : 'scale(1)';" +
                            "  });" +
                            "};" +

                            // Deteksi scroll manual untuk update dots
                            "container.addEventListener('scroll', () => {" +
                            "  const activeIdx = Math.round(container.scrollLeft / container.offsetWidth);" +
                            "  index = activeIdx;" +
                            "  updateDots(activeIdx);" +
                            "});" +

                            // Interval Auto-slide
                            "setInterval(() => {" +
                            "  const dotsCount = this.querySelectorAll('.carousel-dot').length;" +
                            "  index = (index + 1) % dotsCount;" +
                            "  container.scrollTo({ left: container.offsetWidth * index, behavior: 'smooth' });" +
                            "}, 5000);",
                    carouselId
            );
        }

        // CSS tambahan untuk menyembunyikan scrollbar di browser berbasis Webkit (Chrome/Edge/Safari)
        getElement().executeJs(
                "const style = document.createElement('style');" +
                        "style.innerHTML = '#'+$0+'::-webkit-scrollbar { display: none; }';" +
                        "this.appendChild(style);",
                carouselId
        );
    }

    /**
     * Mengatur styling tombol navigasi agar melayang di atas gambar.
     */
    private void styleNavButton(Button button, boolean isLeft) {
        button.addClassNames(LumoUtility.Position.ABSOLUTE, LumoUtility.BorderRadius.MEDIUM);
        button.getStyle()
                .set("top", "50%")
                .set("transform", "translateY(-50%)")
                .set("background", "rgba(0,0,0,0.3)")
                .set("color", "white")
                .set("z-index", "10")
                .set("border", "none")
                .set("min-width", "40px")
                .set("height", "40px")
                .set("cursor", "pointer");

        if (isLeft) button.getStyle().set("left", "10px");
        else button.getStyle().set("right", "10px");
    }
}