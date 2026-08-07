package com.github.jbescos.presentation;

import com.github.jbescos.gameplay.roguelite.RogueliteSlotType;

/** Maps card categories to cells in the shared card-type icon atlas. */
public final class RogueliteCardTypeIconAtlas {
    public static final int COLUMNS = 6;
    public static final int ROWS = 1;
    public static final int WARNING_INDEX = 5;

    private RogueliteCardTypeIconAtlas() {
    }

    public static int indexFor(RogueliteSlotType slotType) {
        if (slotType == null) {
            return -1;
        }
        switch (slotType) {
            case DRIVER:
                return 0;
            case TUNING:
                return 1;
            case TECHNIQUE:
                return 2;
            case POWERUP:
                return 3;
            case REVENGE:
                return 4;
            default:
                return -1;
        }
    }
}
