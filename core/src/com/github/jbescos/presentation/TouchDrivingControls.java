package com.github.jbescos.presentation;

/** Presentation-only visibility gate for touch steering and pedals. */
public final class TouchDrivingControls {
    private TouchDrivingControls() {}

    public static boolean shouldEnable(
            boolean presentationEnabled,
            boolean touchCapablePlatform,
            boolean playing) {
        return presentationEnabled
                && touchCapablePlatform
                && playing;
    }

    public static boolean shouldShowPedals(boolean sandboxMode, boolean manualControl) {
        return sandboxMode && manualControl;
    }
}
