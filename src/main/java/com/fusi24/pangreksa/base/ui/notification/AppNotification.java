package com.fusi24.pangreksa.base.ui.notification;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

public class AppNotification {

    private static Notification create(String message) {
        Notification notification = new Notification(message, 3000);
        notification.setPosition(Notification.Position.MIDDLE);
        return notification;
    }

    public static void success(String message) {
        Notification notification = create(message);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        notification.open();
    }

    public static void error(String message) {
        Notification notification = create(message);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.setDuration(4000);
        notification.open();
    }

    public static void warning(String message) {
        Notification notification = create(message);
        notification.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
        notification.open();
    }

    public static void info(String message) {
        Notification notification = create(message);
        notification.open();
    }
}