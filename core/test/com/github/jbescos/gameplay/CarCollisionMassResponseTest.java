package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CarCollisionMassResponseTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void heavierCollisionMassRetainsSomeRecoilAndPushesLighterCarsMore() {
        assertEquals(0f, CarCollisionMassResponse.correctionFraction(1f), EPSILON);
        assertEquals(0.5f, CarCollisionMassResponse.correctionFraction(2f), EPSILON);
        assertEquals(0.75f, CarCollisionMassResponse.correctionFraction(4f), EPSILON);
        assertEquals(11f / 12f,
                CarCollisionMassResponse.correctionFraction(12f), EPSILON);
        assertEquals(0.25f, CarCollisionMassResponse.reboundMultiplier(4f, 1f), EPSILON);
        assertEquals(1f / 12f,
                CarCollisionMassResponse.reboundMultiplier(12f, 1f), EPSILON);
        assertEquals(4f, CarCollisionMassResponse.reboundMultiplier(1f, 4f), EPSILON);
        assertEquals(1f, CarCollisionMassResponse.reboundMultiplier(4f, 4f), EPSILON);
    }

    @Test
    public void unstoppableResponseCancelsNormalTangentAndPositionCorrection() {
        assertEquals(
                7f,
                CarCollisionMassResponse.cancellationImpulseX(
                        0f, 1f, 11f, 7f, 1f),
                EPSILON);
        assertEquals(
                11f,
                CarCollisionMassResponse.cancellationImpulseY(
                        0f, 1f, 11f, 7f, 1f),
                EPSILON);
        assertEquals(
                -7f,
                CarCollisionMassResponse.cancellationImpulseX(
                        0f, 1f, 11f, 7f, -1f),
                EPSILON);
        assertEquals(
                12.5f,
                CarCollisionMassResponse.collisionFreeCoordinate(10f, 5f, 0.5f),
                EPSILON);
    }

    @Test
    public void collisionFieldProtectionIsOnlyAsymmetric() {
        assertFalse(CarCollisionMassResponse.protectsFromContact(false, false));
        assertTrue(CarCollisionMassResponse.protectsFromContact(true, false));
        assertFalse(CarCollisionMassResponse.protectsFromContact(false, true));
        assertFalse(CarCollisionMassResponse.protectsFromContact(true, true));
    }
}
