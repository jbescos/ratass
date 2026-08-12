package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class AutomaticRecoveryExplosionTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void stationaryCarReceivesDistanceScaledSeparationSpeed() {
        assertEquals(
                10f,
                AutomaticRecoveryExplosion.outwardSpeedChange(0f, 0f, 4f, 10f),
                EPSILON);
        assertEquals(
                7.25f,
                AutomaticRecoveryExplosion.outwardSpeedChange(0f, 2f, 4f, 10f),
                EPSILON);
    }

    @Test
    public void incomingSpeedIsReversedBeforeAddingSeparationSpeed() {
        assertEquals(
                12.25f,
                AutomaticRecoveryExplosion.outwardSpeedChange(-5f, 2f, 4f, 10f),
                EPSILON);
    }

    @Test
    public void carAlreadyEscapingFasterThanTheBlastIsNotPulledBack() {
        assertEquals(
                0f,
                AutomaticRecoveryExplosion.outwardSpeedChange(8f, 2f, 4f, 10f),
                EPSILON);
        assertEquals(
                0f,
                AutomaticRecoveryExplosion.outwardSpeedChange(0f, 4f, 4f, 10f),
                EPSILON);
    }
}
