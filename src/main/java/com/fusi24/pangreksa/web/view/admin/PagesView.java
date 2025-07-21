package com.fusi24.pangreksa.web.view.admin;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.web.model.entity.FwPages;
import com.fusi24.pangreksa.web.view.template.RefactorPageFormView;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;

import com.fusi24.pangreksa.web.repo.FwPagesRepository;
import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.lang3.StringUtils;

import java.util.List;

@Route("pages-page-access")
@PageTitle("Pages")
@Menu(order = 8, icon = "vaadin:clipboard-check", title = "Pages")
@RolesAllowed("PAGES")
public class PagesView extends Main {

    public static final String VIEW_NAME = "Pages";
    private final Grid<FwPages> grid = new Grid<>(FwPages.class, false);
    private final FwPagesRepository pagesRepository;
    private ListDataProvider<FwPages> dataProvider;
    private final PageFilter pageFilter;


    public PagesView(FwPagesRepository pagesRepository) {
        this.pagesRepository = pagesRepository;
        this.pageFilter = new PageFilter(this::onFilterChanged);
        
        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        add(new ViewToolbar(VIEW_NAME));
        
        setSizeFull();
        
        add(pageFilter);
        configureGrid();
        loadData();
    }
    
    private void onFilterChanged() {
        fetchData(
            pageFilter.getUrlFilterValue(),
            pageFilter.getDescriptionFilterValue()
        );
    }

    private void configureGrid() {
        grid.addColumn(FwPages::getPageUrl)
                .setHeader("URL")
                .setSortable(true)
                .setAutoWidth(true);
                
        grid.addColumn(FwPages::getDescription)
                .setHeader("Description")
                .setSortable(true)
                .setAutoWidth(true);
                
        // Add Action column with edit button
        grid.addComponentColumn(page -> {
            Button editButton = new Button(new Icon(VaadinIcon.EDIT));
            editButton.addThemeVariants(ButtonVariant.LUMO_ICON);
            editButton.getElement().setAttribute("aria-label", "Edit");
            editButton.setTooltipText("Edit page");
            editButton.addClickListener(e -> {
                getUI().ifPresent(ui -> {
                    ui.navigate(RefactorPageFormView.class, String.valueOf(page.getId()));
                });
            });
            return editButton;
        }).setHeader("Actions").setAutoWidth(true);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.getStyle().set("flex-grow", "1");
        
        add(grid);
    }

    private void loadData() {
        // Initial load with no filters
        fetchData(null, null);
    }
    
    private void fetchData(String urlFilter, String descriptionFilter) {
        // Fetch filtered data from the database
        List<FwPages> filteredPages = pagesRepository.findByUrlContainingAndDescriptionContainingIgnoreCase(
            urlFilter,
            descriptionFilter
        );
        
        // Update the data provider with the filtered results
        dataProvider = new ListDataProvider<>(filteredPages);
        grid.setItems(dataProvider);
        
        // If no pages found, show a notification
        if (filteredPages.isEmpty()) {
            Notification.show("No pages found");
        }
    }

    public class PageFilter extends VerticalLayout {
        private final TextField urlFilter;
        private final TextField descriptionFilter;
        private final Runnable onFilterChange;
        
        public PageFilter(Runnable onFilterChange) {
            this.onFilterChange = onFilterChange;
            setWidthFull();
            setPadding(false);
            setMargin(false);
            
            HorizontalLayout filterLayout = new HorizontalLayout();
            filterLayout.setWidthFull();
            filterLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
            
            Button filterButton = new Button("Filter", new Icon(VaadinIcon.SLIDERS));
            filterButton.addClickListener(e -> showFilterDialog());
            
            filterLayout.add(filterButton);
            add(filterLayout);
            
            // Initialize filters with default values
            this.urlFilter = new TextField();
            this.descriptionFilter = new TextField();
        }
        
        private void showFilterDialog() {
            Dialog dialog = new Dialog();
            dialog.setHeaderTitle("Filter Pages");
            
            VerticalLayout dialogLayout = new VerticalLayout();
            dialogLayout.setPadding(false);
            dialogLayout.setSpacing(true);
            dialogLayout.setWidth("400px");
            
            urlFilter.setLabel("URL");
            urlFilter.setPlaceholder("Contains...");
            urlFilter.setClearButtonVisible(true);
            urlFilter.setWidthFull();
            
            descriptionFilter.setLabel("Description");
            descriptionFilter.setPlaceholder("Contains...");
            descriptionFilter.setClearButtonVisible(true);
            descriptionFilter.setWidthFull();
            
            dialogLayout.add(urlFilter, descriptionFilter);
            
            // Create and add buttons to footer
            HorizontalLayout buttonsLayout = new HorizontalLayout();
            buttonsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
            buttonsLayout.setWidthFull();
            buttonsLayout.setSpacing(true);
            
            Button applyButton = new Button("Apply", e -> {
                onFilterChange.run();
                dialog.close();
            });
            applyButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
            
            Button cancelButton = new Button("Cancel", e -> dialog.close());
            
            buttonsLayout.add(cancelButton, applyButton);
            dialog.getFooter().add(buttonsLayout);
            
            dialog.add(dialogLayout);
            dialog.open();
        }
        
        public String getUrlFilterValue() {
            return StringUtils.trimToNull(urlFilter.getValue());
        }
        
        public String getDescriptionFilterValue() {
            return StringUtils.trimToNull(descriptionFilter.getValue());
        }
    }
}

