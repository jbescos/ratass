package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RevengeLifecycleTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void delayedWorkflowRestartsItsTargetAndDelayOnANewHit() {
        TargetedRevengeEffect effect =
                new TargetedRevengeEffect(RogueliteCardId.RECOVERY_BEACON);

        assertTrue(effect.onHitBy(42, 12f));
        effect.advance(2.9f, 2.9f, new RogueliteDrivingFrame());
        assertTrue(effect.onHitBy(7, 12f));

        assertEquals(7, effect.revengeTargetVehicleId());
        assertEquals(0f, effect.readiness(), EPSILON);
        effect.advance(2.9f, 2.9f, new RogueliteDrivingFrame());
        assertFalse(effect.isReady());
        effect.advance(0.2f, 0.2f, new RogueliteDrivingFrame());
        assertTrue(effect.isReady());
    }

    @Test
    public void retargetingClearsTriadSecondaryParticipant() {
        TriadCoupRevengeEffect effect = new TriadCoupRevengeEffect();
        effect.onHitBy(42, 12f);
        effect.setRevengeSecondaryTargetVehicleId(7);

        effect.onHitBy(9, 12f);

        assertEquals(9, effect.revengeTargetVehicleId());
        assertEquals(-1, effect.revengeSecondaryTargetVehicleId());
    }

    @Test
    public void cancellingTheTargetFullyDisarmsPreparedRevenge() {
        CrownBreakerRevengeEffect effect = new CrownBreakerRevengeEffect();
        effect.onHitBy(42, 12f);

        assertFalse(effect.cancelRevengeTarget(7));
        assertTrue(effect.cancelRevengeTarget(42));

        assertFalse(effect.isArmed());
        assertFalse(effect.isActive());
        assertEquals(-1, effect.revengeTargetVehicleId());
        assertEquals(1f, effect.accelerationBonus() + 1f, EPSILON);
    }
}
