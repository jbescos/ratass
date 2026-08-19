package com.github.jbescos.gameplay.roguelite.strategy;

import com.github.jbescos.gameplay.roguelite.DriverProfileCatalog;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public final class CardStrategyTrainingEnvironmentTest {
    @Test
    public void completesAHeadlessChampionshipWithStableObservations() {
        CardStrategyTrainingEnvironment environment = environment("strategy00");
        environment.reset(1234L);

        int decisions = 0;
        int observationSize = environment.getObservationSize();
        while (!environment.isDone()) {
            float[][] candidates = environment.getCandidateObservations();
            assertEquals(environment.getActionCount(), candidates.length);
        assertEquals(environment.getActionCount(), environment.getOfferTiers().length);
        assertEquals(environment.getActionCount(), environment.getOfferTypes().length);
        assertEquals(environment.getActionCount(), environment.getTrainingTargetScores().length);
            for (int i = 0; i < candidates.length; i++) {
                assertEquals(observationSize, candidates[i].length);
            }
            environment.step(0);
            decisions++;
            assertTrue("strategy episode did not terminate", decisions < 200);
        }

        assertTrue(decisions > 5);
        assertTrue(environment.getLevel() > 1);
        assertTrue(environment.getTotalExperience() > 0);
        assertTrue(environment.getFinalPosition() >= 1);
        assertTrue(environment.getFinalPosition() <= 10);
        assertFalse(environment.getActionCount() > 0);
    }

    @Test
    public void resetWithSameSeedIsDeterministic() {
        CardStrategyTrainingEnvironment left = environment("strategy00");
        CardStrategyTrainingEnvironment right = environment("strategy00");
        left.reset(991L);
        right.reset(991L);

        for (int decision = 0; decision < 8; decision++) {
            assertEquals(left.isDone(), right.isDone());
            assertEquals(left.getActionCount(), right.getActionCount());
            String[] leftIds = left.getOfferIds();
            String[] rightIds = right.getOfferIds();
            assertEquals(leftIds.length, rightIds.length);
            for (int i = 0; i < leftIds.length; i++) {
                assertEquals(leftIds[i], rightIds[i]);
            }
            left.step(0);
            right.step(0);
        }
    }

    @Test
    public void raceStrengthTeacherAlwaysReturnsALegalAction() {
        CardStrategyTrainingEnvironment environment = environment("strategy00");
        environment.reset(9173L);

        while (!environment.isDone()) {
            int action = environment.getRaceStrengthAction();
            float[] scores = environment.getRaceStrengthScores();
            assertTrue(action >= 0);
            assertTrue(action < environment.getActionCount());
            assertEquals(environment.getActionCount(), scores.length);
            for (int i = 0; i < scores.length; i++) {
                assertTrue(scores[action] >= scores[i]);
            }
            environment.step(action);
        }
    }

    @Test
    public void championshipProvidesSeveralTierThreeDecisions() {
        CardStrategyTrainingEnvironment environment = environment("strategy00");
        environment.reset(8127L);

        int tierThreeDecisions = 0;
        while (!environment.isDone()) {
            if (environment.getLevel() >= 20) {
                tierThreeDecisions++;
            }
            environment.step(environment.getRaceStrengthAction());
        }

        assertTrue(environment.getLevel() > 20);
        assertTrue(tierThreeDecisions >= 3);
    }

    private static CardStrategyTrainingEnvironment environment(String profileId) {
        return new CardStrategyTrainingEnvironment(
                DriverProfileCatalog.fallback(),
                CardStrategyRewardProfiles.forProfile(profileId),
                10,
                19,
                5);
    }
}
