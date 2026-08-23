package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.List;

/** Immutable antenna broadcasts, resolved against each receiving car's own build. */
public final class AntennaNetworkBonuses {
    private static final int POWER = 0;
    private static final int GRIP = 1;
    private static final int AERO = 2;
    private static final int MASS = 3;

    public static final AntennaNetworkBonuses NONE = new AntennaNetworkBonuses(
            0f,
            0f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            1f,
            false,
            false,
            false,
            false,
            new RogueliteCardId[0],
            false);

    private final float powerBonus;
    private final float gripBonus;
    private final float aerodynamicEfficiency;
    private final float massMultiplier;
    private final float powerTechniqueScale;
    private final float gripTechniqueScale;
    private final float aeroTechniqueScale;
    private final float massTechniqueScale;
    private final float lapExperienceBankMultiplier;
    private final boolean powerTuningFound;
    private final boolean gripTuningFound;
    private final boolean aeroTuningFound;
    private final boolean massTuningFound;
    private final RogueliteCardId[] techniqueCards;
    private final boolean recipientResolved;

    private AntennaNetworkBonuses(
            float powerBonus,
            float gripBonus,
            float aerodynamicEfficiency,
            float massMultiplier,
            float powerTechniqueScale,
            float gripTechniqueScale,
            float aeroTechniqueScale,
            float massTechniqueScale,
            float lapExperienceBankMultiplier,
            boolean powerTuningFound,
            boolean gripTuningFound,
            boolean aeroTuningFound,
            boolean massTuningFound,
            RogueliteCardId[] techniqueCards,
            boolean recipientResolved) {
        this.powerBonus = powerBonus;
        this.gripBonus = gripBonus;
        this.aerodynamicEfficiency = aerodynamicEfficiency;
        this.massMultiplier = massMultiplier;
        this.powerTechniqueScale = powerTechniqueScale;
        this.gripTechniqueScale = gripTechniqueScale;
        this.aeroTechniqueScale = aeroTechniqueScale;
        this.massTechniqueScale = massTechniqueScale;
        this.lapExperienceBankMultiplier = lapExperienceBankMultiplier;
        this.powerTuningFound = powerTuningFound;
        this.gripTuningFound = gripTuningFound;
        this.aeroTuningFound = aeroTuningFound;
        this.massTuningFound = massTuningFound;
        this.techniqueCards = techniqueCards;
        this.recipientResolved = recipientResolved;
    }

    public static Builder builder() {
        return new Builder();
    }

    public float getPowerBonus() {
        return powerBonus;
    }

    public float getGripBonus() {
        return gripBonus;
    }

    public float getAerodynamicEfficiency() {
        return aerodynamicEfficiency;
    }

    public float getMassMultiplier() {
        return massMultiplier;
    }

    public float getPowerTechniqueScale() {
        return powerTechniqueScale;
    }

    public float getGripTechniqueScale() {
        return gripTechniqueScale;
    }

    public float getAeroTechniqueScale() {
        return aeroTechniqueScale;
    }

    public float getMassTechniqueScale() {
        return massTechniqueScale;
    }

    public float getLapExperienceBankMultiplier() {
        return lapExperienceBankMultiplier;
    }

    /**
     * Keeps the receiver's own card and imports its strongest missing network values.
     * T1 imports one Tuning attribute, T2 imports two, and T3 imports two plus one
     * Technique card's multipliers.
     */
    public AntennaNetworkBonuses forRecipient(
            RogueliteCardId tuningCardId,
            RogueliteCardId techniqueCardId,
            RogueliteCardId antennaCardId) {
        if (!AntennaPowerupSpec.isAntennaCard(antennaCardId)) {
            return NONE;
        }
        if (recipientResolved) {
            return this;
        }

        float localPower = 0f;
        float localGrip = 0f;
        float localAero = 1f;
        float localMass = 1f;
        boolean localPowerFound = false;
        boolean localGripFound = false;
        boolean localAeroFound = false;
        boolean localMassFound = false;
        if (isTuningCard(tuningCardId) && !isTuningAmplifier(tuningCardId)) {
            TieredTuningEffect tuning = new TieredTuningEffect(tuningCardId);
            if (tuning.modifiesPower()) {
                localPower = tuning.accelerationBonus();
                localPowerFound = true;
            }
            if (tuning.modifiesGrip()) {
                localGrip = tuning.gripBonus(0f);
                localGripFound = true;
            }
            if (tuning.modifiesAero()) {
                localAero = 1f / Math.max(0.01f, tuning.dragMultiplier());
                localAeroFound = true;
            }
            if (tuning.modifiesMass()) {
                localMass = tuning.massMultiplier();
                localMassFound = true;
            }
        }

        float[] values = {localPower, localGrip, localAero, localMass};
        boolean[] localFound = {
            localPowerFound, localGripFound, localAeroFound, localMassFound
        };
        boolean[] networkFound = {
            powerTuningFound, gripTuningFound, aeroTuningFound, massTuningFound
        };
        float[] networkValues = {
            powerBonus, gripBonus, aerodynamicEfficiency, massMultiplier
        };
        boolean[] selected = new boolean[4];
        int importCount = AntennaPowerupSpec.sharedTuningAttributeCount(antennaCardId);
        for (int imported = 0; imported < importCount; imported++) {
            int bestStat = -1;
            float bestScore = 0f;
            for (int stat = 0; stat < networkValues.length; stat++) {
                if (selected[stat]
                        || !networkFound[stat]
                        || !improves(stat, networkValues[stat], localFound[stat], values[stat])) {
                    continue;
                }
                float score = benefit(stat, networkValues[stat]);
                if (score > bestScore) {
                    bestScore = score;
                    bestStat = stat;
                }
            }
            if (bestStat < 0) {
                break;
            }
            selected[bestStat] = true;
            values[bestStat] = networkValues[bestStat];
            localFound[bestStat] = true;
        }

        float localPowerTechnique = techniqueScale(techniqueCardId, POWER);
        float localGripTechnique = techniqueScale(techniqueCardId, GRIP);
        float localAeroTechnique = techniqueScale(techniqueCardId, AERO);
        float localMassTechnique = techniqueScale(techniqueCardId, MASS);
        float localLapExperience = LapExperienceTechniqueEffect.multiplierFor(techniqueCardId);
        if (AntennaPowerupSpec.sharesTechnique(antennaCardId)) {
            RogueliteCardId importedTechnique = bestImportedTechnique(techniqueCardId);
            if (importedTechnique != null) {
                localPowerTechnique = Math.max(
                        localPowerTechnique, techniqueScale(importedTechnique, POWER));
                localGripTechnique = Math.max(
                        localGripTechnique, techniqueScale(importedTechnique, GRIP));
                localAeroTechnique = Math.max(
                        localAeroTechnique, techniqueScale(importedTechnique, AERO));
                localMassTechnique = Math.max(
                        localMassTechnique, techniqueScale(importedTechnique, MASS));
                localLapExperience = Math.max(
                        localLapExperience,
                        LapExperienceTechniqueEffect.multiplierFor(importedTechnique));
            }
        }

        return new AntennaNetworkBonuses(
                values[POWER],
                values[GRIP],
                values[AERO],
                values[MASS],
                localPowerTechnique,
                localGripTechnique,
                localAeroTechnique,
                localMassTechnique,
                localLapExperience,
                localFound[POWER],
                localFound[GRIP],
                localFound[AERO],
                localFound[MASS],
                new RogueliteCardId[0],
                true);
    }

    public float techniquePerformanceGain(RogueliteCardId techniqueCardId) {
        if (!RaceTechniqueEffect.hasMultiplicativeStats(techniqueCardId)) {
            return 0f;
        }
        int mask = RaceTechniqueEffect.amplifiedStatMask(techniqueCardId);
        float gain = 0f;
        if ((mask & RaceTechniqueEffect.POWER_STAT) != 0) {
            gain += powerTechniqueScale - 1f;
        }
        if ((mask & RaceTechniqueEffect.GRIP_STAT) != 0) {
            gain += gripTechniqueScale - 1f;
        }
        if ((mask & RaceTechniqueEffect.AERO_STAT) != 0) {
            gain += aeroTechniqueScale - 1f;
        }
        if ((mask & RaceTechniqueEffect.MASS_STAT) != 0) {
            gain += massTechniqueScale - 1f;
        }
        return gain;
    }

    public static final class Builder {
        private float powerBonus;
        private float gripBonus;
        private float aerodynamicEfficiency = 1f;
        private float massMultiplier = 1f;
        private float powerTechniqueScale = 1f;
        private float gripTechniqueScale = 1f;
        private float aeroTechniqueScale = 1f;
        private float massTechniqueScale = 1f;
        private float lapExperienceBankMultiplier = 1f;
        private boolean participantFound;
        private boolean powerTuningFound;
        private boolean gripTuningFound;
        private boolean aeroTuningFound;
        private boolean massTuningFound;
        private final List<RogueliteCardId> techniqueCards =
                new ArrayList<RogueliteCardId>();

        public Builder include(RogueliteLoadout loadout) {
            return include(loadout, null);
        }

        public Builder includeActive(
                RogueliteLoadout loadout,
                RogueliteCardId activeAntennaCardId) {
            return includeActive(loadout, activeAntennaCardId, true);
        }

        public Builder includeActive(
                RogueliteLoadout loadout,
                RogueliteCardId activeAntennaCardId,
                boolean includeBuildCards) {
            if (loadout == null
                    || !AntennaPowerupSpec.isAntennaCard(activeAntennaCardId)) {
                return this;
            }
            participantFound = true;
            if (includeBuildCards) {
                includeTuning(loadout.get(RogueliteSlotType.TUNING));
                includeTechnique(loadout.get(RogueliteSlotType.TECHNIQUE));
            }
            return this;
        }

        public Builder include(
                RogueliteLoadout loadout,
                RogueliteCardId previewCard) {
            if (loadout == null) {
                return this;
            }
            RogueliteCardId antenna = cardInSlot(
                    loadout, previewCard, RogueliteSlotType.POWERUP);
            if (!AntennaPowerupSpec.isAntennaCard(antenna)) {
                return this;
            }
            participantFound = true;
            includeTuning(cardInSlot(loadout, previewCard, RogueliteSlotType.TUNING));
            includeTechnique(cardInSlot(loadout, previewCard, RogueliteSlotType.TECHNIQUE));
            return this;
        }

        public AntennaNetworkBonuses build() {
            if (!participantFound) {
                return NONE;
            }
            return new AntennaNetworkBonuses(
                    powerBonus,
                    gripBonus,
                    aerodynamicEfficiency,
                    massMultiplier,
                    powerTechniqueScale,
                    gripTechniqueScale,
                    aeroTechniqueScale,
                    massTechniqueScale,
                    lapExperienceBankMultiplier,
                    powerTuningFound,
                    gripTuningFound,
                    aeroTuningFound,
                    massTuningFound,
                    techniqueCards.toArray(new RogueliteCardId[techniqueCards.size()]),
                    false);
        }

        private void includeTuning(RogueliteCardId cardId) {
            if (cardId == null
                    || RogueliteCardCatalog.get(cardId).getSlotType()
                            != RogueliteSlotType.TUNING
                    || cardId == RogueliteCardId.TECHNIQUE_COUPLER
                    || cardId == RogueliteCardId.TECHNIQUE_MATRIX
                    || cardId == RogueliteCardId.TECHNIQUE_SINGULARITY) {
                return;
            }
            TieredTuningEffect tuning = new TieredTuningEffect(cardId);
            if (tuning.modifiesPower()) {
                powerBonus = powerTuningFound
                        ? Math.max(powerBonus, tuning.accelerationBonus())
                        : tuning.accelerationBonus();
                powerTuningFound = true;
            }
            if (tuning.modifiesGrip()) {
                gripBonus = gripTuningFound
                        ? Math.max(gripBonus, tuning.gripBonus(0f))
                        : tuning.gripBonus(0f);
                gripTuningFound = true;
            }
            if (tuning.modifiesAero()) {
                float efficiency = 1f / Math.max(0.01f, tuning.dragMultiplier());
                aerodynamicEfficiency = aeroTuningFound
                        ? Math.max(aerodynamicEfficiency, efficiency)
                        : efficiency;
                aeroTuningFound = true;
            }
            if (tuning.modifiesMass()) {
                massMultiplier = massTuningFound
                        ? Math.min(massMultiplier, tuning.massMultiplier())
                        : tuning.massMultiplier();
                massTuningFound = true;
            }
        }

        private void includeTechnique(RogueliteCardId cardId) {
            if (cardId != null && !techniqueCards.contains(cardId)) {
                techniqueCards.add(cardId);
            }
            lapExperienceBankMultiplier = Math.max(
                    lapExperienceBankMultiplier,
                    LapExperienceTechniqueEffect.multiplierFor(cardId));
            if (!RaceTechniqueEffect.hasMultiplicativeStats(cardId)) {
                return;
            }
            float scale = RaceTechniqueEffect.networkShareScale(cardId);
            int mask = RaceTechniqueEffect.amplifiedStatMask(cardId);
            if ((mask & RaceTechniqueEffect.POWER_STAT) != 0) {
                powerTechniqueScale = Math.max(powerTechniqueScale, scale);
            }
            if ((mask & RaceTechniqueEffect.GRIP_STAT) != 0) {
                gripTechniqueScale = Math.max(gripTechniqueScale, scale);
            }
            if ((mask & RaceTechniqueEffect.AERO_STAT) != 0) {
                aeroTechniqueScale = Math.max(aeroTechniqueScale, scale);
            }
            if ((mask & RaceTechniqueEffect.MASS_STAT) != 0) {
                massTechniqueScale = Math.max(massTechniqueScale, scale);
            }
        }

        private static RogueliteCardId cardInSlot(
                RogueliteLoadout loadout,
                RogueliteCardId previewCard,
                RogueliteSlotType slotType) {
            if (previewCard != null
                    && RogueliteCardCatalog.get(previewCard).getSlotType() == slotType) {
                return previewCard;
            }
            return loadout.get(slotType);
        }
    }

    private RogueliteCardId bestImportedTechnique(RogueliteCardId ownTechniqueCardId) {
        RogueliteCardId best = null;
        float bestScore = 0f;
        for (int i = 0; i < techniqueCards.length; i++) {
            RogueliteCardId candidate = techniqueCards[i];
            if (candidate == ownTechniqueCardId) {
                continue;
            }
            float score = techniqueImprovement(candidate, ownTechniqueCardId);
            if (score > bestScore) {
                bestScore = score;
                best = candidate;
            }
        }
        return best;
    }

    private static float techniqueImprovement(
            RogueliteCardId candidate,
            RogueliteCardId ownTechniqueCardId) {
        float score = 0f;
        for (int stat = POWER; stat <= MASS; stat++) {
            score += Math.max(
                    0f,
                    techniqueScale(candidate, stat)
                            - techniqueScale(ownTechniqueCardId, stat));
        }
        score += Math.max(
                0f,
                LapExperienceTechniqueEffect.multiplierFor(candidate)
                        - LapExperienceTechniqueEffect.multiplierFor(ownTechniqueCardId));
        return score;
    }

    private static float techniqueScale(RogueliteCardId cardId, int stat) {
        if (!RaceTechniqueEffect.hasMultiplicativeStats(cardId)) {
            return 1f;
        }
        int mask = RaceTechniqueEffect.amplifiedStatMask(cardId);
        int statMask = stat == POWER
                ? RaceTechniqueEffect.POWER_STAT
                : stat == GRIP
                        ? RaceTechniqueEffect.GRIP_STAT
                        : stat == AERO
                                ? RaceTechniqueEffect.AERO_STAT
                                : RaceTechniqueEffect.MASS_STAT;
        return (mask & statMask) == 0
                ? 1f
                : RaceTechniqueEffect.networkShareScale(cardId);
    }

    private static boolean improves(
            int stat,
            float networkValue,
            boolean localFound,
            float localValue) {
        if (benefit(stat, networkValue) <= 0f) {
            return false;
        }
        if (!localFound) {
            return true;
        }
        return stat == MASS
                ? networkValue < localValue
                : networkValue > localValue;
    }

    private static float benefit(int stat, float value) {
        return stat == MASS ? 1f - value : value - (stat >= AERO ? 1f : 0f);
    }

    private static boolean isTuningCard(RogueliteCardId cardId) {
        return cardId != null
                && RogueliteCardCatalog.get(cardId).getSlotType()
                        == RogueliteSlotType.TUNING;
    }

    private static boolean isTuningAmplifier(RogueliteCardId cardId) {
        return cardId == RogueliteCardId.TECHNIQUE_COUPLER
                || cardId == RogueliteCardId.TECHNIQUE_MATRIX
                || cardId == RogueliteCardId.TECHNIQUE_SINGULARITY;
    }
}
