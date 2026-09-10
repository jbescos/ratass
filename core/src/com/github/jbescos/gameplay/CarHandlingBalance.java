package com.github.jbescos.gameplay;

/** Keeps braking and steering response usable as card effects raise car performance. */
public final class CarHandlingBalance {
    private CarHandlingBalance() {
    }

    public static float driveTractionMultiplier(float multiplier) {
        // Soften compounded power/lightweight traction, without weakening penalties.
        float traction = sanitizePositive(multiplier);
        return traction > 1f ? (float) Math.sqrt(traction) : traction;
    }

    public static float lateralCorrectionGripMultiplier(float gripMultiplier) {
        // Extra grip raises the tire-force limit, not the velocity-correction gain.
        return Math.min(1f, sanitizePositive(gripMultiplier));
    }

    public static float brakeMultiplier(float maxSpeedMultiplier) {
        // Linear growth keeps braking lookahead from collapsing at high card bonuses.
        return sanitizeAtLeastOne(maxSpeedMultiplier);
    }

    public static float yawRateMultiplier(float maxSpeedMultiplier) {
        return yawRateMultiplier(maxSpeedMultiplier, 1f);
    }

    public static float yawRateMultiplier(float maxSpeedMultiplier, float gripMultiplier) {
        // At the same radius, corner speed and yaw rate grow with sqrt(tire grip).
        return Math.max(sanitizeAtLeastOne(maxSpeedMultiplier),
                (float) Math.sqrt(sanitizeAtLeastOne(gripMultiplier)));
    }

    public static float yawGripMultiplier(float gripMultiplier, float lateralSlip) {
        if (!Float.isFinite(gripMultiplier) || gripMultiplier <= 0f) {
            return 1f;
        }
        if (gripMultiplier <= 1f) {
            return gripMultiplier;
        }
        float plantedGrip = (float) Math.pow(gripMultiplier, 0.75f);
        float slide = Float.isFinite(lateralSlip)
                ? Math.max(0f, Math.min(1f, lateralSlip / 0.20f))
                : 0f;
        return plantedGrip + (gripMultiplier - plantedGrip) * slide;
    }

    public static float steeringTorqueMultiplier(
            float maxSpeedMultiplier,
            float gripMultiplier,
            float referenceInertiaCompensation,
            float currentInertiaCompensation) {
        float speed = sanitizeAtLeastOne(maxSpeedMultiplier);
        float grip = sanitizeAtLeastOne(gripMultiplier);
        float massCorrection = Math.max(
                1f,
                sanitizePositive(referenceInertiaCompensation)
                        / sanitizePositive(currentInertiaCompensation));
        return speed * (float) Math.pow(grip, 0.75f) * massCorrection;
    }

    private static float sanitizeAtLeastOne(float multiplier) {
        return Float.isFinite(multiplier) ? Math.max(1f, multiplier) : 1f;
    }

    private static float sanitizePositive(float multiplier) {
        return Float.isFinite(multiplier) && multiplier > 0f ? multiplier : 1f;
    }
}
