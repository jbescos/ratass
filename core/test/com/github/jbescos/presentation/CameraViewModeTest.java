package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CameraViewModeTest {
    @Test
    public void cyclesThroughFollowAndWholeMapModes() {
        assertEquals(CameraViewMode.CHASE, CameraViewMode.TOP_DOWN.cycle(1));
        assertEquals(CameraViewMode.WHOLE_MAP, CameraViewMode.CHASE.cycle(1));
        assertEquals(CameraViewMode.TOP_DOWN, CameraViewMode.WHOLE_MAP.cycle(1));
        assertEquals(CameraViewMode.WHOLE_MAP, CameraViewMode.TOP_DOWN.cycle(-1));
    }

    @Test
    public void exposesFixedWholeMapBehavior() {
        assertTrue(CameraViewMode.WHOLE_MAP.showsWholeMap());
        assertFalse(CameraViewMode.WHOLE_MAP.followsBehind());
        assertTrue(CameraViewMode.CHASE.followsBehind());
    }

    @Test
    public void loadsStoredAndLegacyCameraSettings() {
        assertEquals(
                CameraViewMode.WHOLE_MAP,
                CameraViewMode.fromStoredValue("whole map", false));
        assertEquals(CameraViewMode.CHASE, CameraViewMode.fromStoredValue(null, true));
        assertEquals(CameraViewMode.TOP_DOWN, CameraViewMode.fromStoredValue("unknown", false));
    }
}
