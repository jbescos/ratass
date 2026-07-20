package com.github.jbescos.gameplay.roguelite;

final class RogueliteEffectFactory {
    private RogueliteEffectFactory() {
    }

    static RogueliteUpgradeEffect create(
            RogueliteCardId id,
            int level,
            RogueliteCardInventory inventory) {
        switch (id) {
            case TURBOCHARGER:
                return new TurbochargerEffect(
                        level,
                        inventory.has(RogueliteCardId.AERODYNAMIC_KIT));
            case AERODYNAMIC_KIT:
                return new AerodynamicKitEffect(level);
            case DRIFT_CAPACITOR:
                return new DriftCapacitorEffect(
                        level,
                        inventory.has(RogueliteCardId.COUNTERSTEER_SERVO));
            case COUNTERSTEER_SERVO:
                return new CountersteerServoEffect(
                        level,
                        inventory.has(RogueliteCardId.DRIFT_CAPACITOR));
            case DRAFT_RECEIVER:
                return new DraftReceiverEffect(
                        level,
                        inventory.has(RogueliteCardId.OVERTAKE_INJECTOR));
            case OVERTAKE_INJECTOR:
                return new OvertakeInjectorEffect(
                        level,
                        inventory.has(RogueliteCardId.DRAFT_RECEIVER));
            case REINFORCED_BUMPER:
                return new ReinforcedBumperEffect(level);
            case KINETIC_RECYCLER:
                return new KineticRecyclerEffect(
                        level,
                        inventory.has(RogueliteCardId.REINFORCED_BUMPER));
            case STORM_TIRES:
                return new StormTiresEffect(level);
            case STORM_DYNAMO:
                return new StormDynamoEffect(
                        level,
                        inventory.has(RogueliteCardId.STORM_TIRES));
            case CLEAN_MOMENTUM:
                return new CleanMomentumEffect(level);
            case RECOVERY_DIFFERENTIAL:
                return new RecoveryDifferentialEffect(
                        level,
                        inventory.has(RogueliteCardId.CLEAN_MOMENTUM));
            default:
                throw new IllegalArgumentException("Unsupported roguelite card: " + id);
        }
    }
}
