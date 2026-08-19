package com.github.jbescos.gameplay.roguelite;

/** Stable strategy-facing access to card synergy and driver-quality calculations. */
public final class RogueliteStrategyMetrics {
    private RogueliteStrategyMetrics() {
    }

    public static float tuningBaselineScore(RogueliteCardId tuningCardId) {
        return RaceTechniqueEffect.tuningBaselineScore(tuningCardId);
    }

    public static float tuningTechniqueScore(
            RogueliteCardId tuningCardId,
            RogueliteCardId techniqueCardId) {
        return RaceTechniqueEffect.tuningTechniqueScore(tuningCardId, techniqueCardId);
    }

    public static float techniqueEffectMultiplier(RogueliteCardId tuningCardId) {
        if (tuningCardId == null) {
            return 1f;
        }
        switch (tuningCardId) {
            case TECHNIQUE_COUPLER:
                return 1.25f;
            case TECHNIQUE_MATRIX:
                return 1.50f;
            case TECHNIQUE_SINGULARITY:
                return 2f;
            default:
                return 1f;
        }
    }

    public static float powerupEffectMultiplier(RogueliteCardId techniqueCardId) {
        if (techniqueCardId == null) {
            return 1f;
        }
        switch (techniqueCardId) {
            case POWERUP_LINK:
                return 1.25f;
            case POWERUP_MATRIX:
                return 1.50f;
            case POWERUP_NEXUS:
                return 2f;
            default:
                return 1f;
        }
    }

    public static float revengeEffectMultiplier(RogueliteCardId powerupCardId) {
        if (powerupCardId == null) {
            return 1f;
        }
        switch (powerupCardId) {
            case GRUDGE_SPARK:
                return 1.25f;
            case VENGEANCE_CORE:
                return 1.50f;
            case NEMESIS_ENGINE:
                return 2f;
            default:
                return 1f;
        }
    }

    public static float driverQualityGain(
            DriverProfileMetadata current,
            DriverProfileMetadata offered) {
        float offeredLap = offered == null ? 0f : offered.getAverageLapSeconds();
        boolean offeredValid = isValidAverageLap(offeredLap);
        if (current == null) {
            return offeredValid ? 1f / offeredLap : 0f;
        }
        float currentLap = current.getAverageLapSeconds();
        boolean currentValid = isValidAverageLap(currentLap);
        if (currentValid && offeredValid) {
            return currentLap - offeredLap;
        }
        if (offeredValid) {
            return 1f;
        }
        return currentValid ? -1f : 0f;
    }

    private static boolean isValidAverageLap(float averageLap) {
        return averageLap > 0f
                && !Float.isNaN(averageLap)
                && !Float.isInfinite(averageLap);
    }
}
