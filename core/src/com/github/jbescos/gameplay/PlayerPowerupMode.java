package com.github.jbescos.gameplay;

/** Selects whether the player's equipped Powerup triggers itself or waits for input. */
public enum PlayerPowerupMode {
    AUTOMATIC,
    MANUAL;

    public boolean isAutomatic() {
        return this == AUTOMATIC;
    }

    public PlayerPowerupMode toggle() {
        return this == AUTOMATIC ? MANUAL : AUTOMATIC;
    }

    public static PlayerPowerupMode fromStoredValue(String value) {
        if (value != null) {
            for (PlayerPowerupMode mode : values()) {
                if (mode.name().equalsIgnoreCase(value.trim())) {
                    return mode;
                }
            }
        }
        return AUTOMATIC;
    }
}
