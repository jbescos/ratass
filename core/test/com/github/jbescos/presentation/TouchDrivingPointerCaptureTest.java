package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TouchDrivingPointerCaptureTest {
    @Test
    public void steeringRemainsCapturedOutsideControlUntilRelease() {
        TouchDrivingPointerCapture capture = new TouchDrivingPointerCapture(2);

        capture.beginFrame();
        capture.updatePointer(0, true, true, false, false, true, 0.4f);
        assertTrue(capture.isSteering());
        assertTrue(capture.isCaptured(0));
        assertEquals(0.4f, capture.getTurn(), 0.0001f);

        capture.beginFrame();
        capture.updatePointer(0, true, false, true, false, true, 0.8f);
        assertTrue(capture.isSteering());
        assertFalse(capture.isThrottling());
        assertEquals(0.8f, capture.getTurn(), 0.0001f);

        capture.beginFrame();
        capture.updatePointer(0, false, false, false, false, true, 0f);
        assertFalse(capture.isSteering());
        assertFalse(capture.isCaptured(0));
    }

    @Test
    public void pedalRemainsCapturedWhileAnotherPointerSteers() {
        TouchDrivingPointerCapture capture = new TouchDrivingPointerCapture(2);

        capture.beginFrame();
        capture.updatePointer(0, true, false, true, false, true, 0f);
        capture.updatePointer(1, true, true, false, false, true, -0.5f);
        assertTrue(capture.isThrottling());
        assertTrue(capture.isSteering());

        capture.beginFrame();
        capture.updatePointer(0, true, false, false, false, true, 0f);
        capture.updatePointer(1, true, false, false, false, true, -0.7f);
        assertTrue(capture.isThrottling());
        assertEquals(-0.7f, capture.getTurn(), 0.0001f);
    }

    @Test
    public void touchStartingOutsideControlsIsNotCapturedLater() {
        TouchDrivingPointerCapture capture = new TouchDrivingPointerCapture(1);

        capture.beginFrame();
        capture.updatePointer(0, true, false, false, false, true, 0f);
        capture.beginFrame();
        capture.updatePointer(0, true, true, false, false, true, 1f);

        assertFalse(capture.isSteering());
        assertFalse(capture.isThrottling());
        assertFalse(capture.isCaptured(0));
    }

    @Test
    public void powerupTriggersOnceAndKeepsPointerCapturedUntilRelease() {
        TouchDrivingPointerCapture capture = new TouchDrivingPointerCapture(1);

        capture.beginFrame();
        capture.updatePointer(
                0, true, false, false, false, true, false, true, 0f);
        assertTrue(capture.isPowerupPressed());
        assertTrue(capture.isPowerupJustPressed());
        assertTrue(capture.isCaptured(0));

        capture.beginFrame();
        capture.updatePointer(
                0, true, false, false, false, false, false, true, 0f);
        assertTrue(capture.isPowerupPressed());
        assertFalse(capture.isPowerupJustPressed());

        capture.beginFrame();
        capture.updatePointer(
                0, false, false, false, false, false, false, true, 0f);
        assertFalse(capture.isPowerupPressed());
        assertFalse(capture.isCaptured(0));
    }
}
