package com.fusi24.pangreksa.web.model;

import com.vaadin.flow.server.menu.MenuEntry;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Setter
@Getter
public class Responsibility {
    private String responsibility;
    private List<MenuEntry> menuEntries;

    public Responsibility(String responsibility) {
        this.responsibility = responsibility;
        this.menuEntries = null;
    }

    public void addMenu(MenuEntry menuEntry) {
        if (this.menuEntries == null) {
            this.menuEntries = new java.util.ArrayList<>();
        }
        this.menuEntries.add(menuEntry);
    }
}
