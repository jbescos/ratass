package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TimeDilationMotionScaleTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void activeCarMovesTwiceAtFourTimesForceScale() {
        float scale = TimeDilationMotionScale.scale(true, 2f);

        assertEquals(2f, scale, EPSILON);
        assertEquals(4f, TimeDilationMotionScale.forceScale(scale), EPSILON);
    }

    @Test
    public void transitionsScaleAndRestoreVelocity() {
        assertEquals(
                2f,
                TimeDilationMotionScale.transitionRatio(1f, 2f),
                EPSILON);
        assertEquals(
                0.5f,
                TimeDilationMotionScale.transitionRatio(2f, 1f),
                EPSILON);
    }
}
