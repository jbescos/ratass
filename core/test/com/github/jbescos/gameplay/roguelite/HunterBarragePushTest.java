package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HunterBarragePushTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void appliesTheConfiguredVelocityKickToANormalCar() {
        float mass = 2.4f;
        float impulse = HunterBarragePush.impulse(mass, 75f, 4.8f);

        assertEquals(10.5f, HunterBarragePush.pushSpeed(75f), EPSILON);
        assertEquals(10.5f, impulse / mass, EPSILON);
    }

    @Test
    public void boundsTheImpulseForAnUnusuallyHeavyCar() {
        float impulse = HunterBarragePush.impulse(20f, 75f, 4.8f);

        assertEquals(38.4f, impulse, EPSILON);
        assertTrue(impulse > 4.8f);
    }

    @Test
    public void rejectsInvalidPhysicsValues() {
        assertEquals(0f, HunterBarragePush.pushSpeed(Float.NaN), EPSILON);
        assertEquals(0f, HunterBarragePush.impulse(-1f, 75f, 4.8f), EPSILON);
        assertEquals(0f, HunterBarragePush.impulse(2.4f, 75f, Float.NaN), EPSILON);
    }
}
