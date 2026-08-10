package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import org.junit.Test;

public class PositionHijackLinkVisualTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void drawsOnlyForAnArmedPositionHijackWithBothCarsAvailable() {
        assertTrue(PositionHijackLinkVisual.shouldDraw(
                RogueliteCardId.RECOVERY_BEACON,
                true,
                true,
                true));
        assertFalse(PositionHijackLinkVisual.shouldDraw(
                RogueliteCardId.RECOVERY_BEACON,
                false,
                true,
                true));
        assertFalse(PositionHijackLinkVisual.shouldDraw(
                RogueliteCardId.PAYBACK_SHIELD,
                true,
                true,
                true));
        assertFalse(PositionHijackLinkVisual.shouldDraw(
                RogueliteCardId.RECOVERY_BEACON,
                true,
                false,
                true));
    }

    @Test
    public void clampsChargeAndIntensifiesThePulse() {
        assertEquals(0f, PositionHijackLinkVisual.charge(-1f), EPSILON);
        assertEquals(1f, PositionHijackLinkVisual.charge(2f), EPSILON);
        assertEquals(0f, PositionHijackLinkVisual.charge(Float.NaN), EPSILON);
        assertTrue(
                PositionHijackLinkVisual.pulse(0f, 1f)
                        > PositionHijackLinkVisual.pulse(0f, 0f));
    }
}
