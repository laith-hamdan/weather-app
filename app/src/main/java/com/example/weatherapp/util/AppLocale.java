package com.example.weatherapp.util;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

import java.util.Locale;

/**
 * Effective UI locale: per-app languages from {@link AppCompatDelegate} when set, otherwise
 * {@link Locale#getDefault()}.
 */
public final class AppLocale {
    private AppLocale() {}

    public static Locale currentLocale() {
        LocaleListCompat list = AppCompatDelegate.getApplicationLocales();
        if (list != null && !list.isEmpty()) {
            Locale l = list.get(0);
            if (l != null) {
                return l;
            }
        }
        return Locale.getDefault();
    }

    public static String currentTag() {
        return currentLocale().toLanguageTag();
    }
}
