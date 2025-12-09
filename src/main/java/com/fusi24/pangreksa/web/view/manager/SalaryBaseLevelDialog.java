package com.fusi24.pangreksa.web.view.manager;

import com.fusi24.pangreksa.web.model.entity.FwAppUser;
import com.fusi24.pangreksa.web.model.entity.HrCompany;
import com.fusi24.pangreksa.web.model.entity.HrSalaryBaseLevel;
import com.fusi24.pangreksa.web.service.SalaryBaseLevelService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;

import java.math.BigDecimal;
import java.time.LocalDate;

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

    public interface SaveListener {
        void onSave();
    }

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

        VerticalLayout layout = new VerticalLayout();
        layout.setPadding(false);
        layout.setSpacing(true);

        companyCB = new ComboBox<>("Company");
        companyCB.setItems(company);
        companyCB.setItemLabelGenerator(HrCompany::getName);
        companyCB.setValue(company);
        companyCB.setReadOnly(true);
        companyCB.setWidthFull();


        levelCodeTF = new TextField("Level Code");
        levelCodeTF.setReadOnly(true);
        levelCodeTF.setWidthFull();

        amountTF = new TextField("Amount");
        amountTF.setAllowedCharPattern("[0-9]");
        amountTF.setWidthFull();

        startDateDP = new DatePicker("Start Date");
        startDateDP.setWidthFull();

        endDateDP = new DatePicker("End Date");
        endDateDP.setWidthFull();

        reasonCB = new ComboBox<>("Reason");
        reasonCB.setItems(
                "Annual Review",
                "Market Adjustment",
                "Regulation Change",
                "Correction",
                "Others"
        );
        reasonCB.setWidthFull();

        // Default start date
        startDateDP.setValue(LocalDate.now());

        // Generate code when start date changes
        startDateDP.addValueChangeListener(ev -> {
            if (ev.getValue() != null) {
                levelCodeTF.setValue(service.generateLevelCode(ev.getValue(), company));
            }
        });

        // Prefill for EDIT (edit = create new version)
        if (editingVersion != null) {
            amountTF.setValue(editingVersion.getBaseSalary() != null
                    ? editingVersion.getBaseSalary().toPlainString()
                    : "");
            reasonCB.setValue(editingVersion.getReason());
            // gunakan startDate default "hari ini" agar versi baru
        }

        // initial code
        if (startDateDP.getValue() != null) {
            levelCodeTF.setValue(service.generateLevelCode(startDateDP.getValue(), company));
        }

        Button cancelBtn = new Button("Cancel", e -> close());
        Button saveBtn = new Button("Save");

        saveBtn.addClickListener(e -> {
            LocalDate start = startDateDP.getValue();
            LocalDate end = endDateDP.getValue();
            String reason = reasonCB.getValue();

            if (start == null) {
                Notification.show("Start Date wajib diisi");
                return;
            }
            if (amountTF.getValue() == null || amountTF.getValue().isBlank()) {
                Notification.show("Amount wajib diisi");
                return;
            }
            if (reason == null || reason.isBlank()) {
                Notification.show("Reason wajib diisi");
                return;
            }

            BigDecimal amount;
            try {
                amount = new BigDecimal(amountTF.getValue());
            } catch (Exception ex) {
                Notification.show("Amount tidak valid");
                return;
            }

            String levelCode = levelCodeTF.getValue();
            if (levelCode == null || levelCode.isBlank()) {
                levelCode = service.generateLevelCode(start, company);
            }

            // close old version jika EDIT
            if (editingVersion != null) {
                service.closeOldVersion(editingVersion, start, user);
            } else {
                // jika ADD, tutup versi aktif sebelumnya (jika ada)
                HrSalaryBaseLevel active = service.getActiveVersion(company);
                if (active != null) {
                    service.closeOldVersion(active, start, user);
                }
            }

            // create new version
            service.createNewVersion(
                    company,
                    amount,
                    start,
                    end,
                    levelCode,
                    reason,
                    user
            );

            listener.onSave();
            close();
        });

        HorizontalLayout actions = new HorizontalLayout(cancelBtn, saveBtn);

        layout.add(
                companyCB,
                levelCodeTF,
                amountTF,
                startDateDP,
                endDateDP,
                reasonCB,
                actions
        );

        add(layout);
    }
}
