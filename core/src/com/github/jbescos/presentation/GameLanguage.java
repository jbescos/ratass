package com.github.jbescos.presentation;

import java.util.Locale;

public enum GameLanguage {
    ENGLISH("en", "English"),
    SPANISH("es", "Español"),
    FRENCH("fr", "Français"),
    GERMAN("de", "Deutsch"),
    ITALIAN("it", "Italiano");

    private final String storedValue;
    private final String displayName;

    GameLanguage(String storedValue, String displayName) {
        this.storedValue = storedValue;
        this.displayName = displayName;
    }

    public String getStoredValue() {
        return storedValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public GameLanguage cycle(int direction) {
        if (direction == 0) {
            return this;
        }
        GameLanguage[] languages = values();
        int next = (ordinal() + (direction < 0 ? -1 : 1) + languages.length) % languages.length;
        return languages[next];
    }

    public static GameLanguage fromDeviceLocale() {
        return fromLocale(Locale.getDefault());
    }

    public static GameLanguage fromLocale(Locale locale) {
        if (locale == null) {
            return ENGLISH;
        }
        String languageCode = locale.getLanguage();
        for (GameLanguage language : values()) {
            if (language.storedValue.equalsIgnoreCase(languageCode)) {
                return language;
            }
        }
        return ENGLISH;
    }

    public static GameLanguage fromConfiguredValue(String value) {
        if (value == null
                || value.trim().length() == 0
                || "auto".equalsIgnoreCase(value.trim())) {
            return fromDeviceLocale();
        }
        return fromStoredValue(value);
    }

    public static GameLanguage fromStoredValue(String value) {
        if (value == null) {
            return ENGLISH;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        for (GameLanguage language : values()) {
            if (language.storedValue.equals(normalized)
                    || language.name().toLowerCase(Locale.ROOT).equals(normalized)) {
                return language;
            }
        }
        return ENGLISH;
    }
}
