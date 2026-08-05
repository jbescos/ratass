package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ThrottleExhaustVisualTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void flameAppearsOnlyAtHighThrottleWithoutBraking() {
        assertEquals(0f, ThrottleExhaustVisual.intensity(0.77f, 0f), EPSILON);
        assertEquals(0f, ThrottleExhaustVisual.intensity(1f, 0.13f), EPSILON);
        assertEquals(1f, ThrottleExhaustVisual.intensity(1f, 0f), EPSILON);
        assertTrue(ThrottleExhaustVisual.intensity(0.90f, 0f) > 0f);
    }

    @Test
    public void strongerThrottleProducesALongerTaperedFlame() {
        float low = ThrottleExhaustVisual.flameLengthScale(0.2f, 0.5f);
        float high = ThrottleExhaustVisual.flameLengthScale(1f, 0.5f);

        assertTrue(high > low);
        assertEquals(0.30f, ThrottleExhaustVisual.flameLengthScale(1f, 1f), EPSILON);
    }
}
