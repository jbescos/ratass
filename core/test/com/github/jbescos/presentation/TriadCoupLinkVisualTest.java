package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import org.junit.Test;

public class TriadCoupLinkVisualTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void drawsOnlyForAnArmedTriadCoupWithItsRequiredCars() {
        assertTrue(TriadCoupLinkVisual.shouldDraw(
                RogueliteCardId.TRIAD_COUP, true, true, true));
        assertFalse(TriadCoupLinkVisual.shouldDraw(
                RogueliteCardId.TRIAD_COUP, false, true, true));
        assertFalse(TriadCoupLinkVisual.shouldDraw(
                RogueliteCardId.RECOVERY_BEACON, true, true, true));
        assertFalse(TriadCoupLinkVisual.shouldDraw(
                RogueliteCardId.TRIAD_COUP, true, false, true));
    }

    @Test
    public void triangleRequiresThreeAvailableDistinctCars() {
        assertTrue(TriadCoupLinkVisual.hasTriangle(1, 2, 3, true));
        assertFalse(TriadCoupLinkVisual.hasTriangle(1, 2, -1, true));
        assertFalse(TriadCoupLinkVisual.hasTriangle(1, 2, 2, true));
        assertFalse(TriadCoupLinkVisual.hasTriangle(1, 2, 3, false));
    }

    @Test
    public void clampsChargeAndIntensifiesThePulse() {
        assertEquals(0f, TriadCoupLinkVisual.charge(Float.NaN), EPSILON);
        assertEquals(0f, TriadCoupLinkVisual.charge(-1f), EPSILON);
        assertEquals(1f, TriadCoupLinkVisual.charge(2f), EPSILON);
        assertTrue(
                TriadCoupLinkVisual.pulse(0f, 1f)
                        > TriadCoupLinkVisual.pulse(0f, 0f));
    }
}
