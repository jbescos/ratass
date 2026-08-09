package com.github.jbescos.gameplay.roguelite;

/** Arms a collision-terminated blindness and handling curse against the exact offender. */
final class OffenderCurseRevengeEffect extends RogueliteUpgradeEffect {
    private final float offenderMassMultiplier;
    private final float offenderGripMultiplier;
    private int offenderVehicleId = -1;

    OffenderCurseRevengeEffect(RogueliteCardId cardId) {
        super(cardId);
        switch (cardId) {
            case SENSOR_JAMMER:
                offenderMassMultiplier = 1.05f;
                offenderGripMultiplier = 1f;
                break;
            case GRID_BLACKOUT:
                offenderMassMultiplier = 1.20f;
                offenderGripMultiplier = 1f;
                break;
            case TOTAL_BLACKOUT:
                offenderMassMultiplier = 1.50f;
                offenderGripMultiplier = 0.80f;
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported offender curse card: " + cardId);
        }
    }

    @Override
    boolean isReady() {
        return offenderVehicleId >= 0;
    }

    @Override
    boolean isArmed() {
        return offenderVehicleId >= 0;
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
    void onHitBy(int vehicleId, float impactStrength) {
        if (vehicleId >= 0 && impactStrength > 0f) {
            offenderVehicleId = vehicleId;
        }
    }

    @Override
    int revengeTargetVehicleId() {
        return offenderVehicleId;
    }

    @Override
    RogueliteRevengeStrike tryActivateOffenderStrike(
            int targetVehicleId,
            float distance,
            boolean offenderAhead) {
        if (!isReady() || targetVehicleId != offenderVehicleId) {
            return null;
        }
        offenderVehicleId = -1;
        return RogueliteRevengeStrike.curse(
                getCardId(),
                offenderMassMultiplier,
                offenderGripMultiplier);
    }
}
