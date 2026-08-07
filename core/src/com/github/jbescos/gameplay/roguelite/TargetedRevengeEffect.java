package com.github.jbescos.gameplay.roguelite;

/** Arms against the exact rival responsible for a qualified hit. */
final class TargetedRevengeEffect extends RogueliteUpgradeEffect {
    private static final float POSITION_SWAP_MIN_DISTANCE = 3.2f;
    private static final float POSITION_SWAP_DELAY_SECONDS = 3f;
    private static final float HOOK_MIN_DISTANCE = 2.8f;
    private static final float HOOK_MAX_DISTANCE = 24f;

    private final float effectDurationSeconds;
    private final float targetSpeedMultiplier;
    private final float targetGripMultiplier;

    private int offenderVehicleId = -1;
    private float activeTimer;
    private float armedAge;

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
                effectDurationSeconds = 20f;
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
        return activeTimer > 0f;
    }

    @Override
    boolean isReady() {
        return offenderVehicleId >= 0
                && activeTimer <= 0f
                && (getCardId() != RogueliteCardId.RECOVERY_BEACON
                        || armedAge >= POSITION_SWAP_DELAY_SECONDS);
    }

    @Override
    boolean isArmed() {
        return offenderVehicleId >= 0;
    }

    @Override
    float readiness() {
        if (getCardId() != RogueliteCardId.RECOVERY_BEACON) {
            return 1f;
        }
        return Math.max(0f, Math.min(1f, armedAge / POSITION_SWAP_DELAY_SECONDS));
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
        if (offenderVehicleId >= 0 && activeTimer <= 0f) {
            armedAge += Math.max(0f, delta);
        }
    }

    @Override
    void onHitBy(int vehicleId, float impactStrength) {
        if (vehicleId >= 0
                && impactStrength > 0f
                && activeTimer <= 0f
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
                if (distance < HOOK_MIN_DISTANCE || distance > HOOK_MAX_DISTANCE) {
                    return null;
                }
                strike = RogueliteRevengeStrike.hook(
                        getCardId(),
                        0.42f,
                        effectDurationSeconds);
                break;
            default:
                strike = RogueliteRevengeStrike.debuff(
                        getCardId(),
                        targetSpeedMultiplier,
                        targetGripMultiplier,
                        effectDurationSeconds);
                break;
        }

        offenderVehicleId = -1;
        armedAge = 0f;
        activeTimer = effectDurationSeconds;
        return strike;
    }
}
