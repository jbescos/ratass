package com.github.jbescos.presentation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TouchDrivingControlsTest {
    @Test
    public void enablesTouchDrivingOnlyForManualSandboxPlay() {
        assertTrue(TouchDrivingControls.shouldEnable(true, true, true, true, true));

        assertFalse(TouchDrivingControls.shouldEnable(true, true, false, true, true));
        assertFalse(TouchDrivingControls.shouldEnable(true, true, true, true, false));
        assertFalse(TouchDrivingControls.shouldEnable(true, true, true, false, true));
    }

    @Test
    public void remainsDisabledWithoutPresentationOrTouchInput() {
        assertFalse(TouchDrivingControls.shouldEnable(false, true, true, true, true));
        assertFalse(TouchDrivingControls.shouldEnable(true, false, true, true, true));
    }
}
