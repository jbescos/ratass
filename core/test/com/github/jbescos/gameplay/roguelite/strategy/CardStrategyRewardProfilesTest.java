package com.github.jbescos.gameplay.roguelite.strategy;

import com.github.jbescos.gameplay.roguelite.RogueliteSlotType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class CardStrategyRewardProfilesTest {
    @Test
    public void balancedProfileKeepsWinningDominant() {
        CardStrategyRewardConfig balanced =
                CardStrategyRewardProfiles.forProfile("strategy00");

        assertEquals(100f, balanced.getChampionshipWin(), 0.001f);
        assertTrue(balanced.getChampionshipWin() > balanced.getFinalPosition());
        for (RogueliteSlotType slot : RogueliteSlotType.values()) {
            assertEquals(0f, balanced.getCardTypeReward(slot), 0.001f);
        }
    }

    @Test
    public void specialistProfilesExposeIndependentAdjustableRewards() {
        assertTrue(CardStrategyRewardProfiles.forProfile("strategy01")
                .getNormalizedExperience() > CardStrategyRewardProfiles.forProfile("strategy00")
                .getNormalizedExperience());
        assertTrue(CardStrategyRewardProfiles.forProfile("strategy02")
                .getCardTypeReward(RogueliteSlotType.TUNING) > 0f);
        assertTrue(CardStrategyRewardProfiles.forProfile("strategy03")
                .getCardTypeReward(RogueliteSlotType.POWERUP) > 0f);
        assertTrue(CardStrategyRewardProfiles.forProfile("strategy04")
                .getCardTypeReward(RogueliteSlotType.REVENGE) > 0f);
        assertTrue(CardStrategyRewardProfiles.forProfile("strategy05")
                .getCardTypeReward(RogueliteSlotType.DRIVER) > 0f);
        assertTrue(CardStrategyRewardProfiles.forProfile("strategy07").getNovelty() > 0f);
        assertTrue(CardStrategyRewardProfiles.forProfile("strategy08")
                .getTechniqueAmplifier() > 0f);
        assertTrue(CardStrategyRewardProfiles.forProfile("strategy08")
                .getPowerupAmplifier() > 0f);
        assertTrue(CardStrategyRewardProfiles.forProfile("strategy08")
                .getRevengeAmplifier() > 0f);
        assertTrue(CardStrategyRewardProfiles.forProfile("strategy08")
                .getAmplifierLink() > 0f);
        assertTrue(CardStrategyRewardProfiles.forProfile("strategy08")
                .getRandomPowerup() > 0f);
        assertTrue(CardStrategyRewardProfiles.forProfile("strategy08")
                .getRandomRevenge() > 0f);
        assertTrue(CardStrategyRewardProfiles.forProfile("strategy09")
                .getChampionshipWin() > CardStrategyRewardProfiles.forProfile("strategy00")
                .getChampionshipWin());
        assertTrue(CardStrategyRewardProfiles.forProfile("strategy10")
                .getCardTypeReward(RogueliteSlotType.REVENGE) > 0f);
        assertTrue(CardStrategyRewardProfiles.forProfile("strategy11")
                .getCardTypeReward(RogueliteSlotType.TECHNIQUE) > 0f);
        assertTrue(CardStrategyRewardProfiles.forProfile("strategy12").getNovelty() > 0f);
    }

    @Test
    public void personalityShapingDoesNotOutweighWinning() {
        for (int index = 0; index <= 12; index++) {
            CardStrategyRewardConfig profile = CardStrategyRewardProfiles.forProfile(
                    String.format("strategy%02d", Integer.valueOf(index)));
            for (RogueliteSlotType slot : RogueliteSlotType.values()) {
                assertTrue(profile.getCardTypeReward(slot) * 3f
                        < profile.getChampionshipWin());
            }
        }
    }
}
