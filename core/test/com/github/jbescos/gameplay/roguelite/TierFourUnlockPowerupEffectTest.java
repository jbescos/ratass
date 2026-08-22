package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TierFourUnlockPowerupEffectTest {
    @Test
    public void directlyEquippedSignalUsesConcurrentTenSecondCooldowns() {
        TierFourUnlockPowerupEffect effect = new TierFourUnlockPowerupEffect();

        assertTrue(effect.isActive());
        assertEquals(10f, effect.activeTimeRemainingSeconds(), 0.0001f);
        assertEquals(10f, effect.cooldownTimeRemainingSeconds(), 0.0001f);

        effect.advance(4f, 4f, drivingFrame());

        assertTrue(effect.isActive());
        assertEquals(6f, effect.activeTimeRemainingSeconds(), 0.0001f);
        assertEquals(6f, effect.cooldownTimeRemainingSeconds(), 0.0001f);

        effect.advance(6f, 6f, drivingFrame());

        assertTrue(effect.isActive());
        assertEquals(10f, effect.activeTimeRemainingSeconds(), 0.0001f);
        assertEquals(10f, effect.cooldownTimeRemainingSeconds(), 0.0001f);
    }

    @Test
    public void randomlyLoadedSignalWaitsForItsCooldown() {
        TierFourUnlockPowerupEffect effect = new TierFourUnlockPowerupEffect();
        effect.onLoadedByRandomCard();

        assertEquals(10f, effect.cooldownTimeRemainingSeconds(), 0.0001f);
        effect.advance(9f, 9f, drivingFrame());
        assertEquals(0f, effect.activeTimeRemainingSeconds(), 0.0001f);
        assertEquals(1f, effect.cooldownTimeRemainingSeconds(), 0.0001f);

        effect.advance(1f, 1f, drivingFrame());

        assertTrue(effect.isActive());
        assertEquals(10f, effect.activeTimeRemainingSeconds(), 0.0001f);
        assertEquals(10f, effect.cooldownTimeRemainingSeconds(), 0.0001f);

        effect.advance(10f, 10f, drivingFrame());

        assertEquals(0f, effect.activeTimeRemainingSeconds(), 0.0001f);
        assertEquals(0f, effect.cooldownTimeRemainingSeconds(), 0.0001f);

        effect.advance(1f, 1f, drivingFrame());

        assertEquals(0f, effect.activeTimeRemainingSeconds(), 0.0001f);
    }

    private static RogueliteDrivingFrame drivingFrame() {
        return new RogueliteDrivingFrame();
    }
}
