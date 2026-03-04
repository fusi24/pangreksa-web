package com.fusi24.pangreksa.web.view.manager;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.HrCompanyCalendar;
import com.fusi24.pangreksa.web.model.entity.HrLeaveApplication;
import com.fusi24.pangreksa.web.service.CalendarService;
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
import org.vaadin.stefan.fullcalendar.FullCalendar;
import org.vaadin.stefan.fullcalendar.FullCalendarBuilder;
import org.vaadin.stefan.fullcalendar.Entry;
import org.vaadin.stefan.fullcalendar.CalendarViewImpl;
import org.vaadin.stefan.fullcalendar.dataprovider.InMemoryEntryProvider;
import org.vaadin.stefan.fullcalendar.Entry;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

@Route("company-calendar-page-access")
@PageTitle("Kalender Perusahaan")
@Menu(order = 19, icon = "vaadin:clipboard-check", title = "Kalender Perusahaan")
@RolesAllowed("KAL_KERJA")
public class CompanyCalendarView extends Main {

    private static final long serialVersionUID = 16L;
    private static final Logger log = LoggerFactory.getLogger(CompanyCalendarView.class);

    private final CurrentUser currentUser;
    private final CommonService commonService;
    private final CalendarService calendarService;

    private Authorization auth;
    private VerticalLayout body;

    public static final String VIEW_NAME = "Kalendar Kerja";

    public CompanyCalendarView(CurrentUser currentUser,
                               CommonService commonService,
                               CalendarService calendarService) {
        setSizeFull();
        this.currentUser = currentUser;
        this.commonService = commonService;
        this.calendarService = calendarService;

        this.auth = commonService.getAuthorization(
                currentUser.require(),
                (String) UI.getCurrent().getSession().getAttribute("responsibility"),
                this.serialVersionUID);

        log.debug("Page {}, Authorization: {} {} {} {}",
                VIEW_NAME, auth.canView, auth.canCreate, auth.canEdit, auth.canDelete);

        addClassNames(
                LumoUtility.BoxSizing.BORDER,
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Gap.SMALL
        );

        add(new ViewToolbar(VIEW_NAME));

        createBody();
    }

    private void createBody() {

        body = new VerticalLayout();
        body.setSizeFull();
        body.setPadding(false);
        body.setSpacing(false);

        FullCalendar calendar = FullCalendarBuilder.create().build();

        calendar.setHeight("700px");
        calendar.setWidthFull();

        calendar.changeView(CalendarViewImpl.DAY_GRID_MONTH);

        calendar.setLocale(new Locale("id", "ID"));

        body.add(calendar);

        add(body);
    }


}