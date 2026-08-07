package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ForcedBrakeControlTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void fullBrakeAlwaysOpposesMotionAndReleasesAtRest() {
        assertEquals(-1f, ForcedBrakeControl.fullBrakeThrottle(12f, 0.08f), EPSILON);
        assertEquals(1f, ForcedBrakeControl.fullBrakeThrottle(-3f, 0.08f), EPSILON);
        assertEquals(0f, ForcedBrakeControl.fullBrakeThrottle(0.04f, 0.08f), EPSILON);
        assertEquals(0f, ForcedBrakeControl.fullBrakeThrottle(-0.04f, 0.08f), EPSILON);
    }

    @Test
    public void reverseGuardOnlyRemovesBackwardSpeed() {
        assertEquals(0f, ForcedBrakeControl.reverseSpeedToRemove(4f), EPSILON);
        assertEquals(0f, ForcedBrakeControl.reverseSpeedToRemove(0f), EPSILON);
        assertEquals(2.5f, ForcedBrakeControl.reverseSpeedToRemove(-2.5f), EPSILON);
    }
}
