package com.github.jbescos.presentation;

/** Presentation-only shaping for the normal high-throttle exhaust flame. */
public final class ThrottleExhaustVisual {
    private static final float START_THROTTLE = 0.78f;
    private static final float MAX_VISIBLE_BRAKE = 0.12f;

    private ThrottleExhaustVisual() {}

    public static float intensity(float throttle, float brake) {
        if (brake > MAX_VISIBLE_BRAKE || throttle < START_THROTTLE) {
            return 0f;
        }
        return clamp((throttle - START_THROTTLE) / (1f - START_THROTTLE));
    }

    public static float flameLengthScale(float intensity, float flicker) {
        return 0.10f + clamp(intensity) * 0.15f + clamp(flicker) * 0.05f;
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
