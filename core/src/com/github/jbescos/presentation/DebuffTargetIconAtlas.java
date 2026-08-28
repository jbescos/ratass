package com.github.jbescos.presentation;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;

/** Maps target-side hostile card effects to cells in the generated icon atlas. */
public final class DebuffTargetIconAtlas {
    public static final int COLUMNS = 3;
    public static final int ROWS = 4;

    private DebuffTargetIconAtlas() {
    }

    public static int indexFor(RogueliteCardId cardId) {
        if (cardId == null) {
            return -1;
        }
        switch (cardId) {
            case DRAFT_VENDETTA:
                return 0;
            case TAR_TETHER:
                return 1;
            case SENSOR_JAMMER:
                return 2;
            case RECOVERY_BEACON:
                return 3;
            case EMP_SNARE:
                return 4;
            case GRID_BLACKOUT:
                return 5;
            case VOID_ANCHOR:
                return 6;
            case TOTAL_BLACKOUT:
                return 7;
            case PAYBACK_SHIELD:
                return 8;
            case TELEMETRY_THEFT:
            case BUILD_HEIST:
            case APEX_PLUNDER:
                return 9;
            case FINAL_RECKONING:
                return 10;
            default:
                return -1;
        }
    }
}
