package com.github.jbescos.gameplay.roguelite;

/** Delays a three-car race-position reversal after recording a qualified hit. */
final class TriadCoupRevengeEffect extends RevengeUpgradeEffect {
    static final float TRIGGER_DELAY_SECONDS = 1f;

    TriadCoupRevengeEffect() {
        super(RogueliteCardId.TRIAD_COUP, RevengeWorkflow.TARGET_DELAYED);
    }

    @Override
    boolean isActive() {
        return isArmed();
    }

    @Override
    boolean isReady() {
        return isArmed() && targetAgeSeconds() >= TRIGGER_DELAY_SECONDS;
    }

    @Override
    boolean isArmed() {
        return hasTarget();
    }

    @Override
    float readiness() {
        return Math.max(0f, Math.min(1f, targetAgeSeconds() / TRIGGER_DELAY_SECONDS));
    }

    @Override
    int activeDisplayPriority() {
        return 5;
    }

    @Override
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        if (isArmed()) {
            advanceTargetAge(delta);
        }
    }

    @Override
    protected void prepareFromHit(int vehicleId, float impactStrength) {
    }

    @Override
    boolean allowsOffRoadOffenderStrike() {
        return true;
    }

    @Override
    RogueliteRevengeStrike tryActivateOffenderStrike(
            int targetVehicleId,
            float distance,
            boolean offenderAhead) {
        if (!isReady() || !targets(targetVehicleId)) {
            return null;
        }
        RogueliteRevengeStrike strike =
                RogueliteRevengeStrike.positionReorder(
                        getCardId(), revengeSecondaryTargetVehicleId());
        clear();
        return strike;
    }

    private void clear() {
        clearTarget();
    }
}
