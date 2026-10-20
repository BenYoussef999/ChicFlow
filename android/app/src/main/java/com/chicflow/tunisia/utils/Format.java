package com.chicflow.tunisia.utils;

import java.util.Locale;

/** Small formatting helpers. */
public final class Format {

    private Format() { }

    /** 89.9 -> "89.90 TND" */
    public static String tnd(double amount) {
        return String.format(Locale.US, "%.2f TND", amount);
    }
}
