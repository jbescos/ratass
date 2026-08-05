package com.github.jbescos.gameplay;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PassingAssistGeometryTest {
    private static final float HALF_WIDTH = 0.57f;
    private static final float SIDE_GAP = 0.20f;
    private static final float SEPARATION_RATIO = 0.55f;

    @Test
    public void centeredCarAheadIsNotAlongside() {
        assertFalse(isAlongside(0.8f, 0f));
    }

    @Test
    public void laterallySeparatedCarReleasesThrottleLimiting() {
        assertTrue(isAlongside(0.8f, 0.75f));
        assertTrue(isAlongside(-0.5f, -0.75f));
    }

    @Test
    public void longitudinallySeparatedCarIsNotAlongside() {
        assertFalse(isAlongside(2.1f, 0.75f));
        assertFalse(isAlongside(-1.9f, 0.75f));
    }

    private static boolean isAlongside(float forwardDistance, float lateralDistance) {
        return PassingAssistGeometry.isAlongside(
                forwardDistance,
                lateralDistance,
                2f,
                1.8f,
                HALF_WIDTH,
                HALF_WIDTH,
                SIDE_GAP,
                SEPARATION_RATIO);
    }
}
