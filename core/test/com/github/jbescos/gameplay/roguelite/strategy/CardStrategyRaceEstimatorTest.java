package com.github.jbescos.gameplay.roguelite.strategy;

import com.github.jbescos.gameplay.roguelite.DriverProfileCatalog;
import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import com.github.jbescos.gameplay.roguelite.RogueliteCompetitorProgress;
import com.github.jbescos.gameplay.roguelite.RogueliteRun;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

public final class CardStrategyRaceEstimatorTest {
    private final DriverProfileCatalog drivers = DriverProfileCatalog.fallback();
    private final CardStrategyRaceEstimator estimator = new CardStrategyRaceEstimator(drivers);

    @Test
    public void completedAmplifierStagesRaiseEstimatedRaceStrength() {
        float base = estimate(
                RogueliteCardId.POWERUP_LINK,
                RogueliteCardId.GRUDGE_SPARK,
                RogueliteCardId.LOADED_GRUDGE);
        float withTechniqueAmplifier = estimate(
                RogueliteCardId.TECHNIQUE_COUPLER,
                RogueliteCardId.POWERUP_LINK,
                RogueliteCardId.GRUDGE_SPARK,
                RogueliteCardId.LOADED_GRUDGE);
        float withoutRevengeAmplifier = estimate(
                RogueliteCardId.TECHNIQUE_COUPLER,
                RogueliteCardId.POWERUP_LINK,
                RogueliteCardId.LUCKY_SPARK,
                RogueliteCardId.LOADED_GRUDGE);

        assertTrue(withTechniqueAmplifier > base);
        assertTrue(withTechniqueAmplifier > withoutRevengeAmplifier);
    }

    @Test
    public void techniqueAmplifierStrengthensRandomPowerups() {
        float relay = estimate(
                RogueliteCardId.POWERUP_LINK,
                RogueliteCardId.LUCKY_SPARK);
        float amplifiedRelay = estimate(
                RogueliteCardId.TECHNIQUE_COUPLER,
                RogueliteCardId.POWERUP_LINK,
                RogueliteCardId.LUCKY_SPARK);

        assertTrue(amplifiedRelay > relay);
    }

    @Test
    public void longerBuildSuppressionRaisesEstimatedRaceStrength() {
        float shortSuppression = estimate(RogueliteCardId.TELEMETRY_THEFT);
        float longSuppression = estimate(RogueliteCardId.APEX_PLUNDER);

        assertTrue(longSuppression > shortSuppression);
    }

    private float estimate(RogueliteCardId... cards) {
        RogueliteRun run = new RogueliteRun(19L, drivers);
        RogueliteCompetitorProgress progress = run.getPlayerProgress();
        for (RogueliteCardId card : cards) {
            progress.getLoadout().equip(card);
        }
        return estimator.estimate(progress, 1.45f);
    }
}
