package com.fusi24.pangreksa.web.model;

import lombok.Setter;

@Setter
public class Authorization {
    public boolean canView = false;
    public boolean canCreate  = false;
    public boolean canEdit  = false;
    public boolean canDelete  = false;

    public Authorization(boolean canView, boolean canCreate, boolean canEdit, boolean canDelete) {
        this.canView = canView;
        this.canCreate = canCreate;
        this.canEdit = canEdit;
        this.canDelete = canDelete;
    }
}
