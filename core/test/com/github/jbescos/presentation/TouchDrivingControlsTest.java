package com.github.jbescos.presentation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TouchDrivingControlsTest {
    @Test
    public void enablesTouchDrivingDuringPlay() {
        assertTrue(TouchDrivingControls.shouldEnable(true, true, true));
        assertFalse(TouchDrivingControls.shouldEnable(true, true, false));
    }

    @Test
    public void remainsDisabledWithoutPresentationOrTouchInput() {
        assertFalse(TouchDrivingControls.shouldEnable(false, true, true));
        assertFalse(TouchDrivingControls.shouldEnable(true, false, true));
    }

    @Test
    public void pedalsRemainExclusiveToManualSandbox() {
        assertTrue(TouchDrivingControls.shouldShowPedals(true, true));
        assertFalse(TouchDrivingControls.shouldShowPedals(false, true));
        assertFalse(TouchDrivingControls.shouldShowPedals(true, false));
    }
}
