package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class SteeringTakeoverIndicatorTest {
    @Test
    public void showsPressedHybridSteeringDirections() {
        assertEquals(
                SteeringTakeoverIndicator.LEFT,
                SteeringTakeoverIndicator.visibleDirections(
                        true, true, true, false));
        assertEquals(
                SteeringTakeoverIndicator.RIGHT,
                SteeringTakeoverIndicator.visibleDirections(
                        true, true, false, true));
        assertEquals(
                SteeringTakeoverIndicator.LEFT | SteeringTakeoverIndicator.RIGHT,
                SteeringTakeoverIndicator.visibleDirections(
                        true, true, true, true));
    }

    @Test
    public void keepsRejectedInputVisibleButHidesTrainingAndManualInput() {
        assertEquals(
                SteeringTakeoverIndicator.LEFT,
                SteeringTakeoverIndicator.visibleDirections(
                        true, true, true, false));
        assertEquals(
                0,
                SteeringTakeoverIndicator.visibleDirections(
                        true, false, true, false));
        assertEquals(
                0,
                SteeringTakeoverIndicator.visibleDirections(
                        false, true, true, false));
    }

    @Test
    public void claimsCameraOnlyForAcceptedHybridTakeover() {
        assertTrue(SteeringTakeoverIndicator.shouldClaimCamera(true, true, true));
        assertFalse(SteeringTakeoverIndicator.shouldClaimCamera(true, true, false));
        assertFalse(SteeringTakeoverIndicator.shouldClaimCamera(true, false, true));
        assertFalse(SteeringTakeoverIndicator.shouldClaimCamera(false, true, true));
    }

    @Test
    public void marksVisibleRejectedInputAsIgnored() {
        assertTrue(
                SteeringTakeoverIndicator.isIgnored(
                        SteeringTakeoverIndicator.LEFT,
                        false));
        assertFalse(
                SteeringTakeoverIndicator.isIgnored(
                        SteeringTakeoverIndicator.RIGHT,
                        true));
        assertFalse(SteeringTakeoverIndicator.isIgnored(0, false));
    }
}
