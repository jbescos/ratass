package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class MapWallBounceTest {
    @Test
    public void replacesIncomingVelocityWithAnOutwardBounce() {
        assertEquals(18f, MapWallBounce.requiredImpulse(-4f, 2f, 5f, 30f), 0.0001f);
    }

    @Test
    public void capsExplosiveImpulsesAndDoesNotSlowCarsAlreadyLeaving() {
        assertEquals(12f, MapWallBounce.requiredImpulse(-20f, 2f, 5f, 12f), 0.0001f);
        assertEquals(0f, MapWallBounce.requiredImpulse(6f, 2f, 5f, 12f), 0.0001f);
    }

    @Test
    public void rejectsInvalidPhysicsValues() {
        assertEquals(0f, MapWallBounce.requiredImpulse(Float.NaN, 2f, 5f, 12f), 0.0001f);
        assertEquals(0f, MapWallBounce.requiredImpulse(-2f, 0f, 5f, 12f), 0.0001f);
    }
}
