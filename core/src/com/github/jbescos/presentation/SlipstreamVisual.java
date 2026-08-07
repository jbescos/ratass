package com.github.jbescos.presentation;

/** Stateless presentation geometry for a tapered slipstream wake. */
public final class SlipstreamVisual {
    private static final float VISIBLE_THRESHOLD = 0.01f;

    private SlipstreamVisual() {}

    public static float intensity(float boost) {
        if (boost <= VISIBLE_THRESHOLD) {
            return 0f;
        }
        float normalized = clamp((boost - VISIBLE_THRESHOLD) / 0.24f);
        return normalized * normalized * (3f - 2f * normalized);
    }

    public static float phase(float seconds, int carIndex, int streakIndex) {
        return wrap01(seconds * 2.15f + carIndex * 0.173f + streakIndex * 0.271f);
    }

    public static float boundarySpread(float depth) {
        return 0.34f + clamp(depth) * 0.20f;
    }

    public static float streakSpread(float phase) {
        return 0.16f + clamp(phase) * 0.30f;
    }

    public static float streakAlpha(float phase) {
        float clamped = clamp(phase);
        float fadeIn = Math.min(1f, clamped * 7f);
        float fadeOut = 1f - clamped;
        return 0.62f * fadeIn * fadeOut * fadeOut;
    }

    private static float wrap01(float value) {
        return value - (float) Math.floor(value);
    }

    private static float clamp(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
