package com.github.jbescos.presentation;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;

/** Rendering-agnostic geometry for the Crown Breaker armed state. */
public final class CrownBreakerStarVisual {
    public static final int POINT_COUNT = 10;

    private CrownBreakerStarVisual() {
    }

    public static boolean isVisible(
            RogueliteCardId equippedRevengeCard,
            RogueliteCardId activeCard,
            boolean revengeArmed) {
        return equippedRevengeCard == RogueliteCardId.CROWN_ENGINE
                && (revengeArmed || activeCard == RogueliteCardId.CROWN_ENGINE);
    }

    public static float radiusScale(float pulse) {
        return 1f + clampPulse(pulse) * 0.06f;
    }

    public static float pointX(int pointIndex, float rotationRadians) {
        return radius(pointIndex)
                * (float)
                        Math.cos(
                                rotationRadians
                                        + pointIndex * Math.PI * 2.0 / POINT_COUNT);
    }

    public static float pointY(int pointIndex, float rotationRadians) {
        return radius(pointIndex)
                * (float)
                        Math.sin(
                                rotationRadians
                                        + pointIndex * Math.PI * 2.0 / POINT_COUNT);
    }

    private static float radius(int pointIndex) {
        return (pointIndex & 1) == 0 ? 1f : 0.46f;
    }

    private static float clampPulse(float pulse) {
        return Math.max(0f, Math.min(1f, pulse));
    }
}
