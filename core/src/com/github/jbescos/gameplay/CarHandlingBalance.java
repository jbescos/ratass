package com.github.jbescos.gameplay;

/** Keeps braking and steering response usable as card effects raise car performance. */
public final class CarHandlingBalance {
    private CarHandlingBalance() {
    }

    public static float brakeMultiplier(float maxSpeedMultiplier) {
        float speed = sanitizeAtLeastOne(maxSpeedMultiplier);
        return speed * speed;
    }

    public static float yawRateMultiplier(float maxSpeedMultiplier) {
        return sanitizeAtLeastOne(maxSpeedMultiplier);
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
