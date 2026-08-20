package com.github.jbescos.gameplay.roguelite;

/** Shared simulation configuration for the three mirror powerup tiers. */
public final class MirrorPowerupSpec {
    public static final float COOLDOWN_SECONDS = 10f;
    public static final float DURATION_SECONDS = 5f;
    public static final int MAX_MIRROR_COPIES = 12;

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

    public static int amplifiedTotalVehicleCount(
            RogueliteCardId cardId,
            float powerupEffectMultiplier) {
        int baseCopies = totalVehicleCount(cardId) - 1;
        if (baseCopies <= 0) {
            return 1;
        }
        float safeMultiplier = Float.isFinite(powerupEffectMultiplier)
                ? Math.max(1f, powerupEffectMultiplier)
                : 1f;
        int copies = Math.round(baseCopies * safeMultiplier);
        return 1 + Math.min(MAX_MIRROR_COPIES, Math.max(1, copies));
    }

    public static float durationSeconds(RogueliteCardId cardId) {
        if (!isMirrorCard(cardId)) {
            return 0f;
        }
        return DURATION_SECONDS;
    }
}
