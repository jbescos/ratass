package com.github.jbescos.presentation;

/** Stable row mapping for the checkbox-state icon atlas. */
public final class CheckboxIconAtlas {
    public static final int COLUMNS = 1;
    public static final int ROWS = 4;

    private static final int UNCHECKED = 0;
    private static final int CHECKED = 1;
    private static final int DISABLED_UNCHECKED = 2;
    private static final int DISABLED_CHECKED = 3;

    private CheckboxIconAtlas() {
    }

    public static int indexFor(boolean checked, boolean enabled) {
        if (enabled) {
            return checked ? CHECKED : UNCHECKED;
        }
        return checked ? DISABLED_CHECKED : DISABLED_UNCHECKED;
    }
}
