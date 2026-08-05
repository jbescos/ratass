package com.github.jbescos.gameplay;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VehicleFootprintOverlapTest {
    private static final float HALF_WIDTH = 0.45f;
    private static final float HALF_LENGTH = 0.90f;
    private static final float MARGIN = 0.04f;

    @Test
    public void detectsAlignedAndRotatedOverlap() {
        assertTrue(overlaps(0f, 0f, 0f, 0f, 0f, 0f));
        assertTrue(overlaps(0f, 0f, 0f, 0.50f, 0f, (float) Math.PI / 2f));
    }

    @Test
    public void requiresACompleteVehicleFootprintOfClearance() {
        assertTrue(overlaps(0f, 0f, 0f, 0f, 1.80f, 0f));
        assertFalse(overlaps(0f, 0f, 0f, 0f, 1.85f, 0f));
        assertFalse(overlaps(0f, 0f, 0f, 0.95f, 0f, 0f));
    }

    private static boolean overlaps(
            float centerAX,
            float centerAY,
            float angleA,
            float centerBX,
            float centerBY,
            float angleB) {
        return VehicleFootprintOverlap.overlaps(
                centerAX,
                centerAY,
                angleA,
                HALF_WIDTH,
                HALF_LENGTH,
                centerBX,
                centerBY,
                angleB,
                HALF_WIDTH,
                HALF_LENGTH,
                MARGIN);
    }
}
