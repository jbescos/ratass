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
}
