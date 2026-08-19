package com.github.jbescos.gameplay.roguelite.strategy;

import com.github.jbescos.gameplay.roguelite.RogueliteCardOffer;
import com.github.jbescos.gameplay.roguelite.RogueliteLoadout;

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
            int priorSelections) {
        if (offer == null) {
            return -config.getSkipPenalty();
        }
        float reward = config.getCardTypeReward(offer.getSlotType()) * offer.getTier();
        if (!offer.isDriver()) {
            reward += CardStrategyChainReward.selection(
                    loadout, offer.getCard().getId(), config);
        }
        if (config.getNovelty() != 0f) {
            reward += config.getNovelty()
                    / (float) Math.sqrt(1f + Math.max(0, priorSelections));
        }
        return reward;
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
