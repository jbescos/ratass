package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class StableCarMotionTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void rejectsAnyNonFinitePartOfThePhysicsState() {
        assertTrue(StableCarMotion.isFiniteMotion(1f, 2f, 3f, 4f, 5f, 6f));
        assertFalse(StableCarMotion.isFiniteMotion(Float.NaN, 2f, 3f, 4f, 5f, 6f));
        assertFalse(
                StableCarMotion.isFiniteMotion(
                        1f,
                        2f,
                        3f,
                        Float.POSITIVE_INFINITY,
                        5f,
                        6f));
        assertFalse(
                StableCarMotion.isFiniteMotion(
                        1f,
                        2f,
                        3f,
                        4f,
                        5f,
                        Float.NEGATIVE_INFINITY));
    }

    @Test
    public void onlySafeFiniteTransformsReplaceTheRecoveryAnchor() {
        StableCarMotion motion = new StableCarMotion();
        motion.rememberSpawn(1f, 2f, 0.25f);
        motion.rememberSafe(8f, 9f, 0.75f, false);
        motion.rememberSafe(Float.NaN, 9f, 0.75f, true);

        assertTrue(motion.hasAnchor());
        assertEquals(1f, motion.getAnchorX(), EPSILON);
        assertEquals(2f, motion.getAnchorY(), EPSILON);
        assertEquals(0.25f, motion.getAnchorAngle(), EPSILON);

        motion.rememberSafe(4f, 5f, 1.25f, true);

        assertEquals(4f, motion.getAnchorX(), EPSILON);
        assertEquals(5f, motion.getAnchorY(), EPSILON);
        assertEquals(1.25f, motion.getAnchorAngle(), EPSILON);
    }
}
