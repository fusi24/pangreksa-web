package com.fusi24.pangreksa.web.model;

import com.vaadin.flow.server.menu.MenuEntry;
import lombok.Getter;
import lombok.Setter;

import java.util.*;

@Setter
@Getter
public class Responsibility {
    private String responsibility;
    private List<MenuEntry> menuEntries;

    private Map<String, List<MenuEntry>> groupMenuEntries;

    public Responsibility(String responsibility) {
        this.responsibility = responsibility;
        this.menuEntries = null;
    }

    public void addMenu(MenuEntry menuEntry) {
        if (this.menuEntries == null) {
            this.menuEntries = new ArrayList<>();
        }
        this.menuEntries.add(menuEntry);
    }

    public void addGroupMenu(String groupName, MenuEntry menuEntry) {
        if (this.groupMenuEntries == null) {
            this.groupMenuEntries = new LinkedHashMap<>();
        }
        this.groupMenuEntries.computeIfAbsent(groupName, k -> new ArrayList<>()).add(menuEntry);
    }
}
