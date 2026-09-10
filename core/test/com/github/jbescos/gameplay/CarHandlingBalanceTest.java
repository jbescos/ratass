package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CarHandlingBalanceTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void boostedGripDoesNotAmplifySidewaysCorrectionGain() {
        assertEquals(0.1f, CarHandlingBalance.lateralCorrectionGripMultiplier(0.1f), 0f);
        assertEquals(1f, CarHandlingBalance.lateralCorrectionGripMultiplier(1f), 0f);
        assertEquals(1f, CarHandlingBalance.lateralCorrectionGripMultiplier(5f), 0f);
        assertEquals(1f, CarHandlingBalance.lateralCorrectionGripMultiplier(Float.NaN), 0f);
    }

    @Test
    public void tractionBonusesGrowWithoutMultiplyingExtremePowerAndLightweightLinearly() {
        assertEquals(0.7f, CarHandlingBalance.driveTractionMultiplier(0.7f), 0f);
        assertEquals(1f, CarHandlingBalance.driveTractionMultiplier(1f), 0f);
        assertEquals(2f, CarHandlingBalance.driveTractionMultiplier(4f), EPSILON);
        assertEquals((float) Math.sqrt(50), CarHandlingBalance.driveTractionMultiplier(50f), EPSILON);
        assertEquals(1f, CarHandlingBalance.driveTractionMultiplier(Float.NaN), 0f);
        float previous = 0f;
        for (float value = 0.1f; value <= 50f; value += 0.1f) {
            float result = CarHandlingBalance.driveTractionMultiplier(value);
            assertTrue(result >= previous);
            previous = result;
        }
    }

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
    public void brakingTracksTheSpeedEnvelopeWithoutSquaringIt() {
        assertEquals(1.4f, CarHandlingBalance.brakeMultiplier(1.4f), EPSILON);
        assertEquals(1.4f, CarHandlingBalance.yawRateMultiplier(1.4f), EPSILON);
    }

    @Test
    public void gripCanRaiseTheTurnRateLimitWithoutPowerOrAeroBonuses() {
        assertEquals(2f, CarHandlingBalance.yawRateMultiplier(1f, 4f), EPSILON);
        assertEquals(3f, CarHandlingBalance.yawRateMultiplier(3f, 4f), EPSILON);
        assertEquals(1f, CarHandlingBalance.yawRateMultiplier(0.1f, 0.1f), 0f);
        assertEquals(1f, CarHandlingBalance.yawRateMultiplier(Float.NaN, Float.NaN), 0f);
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
