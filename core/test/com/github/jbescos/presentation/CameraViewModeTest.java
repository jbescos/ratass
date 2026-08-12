package com.github.jbescos.presentation;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CameraViewModeTest {
    @Test
    public void exposesFollowAndDragCreatedFreeModes() {
        assertTrue(CameraViewMode.FREE.isFree());
        assertFalse(CameraViewMode.TOP_DOWN.isFree());
    }
}
