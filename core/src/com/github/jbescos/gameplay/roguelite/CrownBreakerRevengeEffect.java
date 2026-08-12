package com.github.jbescos.gameplay.roguelite;

/** Stores the rival who landed a qualified hit and arms a targeted counterattack. */
final class CrownBreakerRevengeEffect extends RevengeUpgradeEffect {
    static final float ARMED_DURATION_SECONDS = 30f;
    static final float PREPARATION_SECONDS = 3f;
    static final float RAM_TRIGGER_DISTANCE = 5f;
    private static final float ATTACKER_LAUNCH_SPEED_RATIO = 0.48f;
    private static final float TARGET_PUSH_SPEED_RATIO = 0.72f;
    private static final float RAM_CONTACT_ACQUIRE_SECONDS = 1f;

    private float armedTimeRemaining;
    private int suppressedRamVehicleId = -1;
    private float ramContactAcquireTimer;
    private boolean ramContactObserved;
    private float effectMultiplier = 1f;
    private boolean amplificationApplied;

    CrownBreakerRevengeEffect() {
        super(RogueliteCardId.CROWN_ENGINE, RevengeWorkflow.TARGET_RETURN_HIT);
    }

    @Override
    boolean isActive() {
        return isArmed();
    }

    @Override
    boolean isReady() {
        return hasTarget() && targetAgeSeconds() >= PREPARATION_SECONDS;
    }

    @Override
    boolean isArmed() {
        return hasTarget();
    }

    @Override
    float readiness() {
        return hasTarget()
                ? Math.min(1f, targetAgeSeconds() / PREPARATION_SECONDS)
                : 0f;
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
        if (hasTarget()) {
            advanceTargetAge(Math.max(0f, delta));
            armedTimeRemaining = Math.max(0f, armedTimeRemaining - Math.max(0f, delta));
            if (armedTimeRemaining <= 0f) {
                clearTarget();
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
        return isArmed() ? 0.55f * effectMultiplier : 0f;
    }

    @Override
    float frontCollisionRecoilMultiplier() {
        return isArmed()
                ? Math.max(0f, 1f + (0.25f - 1f) * effectMultiplier)
                : 1f;
    }

    @Override
    float frontCollisionPushMultiplier() {
        return isArmed() ? 1f + 0.70f * effectMultiplier : 1f;
    }

    @Override
    protected void prepareFromHit(int vehicleId, float impactStrength) {
        armedTimeRemaining = ARMED_DURATION_SECONDS;
        effectMultiplier = 1f;
        amplificationApplied = false;
    }

    @Override
    protected boolean ignoresHit(int vehicleId) {
        if (vehicleId != suppressedRamVehicleId) {
            return false;
        }
        ramContactObserved = true;
        return true;
    }

    @Override
    protected void onTargetCancelled() {
        armedTimeRemaining = 0f;
        effectMultiplier = 1f;
        amplificationApplied = false;
    }

    @Override
    void amplifyActiveRevenge(float multiplier) {
        if (!isArmed() || amplificationApplied) {
            return;
        }
        float safeMultiplier = Float.isFinite(multiplier)
                ? Math.max(1f, multiplier)
                : 1f;
        armedTimeRemaining *= safeMultiplier;
        effectMultiplier = safeMultiplier;
        amplificationApplied = true;
    }

    @Override
    void onContactEnded(int vehicleId) {
        if (vehicleId == suppressedRamVehicleId) {
            clearRamContactSuppression();
        }
    }

    @Override
    RogueliteRevengeStrike tryActivateOffenderHit(int targetVehicleId) {
        if (!isReady() || !targets(targetVehicleId)) {
            return null;
        }
        return activateRam();
    }

    @Override
    RogueliteRevengeStrike tryActivateOffenderStrike(
            int targetVehicleId,
            float distance,
            boolean offenderAhead) {
        if (!isReady()
                || !targets(targetVehicleId)
                || !Float.isFinite(distance)
                || distance > RAM_TRIGGER_DISTANCE) {
            return null;
        }
        return activateRam();
    }

    private RogueliteRevengeStrike activateRam() {
        suppressedRamVehicleId = revengeTargetVehicleId();
        ramContactAcquireTimer = RAM_CONTACT_ACQUIRE_SECONDS;
        ramContactObserved = false;
        clearTarget();
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
