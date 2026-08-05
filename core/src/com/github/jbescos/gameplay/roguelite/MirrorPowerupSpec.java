package com.github.jbescos.gameplay.roguelite;

/** Shared simulation configuration for the three mirror powerup tiers. */
public final class MirrorPowerupSpec {
    public static final float COOLDOWN_SECONDS = 10f;
    public static final float DURATION_SECONDS = 5f;

    private MirrorPowerupSpec() {
    }

    public static boolean isMirrorCard(RogueliteCardId cardId) {
        return cardId == RogueliteCardId.MIRROR_DUO
                || cardId == RogueliteCardId.MIRROR_TRIO
                || cardId == RogueliteCardId.OVERDRIVE_COIL;
    }

    public static int totalVehicleCount(RogueliteCardId cardId) {
        if (cardId == RogueliteCardId.MIRROR_DUO) {
            return 2;
        }
        if (cardId == RogueliteCardId.MIRROR_TRIO) {
            return 3;
        }
        if (cardId == RogueliteCardId.OVERDRIVE_COIL) {
            return 4;
        }
        return 1;
    }

    public static float durationSeconds(RogueliteCardId cardId) {
        if (!isMirrorCard(cardId)) {
            return 0f;
        }
        return DURATION_SECONDS;
    }
}
