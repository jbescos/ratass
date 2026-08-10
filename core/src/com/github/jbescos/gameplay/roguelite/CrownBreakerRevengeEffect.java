package com.github.jbescos.gameplay.roguelite;

/** Stores the rival who landed a qualified hit and arms a targeted counterattack. */
final class CrownBreakerRevengeEffect extends RogueliteUpgradeEffect {
    static final float ARMED_DURATION_SECONDS = 30f;
    private static final float ATTACKER_LAUNCH_SPEED_RATIO = 0.48f;
    private static final float TARGET_PUSH_SPEED_RATIO = 0.72f;
    private static final float RAM_CONTACT_ACQUIRE_SECONDS = 1f;

    private int offenderVehicleId = -1;
    private float armedTimeRemaining;
    private int suppressedRamVehicleId = -1;
    private float ramContactAcquireTimer;
    private boolean ramContactObserved;

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
    float activeTimeRemainingSeconds() {
        return armedTimeRemaining;
    }

    @Override
    int activeDisplayPriority() {
        return 6;
    }

    @Override
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        if (offenderVehicleId >= 0) {
            armedTimeRemaining = Math.max(0f, armedTimeRemaining - Math.max(0f, delta));
            if (armedTimeRemaining <= 0f) {
                offenderVehicleId = -1;
            }
        }
        if (suppressedRamVehicleId < 0 || ramContactObserved) {
            return;
        }
        ramContactAcquireTimer = Math.max(0f, ramContactAcquireTimer - delta);
        if (ramContactAcquireTimer <= 0f) {
            clearRamContactSuppression();
        }
    }

    @Override
    float accelerationBonus() {
        return isReady() ? 0.55f : 0f;
    }

    @Override
    float maxSpeedBonus() {
        return isReady() ? 0.22f : 0f;
    }

    @Override
    float frontCollisionRecoilMultiplier() {
        return isReady() ? 0.25f : 1f;
    }

    @Override
    float frontCollisionPushMultiplier() {
        return isReady() ? 1.70f : 1f;
    }

    @Override
    void onHitBy(int vehicleId, float impactStrength) {
        if (vehicleId == suppressedRamVehicleId) {
            ramContactObserved = true;
            return;
        }
        if (vehicleId >= 0
                && impactStrength > 0f
                && offenderVehicleId < 0) {
            offenderVehicleId = vehicleId;
            armedTimeRemaining = ARMED_DURATION_SECONDS;
        }
    }

    @Override
    void onContactEnded(int vehicleId) {
        if (vehicleId == suppressedRamVehicleId) {
            clearRamContactSuppression();
        }
    }

    @Override
    int revengeTargetVehicleId() {
        return offenderVehicleId;
    }

    @Override
    RogueliteRevengeStrike tryActivateOffenderHit(int targetVehicleId) {
        if (!isReady()
                || targetVehicleId != offenderVehicleId) {
            return null;
        }
        suppressedRamVehicleId = offenderVehicleId;
        ramContactAcquireTimer = RAM_CONTACT_ACQUIRE_SECONDS;
        ramContactObserved = false;
        offenderVehicleId = -1;
        armedTimeRemaining = 0f;
        return RogueliteRevengeStrike.hardImpact(
                getCardId(),
                ATTACKER_LAUNCH_SPEED_RATIO,
                TARGET_PUSH_SPEED_RATIO);
    }

    private void clearRamContactSuppression() {
        suppressedRamVehicleId = -1;
        ramContactAcquireTimer = 0f;
        ramContactObserved = false;
    }
}
