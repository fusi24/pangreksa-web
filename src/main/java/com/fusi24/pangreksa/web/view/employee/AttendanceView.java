package com.fusi24.pangreksa.web.view.employee;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.base.util.ConfirmationDialogUtil;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.model.entity.*;
import com.fusi24.pangreksa.web.repo.FwAppUserRepository;
import com.fusi24.pangreksa.web.service.*;
import com.fusi24.pangreksa.web.view.common.CheckInOutDialog;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Route("attendance-page-access")
@PageTitle("Kehadiran Karyawan")
@Menu(order = 25, icon = "vaadin:user-check", title = "Kehadiran Karyawan")
@RolesAllowed({"HR", "KARYAWAN"})
public class AttendanceView extends Main {

    public static final String VIEW_NAME = "Kehadiran Karyawan";
    private static final long serialVersionUID = 862476621L;
    private static final Logger log = LoggerFactory.getLogger(AttendanceView.class);

    private final CurrentUser currentUser;
    private final CommonService commonService;
    private final SystemService systemService;
    private final CompanyService companyService;
    private final AttendanceService attendanceService;
    private final PersonService personService;
    private final RoleManagementService roleManagementService;
    private final HrWorkScheduleService hrWorkScheduleService;
    private final FwAppUserRepository appUserRepository;

    private Authorization auth;
    private final List<FwAppuserResp> userAppuserResps;

    private Grid<HrAttendance> grid = new Grid<>(HrAttendance.class, false);
    private TextField searchField = new TextField();

    // Filters
    private ComboBox<HrCompany> companyFilter = new ComboBox<>();
    private ComboBox<HrOrgStructure> orgStructureFilter = new ComboBox<>();
    private DatePicker startDateFilter = new DatePicker();
    private DatePicker endDateFilter = new DatePicker();

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public AttendanceView(
            CurrentUser currentUser,
            CommonService commonService,
            SystemService systemService,
            AttendanceService attendanceService,
            PersonService personService,
            RoleManagementService roleManagementService,
            CompanyService companyService,
            HrWorkScheduleService hrWorkScheduleService,
            FwAppUserRepository appUserRepository) {

        this.currentUser = currentUser;
        this.commonService = commonService;
        this.systemService = systemService;
        this.attendanceService = attendanceService;
        this.personService = personService;
        this.roleManagementService = roleManagementService;
        this.companyService = companyService;
        this.hrWorkScheduleService = hrWorkScheduleService;
        this.appUserRepository = appUserRepository;

        this.attendanceService.setUser(currentUser.require());
        this.auth = commonService.getAuthorization(
                currentUser.require(),
                (String) UI.getCurrent().getSession().getAttribute("responsibility"),
                this.serialVersionUID
        );

        this.userAppuserResps = roleManagementService.findResponsibilitesByUser(attendanceService.getCurrentUser());
        boolean isEmployee = userAppuserResps.stream()
                .map(p -> p.getResponsibility())
                .map(r -> r.getLabel())
                .anyMatch("KARYAWAN"::equals);

        // Auto-show check-in popup for KARYAWAN on working days
        if (isEmployee && attendanceService.shouldShowCheckInPopup()) {
            openSelfServiceCheckInOut();
        }

        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar(VIEW_NAME));
        log.debug("Page {}, Authorization: {} {} {} {}", VIEW_NAME, auth.canView, auth.canCreate, auth.canEdit, auth.canDelete);
        initializeView();
    }

    private void initializeView() {
        this.setHeightFull();

        // === Grid Configuration ===
        grid.addColumn(att -> att.getPerson().getFirstName() + " " + att.getPerson().getLastName())
                .setHeader("Karyawan").setAutoWidth(true);
        grid.addColumn(att -> att.getAttendanceDate().format(dateFormatter))
                .setHeader("Tanggal").setWidth("120px");
        grid.addColumn(att -> att.getCheckIn() != null ? att.getCheckIn().format(DateTimeFormatter.ofPattern("HH:mm")) : "-")
                .setHeader("Clock-In").setWidth("100px");
        grid.addColumn(att -> att.getCheckOut() != null ? att.getCheckOut().format(DateTimeFormatter.ofPattern("HH:mm")) : "-")
                .setHeader("Clock-Out").setWidth("100px");
        grid.addColumn(HrAttendance::getStatus).setHeader("Status").setWidth("130px");
        grid.addColumn(HrAttendance::getNotes).setHeader("Catatan").setAutoWidth(true);

        // HR-only actions
        boolean isHr = userAppuserResps.stream()
                .map(p -> p.getResponsibility())
                .map(r -> r.getLabel())
                .anyMatch(label -> StringUtils.contains(label, "HR"));

        if (isHr) {
            grid.addComponentColumn(this::createActionButtons)
                    .setHeader("Aksi").setWidth("120px");
        } else {
            grid.addComponentColumn(this::createCheckOutButtons)
                    .setHeader("Aksi").setWidth("120px");
        }

        // === Filters ===
        if (isHr) {
            companyFilter.setPlaceholder("Perusahaan");
            companyFilter.setItems(companyService.getallCompanies());
            companyFilter.setItemLabelGenerator(HrCompany::getName);
            companyFilter.setWidth("150px");
            companyFilter.addValueChangeListener(e -> {
                orgStructureFilter.setItems(new ArrayList<>());
                if (e.getValue() != null) {
                    orgStructureFilter.setItems(companyService.getAllOrgStructuresInCompany(e.getValue()));
                }
                applyFilters();
            });

            orgStructureFilter.setPlaceholder("Org Structure");
            orgStructureFilter.setItemLabelGenerator(HrOrgStructure::getName);
            orgStructureFilter.setWidth("150px");
            orgStructureFilter.addValueChangeListener(e -> applyFilters());
        } else {
            companyFilter.setEnabled(false);
            orgStructureFilter.setEnabled(false);
        }

        // Date range
        startDateFilter.setPlaceholder("Mulai");
        startDateFilter.setValue(LocalDate.now().minusDays(30));
        endDateFilter.setPlaceholder("Sampai");
        endDateFilter.setValue(LocalDate.now());
        startDateFilter.addValueChangeListener(e -> applyFilters());
        endDateFilter.addValueChangeListener(e -> applyFilters());

        HorizontalLayout filterBar = new HorizontalLayout(
                isHr ? companyFilter : new Span(""),
                isHr ? orgStructureFilter : new Span(""),
                startDateFilter,
                endDateFilter
        );
        filterBar.setSpacing(true);

        searchField.setPlaceholder("Cari nama karyawan");
        searchField.setWidth("300px");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> applyFilters());

        Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH), e -> applyFilters());
        refreshButton.setAriaLabel("Refresh");

        Button addAttendanceButton = new Button("Tambah Absensi", e -> openAddAttendanceDialog());
        addAttendanceButton.setVisible(isHr);
        addAttendanceButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        HorizontalLayout toolbar = new HorizontalLayout(filterBar, searchField, refreshButton, addAttendanceButton);
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        VerticalLayout layout = new VerticalLayout(toolbar, grid);
        layout.setHeightFull();
        layout.setPadding(false);
        layout.setSpacing(false);
        grid.setSizeFull();

        add(layout);
        applyFilters();
    }

    private Component createActionButtons(HrAttendance attendance) {
        HorizontalLayout actions = new HorizontalLayout();

        Button deleteBtn = new Button(new Icon(VaadinIcon.TRASH));
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_TERTIARY_INLINE);
        deleteBtn.setTooltipText("Hapus Absensi");
        deleteBtn.addClickListener(e -> {
            String name = attendance.getPerson().getFirstName() + " " + attendance.getPerson().getLastName();
            ConfirmationDialogUtil.showConfirmation(
                    "Hapus Absensi?",
                    "Hapus data absensi untuk " + name + " pada " + attendance.getAttendanceDate() + "?",
                    "Hapus",
                    ev -> {
                        try {
                            attendanceService.deleteAttendance(attendance);
                            Notification.show("Berhasil dihapus", 3000, Notification.Position.MIDDLE);
                            applyFilters();
                        } catch (Exception ex) {
                            Notification.show("Gagal: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                        }
                    }
            );
        });
        actions.add(deleteBtn);
        return actions;
    }

    private Component createCheckOutButtons(HrAttendance attendance) {
        // Only show button if check-out hasn't happened yet
        if (attendance.getCheckOut() == null) {
            Button clockOutBtn = new Button("Clock-Out Sekarang");
            clockOutBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
            clockOutBtn.setTooltipText("Catat waktu pulang sekarang");
            clockOutBtn.addClickListener(e -> {
                // Set current Jakarta time (or your system's local time)
                // If you're using Jakarta time explicitly:
                // ZoneId jakartaZone = ZoneId.of("Asia/Jakarta");
                // LocalDateTime now = LocalDateTime.now(jakartaZone);

                // Or just use system default if already configured correctly:
                LocalDateTime now = LocalDateTime.now();

                attendance.setCheckOut(now);
                try {
                    attendanceService.saveAttendance(attendance, attendanceService.getCurrentUser()); // or update method
                    Notification.show("Clock-out berhasil", 3000, Notification.Position.MIDDLE);
                    applyFilters(); // refresh grid
                } catch (Exception ex) {
                    Notification.show("Gagal clock-out: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                    // Optionally revert the change in UI
                    attendance.setCheckOut(null);
                }
            });

            HorizontalLayout actions = new HorizontalLayout(clockOutBtn);
            actions.setPadding(false);
            actions.setSpacing(true);
            return actions;
        }

        // If already clocked out, show nothing (or you could show a disabled button or timestamp)
        return new Span("-"); // or return new HorizontalLayout() if you prefer empty cell
    }

    private void applyFilters() {
        String searchTerm = searchField.getValue();
        LocalDate start = startDateFilter.getValue();
        LocalDate end = endDateFilter.getValue();
        HrCompany company = companyFilter.getValue();
        HrOrgStructure orgStructure = orgStructureFilter.getValue();

        DataProvider<HrAttendance, Void> provider = DataProvider.fromCallbacks(
                query -> {
                    int offset = query.getOffset();
                    int limit = query.getLimit();
                    int page = offset / limit;
                    PageRequest pageReq = PageRequest.of(page, limit);
                    return attendanceService.getAttendancePage(pageReq, start, end, searchTerm, company, orgStructure)
                            .getContent().stream();
                },
                query -> (int) attendanceService.countAttendance(start, end, searchTerm, company, orgStructure)
        );
        grid.setDataProvider(provider);
    }

    // === KARYAWAN: Self-service check-in/out ===
    private void openSelfServiceCheckInOut() {
        HrAttendance currentRecord = attendanceService.getOrCreateTodayAttendance(attendanceService.getCurrentUser());
        if (currentRecord != null) {
            CheckInOutDialog dialog = new CheckInOutDialog(
                    attendanceService,
                    currentRecord,
                    currentUser,
                    this::applyFilters
            );
            dialog.open();
        }
    }

    // === HR: Manual add attendance ===
    private void openAddAttendanceDialog() {
        AddAttendanceDialog dialog = new AddAttendanceDialog(
                attendanceService,
                hrWorkScheduleService,
                personService,
                currentUser,
                this::applyFilters
        );
        dialog.open();
    }

    public class AddAttendanceDialog extends Dialog {

        private final AttendanceService attendanceService;
        private final HrWorkScheduleService workScheduleService;
        private final PersonService personService;
        private final CurrentUser currentUser;
        private final Runnable onSuccess;

        private ComboBox<HrPerson> personField = new ComboBox<>("Karyawan");
        private DatePicker dateField = new DatePicker("Tanggal");
        private DateTimePicker checkInField = new DateTimePicker("Check-In");
        private DateTimePicker checkOutField = new DateTimePicker("Check-Out");
        private TextArea notesField = new TextArea("Catatan");

        public AddAttendanceDialog(
                AttendanceService attendanceService,
                HrWorkScheduleService workScheduleService,
                PersonService personService,
                CurrentUser currentUser,
                Runnable onSuccess) {

            this.attendanceService = attendanceService;
            this.workScheduleService = workScheduleService;
            this.personService = personService;
            this.currentUser = currentUser;
            this.onSuccess = onSuccess;

            setHeaderTitle("Tambah Absensi Manual");
            setModal(true);
            setWidth("600px");

            configureForm();
        }

        private void configureForm() {
            // Load active employees
            List<HrPerson> employees = personService.findAllPerson();
            personField.setItems(employees);
            personField.setItemLabelGenerator(p -> p.getFirstName() + " " + p.getLastName());
            personField.setRequired(true);
            personField.setWidthFull();

            dateField.setRequired(true);
            dateField.setValue(LocalDate.now());
            dateField.setWidthFull();

            checkInField.setWidthFull();
            checkOutField.setWidthFull();
            notesField.setWidthFull();

            Button saveButton = new Button("Simpan", e -> saveAttendance());
            saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

            Button cancelButton = new Button("Batal", e -> close());
            HorizontalLayout buttons = new HorizontalLayout(saveButton, cancelButton);

            FormLayout form = new FormLayout(
                    personField,
                    dateField,
                    checkInField,
                    checkOutField,
                    notesField,
                    buttons
            );
            form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));
            add(form);
        }

        private void saveAttendance() {
            if (personField.getValue() == null || dateField.getValue() == null) {
                Notification.show("Karyawan dan tanggal wajib diisi", 3000, Notification.Position.MIDDLE);
                return;
            }

            try {
                FwAppUser user = appUserRepository.findByPersonId(personField.getValue().getId()).orElse(null);
                HrAttendance att = new HrAttendance();
                att.setPerson(personField.getValue());
                att.setAppUser(user); // ensure AppUserInfo is linked
                att.setAttendanceDate(dateField.getValue());

                // Resolve work schedule for this person on this date
                HrWorkSchedule schedule = workScheduleService.getActiveScheduleForUser(
                        user, dateField.getValue()
                );
                if (schedule == null) {
                    Notification.show("Tidak ada jadwal kerja untuk karyawan ini pada tanggal tersebut", 4000, Notification.Position.MIDDLE);
                    return;
                }
                att.setWorkSchedule(schedule);

                att.setCheckIn(checkInField.getValue());
                att.setCheckOut(checkOutField.getValue());
                att.setNotes(notesField.getValue());

                // Save — status will be auto-set in service
                attendanceService.saveAttendance(att, currentUser.require());
                Notification.show("Absensi berhasil ditambahkan", 3000, Notification.Position.MIDDLE);
                close();
                if (onSuccess != null) onSuccess.run();
            } catch (Exception ex) {
                Notification.show("Gagal: " + ex.getMessage(), 5000, Notification.Position.MIDDLE);
                ex.printStackTrace();
            }
        }
    }
}