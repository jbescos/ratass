package com.github.jbescos.gameplay.roguelite.strategy;

import com.github.jbescos.gameplay.roguelite.RogueliteCardCatalog;
import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import com.github.jbescos.gameplay.roguelite.RogueliteLoadout;
import com.github.jbescos.gameplay.roguelite.RogueliteSlotType;
import com.github.jbescos.gameplay.roguelite.RogueliteStrategyMetrics;

/** Shared tuning-to-Technique compatibility calculation for selectors and training rewards. */
final class TuningTechniqueSynergy {
    private TuningTechniqueSynergy() {
    }

    static float selectionGain(RogueliteLoadout loadout, RogueliteCardId candidate) {
        return selectionGain(loadout, candidate, false);
    }

    static float statSelectionGain(RogueliteLoadout loadout, RogueliteCardId candidate) {
        return selectionGain(loadout, candidate, true);
    }

    private static float selectionGain(
            RogueliteLoadout loadout,
            RogueliteCardId candidate,
            boolean statCardsOnly) {
        if (loadout == null || candidate == null) {
            return 0f;
        }
        RogueliteSlotType candidateSlot = RogueliteCardCatalog.get(candidate).getSlotType();
        if (candidateSlot != RogueliteSlotType.TUNING
                && candidateSlot != RogueliteSlotType.TECHNIQUE) {
            return 0f;
        }
        RogueliteCardId currentTuning = loadout.get(RogueliteSlotType.TUNING);
        RogueliteCardId currentTechnique = loadout.get(RogueliteSlotType.TECHNIQUE);
        RogueliteCardId candidateTuning = candidateSlot == RogueliteSlotType.TUNING
                ? candidate : currentTuning;
        RogueliteCardId candidateTechnique = candidateSlot == RogueliteSlotType.TECHNIQUE
                ? candidate : currentTechnique;
        if (statCardsOnly && isAmplifierPair(candidateTuning, candidateTechnique)) {
            return 0f;
        }

        float candidateScore = RogueliteStrategyMetrics.tuningTechniqueScore(
                candidateTuning, candidateTechnique);
        if (!isFinite(candidateScore)) {
            return 0f;
        }
        float currentScore = RogueliteStrategyMetrics.tuningTechniqueScore(
                currentTuning, currentTechnique);
        if (!isFinite(currentScore)) {
            currentScore = RogueliteStrategyMetrics.tuningBaselineScore(currentTuning);
        }
        if (!isFinite(currentScore)) {
            currentScore = RogueliteStrategyMetrics.tuningBaselineScore(candidateTuning);
        }
        return isFinite(currentScore) ? candidateScore - currentScore : 0f;
    }

    private static boolean isAmplifierPair(
            RogueliteCardId tuning,
            RogueliteCardId technique) {
        return RogueliteStrategyMetrics.techniqueEffectMultiplier(tuning) > 1f
                || RogueliteStrategyMetrics.powerupEffectMultiplier(technique) > 1f;
    }

    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }
}
