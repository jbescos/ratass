package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CarHandlingBalanceTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void baselineAndDebuffedStatsDoNotChangeHandling() {
        assertEquals(1f, CarHandlingBalance.brakeMultiplier(1f), EPSILON);
        assertEquals(1f, CarHandlingBalance.brakeMultiplier(0.6f), EPSILON);
        assertEquals(
                1f,
                CarHandlingBalance.steeringTorqueMultiplier(0.8f, 0.7f, 1f, 1f),
                EPSILON);
    }

    @Test
    public void brakingTracksTheSquaredSpeedEnvelope() {
        assertEquals(1.96f, CarHandlingBalance.brakeMultiplier(1.4f), EPSILON);
        assertEquals(1.4f, CarHandlingBalance.yawRateMultiplier(1.4f), EPSILON);
    }

    @Test
    public void steeringAlsoCountersAdditionalGripDamping() {
        assertEquals(
                1.8019f,
                CarHandlingBalance.steeringTorqueMultiplier(1.4f, 1.4f, 1f, 1f),
                EPSILON);
        assertEquals(1.2871f, CarHandlingBalance.yawGripMultiplier(1.4f, 0f), EPSILON);
        assertEquals(1.4f, CarHandlingBalance.yawGripMultiplier(1.4f, 0.20f), EPSILON);
        assertEquals(0.6f, CarHandlingBalance.yawGripMultiplier(0.6f, 1f), EPSILON);
    }

    @Test
    public void reducedMassDoesNotReduceSteeringTorque() {
        assertEquals(
                1.375f,
                CarHandlingBalance.steeringTorqueMultiplier(1f, 1f, 1.1f, 0.8f),
                EPSILON);
        assertEquals(
                1f,
                CarHandlingBalance.steeringTorqueMultiplier(1f, 1f, 1f, 1.2f),
                EPSILON);
    }

    @Test
    public void invalidInputsFallBackToBaseline() {
        assertEquals(1f, CarHandlingBalance.brakeMultiplier(Float.NaN), EPSILON);
        assertEquals(1f, CarHandlingBalance.yawRateMultiplier(Float.NaN), EPSILON);
        assertEquals(1f, CarHandlingBalance.yawGripMultiplier(Float.NaN, 0f), EPSILON);
        assertEquals(
                1f,
                CarHandlingBalance.steeringTorqueMultiplier(
                        Float.POSITIVE_INFINITY,
                        Float.NaN,
                        Float.NaN,
                        Float.NaN),
                EPSILON);
    }
}
