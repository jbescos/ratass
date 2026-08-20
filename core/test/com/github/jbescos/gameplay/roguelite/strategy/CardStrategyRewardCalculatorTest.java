package com.github.jbescos.gameplay.roguelite.strategy;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class CardStrategyRewardCalculatorTest {
    @Test
    public void explorerRewardsSelectingAndPenalizesSkipping() {
        CardStrategyRewardConfig config = CardStrategyRewardProfiles.forProfile("strategy01");

        assertTrue(config.getCardSelection() > 0f);
        assertTrue(config.getSkipPenalty() > 0f);
    }

    @Test
    public void winnerOnlyGetsLapRewardForFirstPlace() {
        CardStrategyRewardCalculator calculator = new CardStrategyRewardCalculator(
                CardStrategyRewardProfiles.forProfile("strategy00"));

        assertTrue(calculator.lapWin(1) > 0f);
        assertEquals(0f, calculator.lapWin(2), 0.001f);
    }
}
