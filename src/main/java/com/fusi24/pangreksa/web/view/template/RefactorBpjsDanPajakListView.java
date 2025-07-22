package com.fusi24.pangreksa.web.view.template;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.service.CommonService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Route("bpjs-dan-pajak-list-page-access")
@PageTitle("BPJS dan Pajak List")
@Menu(order = 26, icon = "vaadin:clipboard-check", title = "BPJS dan Pajak List")
@RolesAllowed("BPJS_PAJAK")
//@PermitAll // When security is enabled, allow all authenticated users
public class RefactorBpjsDanPajakListView extends Main {
    private static final long serialVersionUID = 23L;
    private static final Logger log = LoggerFactory.getLogger(RefactorBpjsDanPajakListView.class);
    private final CurrentUser currentUser;
    private final CommonService commonService;
    private Authorization auth;

    public static final String VIEW_NAME = "BPJS dan Pajak List";

    private VerticalLayout body;

    public RefactorBpjsDanPajakListView(CurrentUser currentUser, CommonService commonService) {
        this.currentUser = currentUser;
        this.commonService = commonService;

        this.auth = commonService.getAuthorization(
                currentUser.require(),
                (String) UI.getCurrent().getSession().getAttribute("responsibility"),
                this.serialVersionUID);

        log.debug("Page {}, Authorization: {} {} {} {}", VIEW_NAME, auth.canView, auth.canCreate, auth.canEdit, auth.canDelete);

        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar(VIEW_NAME));
        createBody();

        setListener();
        setAuthorization();
    }

    private void setAuthorization(){
        if(!this.auth.canView){
            // User does not have permission to view this page
        }
    }

    private void createBody() {
        this.body = new VerticalLayout();
        body.setPadding(false);
        body.setSpacing(false);


        add(body);
    }

    private void setListener() {
        // Add listeners for UI components here
        // For example, you can add a click listener to a button
        // saveButton.addClickListener(event -> savePage());
    }
}

