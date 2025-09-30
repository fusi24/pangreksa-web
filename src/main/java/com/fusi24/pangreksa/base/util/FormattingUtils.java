package com.fusi24.pangreksa.base.util;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Locale;

public class FormattingUtils {

    // Define a static DecimalFormat instance for performance and consistency
    // Using Locale.US: thousands comma (,), decimal dot (.) -> 1,234.56
    // Using new Locale("id", "ID"): thousands dot (.), decimal comma (,) -> 1.234,56
    private static final NumberFormat IDR_FORMATTER = NumberFormat.getCurrencyInstance(new Locale("id", "ID"));
    
    // NOTE: If you want standard number formatting (no currency symbol)
    // use NumberFormat.getInstance(new Locale("id", "ID"));
    // and apply a pattern like "#,##0.00"

    static {
        // Customize the standard formatter
        // Use a simple number format pattern instead of currency symbol
        if (IDR_FORMATTER instanceof DecimalFormat) {
            DecimalFormat df = (DecimalFormat) IDR_FORMATTER;
            // Pattern for thousand separator and two decimal places
            df.applyPattern("#,##0.00"); 
        }
    }

    /**
     * Formats a BigDecimal as a String with a thousand separator and two decimal places.
     */
    public static String formatPayrollAmount(BigDecimal amount) {
        if (amount == null) {
            return ""; // Handle null values gracefully
        }
        return IDR_FORMATTER.format(amount);
    }
}