package com.fusi24.pangreksa.web.view.employee;

import com.fusi24.pangreksa.base.ui.notification.AppNotification;
import com.fusi24.pangreksa.security.CurrentUser;
import com.pangreksa.service.model.entity.FwAppUser;
import com.pangreksa.service.model.entity.HrContract;
import com.pangreksa.service.model.entity.HrPerson;
import com.pangreksa.service.model.enumerate.ContractTypeEnum;
import com.pangreksa.service.model.repo.FwAppUserRepository;
import com.pangreksa.service.model.repo.HrPersonRepository;
import com.pangreksa.service.model.repo.FwSystemRepository;
import com.pangreksa.service.service.ContractService;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.component.upload.receivers.MemoryBuffer;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public class ContractFormDialog extends Dialog {

    private final ContractService contractService;
    private final CurrentUser currentUser;
    private final HrPersonRepository personRepository;
    private final FwAppUserRepository appUserRepository;
    private final FwSystemRepository systemRepository;

    private final Runnable onSave;

    // =====================================================
    // FIELD
    // =====================================================

    private final ComboBox<HrPerson> employeeField =
            new ComboBox<>("Karyawan");

    private final ComboBox<ContractTypeEnum> contractTypeField =
            new ComboBox<>("Tipe Kontrak");

    private final DatePicker startDateField =
            new DatePicker("Tanggal Mulai");

    private final DatePicker endDateField =
            new DatePicker("Tanggal Berakhir");

    private final ComboBox<HrPerson> approverField =
            new ComboBox<>("Manager Approval");

    private final TextArea notesField =
            new TextArea("Catatan");

    private final MemoryBuffer uploadBuffer =
            new MemoryBuffer();

    private final Upload upload =
            new Upload(uploadBuffer);

    private String uploadedFilePath;

    // =====================================================
    // CONSTRUCTOR
    // =====================================================

    public ContractFormDialog(
            ContractService contractService,
            CurrentUser currentUser,
            HrPersonRepository personRepository,
            FwAppUserRepository appUserRepository,
            FwSystemRepository systemRepository,
            Runnable onSave
    ) {

        this.contractService = contractService;
        this.currentUser = currentUser;
        this.personRepository = personRepository;
        this.appUserRepository = appUserRepository;
        this.systemRepository = systemRepository;
        this.onSave = onSave;

        setWidth("600px");

        setHeaderTitle("Form Kontrak");

        configureForm();
    }

    // =====================================================
    // FORM
    // =====================================================

    private void configureForm() {

        VerticalLayout layout =
                new VerticalLayout();

        // =================================================
        // EMPLOYEE
        // =================================================

        employeeField.setItems(
                personRepository.findAll()
        );

        employeeField.setItemLabelGenerator(person ->
                person.getFirstName() + " " +
                        person.getLastName()
        );

        employeeField.setWidthFull();

        // =================================================
        // CONTRACT TYPE
        // =================================================

        contractTypeField.setItems(
                ContractTypeEnum.values()
        );

        contractTypeField.setWidthFull();

        contractTypeField.addValueChangeListener(e -> {

            if (e.getValue() == ContractTypeEnum.PKWTT) {

                endDateField.clear();

                endDateField.setEnabled(false);

            } else {

                endDateField.setEnabled(true);
            }
        });

        // =================================================
        // DATE
        // =================================================

        startDateField.setWidthFull();

        endDateField.setWidthFull();

        // =================================================
        // APPROVER
        // =================================================

        List<HrPerson> approvers =
                appUserRepository.findAll()
                        .stream()
                        .map(FwAppUser::getPerson)
                        .toList();

        approverField.setItems(approvers);

        approverField.setItemLabelGenerator(person ->
                person.getFirstName() + " " +
                        person.getLastName()
        );

        approverField.setWidthFull();

        // =================================================
        // NOTES
        // =================================================

        notesField.setWidthFull();

        notesField.setHeight("120px");

        // =================================================
        // UPLOAD
        // =================================================

        upload.setAcceptedFileTypes(
                ".pdf",
                ".doc",
                ".docx"
        );

        upload.setMaxFiles(1);

        upload.addSucceededListener(event -> {

            try {

                String contractPath =
                        systemRepository
                                .findByKey("CONTRACT_FILE_PATH")
                                .stream()
                                .findFirst()
                                .orElseThrow()
                                .getStringVal();

                File folder =
                        new File(contractPath);

                if (!folder.exists()) {
                    folder.mkdirs();
                }

                String originalFileName =
                        event.getFileName();

                String extension =
                        originalFileName.substring(
                                originalFileName.lastIndexOf(".")
                        );

                String generatedFileName =
                        UUID.randomUUID() + extension;

                File targetFile =
                        new File(folder, generatedFileName);

                try (
                        InputStream inputStream =
                                uploadBuffer.getInputStream();

                        FileOutputStream outputStream =
                                new FileOutputStream(targetFile)
                ) {

                    inputStream.transferTo(outputStream);
                }

                uploadedFilePath =
                        targetFile.getAbsolutePath();

                AppNotification.success(
                        "File berhasil upload"
                );

            } catch (Exception ex) {

                AppNotification.error(
                        ex.getMessage()
                );
            }
        });

        // =================================================
        // BUTTON
        // =================================================

        Button saveDraftButton =
                new Button("Save Draft");

        saveDraftButton.addThemeVariants(
                ButtonVariant.LUMO_PRIMARY
        );

        saveDraftButton.addClickListener(e ->
                save(false)
        );

        Button submitButton =
                new Button("Submit Approval");

        submitButton.addThemeVariants(
                ButtonVariant.LUMO_SUCCESS
        );

        submitButton.addClickListener(e ->
                save(true)
        );

        Button cancelButton =
                new Button("Batal");

        cancelButton.addClickListener(e -> close());

        Div buttonLayout =
                new Div(
                        saveDraftButton,
                        submitButton,
                        cancelButton
                );

        layout.add(
                employeeField,
                contractTypeField,
                startDateField,
                endDateField,
                approverField,
                notesField,
                upload,
                buttonLayout
        );

        add(layout);
    }

    // =====================================================
    // SAVE
    // =====================================================

    private void save(boolean submitApproval) {

        try {

            if (employeeField.getValue() == null) {
                AppNotification.error(
                        "Karyawan wajib dipilih"
                );
                return;
            }

            if (contractTypeField.getValue() == null) {
                AppNotification.error(
                        "Tipe kontrak wajib dipilih"
                );
                return;
            }

            if (startDateField.getValue() == null) {
                AppNotification.error(
                        "Tanggal mulai wajib diisi"
                );
                return;
            }

            HrContract contract =
                    new HrContract();

            contract.setPerson(
                    employeeField.getValue()
            );

            contract.setContractType(
                    contractTypeField.getValue()
            );

            contract.setStartDate(
                    startDateField.getValue()
            );

            contract.setEndDate(
                    endDateField.getValue()
            );

            contract.setSubmittedTo(
                    approverField.getValue()
            );

            contract.setNotes(
                    notesField.getValue()
            );

            contract.setAttachmentPath(
                    uploadedFilePath
            );

            contract.setCreatedAt(
                    LocalDateTime.now()
            );

            contract.setUpdatedAt(
                    LocalDateTime.now()
            );

            contract.setCreatedBy(
                    getCurrentAppUser()
            );

            HrContract saved =
                    contractService.createDraft(
                            contract,
                            getCurrentAppUser()
                    );

            if (submitApproval) {

                contractService.submitApproval(
                        saved,
                        getCurrentAppUser()
                );
            }

            AppNotification.success(
                    "Kontrak berhasil disimpan"
            );

            close();

            if (onSave != null) {
                onSave.run();
            }

        } catch (Exception ex) {

            AppNotification.error(
                    ex.getMessage()
            );
        }
    }

    private FwAppUser getCurrentAppUser() {

        String username =
                currentUser.require()
                        .getUserId()
                        .toString();

        return appUserRepository
                .findByUsername(username)
                .orElseThrow();
    }
}