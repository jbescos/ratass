package com.github.jbescos.gameplay.roguelite.strategy;

import com.github.jbescos.gameplay.roguelite.RogueliteSlotType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class CardStrategyRewardProfilesTest {
    @Test
    public void winnerProfileKeepsWinningDominant() {
        CardStrategyRewardConfig winner =
                CardStrategyRewardProfiles.forProfile("strategy00");

        assertEquals(200f, winner.getChampionshipWin(), 0.001f);
        assertTrue(winner.getLapWin() > 0f);
        assertTrue(winner.getChampionshipWin() > winner.getFinalPosition());
        for (RogueliteSlotType slot : RogueliteSlotType.values()) {
            assertEquals(0f, winner.getCardTypeReward(slot), 0.001f);
        }
    }

    @Test
    public void retainedSpecialistsExposeIndependentRewards() {
        assertTrue(CardStrategyRewardProfiles.forProfile("strategy01")
                .getCardSelection() > 0f);
        assertTrue(CardStrategyRewardProfiles.forProfile("strategy01")
                .getCardTypeRotation() > 0f);
        assertTrue(CardStrategyRewardProfiles.forProfile("strategy02")
                .getTuningTechniqueSynergy() > 0f);
        assertTrue(CardStrategyRewardProfiles.forProfile("strategy08")
                .getTechniqueAmplifier() > 0f);
        assertTrue(CardStrategyRewardProfiles.forProfile("strategy08")
                .getPowerupAmplifier() > 0f);
        assertTrue(CardStrategyRewardProfiles.forProfile("strategy08")
                .getRevengeAmplifier() > 0f);
        assertTrue(CardStrategyRewardProfiles.forProfile("strategy08")
                .getAmplifierLink() > 0f);
        assertEquals(0f, CardStrategyRewardProfiles.forProfile("strategy08")
                .getRandomPowerup(), 0.001f);
        assertEquals(0f, CardStrategyRewardProfiles.forProfile("strategy08")
                .getRandomRevenge(), 0.001f);
    }

    @Test
    public void personalityShapingDoesNotOutweighWinning() {
        int[] retained = {0, 1, 2, 8};
        for (int index : retained) {
            CardStrategyRewardConfig profile = CardStrategyRewardProfiles.forProfile(
                    String.format("strategy%02d", Integer.valueOf(index)));
            for (RogueliteSlotType slot : RogueliteSlotType.values()) {
                assertTrue(profile.getCardTypeReward(slot) * 3f
                        < profile.getChampionshipWin());
            }
        }
    }
}
