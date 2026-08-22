package com.github.jbescos.gameplay;

import com.badlogic.gdx.math.MathUtils;

/** Action-level shaping used only by the headless recovery training objective. */
public final class RecoveryRewardShaping {
    private RecoveryRewardShaping() {
    }

    public static float launchSignal(
            float throttle,
            float turn,
            float targetAlignment) {
        float clampedThrottle = MathUtils.clamp(throttle, -1f, 1f);
        float clampedTurn = MathUtils.clamp(turn, -1f, 1f);
        float clampedAlignment = MathUtils.clamp(targetAlignment, -1f, 1f);
        if (clampedAlignment >= 0f) {
            return clampedThrottle * clampedAlignment;
        }

        // When facing away from the target, moving straight in either direction
        // reinforces the deadlock. Reward motion only when it is paired with a turn.
        return Math.abs(clampedThrottle)
                * Math.abs(clampedTurn)
                * -clampedAlignment;
    }
}
