package com.github.jbescos.presentation;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;

/** Presentation-only rules for the Position Hijack charge link. */
public final class PositionHijackLinkVisual {
    private PositionHijackLinkVisual() {
    }

    public static boolean shouldDraw(
            RogueliteCardId revengeCardId,
            boolean revengeArmed,
            boolean sourceAvailable,
            boolean targetAvailable) {
        return revengeCardId == RogueliteCardId.RECOVERY_BEACON
                && revengeArmed
                && sourceAvailable
                && targetAvailable;
    }

    public static float charge(float readiness) {
        if (!Float.isFinite(readiness)) {
            return 0f;
        }
        return Math.max(0f, Math.min(1f, readiness));
    }

    public static float pulse(float effectSeconds, float readiness) {
        float safeTime = Float.isFinite(effectSeconds) ? effectSeconds : 0f;
        float charge = charge(readiness);
        float wave = 0.5f + 0.5f * (float) Math.sin(safeTime * (7f + charge * 8f));
        return Math.max(0f, Math.min(1f, 0.25f + charge * 0.55f + wave * 0.20f));
    }
}
