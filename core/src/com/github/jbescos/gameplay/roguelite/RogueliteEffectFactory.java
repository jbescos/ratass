package com.github.jbescos.gameplay.roguelite;

final class RogueliteEffectFactory {
    private RogueliteEffectFactory() {
    }

    static RogueliteUpgradeEffect create(
            RogueliteCardId id,
            RogueliteLoadout loadout) {
        switch (id) {
            case TURBOCHARGER:
                return new TurbochargerEffect(
                        loadout.has(RogueliteCardId.AERODYNAMIC_KIT));
            case AERODYNAMIC_KIT:
                return new AerodynamicKitEffect();
            case DRIFT_CAPACITOR:
                return new DriftCapacitorEffect(
                        loadout.has(RogueliteCardId.COUNTERSTEER_SERVO));
            case COUNTERSTEER_SERVO:
                return new CountersteerServoEffect(
                        loadout.has(RogueliteCardId.DRIFT_CAPACITOR));
            case DRAFT_RECEIVER:
                return new DraftReceiverEffect(
                        loadout.has(RogueliteCardId.OVERTAKE_INJECTOR));
            case OVERTAKE_INJECTOR:
                return new OvertakeInjectorEffect(
                        loadout.has(RogueliteCardId.DRAFT_RECEIVER));
            case REINFORCED_BUMPER:
                return new ReinforcedBumperEffect();
            case KINETIC_RECYCLER:
                return new KineticRecyclerEffect(
                        loadout.has(RogueliteCardId.REINFORCED_BUMPER));
            case STORM_TIRES:
                return new StormTiresEffect();
            case STORM_DYNAMO:
                return new StormDynamoEffect(
                        loadout.has(RogueliteCardId.STORM_TIRES));
            case CLEAN_MOMENTUM:
                return new CleanMomentumEffect();
            case RECOVERY_DIFFERENTIAL:
                return new RecoveryDifferentialEffect(
                        loadout.has(RogueliteCardId.CLEAN_MOMENTUM));
            default:
                throw new IllegalArgumentException("Unsupported roguelite card: " + id);
        }
    }
}
