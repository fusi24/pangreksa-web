package com.fusi24.pangreksa.base.util;

import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.function.SerializableConsumer;

public class ConfirmationDialogUtil {

    /**
     * Shows a confirmation dialog for dangerous, irreversible actions.
     *
     * @param header The title of the dialog (e.g., "Delete Record?").
     * @param text The main message explaining the action (e.g., "Are you sure you want to delete this item?").
     * @param confirmText The label for the confirmation button (e.g., "Delete").
     * @param confirmAction The action to execute if the user confirms.
     */
    public static void showConfirmation(
            String header,
            String text,
            String confirmText,
            SerializableConsumer<ConfirmDialog.ConfirmEvent> confirmAction) {

        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader(header);
        dialog.setText(text);

        // Configure the Cancel button
        dialog.setCancelable(true);
        dialog.setCancelText("Cancel");

        // Configure the Confirm (Action) button
        dialog.setConfirmText(confirmText);
        // Standard style for dangerous actions
        dialog.setConfirmButtonTheme("error primary");

        // Attach the provided action to the confirm listener
        if(confirmAction == null) {
            throw new RuntimeException("Confirm Action is required");
        }
        dialog.addConfirmListener(confirmAction::accept);

        dialog.open();
    }
}