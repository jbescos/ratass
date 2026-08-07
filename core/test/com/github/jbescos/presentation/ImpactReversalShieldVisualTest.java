package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ImpactReversalShieldVisualTest {
    @Test
    public void shieldExpandsAndBrightensAcrossThePulse() {
        assertTrue(
                ImpactReversalShieldVisual.lateralOffsetScale(1f)
                        > ImpactReversalShieldVisual.lateralOffsetScale(0f));
        assertTrue(
                ImpactReversalShieldVisual.longitudinalOffsetScale(1f)
                        > ImpactReversalShieldVisual.longitudinalOffsetScale(0f));
        assertTrue(
                ImpactReversalShieldVisual.arrowTipScale(1f)
                        > ImpactReversalShieldVisual.arrowTipScale(0f));
        assertTrue(
                ImpactReversalShieldVisual.alpha(1f)
                        > ImpactReversalShieldVisual.alpha(0f));
    }

    @Test
    public void pulseIsClampedBeforeCalculatingGeometry() {
        assertEquals(
                ImpactReversalShieldVisual.lateralOffsetScale(0f),
                ImpactReversalShieldVisual.lateralOffsetScale(-2f),
                0f);
        assertEquals(
                ImpactReversalShieldVisual.arrowTipScale(1f),
                ImpactReversalShieldVisual.arrowTipScale(3f),
                0f);
        assertTrue(ImpactReversalShieldVisual.alpha(0f) > 0f);
        assertTrue(ImpactReversalShieldVisual.alpha(1f) <= 1f);
    }
}
