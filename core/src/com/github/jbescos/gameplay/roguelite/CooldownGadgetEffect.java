package com.github.jbescos.gameplay.roguelite;

/** A deterministic, context-triggered gadget with a cooldown. */
final class CooldownGadgetEffect extends RogueliteUpgradeEffect {
    private final float cooldownSeconds;
    private final float durationSeconds;
    private final float accelerationBonus;
    private final float maxSpeedBonus;
    private final float gripBonus;
    private final float steeringBonus;
    private final float recoilMultiplier;
    private final float pushMultiplier;
    private final float slipstreamRangeMultiplier;
    private final float slipstreamStrengthMultiplier;
    private final float forwardLaunchSpeedRatio;

    private float cooldownTimer;
    private float activeTimer;
    private float pendingForwardLaunchSpeedRatio;
    private boolean raceStarted;

    CooldownGadgetEffect(RogueliteCardId cardId, float cycleOffset) {
        super(cardId);
        float cooldown;
        float duration;
        float acceleration;
        float speed;
        float grip;
        float steering;
        float recoil = 1f;
        float push = 1f;
        float draftRange = 1f;
        float draftStrength = 1f;
        float launchSpeedRatio = 0f;
        switch (cardId) {
            case NITRO_PULSE:
                cooldown = 9f;
                duration = 1.4f;
                acceleration = 0.20f;
                speed = 0.06f;
                grip = 0.03f;
                steering = 0f;
                launchSpeedRatio = 0.18f;
                break;
            case GRIP_FAN:
                cooldown = 8.5f;
                duration = 1.8f;
                acceleration = 0.08f;
                speed = 0.02f;
                grip = 0.18f;
                steering = 0.12f;
                break;
            case RAM_REACTOR:
                cooldown = 8f;
                duration = 1.6f;
                acceleration = 0.16f;
                speed = 0.04f;
                grip = 0.08f;
                steering = 0.04f;
                recoil = 0.35f;
                push = 1.85f;
                break;
            case DRAFT_MAGNET:
                cooldown = 8f;
                duration = 2f;
                acceleration = 0.12f;
                speed = 0.05f;
                grip = 0.05f;
                steering = 0f;
                draftRange = 1.50f;
                draftStrength = 1.35f;
                break;
            case PHASE_SHIELD:
                cooldown = 7.2f;
                duration = 1.8f;
                acceleration = 0.18f;
                speed = 0.06f;
                grip = 0.12f;
                steering = 0.06f;
                recoil = 0.20f;
                push = 1.18f;
                break;
            case ROCKET_EXHAUST:
                cooldown = 7f;
                duration = 1.7f;
                acceleration = 0.28f;
                speed = 0.09f;
                grip = 0.06f;
                steering = 0f;
                launchSpeedRatio = 0.27f;
                break;
            case GRAVITY_WELL:
                cooldown = 6.5f;
                duration = 2.1f;
                acceleration = 0.20f;
                speed = 0.06f;
                grip = 0.28f;
                steering = 0.16f;
                recoil = 0.55f;
                push = 1.30f;
                break;
            case OVERDRIVE_COIL:
                cooldown = 6f;
                duration = 2f;
                acceleration = 0.30f;
                speed = 0.11f;
                grip = 0.10f;
                steering = 0.04f;
                break;
            case HYPERDRIVE:
                cooldown = 5f;
                duration = 2.4f;
                acceleration = 0.38f;
                speed = 0.15f;
                grip = 0.16f;
                steering = 0.08f;
                recoil = 0.65f;
                push = 1.25f;
                launchSpeedRatio = 0.40f;
                break;
            case CROWN_ENGINE:
                cooldown = 5.5f;
                duration = 2.5f;
                acceleration = 0.28f;
                speed = 0.12f;
                grip = 0.25f;
                steering = 0.12f;
                recoil = 0.25f;
                push = 1.70f;
                break;
            default:
                throw new IllegalArgumentException("Unsupported gadget card: " + cardId);
        }
        cooldownSeconds = cooldown;
        durationSeconds = duration;
        accelerationBonus = acceleration;
        maxSpeedBonus = speed;
        gripBonus = grip;
        steeringBonus = steering;
        recoilMultiplier = recoil;
        pushMultiplier = push;
        slipstreamRangeMultiplier = draftRange;
        slipstreamStrengthMultiplier = draftStrength;
        forwardLaunchSpeedRatio = launchSpeedRatio;
        float phase = RogueliteEffectMath.clamp(cycleOffset, 0f, 1f);
        cooldownTimer = cooldownSeconds * (0.18f + phase * 0.42f);
    }

    @Override
    boolean isActive() {
        return activeTimer > 0f;
    }

    @Override
    boolean isReady() {
        return raceStarted && cooldownTimer <= 0f && activeTimer <= 0f;
    }

    @Override
    float readiness() {
        if (isActive() || isReady()) {
            return 1f;
        }
        return RogueliteEffectMath.clamp(
                1f - cooldownTimer / Math.max(0.001f, cooldownSeconds),
                0f,
                1f);
    }

    @Override
    int activeDisplayPriority() {
        return 3;
    }

    @Override
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        if (!raceStarted) {
            raceStarted = frame.throttle > 0.05f || frame.speedRatio > 0.05f;
            return;
        }
        cooldownTimer = Math.max(0f, cooldownTimer - timerDelta);
        if (activeTimer > 0f) {
            activeTimer = Math.max(0f, activeTimer - timerDelta);
            return;
        }
        if (cooldownTimer <= 0f && shouldActivate(frame)) {
            activeTimer = durationSeconds;
            cooldownTimer = cooldownSeconds;
            pendingForwardLaunchSpeedRatio = forwardLaunchSpeedRatio;
        }
    }

    private boolean shouldActivate(RogueliteDrivingFrame frame) {
        if (!frame.onRoad || frame.throttle <= 0.05f) {
            return false;
        }
        boolean longStraight =
                frame.speedRatio >= 0.24f
                        && frame.cornerSeverity <= 0.10f
                        && (frame.nextCornerSeverity <= 0.13f
                                || frame.nextCornerDistance >= 0.52f);
        boolean cornerDemand =
                frame.cornerSeverity >= 0.13f
                        || (frame.nextCornerSeverity >= 0.18f
                                && frame.nextCornerDistance <= 0.56f);
        switch (getCardId()) {
            case NITRO_PULSE:
            case ROCKET_EXHAUST:
            case OVERDRIVE_COIL:
            case HYPERDRIVE:
                return longStraight;
            case GRIP_FAN:
                return cornerDemand;
            case RAM_REACTOR:
                return frame.speedRatio >= 0.12f
                        && frame.opponentAheadProximity >= 0.42f;
            case DRAFT_MAGNET:
                return frame.speedRatio >= 0.18f
                        && (frame.opponentAheadProximity >= 0.10f
                                || frame.slipstreamBoost >= 0.01f);
            case PHASE_SHIELD:
                return frame.nearbyOpponentProximity >= 0.35f
                        || frame.recentlyImpacted;
            case GRAVITY_WELL:
                return cornerDemand || frame.nearbyOpponentProximity >= 0.40f;
            case CROWN_ENGINE:
                return longStraight
                        || cornerDemand
                        || frame.opponentAheadProximity >= 0.30f;
            default:
                return false;
        }
    }

    @Override
    float accelerationBonus() {
        return isActive() ? accelerationBonus : 0f;
    }

    @Override
    float maxSpeedBonus() {
        return isActive() ? maxSpeedBonus : 0f;
    }

    @Override
    float gripBonus(float slip) {
        return isActive() ? gripBonus : 0f;
    }

    @Override
    float steeringBonus(float slip) {
        return isActive() ? steeringBonus : 0f;
    }

    @Override
    float frontCollisionRecoilMultiplier() {
        return isActive() ? recoilMultiplier : 1f;
    }

    @Override
    float frontCollisionPushMultiplier() {
        return isActive() ? pushMultiplier : 1f;
    }

    @Override
    float consumeForwardLaunchSpeedRatio() {
        float launchSpeedRatio = pendingForwardLaunchSpeedRatio;
        pendingForwardLaunchSpeedRatio = 0f;
        return launchSpeedRatio;
    }

    @Override
    float slipstreamRangeMultiplier() {
        return isActive() ? slipstreamRangeMultiplier : 1f;
    }

    @Override
    float slipstreamStrengthMultiplier() {
        return isActive() ? slipstreamStrengthMultiplier : 1f;
    }

    @Override
    boolean isRamChargeActive() {
        return isActive()
                && (getCardId() == RogueliteCardId.RAM_REACTOR
                        || getCardId() == RogueliteCardId.CROWN_ENGINE);
    }

    @Override
    void consumeRamCharge() {
        if (isRamChargeActive()) {
            activeTimer = 0f;
        }
    }
}
