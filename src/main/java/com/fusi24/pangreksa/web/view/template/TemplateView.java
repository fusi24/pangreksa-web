package com.fusi24.pangreksa.web.view.template;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import jakarta.annotation.security.PermitAll;

@Route("TEMPLATE_ROUTE")
@PageTitle("TEMPLATE_PAGE_TITLE")
@Menu(order = 99999, icon = "vaadin:clipboard-check", title = "TEMPLATE_PAGE_TITLE")
@RolesAllowed("TEMPLATE_ROLE")
//@PermitAll // When security is enabled, allow all authenticated users
public class TemplateView extends Main {

    public static final String VIEW_NAME = "TEMPLATE_PAGE_TITLE";

    public TemplateView() {
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar(VIEW_NAME));
    }
}

