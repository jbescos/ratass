package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;

import com.github.jbescos.RatassGame;
import org.junit.Test;

public class BenchmarkStatsTest {
    @Test
    public void appliesIndependentBonusesAndPenaltiesThroughNormalUpgradeCalculations() {
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        upgrades.setBenchmarkStats(5f, 0.1f, 2.2f, 0.4f);
        assertEquals(5f, upgrades.getAccelerationMultiplier(), 0.0001f);
        assertEquals(0.1f, upgrades.getGripMultiplier(0f), 0.0001f);
        assertEquals(2.2f, upgrades.getAerodynamicEfficiencyMultiplier(1f), 0.0001f);
        assertEquals(0.4f, upgrades.getMassMultiplier(), 0.0001f);
        assertEquals(12.5f, upgrades.getDriveForceLimitMultiplier(), 0.0001f);
        assertEquals((float) Math.cbrt(11), upgrades.getMaxSpeedMultiplier(), 0.0001f);
        assertFalse(upgrades.isPowerupReady());

        upgrades.setBenchmarkStats(1f, 1f, 1f, 1f);
        assertEquals(1f, upgrades.getAccelerationMultiplier(), 0f);
        assertEquals(1f, upgrades.getGripMultiplier(0f), 0f);
        assertEquals(1f, upgrades.getMassMultiplier(), 0f);
        assertEquals(1f, upgrades.getDriveForceLimitMultiplier(), 0f);
        assertEquals(1f, upgrades.getMaxSpeedMultiplier(), 0f);
    }

    @Test
    public void rejectsInvalidStatsInEveryPosition() {
        for (int index = 0; index < 4; index++) {
            for (float invalid : new float[] {0f, -1f, 0.09f, Float.NaN, Float.POSITIVE_INFINITY}) {
                float[] stats = {1f, 1f, 1f, 1f};
                stats[index] = invalid;
                assertThrows(IllegalArgumentException.class, () ->
                        new RatassGame.RlTrainingConfig().withBenchmarkStats(
                                stats[0], stats[1], stats[2], stats[3]));
                assertThrows(IllegalArgumentException.class, () ->
                        new RogueliteCarUpgrades().setBenchmarkStats(
                                stats[0], stats[1], stats[2], stats[3]));
            }
        }
    }
}
