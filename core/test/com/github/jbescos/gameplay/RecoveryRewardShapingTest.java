package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RecoveryRewardShapingTest {
    @Test
    public void alignedLaunchRewardsForwardAndPenalizesReverse() {
        assertEquals(0.8f, RecoveryRewardShaping.launchSignal(1f, 0f, 0.8f), 0.0001f);
        assertEquals(-0.8f, RecoveryRewardShaping.launchSignal(-1f, 0f, 0.8f), 0.0001f);
    }

    @Test
    public void reversedLaunchRequiresTurning() {
        assertEquals(0f, RecoveryRewardShaping.launchSignal(1f, 0f, -1f), 0.0001f);
        assertEquals(0.5f, RecoveryRewardShaping.launchSignal(1f, -0.5f, -1f), 0.0001f);
        assertEquals(0.5f, RecoveryRewardShaping.launchSignal(-1f, 0.5f, -1f), 0.0001f);
    }

    @Test
    public void launchInputsAreClamped() {
        assertEquals(1f, RecoveryRewardShaping.launchSignal(2f, -3f, -4f), 0.0001f);
    }

    @Test
    public void pathInefficiencyCountsTravelThatDoesNotApproachTarget() {
        assertEquals(
                0.75f,
                RecoveryRewardShaping.pathInefficiency(0f, 0f, 1f, 0f, 0.25f),
                0.0001f);
        assertEquals(
                0f,
                RecoveryRewardShaping.pathInefficiency(0f, 0f, 1f, 0f, 1f),
                0.0001f);
    }

    @Test
    public void rotationPenaltyStartsOnlyAfterUsefulAlignment() {
        assertEquals(
                0f,
                RecoveryRewardShaping.alignedRotation(2f, 0.2f, 0f, false),
                0.0001f);
        assertEquals(
                2f,
                RecoveryRewardShaping.alignedRotation(2f, 1f, 0f, false),
                0.0001f);
        assertEquals(
                2f,
                RecoveryRewardShaping.alignedRotation(2f, 0f, 1f, true),
                0.0001f);
    }
}
