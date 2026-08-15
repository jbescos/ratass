package com.github.jbescos.gameplay;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/** Combines an automatic driving action with explicit per-axis player input. */
public final class HybridPlayerControl {
    private HybridPlayerControl() {}

    public static float digitalAxis(boolean positivePressed, boolean negativePressed) {
        if (positivePressed == negativePressed) {
            return 0f;
        }
        return positivePressed ? 1f : -1f;
    }

    public static float overrideAxis(
            float automaticValue,
            float playerValue,
            boolean playerAxisActive,
            boolean takeoverAllowed) {
        float selected = takeoverAllowed && playerAxisActive ? playerValue : automaticValue;
        return MathUtils.clamp(selected, -1f, 1f);
    }

    public static boolean isSteeringTakeoverModeEnabled(
            boolean sandboxMode,
            boolean automaticSandboxControl) {
        return !sandboxMode || automaticSandboxControl;
    }

    public static boolean isSteeringTakeoverAllowed(
            boolean controlAvailable,
            ArenaMap arenaMap,
            Vector2 carPosition) {
        return controlAvailable
                && (arenaMap == null
                        || (arenaMap.supports(carPosition)
                                && !arenaMap.isManualSteeringRestricted(carPosition)));
    }
}
