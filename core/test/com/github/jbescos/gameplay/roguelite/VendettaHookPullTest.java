package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VendettaHookPullTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void closesTheCurrentGapByTheRequiredFraction() {
        assertEquals(0.02f, VendettaHookPull.stepFraction(0f, 0.1f, 5f), EPSILON);
        assertEquals(0.04f, VendettaHookPull.stepFraction(2.5f, 0.1f, 5f), EPSILON);
        assertEquals(1f, VendettaHookPull.stepFraction(4.9f, 0.1f, 5f), EPSILON);
    }

    @Test
    public void reachesAMovingOffenderAtTheEndOfTheEffect() {
        float sourcePosition = 0f;
        float offenderPosition = 10f;
        for (int second = 0; second < 5; second++) {
            offenderPosition += 1f;
            float fraction = VendettaHookPull.stepFraction(second, 1f, 5f);
            sourcePosition += (offenderPosition - sourcePosition) * fraction;
        }

        assertEquals(offenderPosition, sourcePosition, EPSILON);
    }

    @Test
    public void clampsTimeOutsideTheEffectWindow() {
        assertEquals(0.02f, VendettaHookPull.stepFraction(-2f, 0.1f, 5f), EPSILON);
        assertEquals(1f, VendettaHookPull.stepFraction(7f, 0.1f, 5f), EPSILON);
        assertTrue(VendettaHookPull.isComplete(5f, 5f));
    }

    @Test
    public void alignsThePulledCarWithTheOffenderHeading() {
        assertEquals(
                -2.4f,
                VendettaHookPull.alignedHeading(1.1f, -2.4f),
                EPSILON);
        assertEquals(
                1.1f,
                VendettaHookPull.alignedHeading(1.1f, Float.NaN),
                EPSILON);
        assertEquals(
                0f,
                VendettaHookPull.alignedHeading(Float.NaN, Float.NaN),
                EPSILON);
    }

    @Test
    public void neverMovesCarsApartWhenTheyAreAlreadyCloserThanContact() {
        assertEquals(
                1f,
                VendettaHookPull.desiredContactDistance(1f, 1.6f, 0.08f),
                EPSILON);
        assertEquals(
                1.52f,
                VendettaHookPull.desiredContactDistance(10f, 1.6f, 0.08f),
                EPSILON);
    }

    @Test
    public void rejectsInvalidDurationsAndDeltas() {
        assertEquals(
                1f,
                VendettaHookPull.stepFraction(2f, 0.1f, 0f),
                EPSILON);
        assertEquals(
                0f,
                VendettaHookPull.stepFraction(2f, Float.NaN, 5f),
                EPSILON);
    }

    @Test
    public void calculatesNearContactDistanceFromCarOrientations() {
        assertEquals(
                1.62f,
                VendettaHookPull.contactDistance(
                        0f,
                        1f,
                        0.57f,
                        0.79f,
                        0f,
                        0.57f,
                        0.79f,
                        0f,
                        0.04f),
                EPSILON);
        assertEquals(
                1.18f,
                VendettaHookPull.contactDistance(
                        1f,
                        0f,
                        0.57f,
                        0.79f,
                        0f,
                        0.57f,
                        0.79f,
                        0f,
                        0.04f),
                EPSILON);
    }
}
