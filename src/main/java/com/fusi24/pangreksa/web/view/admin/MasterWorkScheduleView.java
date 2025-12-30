package com.fusi24.pangreksa.web.view.admin;

import com.fusi24.pangreksa.base.ui.component.ViewToolbar;
import com.fusi24.pangreksa.base.util.ConfirmationDialogUtil;
import com.fusi24.pangreksa.web.model.entity.HrOrgStructure;
import com.fusi24.pangreksa.web.model.entity.HrWorkSchedule;
import com.fusi24.pangreksa.web.model.entity.HrWorkScheduleAssignment;
import com.fusi24.pangreksa.web.model.enumerate.WorkScheduleLabel;
import com.fusi24.pangreksa.web.model.enumerate.WorkScheduleType;
import com.fusi24.pangreksa.web.repo.HrOrgStructureRepository;
import com.fusi24.pangreksa.web.repo.HrWorkScheduleRepository;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.combobox.MultiSelectComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.theme.lumo.LumoUtility;
import com.vaadin.flow.component.select.Select;
import jakarta.annotation.security.RolesAllowed;
import org.apache.commons.lang3.BooleanUtils;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Route("master-work-schedule-page-access")
@PageTitle("Master Work Schedule")
@Menu(order = 6, icon = "vaadin:calendar", title = "Master Work Schedule")
@RolesAllowed("USERS_MGT")
public class MasterWorkScheduleView extends Main {

    public static final String VIEW_NAME = "Master Work Schedule";

    private final HrWorkScheduleRepository scheduleRepo;
    private final HrOrgStructureRepository orgStructureRepo;

    private final Grid<HrWorkSchedule> grid = new Grid<>(HrWorkSchedule.class);
    private final TextField searchField = new TextField("Search");
    private final Button addButton = new Button("Add Work Schedule");

    private final Dialog dialog = new Dialog();
    private final FormLayout form = new FormLayout();
    private final Binder<HrWorkSchedule> binder = new Binder<>(HrWorkSchedule.class);

    private final ComboBox<WorkScheduleType> typeField = new ComboBox<>("Schedule Type");
    private final TextField nameField = new TextField("Name");
    private final TimePicker checkInField = new TimePicker("Check-In");
    private final TimePicker checkOutField = new TimePicker("Check-Out");
    private final TimePicker breakStartField = new TimePicker("Break Start");
    private final TimePicker breakEndField = new TimePicker("Break End");
    private final ComboBox<WorkScheduleLabel> labelField = new ComboBox<>("Label");
    private final Checkbox isOvertimeAutoField = new Checkbox("Auto Overtime");
    private final Checkbox isActiveField = new Checkbox("Active");
    private final Select<String> assignmentScopeField = new Select<>();
    private final MultiSelectComboBox<HrOrgStructure> assignedOrgMultiSelect = new MultiSelectComboBox<>("Assigned To");

    private HrWorkSchedule currentSchedule;
    private List<HrOrgStructure> allOrgStructures;

    @Autowired
    public MasterWorkScheduleView(HrWorkScheduleRepository scheduleRepo, HrOrgStructureRepository orgStructureRepo) {
        this.scheduleRepo = scheduleRepo;
        this.orgStructureRepo = orgStructureRepo;

        addClassNames(LumoUtility.BoxSizing.BORDER, LumoUtility.Display.FLEX, LumoUtility.FlexDirection.COLUMN,
                LumoUtility.Padding.MEDIUM, LumoUtility.Gap.SMALL);

        this.allOrgStructures = (List<HrOrgStructure>) orgStructureRepo.findAll();

        configureGrid();
        configureForm();
        addToolbar();
        add(grid, dialog);
        refreshGrid();
    }

    private void configureGrid() {
        grid.removeAllColumns();

        grid.addColumn(HrWorkSchedule::getName).setHeader("Name").setAutoWidth(true);
        grid.addColumn(ws -> ws.getType() != null ? ws.getType().name() : "")
                .setHeader("Type");

        grid.addColumn(ws -> formatTime(ws.getCheckIn()))
                .setHeader("Check-In");

        grid.addColumn(ws -> formatTime(ws.getCheckOut()))
                .setHeader("Check-Out");

        grid.addColumn(ws -> ws.getLabel() != null ? ws.getLabel().name() : "")
                .setHeader("Label");

        grid.addColumn(ws -> BooleanUtils.toString(ws.getIsActive(), "Active", "Inactive"))
                .setHeader("Status");

        grid.addComponentColumn(this::createActionButtons)
                .setHeader("Actions")
                .setAutoWidth(true);
    }

    private HorizontalLayout createActionButtons(HrWorkSchedule ws) {
        Button detail = new Button("Detail");
        detail.addClickListener(e -> openDetailDialog(ws));

        Button edit = new Button("Edit");
        edit.addClickListener(e -> openEditDialog(ws));

        return new HorizontalLayout(detail, edit);
    }

    private void openDetailDialog(HrWorkSchedule ws) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Work Schedule Detail");
        dialog.setWidth("500px");

        FormLayout layout = new FormLayout();

        layout.add(
                createReadOnlyField("Name", ws.getName()),
                createReadOnlyField("Type", ws.getType() != null ? ws.getType().name() : "-"),
                createReadOnlyField("Check-In", formatTime(ws.getCheckIn())),
                createReadOnlyField("Check-Out", formatTime(ws.getCheckOut())),
                createReadOnlyField("Break Start", formatTime(ws.getBreakStart())),
                createReadOnlyField("Break End", formatTime(ws.getBreakEnd())),
                createReadOnlyField("Label", ws.getLabel() != null ? ws.getLabel().name() : "-"),
                createReadOnlyField("Overtime Auto", BooleanUtils.toString(ws.getIsOvertimeAuto(), "Yes", "No")),
                createReadOnlyField("Assignment", resolveAssignment(ws)),
                createReadOnlyField("Status", BooleanUtils.toString(ws.getIsActive(), "Active", "Inactive"))
        );

        Button close = new Button("Close", e -> dialog.close());
        dialog.getFooter().add(close);

        dialog.add(layout);
        dialog.open();
    }

    private TextField createReadOnlyField(String label, String value) {
        TextField field = new TextField(label);
        field.setValue(value != null ? value : "");
        field.setReadOnly(true);
        field.setWidthFull();
        return field;
    }

    private String resolveAssignment(HrWorkSchedule ws) {
        if ("All".equals(ws.getAssignmentScope())) {
            return "All Organization";
        }
        return ws.getAssignments().stream()
                .map(a -> a.getOrgStructure().getName())
                .collect(Collectors.joining(", "));
    }



    private String formatTime(LocalTime time) {
        return time != null ? time.toString() : "";
    }

    private void configureForm() {
        typeField.setItems(WorkScheduleType.values());
        typeField.setItemLabelGenerator(WorkScheduleType::name);
        typeField.setRequired(true);

        nameField.setRequired(true);
        nameField.setMaxLength(100);

        labelField.setItems(WorkScheduleLabel.values());
        labelField.setItemLabelGenerator(WorkScheduleLabel::name);
        labelField.setRequired(true);

        assignmentScopeField.setLabel("Assignment");
        assignmentScopeField.setItems("All", "Selected");
        assignmentScopeField.setRequiredIndicatorVisible(true);
        assignmentScopeField.addValueChangeListener(e -> {
            boolean isAssigned = "Selected".equals(e.getValue());
            assignedOrgMultiSelect.setVisible(isAssigned);
        });

        // Multi-select via ComboBox (Vaadin doesn't have built-in MultiSelectComboBox in v21 without add-on)
        // So we use regular ComboBox + manual list selection simulation
        // For true multi-select, consider Vaadin 23+ or add-on.
        // For now, we'll use single-select as MVP, but note: your requirement says "multiple"
        // → If you have MultiSelectComboBox, replace with that.

        assignedOrgMultiSelect.setItems(allOrgStructures);
        assignedOrgMultiSelect.setItemLabelGenerator(HrOrgStructure::getName);
        assignedOrgMultiSelect.setVisible(false);
//        assignedOrgMultiSelect.setHelperText("Select one or more departments (hold Ctrl/Cmd to multi-select)");
//        assignedOrgMultiSelect.setAllowCustomValue(false);
//        assignedOrgMultiSelect.setAllowNull(true);

        isActiveField.setLabel("Active");

        form.add(
                typeField, nameField,
                checkInField, checkOutField,
                breakStartField, breakEndField,
                labelField, isOvertimeAutoField,
                assignmentScopeField, assignedOrgMultiSelect,
                isActiveField
        );
        form.setResponsiveSteps(
                new FormLayout.ResponsiveStep("0", 1, FormLayout.ResponsiveStep.LabelsPosition.TOP),
                new FormLayout.ResponsiveStep("600px", 2, FormLayout.ResponsiveStep.LabelsPosition.TOP)
        );

        // Binder
        binder.forField(nameField).asRequired("Name is required").bind(HrWorkSchedule::getName, HrWorkSchedule::setName);
        binder.forField(typeField).asRequired("Type is required").bind(HrWorkSchedule::getType, HrWorkSchedule::setType);
        binder.forField(checkInField).asRequired("Check-In is required").bind(HrWorkSchedule::getCheckIn, HrWorkSchedule::setCheckIn);
        binder.forField(checkOutField).asRequired("Check-Out is required").bind(HrWorkSchedule::getCheckOut, HrWorkSchedule::setCheckOut);
        binder.forField(breakStartField).bind(HrWorkSchedule::getBreakStart, HrWorkSchedule::setBreakStart);
        binder.forField(breakEndField).bind(HrWorkSchedule::getBreakEnd, HrWorkSchedule::setBreakEnd);
        binder.forField(labelField).asRequired("Label is required").bind(HrWorkSchedule::getLabel, HrWorkSchedule::setLabel);
        binder.forField(isOvertimeAutoField).bind(HrWorkSchedule::getIsOvertimeAuto, HrWorkSchedule::setIsOvertimeAuto);
        binder.forField(isActiveField).bind(HrWorkSchedule::getIsActive, HrWorkSchedule::setIsActive);
        binder.forField(assignmentScopeField).asRequired("Assignment is required").bind(HrWorkSchedule::getAssignmentScope, HrWorkSchedule::setAssignmentScope);

        // Special handling for assignment
//        binder.withValidator(bean -> {
//            if ("Selected".equals(bean.getAssignmentScope()) && (bean.getAssignments() == null || bean.getAssignments().isEmpty())) {
//                return false;
//            }
//            return true;
//        }, "At least one organization must be selected when assignment is 'Selected'");

        dialog.add(new H3("Work Schedule Details"), form);

        Button saveButton = new Button("Save", e -> saveSchedule());
        Button cancelButton = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(cancelButton, saveButton);
    }

    private void addToolbar() {
        searchField.setPlaceholder("Search by name, type, or label...");
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.EAGER);
        searchField.addValueChangeListener(e -> refreshGrid());

        addButton.addClickListener(e -> openNewDialog());

        HorizontalLayout toolbar = new HorizontalLayout(searchField, addButton);
        toolbar.setWidthFull();
        searchField.setWidth("300px");
        toolbar.setVerticalComponentAlignment(FlexComponent.Alignment.END, addButton);

        add(new ViewToolbar(VIEW_NAME));
        add(toolbar);
    }

    private void refreshGrid() {
        String term = searchField.getValue();
        List<HrWorkSchedule> all = scheduleRepo.findAllWithAssociations();
        if (term == null || term.isEmpty()) {
            grid.setItems(all);
        } else {
            grid.setItems(all.stream()
                    .filter(ws ->
                            (ws.getName() != null && ws.getName().toLowerCase().contains(term.toLowerCase())) ||
                                    (ws.getType() != null && ws.getType().name().toLowerCase().contains(term.toLowerCase())) ||
                                    (ws.getLabel() != null && ws.getLabel().name().toLowerCase().contains(term.toLowerCase()))
                    )
                    .collect(Collectors.toList())
            );
        }
    }

    private void openNewDialog() {
        currentSchedule = new HrWorkSchedule();
        currentSchedule.setIsActive(true);
        currentSchedule.setAssignmentScope("All");
        currentSchedule.setAssignments(new ArrayList<>());
        binder.readBean(currentSchedule);
        assignedOrgMultiSelect.setValue(Set.of());
        dialog.open();
    }

    private void openEditDialog(HrWorkSchedule schedule) {
        currentSchedule = schedule;
        binder.readBean(schedule);

        // For simplicity, we only show first assignment in single-select
        // In real multi-select, you'd load all into a list
        if (!schedule.getAssignments().isEmpty()) {
            assignedOrgMultiSelect.setValue(schedule.getAssignments().stream().map(p -> p.getOrgStructure()).collect(Collectors.toSet()));
        } else {
            assignedOrgMultiSelect.setValue(Set.of());
        }
        dialog.open();
    }

    private void saveSchedule() {
        try {
            binder.writeBean(currentSchedule);

            // Update assignments based on UI
            if ("Selected".equals(currentSchedule.getAssignmentScope())) {
                Set<HrOrgStructure> selected = assignedOrgMultiSelect.getValue();
                if (selected != null && !selected.isEmpty()) {
                    List<HrWorkScheduleAssignment> assignments = selected.stream().map(p -> {
                        HrWorkScheduleAssignment current = currentSchedule.getAssignments().stream().filter(q -> q.getOrgStructure().getId().equals(p.getId())).findFirst().orElse(null);
                        if(current != null) return current;
                        return HrWorkScheduleAssignment.builder()
                                .schedule(currentSchedule)
                                .orgStructure(p)
                                .build();
                    }).collect(Collectors.toList());
                    currentSchedule.getAssignments().clear();
                    currentSchedule.getAssignments().addAll(assignments);
                } else {
                    currentSchedule.setAssignments(new ArrayList<>());
                }
            } else {
                currentSchedule.setAssignments(new ArrayList<>());
            }

            // Validate: if "Selected", must have at least one assignment
            if ("Selected".equals(currentSchedule.getAssignmentScope()) && currentSchedule.getAssignments().isEmpty()) {
                Notification.show("Please select at least one organization structure.");
                return;
            }

            currentSchedule.setEffectiveDate(LocalDate.now());

            // Save
            scheduleRepo.save(currentSchedule);
            dialog.close();
            refreshGrid();
            Notification.show("Work schedule saved successfully!");
        } catch (Exception e) {
            Notification.show("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void deleteSchedule(HrWorkSchedule schedule) {
        String header = "Delete Work Schedule: " + schedule.getName() + "?";
        String message = "This will permanently delete the schedule and its assignments. Are you sure?";
        ConfirmationDialogUtil.showConfirmation(
                header,
                message,
                "Delete",
                event -> {
                    try {
                        scheduleRepo.delete(schedule);
                        refreshGrid();
                        Notification.show("Deleted successfully!");
                    } catch (Exception ex) {
                        Notification.show("Deletion failed: " + ex.getMessage());
                    }
                }
        );
    }
}