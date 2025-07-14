package com.fusi24.pangreksa.web.view.template;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.security.PermitAll;

@Route("komponen-gaji-list-page-access")
@PageTitle("Komponen Gaji List")
@Menu(order = 23, icon = "vaadin:clipboard-check", title = "Komponen Gaji List")
@RolesAllowed("KOMP_GAJI")
//@PermitAll // When security is enabled, allow all authenticated users
public class RefactorKomponenGajiListView extends Main {

    public static final String VIEW_NAME = "Komponen Gaji List";

    public RefactorKomponenGajiListView() {
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar(VIEW_NAME));
    }
}

