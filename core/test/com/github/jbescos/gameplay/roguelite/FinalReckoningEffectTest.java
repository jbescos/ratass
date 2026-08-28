package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class FinalReckoningEffectTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void hitImmediatelyStartsFifteenSecondHunt() {
        FinalReckoningEffect effect = new FinalReckoningEffect();
        RogueliteDrivingFrame frame = new RogueliteDrivingFrame();

        assertTrue(effect.onHitBy(42, 10f));
        assertTrue(effect.isArmed());
        assertTrue(effect.isReady());
        assertEquals(42, effect.revengeTargetVehicleId());
        assertEquals(15f, effect.activeTimeRemainingSeconds(), EPSILON);
        assertEquals(0.50f, effect.accelerationBonus(), EPSILON);

        effect.advance(0.2f, 0.2f, frame);
        assertTrue(effect.isReady());
        assertEquals(14.8f, effect.activeTimeRemainingSeconds(), EPSILON);
        assertEquals(0.50f, effect.accelerationBonus(), EPSILON);
        assertEquals(0f, effect.frontCollisionRecoilMultiplier(), EPSILON);
        assertEquals(2.50f, effect.frontCollisionPushMultiplier(), EPSILON);
    }

    @Test
    public void amplifierScalesImmediateHuntDurationAndBuffs() {
        FinalReckoningEffect effect = new FinalReckoningEffect();
        RogueliteDrivingFrame frame = new RogueliteDrivingFrame();
        effect.onHitBy(42, 10f);

        effect.amplifyActiveRevenge(2f);
        effect.amplifyActiveRevenge(2f);

        assertEquals(30f, effect.activeTimeRemainingSeconds(), EPSILON);
        assertTrue(effect.isReady());
        assertEquals(1f, effect.accelerationBonus(), EPSILON);
        assertEquals(0f, effect.frontCollisionRecoilMultiplier(), EPSILON);
        assertEquals(4f, effect.frontCollisionPushMultiplier(), EPSILON);
    }

    @Test
    public void eachRammerHasACooldownWithoutConsumingTheHunt() {
        FinalReckoningEffect effect = readyEffect();
        RogueliteDrivingFrame frame = new RogueliteDrivingFrame();

        RogueliteRevengeStrike first = effect.tryActivateHuntRam(3, 42, 4f);
        assertNotNull(first);
        assertEquals(RogueliteCardId.FINAL_RECKONING, first.getCardId());
        assertNull(effect.tryActivateHuntRam(3, 42, 4f));
        assertNotNull(effect.tryActivateHuntRam(4, 42, 4f));
        assertTrue(effect.isReady());
        assertEquals(42, effect.revengeTargetVehicleId());

        effect.advance(FinalReckoningEffect.RAM_COOLDOWN_SECONDS, 0f, frame);
        assertNotNull(effect.tryActivateHuntRam(3, 42, 4f));
        assertTrue(effect.isReady());
    }

    @Test
    public void huntExpiresOnlyAfterTheFullActiveWindow() {
        FinalReckoningEffect effect = readyEffect();
        RogueliteDrivingFrame frame = new RogueliteDrivingFrame();

        effect.advance(14.9f, 14.9f, frame);
        assertTrue(effect.isReady());
        effect.advance(0.2f, 0.2f, frame);

        assertFalse(effect.isActive());
        assertEquals(-1, effect.revengeTargetVehicleId());
    }

    private static FinalReckoningEffect readyEffect() {
        FinalReckoningEffect effect = new FinalReckoningEffect();
        effect.onHitBy(42, 10f);
        effect.advance(
                FinalReckoningEffect.PREPARATION_SECONDS,
                FinalReckoningEffect.PREPARATION_SECONDS,
                new RogueliteDrivingFrame());
        return effect;
    }
}
