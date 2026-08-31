package com.github.jbescos.presentation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TouchDrivingControlsTest {
    @Test
    public void enablesTouchDrivingOnlyDuringManualPlay() {
        assertTrue(TouchDrivingControls.shouldEnable(true, true, true, true));
        assertFalse(TouchDrivingControls.shouldEnable(true, true, true, false));
        assertFalse(TouchDrivingControls.shouldEnable(true, true, false, true));
    }

    @Test
    public void remainsDisabledWithoutPresentationOrTouchInput() {
        assertFalse(TouchDrivingControls.shouldEnable(false, true, true, true));
        assertFalse(TouchDrivingControls.shouldEnable(true, false, true, true));
    }

    @Test
    public void pedalsFollowManualModeOnEveryGameType() {
        assertTrue(TouchDrivingControls.shouldShowPedals(true));
        assertFalse(TouchDrivingControls.shouldShowPedals(false));
    }

    @Test
    public void manualPowerupHasIndependentTouchControl() {
        assertTrue(TouchDrivingControls.shouldEnablePowerup(true, true, true, true));
        assertFalse(TouchDrivingControls.shouldEnablePowerup(true, true, true, false));
        assertFalse(TouchDrivingControls.shouldEnablePowerup(true, false, true, true));
        assertFalse(TouchDrivingControls.shouldEnablePowerup(true, true, false, true));
    }
}
