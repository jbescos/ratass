package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class SlipstreamVisualTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void wakeAppearsOnlyForMeaningfulSlipstreamBoost() {
        assertEquals(0f, SlipstreamVisual.intensity(0.01f), EPSILON);
        assertTrue(SlipstreamVisual.intensity(0.10f) > 0f);
        assertEquals(1f, SlipstreamVisual.intensity(1f), EPSILON);
    }

    @Test
    public void animatedStreaksWrapAndRemainStaggered() {
        float first = SlipstreamVisual.phase(10f, 2, 0);
        float second = SlipstreamVisual.phase(10f, 2, 1);

        assertTrue(first >= 0f && first < 1f);
        assertTrue(second >= 0f && second < 1f);
        assertTrue(Math.abs(first - second) > EPSILON);
    }

    @Test
    public void wakeSpreadsAndFadesBehindTheCar() {
        assertTrue(
                SlipstreamVisual.boundarySpread(1f)
                        > SlipstreamVisual.boundarySpread(0f));
        assertTrue(
                SlipstreamVisual.streakSpread(1f)
                        > SlipstreamVisual.streakSpread(0f));
        assertEquals(0f, SlipstreamVisual.streakAlpha(0f), EPSILON);
        assertEquals(0f, SlipstreamVisual.streakAlpha(1f), EPSILON);
        assertTrue(SlipstreamVisual.streakAlpha(0.25f) > 0f);
    }
}
