package com.github.jbescos.presentation;

/** Rendering-agnostic dimensions for the armed Impact Reversal shield. */
public final class ImpactReversalShieldVisual {
    private ImpactReversalShieldVisual() {
    }

    public static float lateralOffsetScale(float pulse) {
        return 0.66f + clampPulse(pulse) * 0.045f;
    }

    public static float longitudinalOffsetScale(float pulse) {
        return 0.68f + clampPulse(pulse) * 0.035f;
    }

    public static float arrowTipScale(float pulse) {
        return 0.88f + clampPulse(pulse) * 0.08f;
    }

    public static float alpha(float pulse) {
        return 0.46f + clampPulse(pulse) * 0.32f;
    }

    private static float clampPulse(float pulse) {
        return Math.max(0f, Math.min(1f, pulse));
    }
}
