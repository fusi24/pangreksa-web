package com.fusi24.pangreksa.web.view.employee;

import com.fusi24.pangreksa.web.service.AttendanceImportService;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.base.ui.notification.AppNotification;
import com.fusi24.pangreksa.base.util.ConfirmationDialogUtil;
import com.fusi24.pangreksa.security.CurrentUser;
import com.pangreksa.service.shared.Authorization;
import com.pangreksa.service.model.entity.*;
import com.pangreksa.service.model.repo.FwAppUserRepository;
import com.pangreksa.service.service.*;
import com.fusi24.pangreksa.web.view.common.CheckInOutDialog;
import com.fusi24.pangreksa.web.view.common.UploadAttendanceDialog;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.DataProvider;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.fusi24.pangreksa.base.ui.TailwindUtility;
import com.vaadin.flow.server.StreamRegistration;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.VaadinSession;
import jakarta.annotation.security.RolesAllowed;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import com.vaadin.flow.data.provider.SortDirection;
import com.vaadin.flow.component.grid.GridSortOrder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import org.springframework.data.domain.Sort;
import com.vaadin.flow.data.provider.QuerySortOrder;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Route("attendance-page-access")
@PageTitle("Kehadiran Karyawan")
@Menu(order = 25, icon = "vaadin:user-check", title = "Kehadiran Karyawan")
@RolesAllowed({"HR", "KARYAWAN"})

public class AttendanceView extends Main {
    private DataProvider<HrAttendance, Void> attendanceProvider;
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
    private final AttendanceImportService attendanceImportService;
    private final HrCompanyBranchService hrCompanyBranchService;

    private Authorization auth;
    private String responsibility;
    private final List<FwAppuserResp> userAppuserResps;

    private Grid<HrAttendance> grid = new Grid<>(HrAttendance.class, false);
    private TextField searchField = new TextField();

    private final java.util.concurrent.ExecutorService executor =
            java.util.concurrent.Executors.newFixedThreadPool(2);

    // Filters
    private ComboBox<HrCompany> companyFilter = new ComboBox<>();
    private ComboBox<HrOrgStructure> orgStructureFilter = new ComboBox<>();
    private DatePicker startDateFilter = new DatePicker();
    private DatePicker endDateFilter = new DatePicker();

    private final DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private Button checkInButton;

    public AttendanceView(
            CurrentUser currentUser,
            CommonService commonService,
            SystemService systemService,
            AttendanceService attendanceService,
            PersonService personService,
            RoleManagementService roleManagementService,
            CompanyService companyService,
            HrWorkScheduleService hrWorkScheduleService,
            FwAppUserRepository appUserRepository,
            AttendanceImportService attendanceImportService,
            HrCompanyBranchService hrCompanyBranchService) {

        this.currentUser = currentUser;
        this.commonService = commonService;
        this.systemService = systemService;
        this.attendanceService = attendanceService;
        this.personService = personService;
        this.roleManagementService = roleManagementService;
        this.companyService = companyService;
        this.hrWorkScheduleService = hrWorkScheduleService;
        this.appUserRepository = appUserRepository;
        this.attendanceImportService = attendanceImportService;
        this.hrCompanyBranchService = hrCompanyBranchService;

        this.attendanceService.setUser(currentUser.require());
        this.responsibility = (String) UI.getCurrent().getSession().getAttribute("responsibility");
        this.auth = commonService.getAuthorization(
                currentUser.require(),
                responsibility,
                this.serialVersionUID
        );

        this.userAppuserResps = roleManagementService.findResponsibilitesByUser(attendanceService.getCurrentUser());
        boolean isEmployee = userAppuserResps.stream()
                .map(p -> p.getResponsibility())
                .map(r -> r.getLabel())
                .anyMatch("KARYAWAN"::equals);

        // Auto-show Clock-In popup for KARYAWAN on working days
        if (isEmployee) {
                openSelfServiceCheckInOut();
        }


        addClassNames(TailwindUtility.BoxSizing.BORDER, TailwindUtility.Display.FLEX, TailwindUtility.FlexDirection.COLUMN,
                TailwindUtility.Padding.MEDIUM, TailwindUtility.Gap.SMALL);

        add(new ViewToolbar(VIEW_NAME));
        log.debug("Page {}, Authorization: {} {} {} {}", VIEW_NAME, auth.canView, auth.canCreate, auth.canEdit, auth.canDelete);
        initializeView();
    }

    private void initializeView() {
        this.setHeightFull();
        if ("Karyawan".equals(this.responsibility)) {
            updateCheckInButtonState();
        }

        // === Grid Configuration ===
        grid.addColumn(att -> att.getPerson().getFirstName() + " " + att.getPerson().getLastName())
                .setHeader("Karyawan").setAutoWidth(true);

        Grid.Column<HrAttendance> checkInColumn =
                grid.addColumn(att ->
                                att.getCheckIn() != null
                                        ? att.getCheckIn().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm"))
                                        : "-"
                        )
                        .setHeader("Clock-In")
                        .setKey("checkIn")              // 🔑 PENTING
                        .setSortable(true)
                        .setComparator(HrAttendance::getCheckIn)
                        .setWidth("140px");

        grid.addColumn(att -> att.getCheckOut() != null ? att.getCheckOut().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")) : "-")
                .setHeader("Clock-Out").setWidth("100px");

        grid.addColumn(att -> formatWorkDuration(att.getTotalWorkMinutes()))
                .setHeader("Total Jam Kerja")
                .setWidth("140px");

        grid.addColumn(att -> formatAttendanceLocation(att))
                .setHeader("Lokasi Absen")
                .setAutoWidth(true)
                .setFlexGrow(1);

        grid.addColumn(att -> {
                    // Jika belum checkout → status disembunyikan
                    if (att.getCheckOut() == null) {
                        return "-";
                    }
                    // Jika sudah checkout → tampilkan status
                    return att.getStatus() != null ? att.getStatus() : "-";
                })
                .setHeader("Status")
                .setWidth("130px");

        grid.addColumn(HrAttendance::getNotes).setHeader("Catatan").setAutoWidth(true);

        // HR-only actions
        boolean isHr = !"Karyawan".equals(this.responsibility);
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

            orgStructureFilter.setPlaceholder("Org Struktur");
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
        searchField.setVisible(isHr);

        Button refreshButton = new Button(new Icon(VaadinIcon.REFRESH), e -> applyFilters());
        MenuBar exportMenu = buildExportMenu();
        checkInButton = new Button("Clock-In / Clock-Out");
        checkInButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        checkInButton.setIcon(new Icon(VaadinIcon.CLOCK));
        checkInButton.addClickListener(e -> openSelfServiceCheckInOut());
        checkInButton.setVisible("Karyawan".equals(this.responsibility));

        refreshButton.setAriaLabel("Refresh");

        Button addAttendanceButton = new Button("Tambah Absensi", e -> openAddAttendanceDialog());
        addAttendanceButton.setVisible(isHr);
        addAttendanceButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button uploadAttendanceButton = new Button("Upload Absensi", e -> openUploadAttendanceDialog());
        uploadAttendanceButton.addThemeVariants(ButtonVariant.LUMO_CONTRAST);
        uploadAttendanceButton.setVisible(isHr);

        HorizontalLayout toolbar = new HorizontalLayout(
                filterBar,
                searchField,
                refreshButton,
                exportMenu,
                checkInButton,
                uploadAttendanceButton,
                addAttendanceButton
        );
        toolbar.setWidthFull();
        toolbar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);

        VerticalLayout layout = new VerticalLayout(toolbar, grid);
        layout.setHeightFull();
        layout.setPadding(false);
        layout.setSpacing(false);
        grid.setSizeFull();

        grid.sort(
                Collections.singletonList(
                        new GridSortOrder<>(checkInColumn, SortDirection.DESCENDING)
                )
        );


        add(layout);
        initDataProvider();
    }

    private void updateCheckInButtonState() {
        if (checkInButton == null) return;

        HrAttendance today = attendanceService.getOrCreateTodayAttendance(attendanceService.getCurrentUser());
        if (today == null) return;

        if (today.getCheckIn() != null && today.getCheckOut() != null) {
            checkInButton.setEnabled(false);
            checkInButton.setText("Sudah Absen Hari Ini");
        } else if (today.getCheckIn() != null) {
            checkInButton.setEnabled(true);
            checkInButton.setText("Clock-Out Sekarang");
        } else {
            checkInButton.setEnabled(true);
            checkInButton.setText("Clock-In Sekarang");
        }
    }

    private void initDataProvider() {

        attendanceProvider = DataProvider.fromCallbacks(
                query -> {
                    int offset = query.getOffset();
                    int limit = query.getLimit();
                    int page = offset / limit;

                    // 🔑 DEFAULT SORT: checkIn DESC
                    Sort sort = Sort.by(Sort.Direction.DESC, "checkIn");

                    if (!query.getSortOrders().isEmpty()) {
                        QuerySortOrder order = query.getSortOrders().get(0);

                        Sort.Direction dir =
                                order.getDirection() == SortDirection.ASCENDING
                                        ? Sort.Direction.ASC
                                        : Sort.Direction.DESC;
                        if ("checkIn".equals(order.getSorted())) {
                            sort = Sort.by(dir, "checkIn");
                        }
                    }


                    PageRequest pageReq = PageRequest.of(page, limit, sort);

                    return attendanceService
                            .getAttendancePage(
                                    pageReq,
                                    startDateFilter.getValue(),
                                    endDateFilter.getValue(),
                                    searchField.getValue(),
                                    companyFilter.getValue(),
                                    orgStructureFilter.getValue(),
                                    "Karyawan".equals(this.responsibility)
                                            ? attendanceService.getCurrentUser().getPerson()
                                            : null
                            )
                            .getContent()
                            .stream();
                },
                query -> (int) attendanceService.countAttendance(
                        startDateFilter.getValue(),
                        endDateFilter.getValue(),
                        searchField.getValue(),
                        companyFilter.getValue(),
                        orgStructureFilter.getValue(),
                        "Karyawan".equals(this.responsibility)
                                ? attendanceService.getCurrentUser().getPerson()
                                : null
                )
        );

        grid.setDataProvider(attendanceProvider);
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

    AppNotification.success("Berhasil dihapus");

    applyFilters();

} catch (Exception ex) {
    AppNotification.error("Gagal: " + ex.getMessage());
}
                    }
            );
        });
        actions.add(deleteBtn);
        return actions;
    }

    private Component createCheckOutButtons(HrAttendance attendance) {

        if (attendance.getCheckOut() != null) {
            return new Span("-");
        }

        Button clockOutBtn = new Button("Clock-Out");
        clockOutBtn.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);

        clockOutBtn.addClickListener(e -> {

            ConfirmationDialogUtil.showConfirmation(
                    "Konfirmasi Clock-Out",
                    "Apakah Anda yakin ingin melakukan Clock-Out sekarang?",
                    "Ya, Clock-Out",
                    ev -> {

                        ZoneId jakartaZone = ZoneId.of("Asia/Jakarta");
                        LocalDateTime now = LocalDateTime.now(jakartaZone);

                        attendance.setCheckOut(now);

                       try {
    attendanceService.saveAttendance(attendance, currentUser.require());

    AppNotification.success("Clock-Out berhasil");

    applyFilters();

} catch (Exception ex) {
    attendance.setCheckOut(null);

    AppNotification.error("Gagal Clock-Out: " + ex.getMessage());
}
                    }
            );

        });

        return new HorizontalLayout(clockOutBtn);
    }

    private void applyFilters() {

        String searchTerm = searchField.getValue();
        LocalDate start = startDateFilter.getValue();
        LocalDate end = endDateFilter.getValue();
        HrCompany company = companyFilter.getValue();
        HrOrgStructure orgStructure = orgStructureFilter.getValue();
        HrPerson emp = "Karyawan".equals(this.responsibility)
                ? attendanceService.getCurrentUser().getPerson()
                : null;

        DataProvider<HrAttendance, Void> provider = DataProvider.fromCallbacks(
                query -> {
                    int offset = query.getOffset();
                    int limit = query.getLimit();
                    int page = offset / limit;

                    // ✅ default sort: tanggal DESC
                    Sort sort = Sort.by(Sort.Direction.DESC, "attendanceDate");

                    // ✅ baca sorting dari Grid
                    if (!query.getSortOrders().isEmpty()) {
                        QuerySortOrder order = query.getSortOrders().get(0);
                        Sort.Direction dir =
                                order.getDirection() == SortDirection.ASCENDING
                                        ? Sort.Direction.ASC
                                        : Sort.Direction.DESC;

                        sort = Sort.by(dir, "attendanceDate");
                    }

                    PageRequest pageReq = PageRequest.of(page, limit, sort);

                    return attendanceService
                            .getAttendancePage(
                                    pageReq,
                                    start,
                                    end,
                                    searchTerm,
                                    company,
                                    orgStructure,
                                    emp
                            )
                            .getContent()
                            .stream();
                },
                query -> (int) attendanceService.countAttendance(
                        start,
                        end,
                        searchTerm,
                        company,
                        orgStructure,
                        emp
                )
        );

        grid.setDataProvider(provider);
    }



    // === KARYAWAN: Self-service Clock-In/out ===
    private void openSelfServiceCheckInOut() {
        HrAttendance currentRecord =
                attendanceService.getOrCreateTodayAttendance(attendanceService.getCurrentUser());

        if (currentRecord == null) return;

        CheckInOutDialog dialog = new CheckInOutDialog(
                attendanceService,
                currentRecord,
                currentUser,
                () -> {
                    applyFilters();
                    updateCheckInButtonState(); // ⬅️ penting
                }
        );
        dialog.open();
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
        private DateTimePicker checkInField = new DateTimePicker("Clock-In");
        private DateTimePicker checkOutField = new DateTimePicker("Clock-Out");
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
        AppNotification.error("Karyawan dan tanggal wajib diisi");
        return;
    }

    try {
        FwAppUser user = appUserRepository
                .findByPersonId(personField.getValue().getId())
                .orElse(null);

        HrAttendance att = new HrAttendance();
        att.setPerson(personField.getValue());
        att.setAppUser(user); // ensure AppUserInfo is linked
        att.setAttendanceDate(dateField.getValue());

        // Resolve Jadwal Kerja for this person on this date
        HrWorkSchedule schedule = workScheduleService
                .getActiveScheduleForUser(user, dateField.getValue());

        if (schedule == null) {
            AppNotification.error(
                "Tidak ada jadwal kerja untuk karyawan ini pada tanggal tersebut"
            );
            return;
        }

        att.setWorkSchedule(schedule);
        att.setCheckIn(checkInField.getValue());
        att.setCheckOut(checkOutField.getValue());
        att.setNotes(notesField.getValue());

        // Save — status will be auto-set in service
        attendanceService.saveAttendance(att, currentUser.require());

        AppNotification.success("Absensi berhasil ditambahkan");

        close();
        if (onSuccess != null) onSuccess.run();

    } catch (Exception ex) {
        AppNotification.error("Gagal: " + ex.getMessage());
        ex.printStackTrace();
    }
}
    }

    private MenuBar buildExportMenu() {
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeName("small");

        MenuItem export = menuBar.addItem(new Icon(VaadinIcon.DOWNLOAD_ALT));
        export.add(" Export");

        export.getSubMenu().addItem("CSV", e -> downloadAttendanceExport(false));
        export.getSubMenu().addItem("XLSX", e -> downloadAttendanceExport(true));

        return menuBar;
    }

    @SuppressWarnings("deprecation")
    private void downloadAttendanceExport(boolean xlsx) {
        Dialog loading = new Dialog();
        loading.setModal(true);
        loading.setCloseOnEsc(false);
        loading.setCloseOnOutsideClick(false);
        loading.setWidth("420px");

        Span title = new Span("Menyiapkan file rekap kehadiran...");
        title.getStyle().set("font-weight", "600");

        ProgressBar progressBar = new ProgressBar(0, 100, 1);
        progressBar.setWidthFull();

        Span percentText = new Span("1%");
        Span statusText = new Span("Mengambil data kehadiran...");

        Button finishButton = new Button("Selesai", e -> loading.close());
        finishButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        finishButton.setVisible(false);

        VerticalLayout box = new VerticalLayout(title, statusText, progressBar, percentText, finishButton);
        box.setPadding(true);
        box.setSpacing(true);
        box.setAlignItems(FlexComponent.Alignment.STRETCH);

        loading.add(box);
        loading.open();

        UI ui = UI.getCurrent();

        CompletableFuture.runAsync(() -> {
            try {
                updateExportProgress(ui, progressBar, percentText, statusText, 10, "Mengambil data kehadiran...");

                List<HrAttendance> attendances = getFilteredAttendancesForExport();

                if (attendances.isEmpty()) {
                    ui.access(() -> {
                        progressBar.setValue(100);
                        percentText.setText("100%");
                        statusText.setText("Tidak ada data untuk diekspor.");
                        finishButton.setVisible(true);
                        AppNotification.error("Tidak ada data untuk diekspor.");
                        ui.push();
                    });
                    return;
                }

                updateExportProgress(ui, progressBar, percentText, statusText, 40, "Menyusun data export...");

                AttendanceExportTable table = buildAttendanceExportTable(attendances);

                updateExportProgress(ui, progressBar, percentText, statusText, 75, "Membuat file...");

                byte[] bytes = xlsx ? buildAttendanceXlsx(table) : buildAttendanceCsv(table);
                String filename = buildAttendanceExportFilename(xlsx);

                updateExportProgress(ui, progressBar, percentText, statusText, 90, "Menyiapkan download...");

                ui.access(() -> {
                    StreamResource resource = new StreamResource(
                            filename,
                            () -> new ByteArrayInputStream(bytes)
                    );

                    StreamRegistration registration = VaadinSession.getCurrent()
                            .getResourceRegistry()
                            .registerResource(resource);

                    UI.getCurrent().getPage().open(registration.getResourceUri().toString(), "_blank");

                    progressBar.setValue(100);
                    percentText.setText("100%");
                    statusText.setText("File berhasil dibuat. Silakan klik Selesai.");
                    finishButton.setVisible(true);
                    ui.push();
                });

            } catch (Exception ex) {
                log.error("Export attendance failed", ex);

                ui.access(() -> {
                    progressBar.setValue(100);
                    percentText.setText("100%");
                    statusText.setText("Export gagal: " + ex.getMessage());
                    finishButton.setText("Tutup");
                    finishButton.setVisible(true);
                    AppNotification.error("Export gagal: " + ex.getMessage());
                    ui.push();
                });
            }
        }, executor);
    }

    private void updateExportProgress(UI ui,
                                      ProgressBar progressBar,
                                      Span percentText,
                                      Span statusText,
                                      int value,
                                      String message) {
        if (ui == null || !ui.isAttached()) return;

        ui.access(() -> {
            progressBar.setValue(value);
            percentText.setText(value + "%");
            statusText.setText(message);
            ui.push();
        });
    }

    private List<HrAttendance> getFilteredAttendancesForExport() {
        HrPerson emp = "Karyawan".equals(this.responsibility)
                ? attendanceService.getCurrentUser().getPerson()
                : null;

        return attendanceService.getAttendanceList(
                startDateFilter.getValue(),
                endDateFilter.getValue(),
                searchField.getValue(),
                companyFilter.getValue(),
                orgStructureFilter.getValue(),
                emp
        );
    }

    private AttendanceExportTable buildAttendanceExportTable(List<HrAttendance> attendances) {
        List<String> headers = List.of(
                "No",
                "Karyawan",
                "Tanggal Absensi",
                "Clock-In",
                "Clock-Out",
                "Total Jam Kerja",
                "Lokasi Absen",
                "Status",
                "Catatan"
        );

        List<List<Object>> rows = new ArrayList<>();

        for (int i = 0; i < attendances.size(); i++) {
            HrAttendance att = attendances.get(i);

            rows.add(List.of(
                    i + 1,
                    getAttendanceEmployeeName(att),
                    att.getAttendanceDate() == null ? "-" : att.getAttendanceDate().format(dateFormatter),
                    att.getCheckIn() == null ? "-" : att.getCheckIn().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")),
                    att.getCheckOut() == null ? "-" : att.getCheckOut().format(DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm")),
                    formatWorkDuration(att.getTotalWorkMinutes()),
                    formatAttendanceLocation(att),
                    resolveAttendanceStatusForExport(att),
                    att.getNotes() == null ? "-" : att.getNotes()
            ));
        }

        List<Object> summaryRow = List.of(
                "",
                "Total Data",
                attendances.size(),
                "",
                "",
                "",
                "",
                "",
                ""
        );

        return new AttendanceExportTable("Rekap Kehadiran", headers, rows, summaryRow);
    }

    private String getAttendanceEmployeeName(HrAttendance att) {
        if (att == null || att.getPerson() == null) return "-";

        String firstName = att.getPerson().getFirstName() == null ? "" : att.getPerson().getFirstName();
        String lastName = att.getPerson().getLastName() == null ? "" : att.getPerson().getLastName();

        String fullName = (firstName + " " + lastName).trim().replaceAll("\\s+", " ");
        return fullName.isBlank() ? "-" : fullName;
    }

    private String resolveAttendanceStatusForExport(HrAttendance att) {
        if (att == null) return "-";
        if (att.getCheckOut() == null) return "-";
        return att.getStatus() == null || att.getStatus().isBlank() ? "-" : att.getStatus();
    }

    private byte[] buildAttendanceCsv(AttendanceExportTable table) {
        StringBuilder sb = new StringBuilder();
        sb.append('\uFEFF');

        sb.append(toAttendanceCsvLine(table.headers())).append("\n");
        for (List<Object> row : table.rows()) {
            sb.append(toAttendanceCsvLine(row)).append("\n");
        }
        sb.append(toAttendanceCsvLine(table.summaryRow())).append("\n");

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    private String toAttendanceCsvLine(List<?> values) {
        return values.stream()
                .map(this::toAttendanceCsvValue)
                .collect(Collectors.joining(","));
    }

    private String toAttendanceCsvValue(Object value) {
        String text = value == null ? "" : String.valueOf(value);
        text = text.replace("\"", "\"\"");
        return "\"" + text + "\"";
    }

    private byte[] buildAttendanceXlsx(AttendanceExportTable table) throws Exception {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Rekap Kehadiran");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle totalStyle = workbook.createCellStyle();
            Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);

            int rowIndex = 0;

            Row headerRow = sheet.createRow(rowIndex++);
            for (int i = 0; i < table.headers().size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(table.headers().get(i));
                cell.setCellStyle(headerStyle);
            }

            for (List<Object> rowData : table.rows()) {
                Row row = sheet.createRow(rowIndex++);
                writeAttendanceXlsxRow(row, rowData, null);
            }

            Row totalRow = sheet.createRow(rowIndex);
            writeAttendanceXlsxRow(totalRow, table.summaryRow(), totalStyle);

            for (int i = 0; i < table.headers().size(); i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }

    private void writeAttendanceXlsxRow(Row row, List<Object> values, CellStyle style) {
        for (int i = 0; i < values.size(); i++) {
            Cell cell = row.createCell(i);
            Object value = values.get(i);

            if (value instanceof Number number) {
                cell.setCellValue(number.doubleValue());
            } else {
                cell.setCellValue(value == null ? "" : String.valueOf(value));
            }

            if (style != null) {
                cell.setCellStyle(style);
            }
        }
    }

    private String buildAttendanceExportFilename(boolean xlsx) {
        String start = startDateFilter.getValue() == null
                ? "start"
                : startDateFilter.getValue().toString();

        String end = endDateFilter.getValue() == null
                ? "end"
                : endDateFilter.getValue().toString();

        return "rekap-kehadiran-" + start + "-to-" + end + (xlsx ? ".xlsx" : ".csv");
    }

    private record AttendanceExportTable(
            String sheetName,
            List<String> headers,
            List<List<Object>> rows,
            List<Object> summaryRow
    ) {}

    private String formatWorkDuration(Integer totalMinutes) {
        if (totalMinutes == null || totalMinutes <= 0) return "-";

        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;

        // format HH:mm
        return String.format("%02d:%02d", hours, minutes);
    }

    private void openUploadAttendanceDialog() {
        UploadAttendanceDialog dialog = new UploadAttendanceDialog(
                attendanceImportService,
                hrCompanyBranchService,
                currentUser,
                this::applyFilters
        );
        dialog.open();
    }

    private String formatAttendanceLocation(HrAttendance att) {
        if (att == null) return "-";

        String code = att.getBranchCode();
        String name = att.getBranchName();

        if ((code == null || code.isBlank()) && (name == null || name.isBlank())) {
            return "-";
        }

        // Contoh output: "YO - YASMIN OFFICE"
        if (code != null && !code.isBlank() && name != null && !name.isBlank()) {
            return code.trim() + " - " + name.trim();
        }

        // fallback kalau salah satu null
        return code != null && !code.isBlank()
                ? code.trim()
                : name.trim();
    }

}