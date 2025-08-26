package com.fusi24.pangreksa.base.util;


import com.vaadin.flow.component.datepicker.DatePicker;

import java.util.List;

public class DatePickerUtil {

    private DatePickerUtil() {
        // prevent instantiation
    }

    public static DatePicker.DatePickerI18n getIndonesianI18n() {
        DatePicker.DatePickerI18n i18n = new DatePicker.DatePickerI18n();

        i18n.setDateFormat("dd-MMM-yyyy"); // format DD-MMM-YYYY

        // Nama bulan dalam bahasa Indonesia
        i18n.setMonthNames(List.of(
                "Januari", "Februari", "Maret", "April", "Mei", "Juni",
                "Juli", "Agustus", "September", "Oktober", "November", "Desember"
        ));

        // Nama hari dalam bahasa Indonesia
        i18n.setWeekdays(List.of(
                "Minggu", "Senin", "Selasa", "Rabu", "Kamis", "Jumat", "Sabtu"
        ));

        // Singkatan hari dalam bahasa Indonesia
        i18n.setWeekdaysShort(List.of(
                "Min", "Sen", "Sel", "Rab", "Kam", "Jum", "Sab"
        ));

        i18n.setFirstDayOfWeek(1); // Senin sebagai awal minggu

        return i18n;
    }

    public static DatePicker.DatePickerI18n getenglishI18n() {
        DatePicker.DatePickerI18n i18n = new DatePicker.DatePickerI18n();

        i18n.setDateFormat("dd-MMM-yyyy"); // format DD-MMM-YYYY
        i18n.setMonthNames(List.of(new String[]{
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
        }));
        i18n.setWeekdays(List.of(new String[]{
                "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
        }));
        i18n.setWeekdaysShort(List.of(new String[]{
                "Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"
        }));
        i18n.setFirstDayOfWeek(1); // Monday

        return i18n;
    }
}

