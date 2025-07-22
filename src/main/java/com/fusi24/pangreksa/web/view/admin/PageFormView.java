package com.fusi24.pangreksa.web.view.admin;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.web.model.entity.FwPages;
import com.fusi24.pangreksa.web.repo.FwPagesRepository;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.data.binder.BeanValidationBinder;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.router.BeforeEvent;
import com.vaadin.flow.router.HasUrlParameter;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Route(value = "page-form-page-access")
@PageTitle("Page Form")
@RolesAllowed("PAGE_FORM")
public class PageFormView extends Main implements HasUrlParameter<String> {

    public static final String VIEW_NAME = "Page Form";

    private final FwPagesRepository pagesRepository;
    private final VerticalLayout content = new VerticalLayout();
    private final TextField urlField = new TextField("Page URL");
    private final TextArea descriptionField = new TextArea("Description");
    private final ComboBox<String> iconComboBox = new ComboBox<>("Icon");
    private final Button saveButton = new Button("Save");
    private final Binder<FwPages> binder = new BeanValidationBinder<>(FwPages.class);
    private FwPages currentPage;
    private boolean hasError = false;

    public PageFormView(FwPagesRepository pagesRepository) {
        this.pagesRepository = pagesRepository;
        
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.MEDIUM);

        add(new ViewToolbar(VIEW_NAME));

        content.setPadding(false);
        
        // Configure form fields and binder
        configureFormFields();
        configureBinder();
        
        // Create form layout
        FormLayout form = new FormLayout();
        form.add(urlField, iconComboBox, descriptionField);
        form.setResponsiveSteps(
            new FormLayout.ResponsiveStep("0", 1),
            new FormLayout.ResponsiveStep("500px", 2)
        );
        
        // Button layout
        Button cancelButton = new Button("Cancel", e -> UI.getCurrent().navigate(PagesView.class));
        
        HorizontalLayout buttonLayout = new HorizontalLayout(cancelButton, saveButton);
        buttonLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        buttonLayout.setWidthFull();
        buttonLayout.setSpacing(true);
        
        // Create and configure Vaadin Card
        Card card = new Card();
        card.add(form);
        card.addToFooter(buttonLayout);
        card.setWidthFull();
        
        // Form will be populated when setParameter is called by Vaadin
        // Only add form if no error occurred
        if (!hasError) {
            content.add(card);
            content.setAlignItems(FlexComponent.Alignment.CENTER);
        }
        
        add(content);
    }
    
    private void configureFormFields() {
        // Configure URL field
        urlField.setWidthFull();
        urlField.setRequired(true);
        
        // Configure description field
        descriptionField.setWidthFull();
        descriptionField.setMaxHeight("150px");

        // Configure icon combo box
        iconComboBox.setWidthFull();
        iconComboBox.setRequired(true);
        
        // Convert VaadinIcon enum to a list of icon names
        List<String> iconNames = Arrays.stream(VaadinIcon.values())
                .map(icon -> "vaadin:" + icon.name().toLowerCase().replace('_', '-'))
                .collect(Collectors.toList());
                
        iconComboBox.setItems(iconNames);
        
        // Set up the renderer to show both icon and name
        iconComboBox.setRenderer(new ComponentRenderer<>(iconName -> {
            String iconVaadinName = iconName.replace("vaadin:", "");
            VaadinIcon icon = VaadinIcon.valueOf(iconVaadinName.toUpperCase().replace('-', '_'));
            HorizontalLayout layout = new HorizontalLayout(icon.create(), new Text(iconName));
            layout.setAlignItems(FlexComponent.Alignment.CENTER);
            layout.setSpacing(true);
            return layout;
        }));
        
        // Configure save button
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> savePage());
    }
    
    @Override
    public void setParameter(BeforeEvent event, String parameter) {
        try {
            if (StringUtils.isEmpty(parameter)) {
                throw new IllegalArgumentException("Page ID is required");
            }
            
            // Edit existing page
            Long pageId = Long.parseLong(parameter);
            currentPage = pagesRepository.findById(pageId)
                    .orElseThrow(() -> new IllegalArgumentException("Page not found"));
            
            // Update form with page data
            binder.readBean(currentPage);
            
        } catch (NumberFormatException e) {
            addError("Invalid page ID format");
        } catch (IllegalArgumentException e) {
            addError(e.getMessage());
        } catch (Exception e) {
            addError("Error loading page: " + e.getMessage());
        }
    }
    
    private void configureBinder() {
        // Configure binder with validation
        binder.forField(urlField)
                .asRequired("URL is required")
                .bind(FwPages::getPageUrl, FwPages::setPageUrl);
                
        binder.forField(descriptionField)
                .bind(FwPages::getDescription, FwPages::setDescription);
                
        binder.forField(iconComboBox)
                .asRequired("Icon is required")
                .bind(FwPages::getPageIcon, FwPages::setPageIcon);
    }
    
    private void savePage() {
        try {
            // Validate and save
            if (binder.writeBeanIfValid(currentPage)) {
                // Save the page and navigate back to list
                pagesRepository.save(currentPage);
                
                // Show success message
                Notification.show("Page saved successfully")
                    .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    
                // Navigate back to list after successful save
                UI.getCurrent().navigate(PagesView.class);
            }
        } catch (Exception e) {
            Notification.show("Error saving page: " + e.getMessage())
                    .addThemeVariants(NotificationVariant.LUMO_ERROR);
        }
    }
        
    private void addError(String message) {
        hasError = true;
        
        // Clear any existing content
        content.removeAll();
        
        // Create error message
        HorizontalLayout error = new HorizontalLayout(new Text(message));
        error.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        error.setSpacing(true);
        error.setPadding(true);
        error.setMargin(true);
        error.addClassNames(
                LumoUtility.Background.ERROR_10,
                LumoUtility.BorderColor.ERROR,
                LumoUtility.Border.ALL,
                LumoUtility.BorderRadius.MEDIUM
        );

        // Add back button
        Button backButton = new Button("Back to List", 
            e -> UI.getCurrent().navigate(PagesView.class));
            
        content.add(error, backButton);
        content.setAlignItems(FlexComponent.Alignment.CENTER);
    }
}

