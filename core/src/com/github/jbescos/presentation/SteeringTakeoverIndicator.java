package com.github.jbescos.presentation;

/** Presentation-only visibility state for accepted player steering takeover. */
public final class SteeringTakeoverIndicator {
    public static final int LEFT = 1;
    public static final int RIGHT = 2;

    private SteeringTakeoverIndicator() {}

    public static int visibleDirections(
            boolean presentationEnabled,
            boolean steeringTakeoverEnabled,
            boolean leftPressed,
            boolean rightPressed) {
        if (!presentationEnabled || !steeringTakeoverEnabled) {
            return 0;
        }
        int directions = 0;
        if (leftPressed) {
            directions |= LEFT;
        }
        if (rightPressed) {
            directions |= RIGHT;
        }
        return directions;
    }

    public static boolean shouldClaimCamera(
            boolean presentationEnabled,
            boolean steeringTakeoverEnabled,
            boolean takeoverApplied) {
        return presentationEnabled && steeringTakeoverEnabled && takeoverApplied;
    }

    public static boolean isIgnored(int visibleDirections, boolean takeoverApplied) {
        return visibleDirections != 0 && !takeoverApplied;
    }
}
