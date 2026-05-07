package com.fusi24.pangreksa.web.view.common;

import com.fusi24.pangreksa.base.ui.view.MainLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;

@PageTitle("Selamat Datang | Pangreksa")
@Route(value = "pilih-tanggung-jawab", layout = MainLayout.class) // Route unik agar tidak bentrok
@PermitAll
public class WelcomeView extends VerticalLayout {

    public WelcomeView() {
        addClassName("welcome-view");
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        Div container = new Div();
        container.addClassName("welcome-container");

        // Pastikan gambar ada di src/main/resources/static/images/select-role.svg
        Image illustration = new Image("images/pointing.jpg", "Pilih Tanggung Jawab");
        illustration.addClassName("welcome-illustration");

        H2 title = new H2("Selamat Datang di Pangreksa");
        title.addClassName("welcome-title");

        Paragraph message = new Paragraph("Mohon pilih 'Tanggung Jawab' Anda pada menu di sebelah kiri untuk melanjutkan.");
        message.addClassName("welcome-message");

        Div hint = new Div();
        hint.addClassName("welcome-hint");
        hint.add(VaadinIcon.ARROW_LEFT.create(), new Span(" Pilih di sini"));

        container.add(illustration, title, message, hint);
        add(container);
    }
}