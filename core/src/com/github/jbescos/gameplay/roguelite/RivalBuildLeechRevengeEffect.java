package com.github.jbescos.gameplay.roguelite;

/** Temporarily disables a hitter's build cards and redirects their lap XP. */
final class RivalBuildLeechRevengeEffect extends RevengeUpgradeEffect {
    private final float durationSeconds;
    private float activeTimer;
    private boolean amplificationApplied;

    RivalBuildLeechRevengeEffect(RogueliteCardId cardId) {
        super(cardId, RevengeWorkflow.TARGET_IMMEDIATE);
        durationSeconds = RivalBuildLeechSpec.durationSeconds(cardId);
    }

    static boolean isSupported(RogueliteCardId cardId) {
        return RivalBuildLeechSpec.isCard(cardId);
    }

    @Override
    boolean isActive() {
        return activeTimer > 0f && hasTarget();
    }

    @Override
    boolean isArmed() {
        return false;
    }

    @Override
    float readiness() {
        return isActive() ? 1f : 0f;
    }

    @Override
    float activeTimeRemainingSeconds() {
        return activeTimer;
    }

    @Override
    int activeDisplayPriority() {
        return 5;
    }

    @Override
    boolean suppressesOffenderBuildAndTransfersLapExperience() {
        return isActive();
    }

    @Override
    float revengeTargetAgeSeconds() {
        return targetAgeSeconds();
    }

    @Override
    protected void prepareFromHit(int vehicleId, float impactStrength) {
        activeTimer = durationSeconds;
        amplificationApplied = false;
    }

    @Override
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        if (!isActive()) {
            return;
        }
        advanceTargetAge(delta);
        activeTimer = Math.max(0f, activeTimer - timerDelta);
        if (activeTimer <= 0f) {
            clearTarget();
        }
    }

    @Override
    void amplifyActiveRevenge(float multiplier) {
        if (isActive() && multiplier > 1f && !amplificationApplied) {
            activeTimer *= multiplier;
            amplificationApplied = true;
        }
    }

    @Override
    protected void onTargetCancelled() {
        activeTimer = 0f;
        amplificationApplied = false;
    }
}
