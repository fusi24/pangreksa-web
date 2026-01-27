package com.fusi24.pangreksa.web.view.manager;

import com.fusi24.pangreksa.web.model.entity.FwAppUser;
import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrSalaryBaseLevel;
import com.fusi24.pangreksa.web.service.SalaryBaseLevelService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class SalaryBaseLevelDialog extends Dialog {

    private final SalaryBaseLevelService service;
    private final HrCompany company;
    private final FwAppUser user;
    private final HrSalaryBaseLevel editingVersion;

    private ComboBox<HrCompany> companyCB;
    private TextField levelCodeTF;
    private TextField amountTF;
    private DatePicker startDateDP;
    private DatePicker endDateDP;
    private ComboBox<String> reasonCB;
    private Checkbox activeCB;

    public interface SaveListener {
        void onSave();
    }

    private static final List<String> REASONS = List.of(
            "Annual Review",
            "Market Adjustment",
            "Regulation Change",
            "Correction",
            "Others"
    );

    public SalaryBaseLevelDialog(
            SalaryBaseLevelService service,
            HrCompany company,
            FwAppUser user,
            HrSalaryBaseLevel editingVersion,
            SaveListener listener
    ) {
        this.service = service;
        this.company = company;
        this.user = user;
        this.editingVersion = editingVersion;

        setWidth("460px");
        setCloseOnEsc(true);
        setCloseOnOutsideClick(false);

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        // ===== Company (readonly based on login user) =====
        companyCB = new ComboBox<>("Perusahaan");
        companyCB.setItems(company);
        companyCB.setItemLabelGenerator(c -> c != null ? c.getName() : "");
        companyCB.setValue(company);
        companyCB.setReadOnly(true);
        companyCB.setWidthFull();

        // ===== Level Code =====
        levelCodeTF = new TextField("Kode Level");
        levelCodeTF.setReadOnly(true);
        levelCodeTF.setWidthFull();

        // ===== Amount =====
        amountTF = new TextField("Nominal");
        amountTF.setAllowedCharPattern("[0-9]");
        amountTF.setWidthFull();

        // ===== Dates =====
        startDateDP = new DatePicker("Tanggal Mulai");
        startDateDP.setWidthFull();

        endDateDP = new DatePicker("Tanggal Selesai");
        endDateDP.setWidthFull();

        // ===== Reason =====
        reasonCB = new ComboBox<>("Keterangan");
        reasonCB.setItems(REASONS);
        reasonCB.setWidthFull();

        // ===== Active checkbox =====
        activeCB = new Checkbox("Active");
        activeCB.setValue(true);

        // ===== Default Tanggal Mulai =====
        startDateDP.setValue(LocalDate.now());

        // ===== ADD mode: auto-generate code on Tanggal Mulai change =====
        if (editingVersion == null) {
            startDateDP.addValueChangeListener(ev -> {
                if (ev.getValue() != null) {
                    levelCodeTF.setValue(service.generateLevelCode(ev.getValue(), company));
                }
            });

            // set initial code for ADD
            if (startDateDP.getValue() != null) {
                levelCodeTF.setValue(service.generateLevelCode(startDateDP.getValue(), company));
            }
        }

        // ===== EDIT mode: prefill (edit = create new version) =====
        if (editingVersion != null) {
            levelCodeTF.setValue(editingVersion.getLevelCode() != null
                    ? editingVersion.getLevelCode()
                    : "");

            amountTF.setValue(editingVersion.getBaseSalary() != null
                    ? editingVersion.getBaseSalary().toPlainString()
                    : "");

            reasonCB.setValue(editingVersion.getReason());

            // versi baru default aktif (sesuai requirement Anda)
            activeCB.setValue(true);
        }

        // ===== Buttons =====
        Button cancelBtn = new Button("Batal", e -> close());
        Button saveBtn = new Button("Simpan");

        saveBtn.addClickListener(e -> {
            LocalDate start = startDateDP.getValue();
            LocalDate end = endDateDP.getValue();
            String reason = reasonCB.getValue();

            if (start == null) {
                Notification.show("Tanggal Mulai wajib diisi");
                return;
            }
            if (amountTF.getValue() == null || amountTF.getValue().isBlank()) {
                Notification.show("Nominal wajib diisi");
                return;
            }
            if (reason == null || reason.isBlank()) {
                Notification.show("Keterangan wajib diisi");
                return;
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(amountTF.getValue());
            } catch (Exception ex) {
                Notification.show("Nominaltidak valid");
                return;
            }

            // ===== Level Code rules =====
            // EDIT: pakai kode lama
            // ADD: pakai hasil generate
            String levelCode = (editingVersion != null)
                    ? editingVersion.getLevelCode()
                    : levelCodeTF.getValue();

            if (editingVersion == null && (levelCode == null || levelCode.isBlank())) {
                levelCode = service.generateLevelCode(start, company);
            }

            Boolean isActive = activeCB.getValue();

            // ===== Versioning behavior =====
            if (editingVersion != null) {
                // close version yang diedit
                service.closeOldVersion(editingVersion, start, user);
            } else {
                // close versi aktif sebelumnya (jika ada)
                HrSalaryBaseLevel active = service.getActiveVersion(company);
                if (active != null) {
                    service.closeOldVersion(active, start, user);
                }
            }

            // ===== Create new version =====
            service.createNewVersion(
                    company,
                    amount,
                    start,
                    end,
                    levelCode,
                    reason,
                    isActive,
                    user
            );

            listener.onSave();
            close();
        });

        HorizontalLayout actions = new HorizontalLayout(cancelBtn, saveBtn);

        // ===== Vertical layout order =====
        layout.add(
                companyCB,
                levelCodeTF,
                amountTF,
                startDateDP,
                endDateDP,
                reasonCB,
                activeCB,
                actions
        );

        add(layout);
    }
}
