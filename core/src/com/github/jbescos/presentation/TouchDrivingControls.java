package com.github.jbescos.presentation;

/** Presentation-only visibility gate for touch steering and pedals. */
public final class TouchDrivingControls {
    private TouchDrivingControls() {}

    public static boolean shouldEnable(
            boolean presentationEnabled,
            boolean touchCapablePlatform,
            boolean playing,
            boolean manualControl) {
        return presentationEnabled
                && touchCapablePlatform
                && playing
                && manualControl;
    }

    public static boolean shouldShowPedals(boolean manualControl) {
        return manualControl;
    }

    public static boolean shouldEnablePowerup(
            boolean presentationEnabled,
            boolean touchCapablePlatform,
            boolean playing,
            boolean manualPowerup) {
        return presentationEnabled
                && touchCapablePlatform
                && playing
                && manualPowerup;
    }
}
