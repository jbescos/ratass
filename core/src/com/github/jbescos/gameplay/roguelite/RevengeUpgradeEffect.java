package com.github.jbescos.gameplay.roguelite;

/** Common preparation, retargeting, and cancellation contract for Revenge cards. */
abstract class RevengeUpgradeEffect extends RogueliteUpgradeEffect {
    private final RevengeWorkflow workflow;
    private final RevengeTargetState targetState;

    RevengeUpgradeEffect(RogueliteCardId cardId, RevengeWorkflow workflow) {
        super(cardId);
        this.workflow = workflow;
        targetState = workflow.isTargeted() ? new RevengeTargetState() : null;
    }

    @Override
    final RevengeWorkflow revengeWorkflow() {
        return workflow;
    }

    @Override
    final boolean onHitBy(int vehicleId, float impactStrength) {
        if (vehicleId < 0
                || impactStrength <= 0f
                || isExecutionInProgress()
                || ignoresHit(vehicleId)) {
            return false;
        }
        if (targetState != null) {
            targetState.restart(vehicleId);
        }
        prepareFromHit(vehicleId, impactStrength);
        return true;
    }

    @Override
    final int revengeTargetVehicleId() {
        return targetState == null ? -1 : targetState.primaryVehicleId();
    }

    @Override
    final void setRevengeSecondaryTargetVehicleId(int vehicleId) {
        if (targetState != null) {
            targetState.captureSecondary(vehicleId);
        }
    }

    @Override
    final int revengeSecondaryTargetVehicleId() {
        return targetState == null ? -1 : targetState.secondaryVehicleId();
    }

    @Override
    final boolean cancelRevengeTarget(int vehicleId) {
        if (targetState == null || !targetState.clearIfTarget(vehicleId)) {
            return false;
        }
        onTargetCancelled();
        return true;
    }

    protected abstract void prepareFromHit(int vehicleId, float impactStrength);

    protected boolean isExecutionInProgress() {
        return false;
    }

    protected boolean ignoresHit(int vehicleId) {
        return false;
    }

    protected void onTargetCancelled() {
    }

    protected final boolean hasTarget() {
        return targetState != null && targetState.isArmed();
    }

    protected final boolean targets(int vehicleId) {
        return revengeTargetVehicleId() == vehicleId;
    }

    protected final float targetAgeSeconds() {
        return targetState == null ? 0f : targetState.ageSeconds();
    }

    protected final void advanceTargetAge(float delta) {
        if (targetState != null) {
            targetState.advance(delta);
        }
    }

    protected final void clearTarget() {
        if (targetState != null) {
            targetState.clear();
        }
    }
}
