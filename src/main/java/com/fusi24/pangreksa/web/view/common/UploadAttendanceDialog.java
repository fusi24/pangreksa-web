package com.fusi24.pangreksa.web.view.common;

import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.entity.HrCompanyBranch;
import com.fusi24.pangreksa.web.service.AttendanceImportService;
import com.fusi24.pangreksa.web.service.HrCompanyBranchService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.server.streams.UploadHandler;

import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.FileUploadCallback;
import com.vaadin.flow.server.streams.FileFactory;
import com.vaadin.flow.server.streams.FileUploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import com.vaadin.flow.server.streams.UploadHandler;
import com.vaadin.flow.server.streams.FileUploadCallback;
import com.vaadin.flow.server.streams.FileFactory;
import com.vaadin.flow.server.streams.FileUploadHandler;
import com.vaadin.flow.server.streams.UploadMetadata;
import java.util.Objects;

public class UploadAttendanceDialog extends Dialog {

    private final AttendanceImportService importService;
    private final HrCompanyBranchService branchService;
    private final CurrentUser currentUser;
    private final Runnable onSuccess;

    private final TextArea notesField = new TextArea("Catatan");
    private final ComboBox<HrCompanyBranch> branchField = new ComboBox<>("Branch");

    private Upload upload; // dibuat di configureContent()

    private Path uploadedPath;          // ✅ file temp hasil upload
    private String uploadedFileName;    // ✅ nama file asli dari metadata

    private final Button submitButton = new Button("Submit");
    private final Button cancelButton = new Button("Cancel");

    public UploadAttendanceDialog(
            AttendanceImportService importService,
            HrCompanyBranchService branchService,
            CurrentUser currentUser,
            Runnable onSuccess
    ) {
        this.importService = Objects.requireNonNull(importService);
        this.branchService = Objects.requireNonNull(branchService);
        this.currentUser = Objects.requireNonNull(currentUser);
        this.onSuccess = onSuccess;

        setHeaderTitle("Upload Absensi");
        setModal(true);
        setWidth("720px");

        configureContent();
        configureActions();
    }

    private void configureContent() {
        notesField.setWidthFull();
        notesField.setMaxLength(255);

        branchField.setWidthFull();
        branchField.setItems(branchService.findAll());
        branchField.setItemLabelGenerator(b ->
                String.format("%s - %s", nvl(b.getBranchCode()), nvl(b.getBranchName()))
        );
        branchField.setRequired(true);

        FileUploadCallback onSuccessCb = (UploadMetadata metadata, File file) -> {
            uploadedFileName = metadata.fileName();
            uploadedPath = file.toPath();

            // optional: auto-enable submit kalau sudah ada file
            submitButton.setEnabled(true);

            UI ui = UI.getCurrent();
            if (ui != null) {
                ui.access(() -> Notification.show("File siap: " + uploadedFileName, 2500, Notification.Position.MIDDLE));
            }
        };

        FileFactory fileFactory = (UploadMetadata metadata) -> {
            // simpan ke temp folder (atau bisa kamu arahkan ke folder khusus)
            try {
                String safeName = (metadata.fileName() == null ? "upload" : metadata.fileName())
                        .replaceAll("[^a-zA-Z0-9._-]", "_");
                Path tmp = Files.createTempFile("attendance-", "-" + safeName);
                return tmp.toFile();
            } catch (Exception e) {
                throw new RuntimeException("Gagal menyiapkan file upload: " + e.getMessage(), e);
            }
        };

        FileUploadHandler handler = UploadHandler.toFile(onSuccessCb, fileFactory);
        upload = new Upload(handler);

        upload.setWidthFull();
        upload.setDropAllowed(true);
        upload.setMaxFiles(1);
        upload.setAcceptedFileTypes(".csv", ".xls", ".xlsx");

        upload.addFileRejectedListener(e ->
                Notification.show("File ditolak: " + e.getErrorMessage(), 4000, Notification.Position.MIDDLE)
        );

        submitButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        // default submit disabled sampai file ada
        submitButton.setEnabled(false);

        HorizontalLayout buttons = new HorizontalLayout(submitButton, cancelButton);

        add(new VerticalLayout(
                new Span("Upload file absensi (CSV/XLS/XLSX)"),
                upload,
                branchField,
                notesField,
                buttons
        ));
    }

    private void configureActions() {
        cancelButton.addClickListener(e -> close());

        submitButton.addClickListener(e -> {
            if (uploadedPath == null || uploadedFileName == null) {
                Notification.show("Silakan upload file terlebih dahulu", 3000, Notification.Position.MIDDLE);
                return;
            }
            if (branchField.getValue() == null) {
                Notification.show("Silakan pilih branch", 3000, Notification.Position.MIDDLE);
                return;
            }

            try {
                var result = importService.importAttendance(
                        uploadedPath,
                        uploadedFileName,
                        notesField.getValue(),
                        branchField.getValue().getId(),
                        currentUser.require()
                );

                Notification.show(
                        "Import selesai. Inserted=" + result.getInserted()
                                + ", Updated=" + result.getUpdated()
                                + ", Skipped(no user)=" + result.getSkippedNoUser()
                                + ", Skipped(no schedule)=" + result.getSkippedNoSchedule()
                                + ", Invalid=" + result.getSkippedInvalidRow(),
                        7000,
                        Notification.Position.MIDDLE
                );

                close();
                if (onSuccess != null) onSuccess.run();

            } catch (Exception ex) {
                Notification.show("Import gagal: " + ex.getMessage(), 7000, Notification.Position.MIDDLE);
                ex.printStackTrace();
            } finally {
                // optional: hapus file temp setelah proses
                try {
                    if (uploadedPath != null) Files.deleteIfExists(uploadedPath);
                } catch (Exception ignored) {}
                uploadedPath = null;
                uploadedFileName = null;
                submitButton.setEnabled(false);
            }
        });
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}