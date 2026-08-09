package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class RogueliteProgressionBalanceTest {
    private static final int CIRCUITS = 19;
    private static final int FIELD_SIZE = 10;

    @Test
    public void representativeChampionshipUsesConfiguredTierLevelThresholds() {
        SimulationResult result = simulateRepresentativeChampionship(1, 3);

        assertEquals(23, result.level);
        assertEquals(RogueliteRun.TIER_TWO_LEVEL - 2, result.selectionsByTier[1]);
        assertEquals(
                RogueliteRun.TIER_THREE_LEVEL - RogueliteRun.TIER_TWO_LEVEL,
                result.selectionsByTier[2]);
        assertEquals(
                result.level - RogueliteRun.TIER_THREE_LEVEL + 1,
                result.selectionsByTier[3]);
    }

    @Test
    public void startingTierDoesNotChangeExperienceProgression() {
        SimulationResult normal = simulateRepresentativeChampionship(1, 3);
        SimulationResult tierTwo = simulateRepresentativeChampionship(2, 3);
        SimulationResult tierThree = simulateRepresentativeChampionship(3, 3);

        assertEquals(normal.level, tierTwo.level);
        assertEquals(normal.level, tierThree.level);
        assertEquals(normal.experience, tierTwo.experience);
        assertEquals(normal.experience, tierThree.experience);
        assertEquals(normal.experienceForNextLevel, tierTwo.experienceForNextLevel);
        assertEquals(normal.experienceForNextLevel, tierThree.experienceForNextLevel);
    }

    private static SimulationResult simulateRepresentativeChampionship(
            int startingTier, int finishingPosition) {
        RogueliteRun run = new RogueliteRun(8026L + startingTier);
        run.reset(startingTier);
        int[] selectionsByTier = new int[4];

        for (int circuit = 1; circuit <= CIRCUITS; circuit++) {
            for (int lap = 0; lap < CustomGameRules.DEFAULT_LAPS; lap++) {
                awardAndResolve(run, RogueliteExperienceAwards.Reason.OVERTAKE, selectionsByTier);
                if (lap == CustomGameRules.DEFAULT_LAPS - 1) {
                    awardAndResolve(
                            run,
                            RogueliteExperienceAwards.Reason.FASTEST_LAP,
                            selectionsByTier);
                }
                if (lap == 0) {
                    awardAndResolve(run, RogueliteExperienceAwards.Reason.REVENGE, selectionsByTier);
                }
                if (circuit % 2 == 0 && lap == 1) {
                    awardAndResolve(
                            run,
                            RogueliteExperienceAwards.Reason.PUSH_OFF_ROAD,
                            selectionsByTier);
                }
                int driftSeconds = lap == CustomGameRules.DEFAULT_LAPS - 1 ? 4 : 3;
                for (int second = 0; second < driftSeconds; second++) {
                    awardAndResolve(run, RogueliteExperienceAwards.Reason.DRIFT, selectionsByTier);
                }
                run.resetPlayerLapExperience();
            }
            run.awardPlayerRacePosition(finishingPosition, FIELD_SIZE);
            resolveReward(run, selectionsByTier);
        }

        RogueliteCompetitorProgress progress = run.getPlayerProgress();
        return new SimulationResult(
                progress.getLevel(),
                progress.getExperience(),
                progress.getExperienceForNextLevel(),
                selectionsByTier);
    }

    private static void awardAndResolve(
            RogueliteRun run,
            RogueliteExperienceAwards.Reason reason,
            int[] selectionsByTier) {
        run.awardPlayerRacecraftExperience(reason, run.getRacecraftXpAward(reason));
        resolveReward(run, selectionsByTier);
    }

    private static void resolveReward(RogueliteRun run, int[] selectionsByTier) {
        if (!run.getPlayerProgress().hasPendingReward()) {
            return;
        }
        List<RogueliteCardOffer> offers = run.createOffers(3);
        assertFalse(offers.isEmpty());
        int tier = offers.get(0).getTier();
        selectionsByTier[tier]++;
        assertTrue(run.select(offers.get(0)));
    }

    private static final class SimulationResult {
        private final int level;
        private final int experience;
        private final int experienceForNextLevel;
        private final int[] selectionsByTier;

        private SimulationResult(
                int level,
                int experience,
                int experienceForNextLevel,
                int[] selectionsByTier) {
            this.level = level;
            this.experience = experience;
            this.experienceForNextLevel = experienceForNextLevel;
            this.selectionsByTier = selectionsByTier;
        }
    }
}
