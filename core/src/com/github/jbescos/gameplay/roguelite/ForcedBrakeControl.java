package com.github.jbescos.gameplay.roguelite;

/** Converts a revenge brake into braking-only input and prevents reverse overshoot. */
public final class ForcedBrakeControl {
    private ForcedBrakeControl() {
    }

    public static float fullBrakeThrottle(float signedForwardSpeed, float stopEpsilon) {
        float epsilon = Math.max(0f, stopEpsilon);
        if (signedForwardSpeed > epsilon) {
            return -1f;
        }
        if (signedForwardSpeed < -epsilon) {
            return 1f;
        }
        return 0f;
    }

    public static float reverseSpeedToRemove(float signedForwardSpeed) {
        return Math.max(0f, -signedForwardSpeed);
    }
}
