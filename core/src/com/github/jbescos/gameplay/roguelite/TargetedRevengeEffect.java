package com.github.jbescos.gameplay.roguelite;

/** Arms against the exact rival responsible for a qualified hit. */
final class TargetedRevengeEffect extends RevengeUpgradeEffect {
    private static final float POSITION_SWAP_MIN_DISTANCE = 3.2f;
    static final float TRIGGER_DELAY_SECONDS = 2f;

    private final float effectDurationSeconds;
    private final float targetSpeedMultiplier;
    private final float targetGripMultiplier;

    private float activeTimer;
    private boolean awaitingCompletion;

    TargetedRevengeEffect(RogueliteCardId cardId) {
        super(cardId, workflowFor(cardId));
        switch (cardId) {
            case TAR_TETHER:
                effectDurationSeconds = 2f;
                targetSpeedMultiplier = 1f;
                targetGripMultiplier = 0f;
                break;
            case EMP_SNARE:
                effectDurationSeconds = 2f;
                targetSpeedMultiplier = 1f;
                targetGripMultiplier = 1f;
                break;
            case VOID_ANCHOR:
                effectDurationSeconds = 3f;
                targetSpeedMultiplier = 1f;
                targetGripMultiplier = 1f;
                break;
            case DRAFT_VENDETTA:
                effectDurationSeconds = 5f;
                targetSpeedMultiplier = 1f;
                targetGripMultiplier = 1f;
                break;
            case RECOVERY_BEACON:
                effectDurationSeconds = 0.8f;
                targetSpeedMultiplier = 1f;
                targetGripMultiplier = 1f;
                break;
            case PAYBACK_SHIELD:
                effectDurationSeconds = 0f;
                targetSpeedMultiplier = 1f;
                targetGripMultiplier = 1f;
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported targeted revenge card: " + cardId);
        }
    }

    @Override
    boolean isActive() {
        return activeTimer > 0f || awaitingCompletion;
    }

    @Override
    boolean isReady() {
        return hasTarget()
                && activeTimer <= 0f
                && !awaitingCompletion
                && targetAgeSeconds() >= triggerDelaySeconds();
    }

    @Override
    boolean isArmed() {
        return hasTarget();
    }

    @Override
    float readiness() {
        float delaySeconds = triggerDelaySeconds();
        if (delaySeconds <= 0f) {
            return 1f;
        }
        return Math.max(0f, Math.min(1f, targetAgeSeconds() / delaySeconds));
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
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        activeTimer = Math.max(0f, activeTimer - timerDelta);
        if (hasTarget() && activeTimer <= 0f && !awaitingCompletion) {
            advanceTargetAge(delta);
        }
    }

    @Override
    protected void prepareFromHit(int vehicleId, float impactStrength) {
        activeTimer = 0f;
    }

    @Override
    protected boolean isExecutionInProgress() {
        return activeTimer > 0f || awaitingCompletion;
    }

    @Override
    protected void onTargetCancelled() {
        activeTimer = 0f;
        awaitingCompletion = false;
    }

    @Override
    boolean allowsOffRoadOffenderStrike() {
        return getCardId() == RogueliteCardId.PAYBACK_SHIELD;
    }

    @Override
    boolean expireOffenderStrikeIfConditionFailed(
            int targetVehicleId,
            boolean offenderAhead) {
        if (!requiresOffenderAhead()
                || !isReady()
                || !targets(targetVehicleId)
                || offenderAhead) {
            return false;
        }
        clearOffender();
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

        RogueliteRevengeStrike strike;
        switch (getCardId()) {
            case EMP_SNARE:
            case VOID_ANCHOR:
                strike = RogueliteRevengeStrike.forceBrake(
                        getCardId(),
                        effectDurationSeconds);
                break;
            case DRAFT_VENDETTA:
                strike = RogueliteRevengeStrike.forceThrottle(getCardId(), effectDurationSeconds);
                break;
            case RECOVERY_BEACON:
                if (!offenderAhead || distance < POSITION_SWAP_MIN_DISTANCE) {
                    return null;
                }
                strike = RogueliteRevengeStrike.positionSwap(getCardId());
                break;
            case PAYBACK_SHIELD:
                if (!offenderAhead) {
                    return null;
                }
                strike = RogueliteRevengeStrike.hook(getCardId());
                awaitingCompletion = true;
                break;
            default:
                strike = RogueliteRevengeStrike.debuff(
                        getCardId(),
                        targetSpeedMultiplier,
                        targetGripMultiplier,
                        effectDurationSeconds);
                break;
        }

        clearOffender();
        activeTimer = effectDurationSeconds;
        return strike;
    }

    @Override
    void completeOffenderStrike(RogueliteCardId cardId) {
        if (getCardId() == cardId) {
            awaitingCompletion = false;
            activeTimer = 0f;
        }
    }

    @Override
    void amplifyActiveRevenge(float multiplier) {
        if (activeTimer <= 0f) {
            return;
        }
        float safeMultiplier = Float.isFinite(multiplier)
                ? Math.max(1f, multiplier)
                : 1f;
        activeTimer *= safeMultiplier;
    }

    private float triggerDelaySeconds() {
        if (getCardId() == RogueliteCardId.RECOVERY_BEACON) {
            return TRIGGER_DELAY_SECONDS;
        }
        if (getCardId() == RogueliteCardId.PAYBACK_SHIELD) {
            return TRIGGER_DELAY_SECONDS;
        }
        return 0f;
    }

    private void clearOffender() {
        clearTarget();
    }

    private boolean requiresOffenderAhead() {
        return getCardId() == RogueliteCardId.RECOVERY_BEACON
                || getCardId() == RogueliteCardId.PAYBACK_SHIELD;
    }

    private static RevengeWorkflow workflowFor(RogueliteCardId cardId) {
        return cardId == RogueliteCardId.RECOVERY_BEACON
                        || cardId == RogueliteCardId.PAYBACK_SHIELD
                ? RevengeWorkflow.TARGET_DELAYED
                : RevengeWorkflow.TARGET_IMMEDIATE;
    }
}
