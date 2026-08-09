package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ImpactReversalReadyVisualTest {
    @Test
    public void armedImageGrowsAndBrightensWithPulse() {
        assertTrue(
                ImpactReversalReadyVisual.sizeScale(1f)
                        > ImpactReversalReadyVisual.sizeScale(0f));
        assertTrue(
                ImpactReversalReadyVisual.alpha(1f)
                        > ImpactReversalReadyVisual.alpha(0f));
    }

    @Test
    public void pulseIsClampedForStableRendering() {
        assertEquals(
                ImpactReversalReadyVisual.sizeScale(0f),
                ImpactReversalReadyVisual.sizeScale(-2f),
                0f);
        assertEquals(
                ImpactReversalReadyVisual.alpha(1f),
                ImpactReversalReadyVisual.alpha(3f),
                0f);
        assertTrue(ImpactReversalReadyVisual.alpha(0f) > 0f);
        assertTrue(ImpactReversalReadyVisual.alpha(1f) <= 1f);
    }
}
