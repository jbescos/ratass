package com.github.jbescos.gameplay;

/** Allocation-free geometry checks used by the algorithmic passing assistance. */
public final class PassingAssistGeometry {
    private PassingAssistGeometry() {}

    public static boolean isAlongside(
            float forwardDistance,
            float lateralDistance,
            float maximumAheadDistance,
            float maximumBehindDistance,
            float selfHalfWidth,
            float targetHalfWidth,
            float sideGap,
            float lateralSeparationRatio) {
        if (forwardDistance > Math.max(0f, maximumAheadDistance)
                || forwardDistance < -Math.max(0f, maximumBehindDistance)) {
            return false;
        }
        float desiredSeparation =
                Math.max(0f, selfHalfWidth)
                        + Math.max(0f, targetHalfWidth)
                        + Math.max(0f, sideGap);
        return Math.abs(lateralDistance)
                >= desiredSeparation * Math.max(0f, lateralSeparationRatio);
    }
}
