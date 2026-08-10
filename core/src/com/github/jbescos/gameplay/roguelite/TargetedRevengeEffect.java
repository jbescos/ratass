package com.github.jbescos.gameplay.roguelite;

/** Arms against the exact rival responsible for a qualified hit. */
final class TargetedRevengeEffect extends RogueliteUpgradeEffect {
    private static final float POSITION_SWAP_MIN_DISTANCE = 3.2f;
    private static final float POSITION_SWAP_DELAY_SECONDS = 3f;
    private static final float HOOK_TRIGGER_DELAY_SECONDS = 3f;

    private final float effectDurationSeconds;
    private final float targetSpeedMultiplier;
    private final float targetGripMultiplier;

    private int offenderVehicleId = -1;
    private float activeTimer;
    private float armedAge;
    private boolean awaitingCompletion;

    TargetedRevengeEffect(RogueliteCardId cardId) {
        super(cardId);
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
        return offenderVehicleId >= 0
                && activeTimer <= 0f
                && !awaitingCompletion
                && armedAge >= triggerDelaySeconds();
    }

    @Override
    boolean isArmed() {
        return offenderVehicleId >= 0;
    }

    @Override
    float readiness() {
        float delaySeconds = triggerDelaySeconds();
        if (delaySeconds <= 0f) {
            return 1f;
        }
        return Math.max(0f, Math.min(1f, armedAge / delaySeconds));
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
        if (offenderVehicleId >= 0 && activeTimer <= 0f && !awaitingCompletion) {
            armedAge += Math.max(0f, delta);
        }
    }

    @Override
    void onHitBy(int vehicleId, float impactStrength) {
        if (vehicleId >= 0
                && impactStrength > 0f
                && activeTimer <= 0f
                && !awaitingCompletion
                && offenderVehicleId < 0) {
            offenderVehicleId = vehicleId;
            armedAge = 0f;
        }
    }

    @Override
    int revengeTargetVehicleId() {
        return offenderVehicleId;
    }

    @Override
    boolean expireOffenderStrikeIfConditionFailed(
            int targetVehicleId,
            boolean offenderAhead) {
        if (getCardId() != RogueliteCardId.RECOVERY_BEACON
                || !isReady()
                || targetVehicleId != offenderVehicleId
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
        if (!isReady() || targetVehicleId != offenderVehicleId) {
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

    private float triggerDelaySeconds() {
        if (getCardId() == RogueliteCardId.RECOVERY_BEACON) {
            return POSITION_SWAP_DELAY_SECONDS;
        }
        if (getCardId() == RogueliteCardId.PAYBACK_SHIELD) {
            return HOOK_TRIGGER_DELAY_SECONDS;
        }
        return 0f;
    }

    private void clearOffender() {
        offenderVehicleId = -1;
        armedAge = 0f;
    }
}
