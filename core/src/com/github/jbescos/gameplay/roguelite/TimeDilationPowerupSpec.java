package com.github.jbescos.gameplay.roguelite;

/** Shared balance values for the time-dilation Powerup family. */
public final class TimeDilationPowerupSpec {
    public static final float DURATION_SECONDS = 2f;
    public static final float OWN_TIME_SCALE = 2f;

    private TimeDilationPowerupSpec() {
    }

    public static boolean isTimeDilationCard(RogueliteCardId cardId) {
        return cardId == RogueliteCardId.TIME_RIPPLE
                || cardId == RogueliteCardId.CHRONO_SHIFT
                || cardId == RogueliteCardId.TEMPORAL_DOMINION;
    }

    public static float durationSeconds(RogueliteCardId cardId) {
        return isTimeDilationCard(cardId) ? DURATION_SECONDS : 0f;
    }

    public static float cooldownSeconds(RogueliteCardId cardId) {
        if (cardId == RogueliteCardId.TIME_RIPPLE) {
            return 60f;
        }
        if (cardId == RogueliteCardId.CHRONO_SHIFT) {
            return 40f;
        }
        if (cardId == RogueliteCardId.TEMPORAL_DOMINION) {
            return 30f;
        }
        return 0f;
    }
}
