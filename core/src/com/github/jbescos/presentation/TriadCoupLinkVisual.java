package com.github.jbescos.presentation;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;

/** Presentation-only rules for the Triad Coup charge links. */
public final class TriadCoupLinkVisual {
    private TriadCoupLinkVisual() {
    }

    public static boolean shouldDraw(
            RogueliteCardId revengeCardId,
            boolean revengeArmed,
            boolean sourceAvailable,
            boolean offenderAvailable) {
        return revengeCardId == RogueliteCardId.TRIAD_COUP
                && revengeArmed
                && sourceAvailable
                && offenderAvailable;
    }

    public static boolean hasTriangle(
            int sourceVehicleId,
            int offenderVehicleId,
            int secondaryVehicleId,
            boolean secondaryAvailable) {
        return secondaryAvailable
                && secondaryVehicleId >= 0
                && secondaryVehicleId != sourceVehicleId
                && secondaryVehicleId != offenderVehicleId;
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
        float wave = 0.5f + 0.5f * (float) Math.sin(safeTime * (8f + charge * 10f));
        return Math.max(0f, Math.min(1f, 0.20f + charge * 0.58f + wave * 0.22f));
    }
}
