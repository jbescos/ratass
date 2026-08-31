package com.github.jbescos.gameplay;

/** Selects whether the player's car uses its equipped driver or direct input. */
public enum PlayerDrivingMode {
    AUTOMATIC,
    MANUAL;

    public PlayerDrivingMode toggle() {
        return this == AUTOMATIC ? MANUAL : AUTOMATIC;
    }

    public boolean isAutomatic() {
        return this == AUTOMATIC;
    }

    public static PlayerDrivingMode fromStoredValue(String value) {
        if (value != null) {
            for (PlayerDrivingMode mode : values()) {
                if (mode.name().equalsIgnoreCase(value.trim())) {
                    return mode;
                }
            }
        }
        return AUTOMATIC;
    }
}
