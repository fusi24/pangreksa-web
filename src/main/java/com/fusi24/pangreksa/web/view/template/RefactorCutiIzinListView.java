package com.fusi24.pangreksa.web.view.template;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.security.PermitAll;

@Route("cuti--izin-list-page-access")
@PageTitle("Cuti & Izin List")
@Menu(order = 17, icon = "vaadin:clipboard-check", title = "Cuti & Izin List")
@RolesAllowed("CUTI_IZIN")
//@PermitAll // When security is enabled, allow all authenticated users
public class RefactorCutiIzinListView extends Main {

    public static final String VIEW_NAME = "Cuti & Izin List";

    public RefactorCutiIzinListView() {
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar(VIEW_NAME));
    }
}

