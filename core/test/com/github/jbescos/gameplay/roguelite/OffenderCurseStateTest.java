package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OffenderCurseStateTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void curseExpiresAfterItsDuration() {
        OffenderCurseState curse = new OffenderCurseState();

        assertTrue(curse.apply(1.20f, 0.80f, 40f));
        assertTrue(curse.isBlind());
        assertEquals(40f, curse.getRemainingSeconds(), EPSILON);
        assertEquals(1.20f, curse.getMassMultiplier(), EPSILON);
        assertEquals(0.80f, curse.getGripMultiplier(), EPSILON);
        assertEquals(0.80f, curse.getPowerMultiplier(), EPSILON);
        assertEquals(0.80f, curse.getAerodynamicEfficiencyMultiplier(), EPSILON);

        assertFalse(curse.advance(39.9f));
        assertTrue(curse.isActive());
        assertTrue(curse.advance(0.1f));
        assertFalse(curse.isActive());
        assertEquals(1f, curse.getMassMultiplier(), EPSILON);
        assertEquals(1f, curse.getGripMultiplier(), EPSILON);
        assertEquals(1f, curse.getPowerMultiplier(), EPSILON);
        assertEquals(1f, curse.getAerodynamicEfficiencyMultiplier(), EPSILON);
        assertFalse(curse.advance(1f));
    }

    @Test
    public void strongerCurseValuesWinWhenEffectsOverlap() {
        OffenderCurseState curse = new OffenderCurseState();

        curse.apply(1.05f, 0.95f, 20f);
        curse.apply(1.50f, 0.50f, 60f);
        curse.apply(1.20f, 0.80f, 40f);

        assertEquals(1.50f, curse.getMassMultiplier(), EPSILON);
        assertEquals(0.50f, curse.getGripMultiplier(), EPSILON);
        assertEquals(0.50f, curse.getPowerMultiplier(), EPSILON);
        assertEquals(0.50f, curse.getAerodynamicEfficiencyMultiplier(), EPSILON);
        assertEquals(60f, curse.getRemainingSeconds(), EPSILON);
    }
}
