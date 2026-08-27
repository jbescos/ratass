package com.github.jbescos.gameplay.roguelite.strategy;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import com.github.jbescos.gameplay.roguelite.RogueliteLoadout;
import com.github.jbescos.gameplay.roguelite.RogueliteSlotType;
import java.util.EnumMap;
import java.util.Map;

/** Adjustable reward weights for one strategic card-selection profile. */
public final class CardStrategyRewardConfig {
    private final float championshipWin;
    private final float finalPosition;
    private final float racePosition;
    private final float levelGain;
    private final float normalizedExperience;
    private final float novelty;
    private final float skipPenalty;
    private final float techniqueAmplifier;
    private final float powerupAmplifier;
    private final float revengeAmplifier;
    private final float amplifierLink;
    private final float randomPowerup;
    private final float randomRevenge;
    private final float lapWin;
    private final float cardSelection;
    private final float tuningTechniqueSynergy;
    private final float cardTypeRotation;
    private final float rivalPowerupOverlapPenalty;
    private final float rivalRevengeOverlapPenalty;
    private final CardStrategyCardPreference cardPreference;
    private final Map<RogueliteSlotType, Float> cardTypeRewards;

    public CardStrategyRewardConfig(
            float championshipWin,
            float finalPosition,
            float racePosition,
            float levelGain,
            float normalizedExperience,
            float novelty,
            float skipPenalty,
            float driver,
            float tuning,
            float technique,
            float powerup,
            float revenge) {
        this(
                championshipWin,
                finalPosition,
                racePosition,
                levelGain,
                normalizedExperience,
                novelty,
                skipPenalty,
                driver,
                tuning,
                technique,
                powerup,
                revenge,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                "",
                0f,
                "",
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f);
    }

    public CardStrategyRewardConfig(
            float championshipWin,
            float finalPosition,
            float racePosition,
            float levelGain,
            float normalizedExperience,
            float novelty,
            float skipPenalty,
            float driver,
            float tuning,
            float technique,
            float powerup,
            float revenge,
            float techniqueAmplifier,
            float powerupAmplifier,
            float revengeAmplifier,
            float amplifierLink,
            float randomPowerup,
            float randomRevenge) {
        this(
                championshipWin,
                finalPosition,
                racePosition,
                levelGain,
                normalizedExperience,
                novelty,
                skipPenalty,
                driver,
                tuning,
                technique,
                powerup,
                revenge,
                techniqueAmplifier,
                powerupAmplifier,
                revengeAmplifier,
                amplifierLink,
                randomPowerup,
                randomRevenge,
                "",
                0f,
                "",
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f);
    }

    public CardStrategyRewardConfig(
            float championshipWin,
            float finalPosition,
            float racePosition,
            float levelGain,
            float normalizedExperience,
            float novelty,
            float skipPenalty,
            float driver,
            float tuning,
            float technique,
            float powerup,
            float revenge,
            float techniqueAmplifier,
            float powerupAmplifier,
            float revengeAmplifier,
            float amplifierLink,
            float randomPowerup,
            float randomRevenge,
            String preferredCardIds,
            float preferredCardReward,
            String discouragedCardIds,
            float discouragedCardPenalty,
            float lapWin,
            float cardSelection,
            float tuningTechniqueSynergy,
            float cardTypeRotation,
            float rivalPowerupOverlapPenalty,
            float rivalRevengeOverlapPenalty) {
        this.championshipWin = finite(championshipWin);
        this.finalPosition = finite(finalPosition);
        this.racePosition = finite(racePosition);
        this.levelGain = finite(levelGain);
        this.normalizedExperience = finite(normalizedExperience);
        this.novelty = finite(novelty);
        this.skipPenalty = Math.max(0f, finite(skipPenalty));
        this.techniqueAmplifier = finite(techniqueAmplifier);
        this.powerupAmplifier = finite(powerupAmplifier);
        this.revengeAmplifier = finite(revengeAmplifier);
        this.amplifierLink = finite(amplifierLink);
        this.randomPowerup = finite(randomPowerup);
        this.randomRevenge = finite(randomRevenge);
        this.lapWin = finite(lapWin);
        this.cardSelection = finite(cardSelection);
        this.tuningTechniqueSynergy = finite(tuningTechniqueSynergy);
        this.cardTypeRotation = finite(cardTypeRotation);
        this.rivalPowerupOverlapPenalty = Math.max(
                0f, finite(rivalPowerupOverlapPenalty));
        this.rivalRevengeOverlapPenalty = Math.max(
                0f, finite(rivalRevengeOverlapPenalty));
        cardPreference = new CardStrategyCardPreference(
                preferredCardIds,
                preferredCardReward,
                discouragedCardIds,
                discouragedCardPenalty);
        cardTypeRewards = new EnumMap<RogueliteSlotType, Float>(RogueliteSlotType.class);
        cardTypeRewards.put(RogueliteSlotType.DRIVER, Float.valueOf(finite(driver)));
        cardTypeRewards.put(RogueliteSlotType.TUNING, Float.valueOf(finite(tuning)));
        cardTypeRewards.put(RogueliteSlotType.TECHNIQUE, Float.valueOf(finite(technique)));
        cardTypeRewards.put(RogueliteSlotType.POWERUP, Float.valueOf(finite(powerup)));
        cardTypeRewards.put(RogueliteSlotType.REVENGE, Float.valueOf(finite(revenge)));
    }

    public float getChampionshipWin() {
        return championshipWin;
    }

    public float getFinalPosition() {
        return finalPosition;
    }

    public float getRacePosition() {
        return racePosition;
    }

    public float getLevelGain() {
        return levelGain;
    }

    public float getNormalizedExperience() {
        return normalizedExperience;
    }

    public float getNovelty() {
        return novelty;
    }

    public float getSkipPenalty() {
        return skipPenalty;
    }

    public float getTechniqueAmplifier() {
        return techniqueAmplifier;
    }

    public float getPowerupAmplifier() {
        return powerupAmplifier;
    }

    public float getRevengeAmplifier() {
        return revengeAmplifier;
    }

    public float getAmplifierLink() {
        return amplifierLink;
    }

    public float getRandomPowerup() {
        return randomPowerup;
    }

    public float getRandomRevenge() {
        return randomRevenge;
    }

    public float getLapWin() {
        return lapWin;
    }

    public float getCardSelection() {
        return cardSelection;
    }

    public float getTuningTechniqueSynergy() {
        return tuningTechniqueSynergy;
    }

    public float getCardTypeRotation() {
        return cardTypeRotation;
    }

    public float getRivalCardOverlapPenalty(RogueliteSlotType slotType) {
        if (slotType == RogueliteSlotType.POWERUP) {
            return rivalPowerupOverlapPenalty;
        }
        if (slotType == RogueliteSlotType.REVENGE) {
            return rivalRevengeOverlapPenalty;
        }
        return 0f;
    }

    public float getCardPreferenceReward(RogueliteLoadout loadout, RogueliteCardId cardId) {
        return cardPreference.reward(loadout, cardId);
    }

    public float getCardTypeReward(RogueliteSlotType slotType) {
        Float value = cardTypeRewards.get(slotType);
        return value == null ? 0f : value.floatValue();
    }

    private static float finite(float value) {
        return Float.isNaN(value) || Float.isInfinite(value) ? 0f : value;
    }
}
