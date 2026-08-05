package com.github.jbescos.presentation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StableCameraStateTest {
    @Test
    public void acceptsFinitePositionDirectionAndZoom() {
        assertTrue(StableCameraState.isUsable(12f, -4f, 0f, 1f, 0.8f));
    }

    @Test
    public void rejectsValuesThatWouldCorruptTheProjectionMatrix() {
        assertFalse(StableCameraState.isUsable(Float.NaN, 0f, 0f, 1f, 1f));
        assertFalse(StableCameraState.isUsable(0f, Float.POSITIVE_INFINITY, 0f, 1f, 1f));
        assertFalse(StableCameraState.isUsable(0f, 0f, Float.NaN, 1f, 1f));
        assertFalse(StableCameraState.isUsable(0f, 0f, 0f, 0f, 1f));
        assertFalse(StableCameraState.isUsable(0f, 0f, 0f, 1f, 0f));
        assertFalse(StableCameraState.isUsable(0f, 0f, 0f, 1f, Float.NaN));
    }

    @Test
    public void holdsFiniteFollowCameraWhileItsTargetIsTemporarilyUnavailable() {
        assertTrue(
                StableCameraState.shouldHoldLastTransform(
                        false, true, false, true, 12f, -4f, 0f, 1f, 0.8f));

        assertFalse(
                StableCameraState.shouldHoldLastTransform(
                        true, true, false, true, 12f, -4f, 0f, 1f, 0.8f));
        assertFalse(
                StableCameraState.shouldHoldLastTransform(
                        false, false, false, true, 12f, -4f, 0f, 1f, 0.8f));
        assertFalse(
                StableCameraState.shouldHoldLastTransform(
                        false, true, true, true, 12f, -4f, 0f, 1f, 0.8f));
        assertFalse(
                StableCameraState.shouldHoldLastTransform(
                        false, true, false, true, Float.NaN, -4f, 0f, 1f, 0.8f));
    }
}
