package com.github.jbescos.gameplay.roguelite;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public enum RogueliteSlotType {
    DRIVER("Driver"),
    TUNING("Tuning"),
    TECHNIQUE("Technique"),
    GADGET("Gadget");

    private static final List<RogueliteSlotType> MODIFICATION_SLOTS =
            Collections.unmodifiableList(
                    Arrays.asList(TUNING, TECHNIQUE, GADGET));

    private final String displayName;

    RogueliteSlotType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean isDriver() {
        return this == DRIVER;
    }

    public static List<RogueliteSlotType> modificationSlots() {
        return MODIFICATION_SLOTS;
    }
}
