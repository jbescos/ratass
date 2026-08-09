package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VendettaHookPullTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void closesTheGapLinearlyOverFiveSeconds() {
        assertEquals(10f, VendettaHookPull.remainingGap(10f, 0f, 5f), EPSILON);
        assertEquals(5f, VendettaHookPull.remainingGap(10f, 2.5f, 5f), EPSILON);
        assertEquals(0f, VendettaHookPull.remainingGap(10f, 5f, 5f), EPSILON);
    }

    @Test
    public void clampsTimeOutsideTheEffectWindow() {
        assertEquals(10f, VendettaHookPull.remainingGap(10f, -2f, 5f), EPSILON);
        assertEquals(0f, VendettaHookPull.remainingGap(10f, 7f, 5f), EPSILON);
        assertTrue(VendettaHookPull.isComplete(5f, 5f));
    }

    @Test
    public void reachesOverlappingContactAtFiveSeconds() {
        assertEquals(
                10f,
                VendettaHookPull.pullDistance(10f, 1.6f, 0.08f, 0f, 5f),
                EPSILON);
        assertEquals(
                5.76f,
                VendettaHookPull.pullDistance(10f, 1.6f, 0.08f, 2.5f, 5f),
                EPSILON);
        assertEquals(
                1.52f,
                VendettaHookPull.pullDistance(10f, 1.6f, 0.08f, 5f, 5f),
                EPSILON);
    }

    @Test
    public void neverMovesApartWhenCarsStartInContact() {
        assertEquals(
                1f,
                VendettaHookPull.pullDistance(1f, 1.6f, 0.08f, 5f, 5f),
                EPSILON);
    }

    @Test
    public void rejectsInvalidDurationsAndDistances() {
        assertEquals(
                0f,
                VendettaHookPull.remainingGap(10f, 2f, 0f),
                EPSILON);
        assertEquals(
                0f,
                VendettaHookPull.remainingGap(Float.NaN, 2f, 5f),
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
