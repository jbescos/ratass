package com.github.jbescos.presentation;

import java.util.Locale;

/** User-selectable world-camera behavior. */
public enum CameraViewMode {
    TOP_DOWN("top_down", "Top Down"),
    CHASE("chase", "Chase"),
    WHOLE_MAP("whole_map", "Whole Map");

    private final String storedValue;
    private final String displayName;

    CameraViewMode(String storedValue, String displayName) {
        this.storedValue = storedValue;
        this.displayName = displayName;
    }

    public String getStoredValue() {
        return storedValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean followsBehind() {
        return this == CHASE;
    }

    public boolean showsWholeMap() {
        return this == WHOLE_MAP;
    }

    public CameraViewMode cycle(int direction) {
        if (direction == 0) {
            return this;
        }
        CameraViewMode[] modes = values();
        int next = ordinal() + (direction < 0 ? -1 : 1);
        if (next < 0) {
            next = modes.length - 1;
        } else if (next >= modes.length) {
            next = 0;
        }
        return modes[next];
    }

    public static CameraViewMode fromStoredValue(String value, boolean legacyFollowBehind) {
        if (value != null) {
            String normalized =
                    value.trim()
                            .toLowerCase(Locale.ROOT)
                            .replace('-', '_')
                            .replace(' ', '_');
            for (CameraViewMode mode : values()) {
                if (mode.storedValue.equals(normalized)) {
                    return mode;
                }
            }
        }
        return legacyFollowBehind ? CHASE : TOP_DOWN;
    }
}
