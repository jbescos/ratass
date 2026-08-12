package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TimeDilationDecisionCadenceTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void poweredCarDecidesEveryTwoStepsInsteadOfFour() {
        float physicsStep = 1f / 60f;
        float normal = physicsStep * 4f;

        assertEquals(
                normal,
                TimeDilationDecisionCadence.intervalSeconds(normal, false, 2f),
                EPSILON);
        assertEquals(
                physicsStep * 2f,
                TimeDilationDecisionCadence.intervalSeconds(normal, true, 2f),
                EPSILON);
    }

    @Test
    public void activationBringsPendingDecisionIntoAcceleratedWindow() {
        float normal = 1f / 15f;
        float accelerated = normal * 0.5f;

        assertEquals(
                accelerated,
                TimeDilationDecisionCadence.transitionTimer(
                        normal * 0.75f,
                        normal,
                        false,
                        true,
                        2f),
                EPSILON);
        assertEquals(
                accelerated,
                TimeDilationDecisionCadence.transitionTimer(
                        accelerated,
                        normal,
                        true,
                        false,
                        2f),
                EPSILON);
    }
}
