package com.fusi24.pangreksa.web.model.enumerate;

public enum CalendarTypeEnum {

    NATIONAL_HOLIDAY("Libur Nasional"),
    EXTRA_NATIONAL_HOLIDAY("Cuti Bersama"),
    COMPANY_HOLIDAY("Libur Perusahaan");

    private final String label;

    CalendarTypeEnum(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
