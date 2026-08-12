package com.github.jbescos.gameplay.roguelite;

/** Fires a timed sequence of remote push shots at the rival responsible for a hit. */
final class HunterBarrageRevengeEffect extends RevengeUpgradeEffect {
    static final int SHOT_COUNT = 3;
    static final float SHOT_INTERVAL_SECONDS = 1f;
    static final int STORM_SHOT_COUNT = 6;
    static final float STORM_SHOT_INTERVAL_SECONDS = 0.5f;

    private final int baseShotCount;
    private final float baseShotIntervalSeconds;
    private int shotsRemaining;
    private int totalShots;
    private float shotIntervalSeconds;
    private float nextShotTimer;
    private boolean amplificationApplied;

    HunterBarrageRevengeEffect() {
        this(RogueliteCardId.HUNTER_BARRAGE);
    }

    HunterBarrageRevengeEffect(RogueliteCardId cardId) {
        super(cardId, RevengeWorkflow.TARGET_SEQUENCE);
        switch (cardId) {
            case HUNTER_BARRAGE:
                baseShotCount = SHOT_COUNT;
                baseShotIntervalSeconds = SHOT_INTERVAL_SECONDS;
                break;
            case HUNTER_STORM:
                baseShotCount = STORM_SHOT_COUNT;
                baseShotIntervalSeconds = STORM_SHOT_INTERVAL_SECONDS;
                break;
            default:
                throw new IllegalArgumentException("Unsupported hunting Revenge: " + cardId);
        }
        totalShots = baseShotCount;
        shotIntervalSeconds = baseShotIntervalSeconds;
    }

    @Override
    boolean isActive() {
        return hasTarget();
    }

    @Override
    boolean isReady() {
        return hasTarget()
                && shotsRemaining > 0
                && nextShotTimer <= 0f;
    }

    @Override
    boolean isArmed() {
        return hasTarget();
    }

    @Override
    float readiness() {
        return Math.max(
                0f,
                Math.min(1f, 1f - nextShotTimer / shotIntervalSeconds));
    }

    @Override
    float activeTimeRemainingSeconds() {
        if (!isArmed()) {
            return 0f;
        }
        return nextShotTimer
                + Math.max(0, shotsRemaining - 1) * shotIntervalSeconds;
    }

    @Override
    int activeDisplayPriority() {
        return 5;
    }

    @Override
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        if (hasTarget() && shotsRemaining > 0) {
            nextShotTimer = Math.max(0f, nextShotTimer - Math.max(0f, timerDelta));
        }
    }

    @Override
    protected void prepareFromHit(int vehicleId, float impactStrength) {
        totalShots = baseShotCount;
        shotsRemaining = baseShotCount;
        shotIntervalSeconds = baseShotIntervalSeconds;
        nextShotTimer = baseShotIntervalSeconds;
        amplificationApplied = false;
    }

    @Override
    protected boolean isExecutionInProgress() {
        return hasTarget() && shotsRemaining < totalShots;
    }

    @Override
    protected void onTargetCancelled() {
        shotsRemaining = 0;
        totalShots = baseShotCount;
        shotIntervalSeconds = baseShotIntervalSeconds;
        nextShotTimer = 0f;
        amplificationApplied = false;
    }

    @Override
    void amplifyActiveRevenge(float multiplier) {
        if (!hasTarget() || amplificationApplied) {
            return;
        }
        float safeMultiplier = Float.isFinite(multiplier)
                ? Math.max(1f, multiplier)
                : 1f;
        int amplifiedShotCount = Math.max(
                baseShotCount,
                Math.round(baseShotCount * safeMultiplier * safeMultiplier));
        shotsRemaining += amplifiedShotCount - totalShots;
        totalShots = amplifiedShotCount;
        shotIntervalSeconds = baseShotIntervalSeconds / safeMultiplier;
        nextShotTimer = Math.min(nextShotTimer, shotIntervalSeconds);
        amplificationApplied = true;
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

        int strikeIndex = totalShots - shotsRemaining + 1;
        shotsRemaining--;
        if (shotsRemaining <= 0) {
            clearTarget();
            nextShotTimer = 0f;
        } else {
            nextShotTimer = shotIntervalSeconds;
        }
        return RogueliteRevengeStrike.pushShot(getCardId(), strikeIndex);
    }
}
