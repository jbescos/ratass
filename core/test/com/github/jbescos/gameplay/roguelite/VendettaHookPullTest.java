package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VendettaHookPullTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void closesTheCurrentGapByTheRequiredFraction() {
        assertEquals(0.1f, VendettaHookPull.stepFraction(0f, 0.1f, 1f), EPSILON);
        assertEquals(0.2f, VendettaHookPull.stepFraction(0.5f, 0.1f, 1f), EPSILON);
        assertEquals(1f, VendettaHookPull.stepFraction(0.9f, 0.1f, 1f), EPSILON);
    }

    @Test
    public void reachesAMovingOffenderAtTheEndOfTheEffect() {
        float sourcePosition = 0f;
        float offenderPosition = 10f;
        float firstPosition = sourcePosition;
        for (int frame = 0; frame < 10; frame++) {
            offenderPosition += 0.1f;
            float fraction = VendettaHookPull.stepFraction(frame * 0.1f, 0.1f, 1f);
            sourcePosition += (offenderPosition - sourcePosition) * fraction;
            if (frame == 0) {
                firstPosition = sourcePosition;
            }
        }

        assertTrue(firstPosition > 0f);
        assertTrue(firstPosition < offenderPosition);
        assertEquals(offenderPosition, sourcePosition, EPSILON);
    }

    @Test
    public void reachesContactWithAMovingOffenderAtSixtyUpdatesPerSecond() {
        float sourcePosition = 0f;
        float offenderPosition = 20f;
        float contactDistance = 1.6f;
        float overlap = 0.08f;
        float delta = 1f / 60f;
        for (int frame = 0; frame < 60; frame++) {
            offenderPosition += 4f * delta;
            float currentDistance = offenderPosition - sourcePosition;
            float destination =
                    offenderPosition
                            - VendettaHookPull.desiredContactDistance(
                                    currentDistance,
                                    contactDistance,
                                    overlap);
            float fraction = VendettaHookPull.stepFraction(frame * delta, delta, 1f);
            sourcePosition += (destination - sourcePosition) * fraction;
        }

        assertEquals(contactDistance - overlap, offenderPosition - sourcePosition, 0.001f);
    }

    @Test
    public void clampsTimeOutsideTheEffectWindow() {
        assertEquals(0.1f, VendettaHookPull.stepFraction(-2f, 0.1f, 1f), EPSILON);
        assertEquals(1f, VendettaHookPull.stepFraction(7f, 0.1f, 1f), EPSILON);
        assertTrue(VendettaHookPull.isComplete(1f, 1f));
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
                VendettaHookPull.stepFraction(2f, Float.NaN, 1f),
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
