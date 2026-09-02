package com.github.jbescos.presentation;

/** Normalizes the short player label used throughout the game UI. */
public final class PlayerDisplayName {
    public static final String DEFAULT = "YOU";
    public static final int MAX_LENGTH = 9;

    private PlayerDisplayName() {
    }

    public static String editableValue(String value) {
        String sanitized = sanitize(value);
        return DEFAULT.equals(sanitized) ? "" : sanitized;
    }

    public static String sanitize(String value) {
        if (value == null) {
            return DEFAULT;
        }
        String trimmed = value.trim();
        StringBuilder result = new StringBuilder(MAX_LENGTH);
        for (int i = 0; i < trimmed.length() && result.length() < MAX_LENGTH; i++) {
            char character = trimmed.charAt(i);
            if (character >= 32 && character <= 126) {
                result.append(character);
            }
        }
        return result.length() == 0 ? DEFAULT : result.toString();
    }

    public static String raceLabel(String name, int level, boolean spanish) {
        String safeName = name == null ? "" : name;
        return safeName + raceLevelSuffix(level, spanish);
    }

    public static String raceLevelSuffix(int level, boolean spanish) {
        return (spanish ? " (Nv " : " (Lv ") + Math.max(0, level) + ")";
    }
}
