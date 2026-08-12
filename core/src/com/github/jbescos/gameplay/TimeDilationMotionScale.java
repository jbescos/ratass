package com.github.jbescos.gameplay;

/** Scales per-car motion while the shared physics world keeps its normal step. */
public final class TimeDilationMotionScale {
    private TimeDilationMotionScale() {
    }

    public static float scale(boolean active, float factor) {
        return active ? sanitizeScale(factor) : 1f;
    }

    public static float transitionRatio(float previousScale, float nextScale) {
        return sanitizeScale(nextScale) / sanitizeScale(previousScale);
    }

    public static float forceScale(float motionScale) {
        float scale = sanitizeScale(motionScale);
        return scale * scale;
    }

    private static float sanitizeScale(float scale) {
        if (scale < 1f || Float.isNaN(scale) || Float.isInfinite(scale)) {
            return 1f;
        }
        return scale;
    }
}
