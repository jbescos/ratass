package com.github.jbescos.presentation;

/** Presentation-only visibility gate for touch steering and pedals. */
public final class TouchDrivingControls {
    private TouchDrivingControls() {}

    public static boolean shouldEnable(
            boolean presentationEnabled,
            boolean touchCapablePlatform,
            boolean sandboxMode,
            boolean playing,
            boolean manualControl) {
        return presentationEnabled
                && touchCapablePlatform
                && sandboxMode
                && playing
                && manualControl;
    }
}
