package com.github.jbescos.presentation;

/** Rendering-agnostic layout for the armed Impact Reversal image. */
public final class ImpactReversalReadyVisual {
    private ImpactReversalReadyVisual() {
    }

    public static float sizeScale(float pulse) {
        return 1.72f + clampPulse(pulse) * 0.10f;
    }

    public static float alpha(float pulse) {
        return 0.72f + clampPulse(pulse) * 0.25f;
    }

    private static float clampPulse(float pulse) {
        return Math.max(0f, Math.min(1f, pulse));
    }
}
