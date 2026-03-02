package com.fusi24.pangreksa.base.ui.notification;

import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;

public class AppNotification {

    private static Notification create(String message, Notification.Position position) {
        Notification notification = new Notification(message, 3000);
        notification.setPosition(position);
        return notification;
    }

    public static void success(String message) {
        Notification notification = create(message, Notification.Position.BOTTOM_END);
        notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        notification.open();
    }

    public static void error(String message) {
        Notification notification = create(message, Notification.Position.MIDDLE);
        notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
        notification.setDuration(4000);
        notification.open();
    }

    public static void warning(String message) {
        Notification notification = create(message, Notification.Position.TOP_CENTER);
        notification.addThemeVariants(NotificationVariant.LUMO_CONTRAST);
        notification.open();
    }

    public static void info(String message) {
        Notification notification = create(message, Notification.Position.BOTTOM_START);
        notification.open();
    }
}