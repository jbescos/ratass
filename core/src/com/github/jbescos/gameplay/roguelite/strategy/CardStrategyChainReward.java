package com.github.jbescos.gameplay.roguelite.strategy;

import com.github.jbescos.gameplay.roguelite.RogueliteCardCatalog;
import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import com.github.jbescos.gameplay.roguelite.RogueliteLoadout;
import com.github.jbescos.gameplay.roguelite.RogueliteSlotType;

/** Scores only the explicit Technique -> Powerup -> Revenge amplifier chain. */
final class CardStrategyChainReward {
    private CardStrategyChainReward() {
    }

    static float selection(
            RogueliteLoadout loadout,
            RogueliteCardId candidate,
            CardStrategyRewardConfig config) {
        if (loadout == null || candidate == null) {
            return 0f;
        }
        float reward = 0f;
        int tier = RogueliteCardCatalog.get(candidate).getTier();
        if (isTechniqueAmplifier(candidate)) {
            reward += config.getTechniqueAmplifier() * tier;
        } else if (isPowerupAmplifier(candidate)) {
            reward += config.getPowerupAmplifier() * tier;
        } else if (isRevengeAmplifier(candidate)) {
            reward += config.getRevengeAmplifier() * tier;
        } else if (isRandomPowerup(candidate)) {
            reward += config.getRandomPowerup() * tier;
        } else if (isRandomRevenge(candidate)) {
            reward += config.getRandomRevenge() * tier;
        }
        int existingLinks = compatibleLinkCount(loadout, null);
        int resultingLinks = compatibleLinkCount(loadout, candidate);
        reward += Math.max(0, resultingLinks - existingLinks) * config.getAmplifierLink();
        return reward;
    }

    private static int compatibleLinkCount(
            RogueliteLoadout loadout,
            RogueliteCardId candidate) {
        RogueliteCardId tuning = card(loadout, candidate, RogueliteSlotType.TUNING);
        RogueliteCardId technique = card(loadout, candidate, RogueliteSlotType.TECHNIQUE);
        RogueliteCardId powerup = card(loadout, candidate, RogueliteSlotType.POWERUP);
        RogueliteCardId revenge = card(loadout, candidate, RogueliteSlotType.REVENGE);
        int links = 0;
        if (isTechniqueAmplifier(tuning) && isPowerupAmplifier(technique)) {
            links++;
        }
        if (isPowerupAmplifier(technique) && isRevengeAmplifier(powerup)) {
            links++;
        }
        if (isPowerupAmplifier(technique) && isRandomPowerup(powerup)) {
            links++;
        }
        if (isRevengeAmplifier(powerup) && isRandomRevenge(revenge)) {
            links++;
        }
        return links;
    }

    private static RogueliteCardId card(
            RogueliteLoadout loadout,
            RogueliteCardId candidate,
            RogueliteSlotType slotType) {
        return candidate != null
                        && RogueliteCardCatalog.get(candidate).getSlotType() == slotType
                ? candidate : loadout.get(slotType);
    }

    private static boolean isTechniqueAmplifier(RogueliteCardId cardId) {
        return cardId == RogueliteCardId.TECHNIQUE_COUPLER
                || cardId == RogueliteCardId.TECHNIQUE_MATRIX
                || cardId == RogueliteCardId.TECHNIQUE_SINGULARITY;
    }

    private static boolean isPowerupAmplifier(RogueliteCardId cardId) {
        return cardId == RogueliteCardId.POWERUP_LINK
                || cardId == RogueliteCardId.POWERUP_MATRIX
                || cardId == RogueliteCardId.POWERUP_NEXUS;
    }

    private static boolean isRevengeAmplifier(RogueliteCardId cardId) {
        return cardId == RogueliteCardId.GRUDGE_SPARK
                || cardId == RogueliteCardId.VENGEANCE_CORE
                || cardId == RogueliteCardId.NEMESIS_ENGINE;
    }

    private static boolean isRandomPowerup(RogueliteCardId cardId) {
        return cardId == RogueliteCardId.LUCKY_SPARK
                || cardId == RogueliteCardId.CHAOS_RELAY
                || cardId == RogueliteCardId.WILDCARD_CORE;
    }

    private static boolean isRandomRevenge(RogueliteCardId cardId) {
        return cardId == RogueliteCardId.LOADED_GRUDGE
                || cardId == RogueliteCardId.CHAOS_RETORT
                || cardId == RogueliteCardId.FATES_REVENGE;
    }
}
