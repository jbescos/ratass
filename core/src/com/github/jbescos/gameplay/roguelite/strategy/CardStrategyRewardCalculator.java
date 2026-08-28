package com.github.jbescos.gameplay.roguelite.strategy;

import com.github.jbescos.gameplay.roguelite.RogueliteCardOffer;
import com.github.jbescos.gameplay.roguelite.RogueliteLoadout;
import com.github.jbescos.gameplay.roguelite.RogueliteSetCatalog;
import com.github.jbescos.gameplay.roguelite.RogueliteSetId;

/** Centralizes reward math so profile weights can be adjusted without changing the simulator. */
final class CardStrategyRewardCalculator {
    private final CardStrategyRewardConfig config;

    CardStrategyRewardCalculator(CardStrategyRewardConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("Card strategy rewards are required.");
        }
        this.config = config;
    }

    float selection(
            RogueliteCardOffer offer,
            RogueliteLoadout loadout,
            int priorSelections,
            int priorTypeSelections,
            float averageTypeSelections,
            float rivalCardOverlap,
            Iterable<RogueliteSetId> enabledSetIds) {
        if (offer == null) {
            return -config.getSkipPenalty();
        }
        float reward = config.getCardSelection()
                + config.getCardTypeReward(offer.getSlotType()) * offer.getTier();
        if (config.getCardTypeRotation() != 0f) {
            reward += config.getCardTypeRotation()
                    * (averageTypeSelections - Math.max(0, priorTypeSelections));
        }
        if (!offer.isDriver()) {
            int setProgress = RogueliteSetCatalog.selectionDepth(
                    loadout, offer.getCard().getId(), enabledSetIds);
            reward += setProgress * config.getSetProgress();
            if (RogueliteSetCatalog.completesSetAfter(
                    loadout, offer.getCard().getId(), enabledSetIds)) {
                reward += config.getSetCompletion();
            }
            reward -= RogueliteSetCatalog.selectionRegression(
                            loadout, offer.getCard().getId(), enabledSetIds)
                    * config.getSetBreakPenalty();
            reward += CardStrategyChainReward.selection(
                    loadout, offer.getCard().getId(), config);
            reward += config.getCardPreferenceReward(loadout, offer.getCard().getId());
            float repetitionScale = 1f / (float) Math.sqrt(1f + Math.max(0, priorSelections));
            reward += TuningTechniqueSynergy.engineerSelectionGain(
                            loadout, offer.getCard().getId())
                    * config.getTuningTechniqueSynergy()
                    * repetitionScale;
            reward -= config.getRivalCardOverlapPenalty(offer.getSlotType())
                    * Math.max(0f, Math.min(1f, rivalCardOverlap));
        }
        if (config.getNovelty() != 0f) {
            reward += config.getNovelty()
                    / (float) Math.sqrt(1f + Math.max(0, priorSelections));
        }
        return reward;
    }

    boolean rewardsSetBuilding() {
        return config.getSetProgress() > 0f || config.getSetCompletion() > 0f;
    }

    float experience(int gained, int experienceForLevel, int levelsGained) {
        float normalized = experienceForLevel <= 0
                ? 0f : gained / (float) experienceForLevel;
        return normalized * config.getNormalizedExperience()
                + Math.max(0, levelsGained) * config.getLevelGain();
    }

    float racePosition(int position, int fieldSize) {
        return normalizedPosition(position, fieldSize) * config.getRacePosition();
    }

    float lapWin(int position) {
        return position == 1 ? config.getLapWin() : 0f;
    }

    float championship(int position, int fieldSize) {
        float reward = normalizedPosition(position, fieldSize) * config.getFinalPosition();
        return position == 1 ? reward + config.getChampionshipWin() : reward;
    }

    private static float normalizedPosition(int position, int fieldSize) {
        if (position <= 0 || fieldSize <= 1) {
            return 0f;
        }
        int clamped = Math.max(1, Math.min(fieldSize, position));
        return (fieldSize - clamped) / (float) (fieldSize - 1);
    }
}
