package com.github.jbescos.gameplay.roguelite;

/** Shared gameplay and strategy-simulation constants for build-leech Revenge cards. */
public final class RivalBuildLeechSpec {
    private RivalBuildLeechSpec() {
    }

    public static boolean isCard(RogueliteCardId cardId) {
        return cardId == RogueliteCardId.TELEMETRY_THEFT
                || cardId == RogueliteCardId.BUILD_HEIST
                || cardId == RogueliteCardId.APEX_PLUNDER;
    }

    public static float durationSeconds(RogueliteCardId cardId) {
        if (cardId == RogueliteCardId.TELEMETRY_THEFT) {
            return 5f;
        }
        if (cardId == RogueliteCardId.BUILD_HEIST) {
            return 10f;
        }
        if (cardId == RogueliteCardId.APEX_PLUNDER) {
            return 15f;
        }
        throw new IllegalArgumentException("Not a build-leech Revenge card: " + cardId);
    }

    public static float expectedLapTransferFraction(RogueliteCardId cardId) {
        return durationSeconds(cardId) / 40f;
    }

    public static float expectedBuildSuppressionFraction(RogueliteCardId cardId) {
        return durationSeconds(cardId) / 40f;
    }
}
