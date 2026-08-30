package com.github.jbescos.presentation;

public final class DriverArtworkAtlas {
    public static final String THEMED_RELATIVE_PATH = "drivers/driver_art_atlas.png";
    public static final int COLUMNS = 5;
    public static final int ROWS = 3;
    public static final int DRIVER_COUNT = 11;

    private static final String PROFILE_PREFIX = "profile";

    private DriverArtworkAtlas() {
    }

    public static int indexForProfile(String profileId) {
        if (profileId == null
                || profileId.length() != PROFILE_PREFIX.length() + 2
                || !profileId.startsWith(PROFILE_PREFIX)) {
            return -1;
        }

        char tens = profileId.charAt(PROFILE_PREFIX.length());
        char units = profileId.charAt(PROFILE_PREFIX.length() + 1);
        if (!Character.isDigit(tens) || !Character.isDigit(units)) {
            return -1;
        }
        int index = (tens - '0') * 10 + units - '0';
        return index < DRIVER_COUNT ? index : -1;
    }
}
