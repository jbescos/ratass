package com.github.jbescos.gameplay.roguelite.strategy;

import com.github.jbescos.gameplay.roguelite.CardAmplifierChain;
import com.github.jbescos.gameplay.roguelite.AntennaNetworkBonuses;
import com.github.jbescos.gameplay.roguelite.AntennaPowerupSpec;
import com.github.jbescos.gameplay.roguelite.DriverProfileCatalog;
import com.github.jbescos.gameplay.roguelite.DriverProfileMetadata;
import com.github.jbescos.gameplay.roguelite.RogueliteCarStatSnapshot;
import com.github.jbescos.gameplay.roguelite.RogueliteCardCatalog;
import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import com.github.jbescos.gameplay.roguelite.RogueliteCardOffer;
import com.github.jbescos.gameplay.roguelite.RogueliteCompetitorProgress;
import com.github.jbescos.gameplay.roguelite.RogueliteLoadout;
import com.github.jbescos.gameplay.roguelite.RogueliteSlotType;
import com.github.jbescos.gameplay.roguelite.RogueliteStrategyMetrics;
import com.github.jbescos.gameplay.roguelite.RivalBuildLeechSpec;

/** Fast strategic estimate used only for card-policy training and benchmarking. */
final class CardStrategyRaceEstimator {
    private final DriverProfileCatalog driverCatalog;
    private final float bestLap;
    private final float worstLap;

    CardStrategyRaceEstimator(DriverProfileCatalog driverCatalog) {
        this.driverCatalog = driverCatalog;
        float best = Float.POSITIVE_INFINITY;
        float worst = 0f;
        for (DriverProfileMetadata driver : driverCatalog.all()) {
            float lap = driver.getAverageLapSeconds();
            if (validLap(lap)) {
                best = Math.min(best, lap);
                worst = Math.max(worst, lap);
            }
        }
        bestLap = best == Float.POSITIVE_INFINITY ? 0f : best;
        worstLap = worst;
    }

    float estimate(
            RogueliteCompetitorProgress progress,
            float gripWeight) {
        return estimate(progress, null, gripWeight, AntennaNetworkBonuses.NONE);
    }

    float estimate(
            RogueliteCompetitorProgress progress,
            float gripWeight,
            AntennaNetworkBonuses antennaNetwork) {
        return estimate(progress, null, gripWeight, antennaNetwork);
    }

    float estimate(
            RogueliteCompetitorProgress progress,
            RogueliteCardOffer preview,
            float gripWeight) {
        return estimate(progress, preview, gripWeight, AntennaNetworkBonuses.NONE);
    }

    float estimate(
            RogueliteCompetitorProgress progress,
            RogueliteCardOffer preview,
            float gripWeight,
            AntennaNetworkBonuses antennaNetwork) {
        RogueliteLoadout loadout = progress.getLoadout();
        DriverProfileMetadata driver = preview != null && preview.isDriver()
                ? preview.getDriver()
                : driverCatalog.get(loadout.getDriverProfileId());
        RogueliteCardId previewCard = preview == null || preview.isDriver()
                ? null
                : preview.getCard().getId();
        RogueliteCarStatSnapshot stats = RogueliteCarStatSnapshot.from(
                loadout, previewCard, antennaNetwork);
        float value = driverQuality(driver) * 2f;
        value += (stats.getAccelerationMultiplier() - 1f) * 1.50f;
        value += (stats.getMaxSpeedMultiplier() - 1f) * 1.10f;
        value += (stats.getGripMultiplier() - 1f) * gripWeight;
        value += (stats.getAerodynamicEfficiency() - 1f) * 1.15f;
        value += (1f - stats.getMassMultiplier()) * 0.85f;

        RogueliteCardId tuning = loadout.get(RogueliteSlotType.TUNING);
        RogueliteCardId technique = loadout.get(RogueliteSlotType.TECHNIQUE);
        if (preview != null && !preview.isDriver()) {
            if (preview.getSlotType() == RogueliteSlotType.TUNING) {
                tuning = previewCard;
            } else if (preview.getSlotType() == RogueliteSlotType.TECHNIQUE) {
                technique = previewCard;
            }
        }
        float synergy = RogueliteStrategyMetrics.tuningTechniqueScore(tuning, technique);
        if (!Float.isNaN(synergy) && !Float.isInfinite(synergy)) {
            value += synergy * 0.18f;
        }
        RogueliteCardId powerup = loadout.get(RogueliteSlotType.POWERUP);
        RogueliteCardId revenge = loadout.get(RogueliteSlotType.REVENGE);
        if (preview != null && !preview.isDriver()) {
            if (preview.getSlotType() == RogueliteSlotType.POWERUP) {
                powerup = previewCard;
            } else if (preview.getSlotType() == RogueliteSlotType.REVENGE) {
                revenge = previewCard;
            }
        }
        if (AntennaPowerupSpec.sharesTechnique(powerup)) {
            value += antennaNetwork.techniquePerformanceGain(technique) * 0.18f;
        }
        float techniqueEffectMultiplier =
                RogueliteStrategyMetrics.techniqueEffectMultiplier(tuning);
        float powerupEffectMultiplier = CardAmplifierChain.combine(
                RogueliteStrategyMetrics.powerupEffectMultiplier(technique),
                techniqueEffectMultiplier);
        float revengeEffectMultiplier = CardAmplifierChain.combine(
                RogueliteStrategyMetrics.revengeEffectMultiplier(powerup),
                powerupEffectMultiplier);
        value += activeCardValue(powerup, 0.10f) * powerupEffectMultiplier;
        value += activeCardValue(revenge, 0.08f) * revengeEffectMultiplier;
        return value;
    }

    private float driverQuality(DriverProfileMetadata driver) {
        if (driver == null) {
            return 0f;
        }
        float lap = driver.getAverageLapSeconds();
        if (validLap(lap) && worstLap > bestLap) {
            return clamp01((worstLap - lap) / (worstLap - bestLap));
        }
        return clamp01(
                (driver.getPaceRating() * 0.45f
                                + driver.getControlRating() * 0.20f
                                + driver.getConsistencyRating() * 0.15f
                                + driver.getFinishRate() * 100f * 0.20f)
                        / 100f);
    }

    private static float activeCardValue(RogueliteCardId cardId, float tierWeight) {
        if (cardId == null) {
            return 0f;
        }
        float value = RogueliteCardCatalog.get(cardId).getTier() * tierWeight;
        if (RivalBuildLeechSpec.isCard(cardId)) {
            value += RivalBuildLeechSpec.expectedLapTransferFraction(cardId) * 0.12f;
            value += RivalBuildLeechSpec.expectedBuildSuppressionFraction(cardId) * 0.12f;
        }
        return value;
    }

    private static boolean validLap(float lap) {
        return lap > 0f && !Float.isNaN(lap) && !Float.isInfinite(lap);
    }

    private static float clamp01(float value) {
        return Math.max(0f, Math.min(1f, value));
    }
}
