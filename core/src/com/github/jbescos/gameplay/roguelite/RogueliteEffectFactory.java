package com.github.jbescos.gameplay.roguelite;

final class RogueliteEffectFactory {
    private RogueliteEffectFactory() {
    }

    static RogueliteUpgradeEffect create(
            RogueliteCardId id,
            float powerupCycleOffset) {
        if (AntennaPowerupSpec.isAntennaCard(id)) {
            return new AntennaPowerupEffect(id);
        }
        if (id == RogueliteCardId.TIER_FOUR_SIGNAL) {
            return new TierFourUnlockPowerupEffect();
        }
        if (RandomCardEffect.isRandomCard(id)) {
            return new RandomCardEffect(id, powerupCycleOffset);
        }
        if (id == RogueliteCardId.TECHNIQUE_COUPLER
                || id == RogueliteCardId.TECHNIQUE_MATRIX
                || id == RogueliteCardId.TECHNIQUE_SINGULARITY) {
            return new TechniqueAmplifierTuningEffect(id);
        }
        if (id == RogueliteCardId.POWERUP_LINK
                || id == RogueliteCardId.POWERUP_MATRIX
                || id == RogueliteCardId.POWERUP_NEXUS) {
            return new PowerupAmplifierTechniqueEffect(id);
        }
        if (id == RogueliteCardId.PRIORITY_HOTLINE) {
            return new BestDriverOverrideEffect(id);
        }
        if (id == RogueliteCardId.GRUDGE_SPARK
                || id == RogueliteCardId.VENGEANCE_CORE
                || id == RogueliteCardId.NEMESIS_ENGINE) {
            return new RevengeAmplifierPowerupEffect(id);
        }
        if (id == RogueliteCardId.TAR_TETHER
                || id == RogueliteCardId.EMP_SNARE
                || id == RogueliteCardId.VOID_ANCHOR
                || id == RogueliteCardId.DRAFT_VENDETTA
                || id == RogueliteCardId.RECOVERY_BEACON
                || id == RogueliteCardId.PAYBACK_SHIELD) {
            return new TargetedRevengeEffect(id);
        }
        if (id == RogueliteCardId.CROWN_ENGINE) {
            return new CrownBreakerRevengeEffect();
        }
        if (id == RogueliteCardId.HUNTER_BARRAGE
                || id == RogueliteCardId.HUNTER_STORM) {
            return new HunterBarrageRevengeEffect(id);
        }
        if (id == RogueliteCardId.TRIAD_COUP) {
            return new TriadCoupRevengeEffect();
        }
        if (id == RogueliteCardId.SENSOR_JAMMER
                || id == RogueliteCardId.GRID_BLACKOUT
                || id == RogueliteCardId.TOTAL_BLACKOUT) {
            return new OffenderCurseRevengeEffect(id);
        }
        RogueliteCardDefinition definition = RogueliteCardCatalog.get(id);
        switch (definition.getSlotType()) {
            case TUNING:
                return new TieredTuningEffect(id);
            case TECHNIQUE:
                return new RaceTechniqueEffect(id);
            case POWERUP:
                return new CooldownPowerupEffect(id, powerupCycleOffset);
            case REVENGE:
                return new ReactiveRevengeEffect(id);
            default:
                throw new IllegalArgumentException("Unsupported roguelite card slot: " + id);
        }
    }
}
