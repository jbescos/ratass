package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CameraZoomControlTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void wheelStepsRespectConfiguredLimits() {
        assertEquals(1.1f, CameraZoomControl.step(1f, 1f, 0.1f, 0.1f, 2.5f), EPSILON);
        assertEquals(0.1f, CameraZoomControl.step(0.1f, -1f, 0.1f, 0.1f, 2.5f), EPSILON);
        assertEquals(2.5f, CameraZoomControl.step(2.5f, 1f, 0.1f, 0.1f, 2.5f), EPSILON);
    }

    @Test
    public void pinchOutIncreasesZoomSettingAndPinchInDecreasesIt() {
        assertEquals(1.5f, CameraZoomControl.scale(1f, 1.5f, 0.1f, 2.5f), EPSILON);
        assertEquals(0.5f, CameraZoomControl.scale(1f, 0.5f, 0.1f, 2.5f), EPSILON);
    }

    @Test
    public void zoomOutStopsAtWholeMapFit() {
        assertEquals(3f, CameraZoomControl.limitToMap(7f, 3f), EPSILON);
        assertTrue(CameraZoomControl.reachesWholeMap(3f, 3f));
        assertFalse(CameraZoomControl.reachesWholeMap(2.9f, 3f));
    }
}
