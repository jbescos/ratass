package com.github.jbescos.presentation;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;

/** Rendering-agnostic state for persistent Crown Breaker-family offender links. */
public final class CrownBreakerLinkVisual {
    private CrownBreakerLinkVisual() {
    }

    public static boolean shouldDraw(
            RogueliteCardId revengeCardId,
            boolean armed,
            boolean sourceAvailable,
            boolean targetAvailable) {
        return (revengeCardId == RogueliteCardId.CROWN_ENGINE
                        || revengeCardId == RogueliteCardId.FINAL_RECKONING)
                && armed
                && sourceAvailable
                && targetAvailable;
    }

    public static float pulse(float elapsedSeconds) {
        if (Float.isNaN(elapsedSeconds) || Float.isInfinite(elapsedSeconds)) {
            return 0f;
        }
        return 0.5f + 0.5f * (float) Math.sin(Math.max(0f, elapsedSeconds) * 6.5f);
    }

    public static float timeoutAlpha(float remainingSeconds) {
        if (Float.isNaN(remainingSeconds) || Float.isInfinite(remainingSeconds)) {
            return 0f;
        }
        return Math.max(0f, Math.min(1f, remainingSeconds / 5f));
    }
}
