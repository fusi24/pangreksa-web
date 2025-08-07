package com.fusi24.pangreksa.web.view.demo;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;

import com.vaadin.flow.component.orderedlayout.FlexComponent.JustifyContentMode;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.security.CurrentUser;
import com.fusi24.pangreksa.web.model.Authorization;
import com.fusi24.pangreksa.web.service.CommonService;
import com.fusi24.pangreksa.web.service.SystemService;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.streams.*;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.UUID;

@Route("upload-demo-page-access")
@PageTitle("Upload Demo")
@Menu(order = 35, icon = "vaadin:clipboard-check", title = "Upload Demo")
@RolesAllowed("DEMOUPL")
//@PermitAll // When security is enabled, allow all authenticated users
public class UploadDemoView extends Main {
    private static final long serialVersionUID = 35L;
    private static final Logger log = LoggerFactory.getLogger(UploadDemoView.class);
    private final SystemService systemService;
    private final CurrentUser currentUser;
    private final CommonService commonService;
    private Authorization auth;

    public static final String VIEW_NAME = "Upload Demo";

    private VerticalLayout body;
    private String DATA_PATH;

    private Image image;
    private VerticalLayout avatarLayout;

    public UploadDemoView(CurrentUser currentUser, CommonService commonService, SystemService systemService) {
        this.currentUser = currentUser;
        this.commonService = commonService;
        this.systemService = systemService;

        this.auth = commonService.getAuthorization(
                currentUser.require(),
                (String) UI.getCurrent().getSession().getAttribute("responsibility"),
                this.serialVersionUID);

        log.debug("Page {}, Authorization: {} {} {} {}", VIEW_NAME, auth.canView, auth.canCreate, auth.canEdit, auth.canDelete);

        this.DATA_PATH = systemService.getStringDataPath();
        checkDataPath();

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

        TextField textField = new TextField("Data Path");
        textField.setValue(DATA_PATH);
        textField.setWidth("500px");

        VerticalLayout uploadLayout = createUploadLayout();
        HorizontalLayout uploadLayout2 = createAvatarUploadLayout();

        body.add(textField, uploadLayout, uploadLayout2, new Button("Save"));
        body.setSpacing(true);

        add(body);
    }

    private VerticalLayout createUploadLayout() {
        VerticalLayout uploadLayout = new VerticalLayout();
        uploadLayout.setWidth("500px");
        uploadLayout.setHeight("300px");
        uploadLayout.getStyle().set("border", "1px dashed #888");

//        InMemoryUploadHandler inMemoryHandler = UploadHandler.inMemory(
//                (metadata, data) -> {
//                    // Get other information about the file.
//                    String fileName = metadata.fileName();
//                    String mimeType = metadata.contentType();
//                    long contentLength = metadata.contentLength();
//
//                    log.debug("got filename: {}, mimeType: {}, contentLength: {}", fileName, mimeType, contentLength);
//
//                    // Do something with the file data...
//                    // processFile(data, fileName);
//                });

        // FileFactory expects a single String argument (fileName)
        FileFactory fileFactory = fileName -> {
            //grab extension from fileName.fileName()
            String extension = fileName.fileName().substring(fileName.fileName().lastIndexOf(".") + 1);
            String newFileName = UUID.randomUUID() + "." + extension; // Generate a unique file name
            log.debug("File extension: {} new Filename: {}", extension, newFileName);

            return new File(DATA_PATH + File.separator + newFileName);
        };

        FileUploadHandler fileHandler = UploadHandler.toFile( (e,f) -> {
            // Get other information about the file.
            String fileName = e.fileName();
            String mimeType = e.contentType();
            long contentLength = e.contentLength();

            log.debug("got filename: {}, mimeType: {}, contentLength: {}", fileName, mimeType, contentLength);
        }, fileFactory);

        Upload upload = new Upload(fileHandler);

//        Upload upload = new Upload(inMemoryHandler);
        upload.setWidthFull();

        uploadLayout.add(upload);
        return uploadLayout;
    }

    private HorizontalLayout createAvatarUploadLayout() {
        HorizontalLayout uploadLayout = new HorizontalLayout();
        uploadLayout.setWidth("500px");
//        uploadLayout.setHeight("300px");
        uploadLayout.getStyle().set("border", "1px dashed #888");

        this.image = new Image();
        image.setVisible(false);

        InMemoryUploadHandler inMemoryHandler = UploadHandler.inMemory(
                (metadata, data) -> {
                    // Get other information about the file.
                    String fileName = metadata.fileName();
                    String mimeType = metadata.contentType();
                    long contentLength = metadata.contentLength();

                    log.debug("got filename: {}, mimeType: {}, contentLength: {}", fileName, mimeType, contentLength);

                    image.removeAll();

                    // Create image from byte array
                    StreamResource imageResource = new StreamResource(
                            UUID.randomUUID() + ".png", // Use PNG or your real format
                            () -> new ByteArrayInputStream(data)
                    );

                    // Update image source with the uploaded file data
                    UI ui = UI.getCurrent();
                    ui.access(() -> {
                        image.removeAll();
                        image = new Image(imageResource, fileName);
                        image.setWidth("150px");
                        image.setHeight("150px");
                        image.getStyle().set("border-radius", "10%"); // Makes it perfectly round
                        image.getStyle().set("object-fit", "cover"); // Ensures it fills the circle properly
                        image.getStyle().set("box-shadow", "0 4px 8px rgba(0, 0, 0, 0.2)");

                        this.avatarLayout.removeAll();
                        avatarLayout.add(image);
                    });

                    // Do something with the file data...
                    // processFile(data, fileName);
                });

        avatarLayout = new VerticalLayout();
        avatarLayout.setWidth("150px");
        avatarLayout.setHeight("150px");
        avatarLayout.setMargin(true);
        //aligment center
        avatarLayout.setJustifyContentMode(JustifyContentMode.CENTER);

        Upload upload = new Upload(inMemoryHandler);
        upload.setWidthFull();

        uploadLayout.add(avatarLayout, upload);
        return uploadLayout;
    }

    private void setListener() {
        // Add listeners for UI components here
        // For example, you can add a click listener to a button
        // saveButton.addClickListener(event -> savePage());
    }

    private void checkDataPath(){
        // Check data path on physical storage DATA_PATH
        // If not exists, then create the folder recursively
        File dataDir = new File(DATA_PATH);
        if (!dataDir.exists()) {
            boolean created = dataDir.mkdirs();
            if (created) {
                log.info("Data directory created: {}", DATA_PATH);
            } else {
                log.error("Failed to create data directory: {}", DATA_PATH);
            }
        } else {
            log.info("Data directory already exists: {}", DATA_PATH);
        }
    }
}
