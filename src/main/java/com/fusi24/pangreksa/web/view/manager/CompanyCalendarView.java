package com.fusi24.pangreksa.web.view.manager;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.HrCompanyCalendar;
import com.fusi24.pangreksa.web.model.entity.HrLeaveApplication;
import com.fusi24.pangreksa.web.service.CalendarService;
import com.fusi24.pangreksa.web.service.CommonService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
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

import java.time.LocalDate;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    private FullCalendar calendar;
    private ComboBox<Integer> yearPicker;
    private ComboBox<Month> monthPicker;

    public static final String VIEW_NAME = "Kalendar Kerja";

    public CompanyCalendarView(CurrentUser currentUser,
                               CommonService commonService,
                               CalendarService calendarService) {
        setSizeFull();
        this.currentUser = currentUser;
        this.commonService = commonService;
        this.calendarService = calendarService;

        addClassNames(
                LumoUtility.BoxSizing.BORDER,
                LumoUtility.Display.FLEX,
                LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM,
                LumoUtility.Gap.MEDIUM // Gap diperbesar sedikit agar tidak terlalu rapat
        );

        add(new ViewToolbar(VIEW_NAME));
        createBody();
    }

    private void createBody() {
        VerticalLayout body = new VerticalLayout();
        body.setSizeFull();
        body.setPadding(false);
        body.setSpacing(true);

        // --- 1. HEADER KONTROL (Filter & Keterangan) ---
        HorizontalLayout headerLayout = new HorizontalLayout();
        headerLayout.setWidthFull();
        headerLayout.setAlignItems(FlexComponent.Alignment.BASELINE); // Sejajarkan komponen di bawah
        headerLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN); // Kiri untuk filter, Kanan untuk Legend

        // Bagian Kiri: Dropdown & Tombol
        HorizontalLayout filterLayout = new HorizontalLayout();
        filterLayout.setAlignItems(FlexComponent.Alignment.BASELINE);

        yearPicker = new ComboBox<>("Tahun");
        int currentYear = LocalDate.now().getYear();
        yearPicker.setItems(IntStream.rangeClosed(currentYear - 5, currentYear + 5).boxed().collect(Collectors.toList()));
        yearPicker.setValue(currentYear);
        yearPicker.setWidth("120px"); // Buat ukuran pas

        monthPicker = new ComboBox<>("Bulan");
        monthPicker.setItems(Month.values());
        monthPicker.setItemLabelGenerator(m -> m.getDisplayName(TextStyle.FULL, Locale.forLanguageTag("id")));
        monthPicker.setValue(LocalDate.now().getMonth());
        monthPicker.setWidth("160px");

        // Tombol Kembali ke Bulan Ini
        Button todayBtn = new Button("Bulan Ini", VaadinIcon.CALENDAR_CLOCK.create());
        todayBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        todayBtn.addClickListener(e -> {
            yearPicker.setValue(LocalDate.now().getYear());
            monthPicker.setValue(LocalDate.now().getMonth());
        });

        yearPicker.addValueChangeListener(e -> updateCalendarView());
        monthPicker.addValueChangeListener(e -> updateCalendarView());

        filterLayout.add(yearPicker, monthPicker, todayBtn);

        // Bagian Kanan: Legend (Keterangan Warna)
        HorizontalLayout legendLayout = new HorizontalLayout();
        legendLayout.setAlignItems(FlexComponent.Alignment.CENTER);

        Span dotMerah = new Span();
        dotMerah.setWidth("12px"); dotMerah.setHeight("12px");
        dotMerah.getStyle().set("background-color", "#d9534f").set("border-radius", "50%");
        Span lblLibur = new Span("Libur");
        lblLibur.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        Span dotBiru = new Span();
        dotBiru.setWidth("12px"); dotBiru.setHeight("12px");
        dotBiru.getStyle().set("background-color", "#4a90e2").set("border-radius", "50%");
        Span lblCuti = new Span("Cuti");
        lblCuti.addClassNames(LumoUtility.FontSize.SMALL, LumoUtility.TextColor.SECONDARY);

        legendLayout.add(dotMerah, lblLibur, dotBiru, lblCuti);

        headerLayout.add(filterLayout, legendLayout);

        // --- 2. WRAPPER KALENDER (Tampilan Card) ---
        VerticalLayout calendarCard = new VerticalLayout();
        calendarCard.setSizeFull();
        calendarCard.setPadding(true);
        // Menambahkan style card (Background putih, shadow halus, sudut melengkung)
        calendarCard.addClassNames(
                LumoUtility.Background.BASE,
//                LumoUtility.BoxShadow.XS,
                LumoUtility.BorderRadius.MEDIUM,
                LumoUtility.Border.ALL,
                LumoUtility.BorderColor.CONTRAST_10
        );

        calendar = FullCalendarBuilder.create().build();
        calendar.setHeight("100%"); // Gunakan 100% agar mengikuti tinggi card
        calendar.setWidthFull();
        calendar.changeView(CalendarViewImpl.DAY_GRID_MONTH);
        calendar.setLocale(Locale.forLanguageTag("id"));

        calendarCard.add(calendar);

        refreshData();

        body.add(headerLayout, calendarCard);
        add(body);
    }

    private void updateCalendarView() {
        if (yearPicker.getValue() != null && monthPicker.getValue() != null) {
            LocalDate targetDate = LocalDate.of(yearPicker.getValue(), monthPicker.getValue(), 1);
            calendar.gotoDate(targetDate);
            refreshData();
        }
    }

    private void refreshData() {
        calendar.getEntryProvider().asInMemory().removeAllEntries();
        loadCompanyHoliday();
        loadEmployeeLeave();
    }

    private void loadCompanyHoliday() {
        int year = yearPicker.getValue();
        List<HrCompanyCalendar> holidays = calendarService.getCompanyCalendarsByYear(year);

        for (HrCompanyCalendar h : holidays) {
            Entry entry = new Entry();
            entry.setTitle(h.getLabel());
            entry.setStart(h.getStartDate());
            entry.setEnd(h.getEndDate().plusDays(1));
            entry.setAllDay(true);
            entry.setColor("#d9534f");
            calendar.getEntryProvider().asInMemory().addEntry(entry);
        }
    }

    private void loadEmployeeLeave() {
        List<HrLeaveApplication> leaves = calendarService.getApprovedLeaves();
        if (leaves == null) return;

        for (HrLeaveApplication l : leaves) {
            Entry entry = new Entry();
            String name = l.getEmployee().getFirstName() + " " + l.getEmployee().getLastName();
            entry.setTitle(name + " - Cuti");
            entry.setStart(l.getStartDate());
            entry.setEnd(l.getEndDate().plusDays(1));
            entry.setAllDay(true);
            entry.setColor("#4a90e2");
            calendar.getEntryProvider().asInMemory().addEntry(entry);
        }
    }
}