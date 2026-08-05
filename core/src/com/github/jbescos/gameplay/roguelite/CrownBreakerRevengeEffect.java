package com.github.jbescos.gameplay.roguelite;

/** Stores the rival who landed a qualified hit and arms a targeted counterattack. */
final class CrownBreakerRevengeEffect extends RogueliteUpgradeEffect {
    private static final float ACTIVATION_RANGE = 3.6f;
    private static final float ATTACKER_LAUNCH_SPEED_RATIO = 0.48f;
    private static final float TARGET_PUSH_SPEED_RATIO = 0.72f;

    private int offenderVehicleId = -1;

    CrownBreakerRevengeEffect() {
        super(RogueliteCardId.CROWN_ENGINE);
    }

    @Override
    boolean isActive() {
        return isArmed();
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
    float readiness() {
        return 1f;
    }

    @Override
    int activeDisplayPriority() {
        return 6;
    }

    @Override
    float accelerationBonus() {
        return isArmed() ? 0.55f : 0f;
    }

    @Override
    float maxSpeedBonus() {
        return isArmed() ? 0.22f : 0f;
    }

    @Override
    float gripBonus(float slip) {
        return isArmed() ? 0.40f : 0f;
    }

    @Override
    float steeringBonus(float slip) {
        return isArmed() ? 0.20f : 0f;
    }

    @Override
    float frontCollisionRecoilMultiplier() {
        return isArmed() ? 0.25f : 1f;
    }

    @Override
    float frontCollisionPushMultiplier() {
        return isArmed() ? 1.70f : 1f;
    }

    @Override
    void onHitBy(int vehicleId, float impactStrength) {
        if (vehicleId >= 0
                && impactStrength > 0f
                && offenderVehicleId < 0) {
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
            float distance) {
        if (!isReady()
                || targetVehicleId != offenderVehicleId
                || distance > ACTIVATION_RANGE) {
            return null;
        }
        offenderVehicleId = -1;
        return RogueliteRevengeStrike.hardImpact(
                getCardId(),
                ATTACKER_LAUNCH_SPEED_RATIO,
                TARGET_PUSH_SPEED_RATIO);
    }
}
