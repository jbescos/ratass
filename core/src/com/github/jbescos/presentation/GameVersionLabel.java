package com.github.jbescos.presentation;

public final class GameVersionLabel {
    private static final String DEFAULT_VERSION_NAME = "1.0";

    private GameVersionLabel() {
    }

    public static String format(String versionName) {
        String normalizedName = versionName == null ? "" : versionName.trim();
        if (normalizedName.length() == 0) {
            normalizedName = DEFAULT_VERSION_NAME;
        }
        return "v" + normalizedName;
    }
}
