package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OffenderCurseStateTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void curseRemainsActiveUntilAQualifiedCollision() {
        OffenderCurseState curse = new OffenderCurseState();

        assertTrue(curse.apply(1.20f, 1f));
        assertTrue(curse.isBlind());
        assertEquals(1.20f, curse.getMassMultiplier(), EPSILON);
        assertEquals(1f, curse.getGripMultiplier(), EPSILON);

        assertTrue(curse.clearOnQualifiedCollision());
        assertFalse(curse.isActive());
        assertEquals(1f, curse.getMassMultiplier(), EPSILON);
        assertEquals(1f, curse.getGripMultiplier(), EPSILON);
        assertFalse(curse.clearOnQualifiedCollision());
    }

    @Test
    public void strongerCurseValuesWinWhenEffectsOverlap() {
        OffenderCurseState curse = new OffenderCurseState();

        curse.apply(1.05f, 1f);
        curse.apply(1.50f, 0.80f);
        curse.apply(1.20f, 0.95f);

        assertEquals(1.50f, curse.getMassMultiplier(), EPSILON);
        assertEquals(0.80f, curse.getGripMultiplier(), EPSILON);
    }
}
