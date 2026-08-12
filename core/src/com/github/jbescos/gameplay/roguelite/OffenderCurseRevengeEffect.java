package com.github.jbescos.gameplay.roguelite;

/** Arms a timed blindness and performance curse against the exact offender. */
final class OffenderCurseRevengeEffect extends RevengeUpgradeEffect {
    private final float offenderMassMultiplier;
    private final float offenderPerformanceMultiplier;
    private final float durationSeconds;

    OffenderCurseRevengeEffect(RogueliteCardId cardId) {
        super(cardId, RevengeWorkflow.TARGET_IMMEDIATE);
        switch (cardId) {
            case SENSOR_JAMMER:
                offenderMassMultiplier = 1.05f;
                offenderPerformanceMultiplier = 0.95f;
                durationSeconds = 20f;
                break;
            case GRID_BLACKOUT:
                offenderMassMultiplier = 1.10f;
                offenderPerformanceMultiplier = 0.90f;
                durationSeconds = 30f;
                break;
            case TOTAL_BLACKOUT:
                offenderMassMultiplier = 1.20f;
                offenderPerformanceMultiplier = 0.80f;
                durationSeconds = 40f;
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported offender curse card: " + cardId);
        }
    }

    @Override
    boolean isReady() {
        return hasTarget();
    }

    @Override
    boolean isArmed() {
        return hasTarget();
    }

    @Override
    boolean isOffenderCurse() {
        return true;
    }

    @Override
    float readiness() {
        return isReady() ? 1f : 0f;
    }

    @Override
    protected void prepareFromHit(int vehicleId, float impactStrength) {
    }

    @Override
    RogueliteRevengeStrike tryActivateOffenderStrike(
            int targetVehicleId,
            float distance,
            boolean offenderAhead) {
        if (!isReady() || !targets(targetVehicleId)) {
            return null;
        }
        clearTarget();
        return RogueliteRevengeStrike.curse(
                getCardId(),
                offenderMassMultiplier,
                offenderPerformanceMultiplier,
                durationSeconds);
    }
}
