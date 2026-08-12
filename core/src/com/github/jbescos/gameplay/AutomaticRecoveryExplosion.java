package com.github.jbescos.gameplay;

/** Calculates the radial velocity change needed to separate recovery-blocking cars. */
public final class AutomaticRecoveryExplosion {
    private static final float EDGE_STRENGTH = 0.45f;

    private AutomaticRecoveryExplosion() {
    }

    public static float outwardSpeedChange(
            float currentOutwardSpeed,
            float distance,
            float radius,
            float peakOutwardSpeed) {
        if (!Float.isFinite(distance)
                || !Float.isFinite(radius)
                || !Float.isFinite(peakOutwardSpeed)
                || radius <= 0f
                || distance >= radius
                || peakOutwardSpeed <= 0f) {
            return 0f;
        }
        float normalizedDistance = clamp(Math.max(0f, distance) / radius, 0f, 1f);
        float strength = 1f + (EDGE_STRENGTH - 1f) * normalizedDistance;
        float safeCurrentSpeed =
                Float.isFinite(currentOutwardSpeed) ? currentOutwardSpeed : 0f;
        return Math.max(0f, peakOutwardSpeed * strength - safeCurrentSpeed);
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
