package com.github.jbescos.presentation;

public final class DriverArtworkAtlas {
    public static final int COLUMNS = 5;
    public static final int ROWS = 2;
    public static final int DRIVER_COUNT = COLUMNS * ROWS;

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
