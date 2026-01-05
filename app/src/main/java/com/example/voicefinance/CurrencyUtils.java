package com.example.voicefinance;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Locale;

public class CurrencyUtils {

    public static NumberFormat getCurrencyInstance() {
        DecimalFormatSymbols symbols = new DecimalFormatSymbols(Locale.getDefault());
        symbols.setCurrencySymbol("৳");
        return new DecimalFormat("¤#,##0.00", symbols);
    }
}
