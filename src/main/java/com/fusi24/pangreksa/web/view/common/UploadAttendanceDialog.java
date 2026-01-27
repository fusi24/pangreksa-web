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
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
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

                showImportResultNotification(result);

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

    private void showImportResultNotification(AttendanceImportService.ImportResult result) {
        boolean hasErrors = result != null && result.getErrors() != null && !result.getErrors().isEmpty();

        Notification n = new Notification();
        n.setPosition(Notification.Position.TOP_END);
        n.setDuration(9000);

        // Theme: hijau kalau benar-benar clean, kuning kalau ada warning/errors
        if (!hasErrors) {
            n.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        } else {
            n.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
            n.addThemeVariants(NotificationVariant.LUMO_WARNING);
        }

        int total = result == null ? 0 : result.totalProcessed();

        H4 title = new H4(!hasErrors ? "Upload absensi berhasil" : "Upload selesai dengan peringatan");

        Div summary = new Div();
        summary.getStyle().set("line-height", "1.4");

        summary.add(new Span("Total diproses: " + total));
        summary.add(new Div(new Span("Inserted: " + safe(result.getInserted())
                + " | Updated: " + safe(result.getUpdated()))));
        summary.add(new Div(new Span("Skipped (No User): " + safe(result.getSkippedNoUser())
                + " | Skipped (No Schedule): " + safe(result.getSkippedNoSchedule())
                + " | Invalid Row: " + safe(result.getSkippedInvalidRow()))));

        Div errorsBox = new Div();
        errorsBox.getStyle().set("margin-top", "8px");

        if (hasErrors) {
            errorsBox.add(new Span("Detail peringatan (max 5):"));
            int max = Math.min(5, result.getErrors().size());
            for (int i = 0; i < max; i++) {
                errorsBox.add(new Div(new Span("• " + result.getErrors().get(i))));
            }
            if (result.getErrors().size() > max) {
                errorsBox.add(new Div(new Span("...dan " + (result.getErrors().size() - max) + " lainnya.")));
            }
        }

        VerticalLayout content = new VerticalLayout(title, summary);
        content.setPadding(false);
        content.setSpacing(false);

        if (hasErrors) {
            content.add(errorsBox);
        }

        n.add(content);
        n.open();
    }

    private int safe(Integer v) {
        return v == null ? 0 : v;
    }

}