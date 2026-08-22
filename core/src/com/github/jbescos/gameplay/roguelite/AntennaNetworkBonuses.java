package com.github.jbescos.gameplay.roguelite;

/** Immutable best-of network values broadcast by every antenna-equipped car. */
public final class AntennaNetworkBonuses {
    public static final AntennaNetworkBonuses NONE = new AntennaNetworkBonuses(
            0f, 0f, 1f, 1f, 1f, 1f, 1f, 1f);

    private final float powerBonus;
    private final float gripBonus;
    private final float aerodynamicEfficiency;
    private final float massMultiplier;
    private final float powerTechniqueScale;
    private final float gripTechniqueScale;
    private final float aeroTechniqueScale;
    private final float massTechniqueScale;

    private AntennaNetworkBonuses(
            float powerBonus,
            float gripBonus,
            float aerodynamicEfficiency,
            float massMultiplier,
            float powerTechniqueScale,
            float gripTechniqueScale,
            float aeroTechniqueScale,
            float massTechniqueScale) {
        this.powerBonus = powerBonus;
        this.gripBonus = gripBonus;
        this.aerodynamicEfficiency = aerodynamicEfficiency;
        this.massMultiplier = massMultiplier;
        this.powerTechniqueScale = powerTechniqueScale;
        this.gripTechniqueScale = gripTechniqueScale;
        this.aeroTechniqueScale = aeroTechniqueScale;
        this.massTechniqueScale = massTechniqueScale;
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
        private boolean participantFound;
        private boolean powerTuningFound;
        private boolean gripTuningFound;
        private boolean aeroTuningFound;
        private boolean massTuningFound;

        public Builder include(RogueliteLoadout loadout) {
            return include(loadout, null);
        }

        public Builder includeActive(
                RogueliteLoadout loadout,
                RogueliteCardId activeAntennaCardId) {
            if (loadout == null
                    || !AntennaPowerupSpec.isAntennaCard(activeAntennaCardId)) {
                return this;
            }
            participantFound = true;
            includeTuning(loadout.get(RogueliteSlotType.TUNING));
            includeTechnique(loadout.get(RogueliteSlotType.TECHNIQUE));
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
                    massTechniqueScale);
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
}
