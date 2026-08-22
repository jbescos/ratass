package com.github.jbescos.presentation;

/** Presentation-only easing for temporary changes to a car's rendered size. */
public final class CarSizeTransition {
    private static final float RESPONSE_PER_SECOND = 22f;
    private static final float SNAP_EPSILON = 0.001f;

    private CarSizeTransition() {}

    public static float update(float currentScale, float targetScale, float deltaSeconds) {
        float safeCurrent = Math.max(1f, currentScale);
        float safeTarget = Math.max(1f, targetScale);
        float safeDelta = Math.max(0f, deltaSeconds);
        float alpha = 1f - (float) Math.exp(-RESPONSE_PER_SECOND * safeDelta);
        float next = safeCurrent + (safeTarget - safeCurrent) * alpha;
        return Math.abs(next - safeTarget) <= SNAP_EPSILON ? safeTarget : next;
    }
}
