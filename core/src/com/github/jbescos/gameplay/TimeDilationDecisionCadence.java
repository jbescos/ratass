package com.github.jbescos.gameplay;

/** Adjusts live policy cadence without changing physics or rendering frequency. */
public final class TimeDilationDecisionCadence {
    private TimeDilationDecisionCadence() {
    }

    public static float intervalSeconds(
            float normalIntervalSeconds,
            boolean accelerated,
            float accelerationFactor) {
        float normal = sanitizeInterval(normalIntervalSeconds);
        if (!accelerated) {
            return normal;
        }
        return normal / Math.max(1f, sanitizeFactor(accelerationFactor));
    }

    public static float transitionTimer(
            float currentTimer,
            float normalIntervalSeconds,
            boolean wasAccelerated,
            boolean accelerated,
            float accelerationFactor) {
        float normal = sanitizeInterval(normalIntervalSeconds);
        float current = Math.max(0f, sanitizeTimer(currentTimer));
        if (wasAccelerated == accelerated) {
            return current;
        }
        float interval = intervalSeconds(normal, accelerated, accelerationFactor);
        return Math.min(current, interval);
    }

    private static float sanitizeInterval(float intervalSeconds) {
        if (intervalSeconds <= 0f
                || Float.isNaN(intervalSeconds)
                || Float.isInfinite(intervalSeconds)) {
            return 0f;
        }
        return intervalSeconds;
    }

    private static float sanitizeFactor(float factor) {
        if (Float.isNaN(factor) || Float.isInfinite(factor)) {
            return 1f;
        }
        return factor;
    }

    private static float sanitizeTimer(float timer) {
        if (Float.isNaN(timer) || Float.isInfinite(timer)) {
            return 0f;
        }
        return timer;
    }
}
