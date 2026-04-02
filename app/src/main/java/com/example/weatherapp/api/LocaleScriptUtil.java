package com.example.weatherapp.api;

final class LocaleScriptUtil {
    private LocaleScriptUtil() {}

    /**
     * True if the query contains Arabic script (including presentation forms). Open-Meteo
     * geocoding does not match these; we use Nominatim for such queries.
     */
    static boolean containsArabicScript(CharSequence text) {
        if (text == null || text.length() == 0) {
            return false;
        }
        for (int i = 0; i < text.length(); ) {
            int cp = Character.codePointAt(text, i);
            Character.UnicodeBlock block = Character.UnicodeBlock.of(cp);
            if (block == Character.UnicodeBlock.ARABIC
                    || block == Character.UnicodeBlock.ARABIC_SUPPLEMENT
                    || block == Character.UnicodeBlock.ARABIC_EXTENDED_A
                    || block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A
                    || block == Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B) {
                return true;
            }
            i += Character.charCount(cp);
        }
        return false;
    }

    static int codePointCount(CharSequence text) {
        if (text == null) {
            return 0;
        }
        return text.toString().codePointCount(0, text.length());
    }
}
