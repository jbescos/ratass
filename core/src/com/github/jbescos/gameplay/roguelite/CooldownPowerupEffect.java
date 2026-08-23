package com.github.jbescos.gameplay.roguelite;

/** A deterministic, context-triggered powerup with a cooldown. */
final class CooldownPowerupEffect extends RogueliteUpgradeEffect {
    private final float cooldownSeconds;
    private final float durationSeconds;
    private final float accelerationBonus;
    private final float gripBonus;
    private final float steeringBonus;
    private final float recoilMultiplier;
    private final float pushMultiplier;
    private final float forwardLaunchSpeedRatio;
    private final float launchTargetSpeedRatio;
    private final boolean invisibility;

    private float cooldownTimer;
    private float activeTimer;
    private float pendingForwardLaunchSpeedRatio;
    private boolean raceStarted;
    private boolean loadedByRandomCard;
    private boolean invisibilityExitHeld;
    private boolean deferInvisibilityExit;

    CooldownPowerupEffect(RogueliteCardId cardId, float cycleOffset) {
        super(cardId);
        float cooldown;
        float duration;
        float acceleration;
        float grip;
        float steering;
        float recoil = 1f;
        float push = 1f;
        float launchSpeedRatio = 0f;
        float launchTargetSpeed = 1f;
        switch (cardId) {
            case TIME_RIPPLE:
            case CHRONO_SHIFT:
            case TEMPORAL_DOMINION:
                cooldown = TimeDilationPowerupSpec.cooldownSeconds(cardId);
                duration = TimeDilationPowerupSpec.durationSeconds(cardId);
                acceleration = 0f;
                grip = 0f;
                steering = 0f;
                break;
            case MIRROR_DUO:
            case MIRROR_TRIO:
            case OVERDRIVE_COIL:
                cooldown = MirrorPowerupSpec.COOLDOWN_SECONDS;
                duration = MirrorPowerupSpec.durationSeconds(cardId);
                acceleration = 0f;
                grip = 0f;
                steering = 0f;
                break;
            case BULK_FIELD:
            case TITAN_FIELD:
            case COLOSSUS_FIELD:
                cooldown = CollisionFieldPowerupSpec.cooldownSeconds(cardId);
                duration = CollisionFieldPowerupSpec.DURATION_SECONDS;
                acceleration = 0f;
                grip = CollisionFieldPowerupSpec.GRIP_BONUS;
                steering = 0f;
                break;
            case NITRO_PULSE:
                cooldown = 9f;
                duration = 1.4f;
                acceleration = 0.20f;
                grip = 0.03f;
                steering = 0f;
                launchSpeedRatio = 0.18f;
                launchTargetSpeed = 0.60f;
                break;
            case ACE_HOTLINE:
                cooldown = 20f;
                duration = 10f;
                acceleration = 0f;
                grip = 0f;
                steering = 0f;
                break;
            case GRIP_FAN:
                cooldown = 8.5f;
                duration = 1.8f;
                acceleration = 0.15f;
                grip = 0.10f;
                steering = 0.06f;
                break;
            case PHASE_SHIELD:
                cooldown = 6.8f;
                duration = 2f;
                acceleration = 0.22f;
                grip = 0.20f;
                steering = 0.12f;
                recoil = 0.20f;
                push = 1.18f;
                break;
            case ROCKET_EXHAUST:
                cooldown = 6.8f;
                duration = 2f;
                acceleration = 0.32f;
                grip = 0.08f;
                steering = 0f;
                launchSpeedRatio = 0.20f;
                launchTargetSpeed = 0.56f;
                break;
            case GRAVITY_WELL:
                cooldown = 5.8f;
                duration = 3.6f;
                acceleration = 0.40f;
                grip = 0.22f;
                steering = 0.12f;
                recoil = 0.55f;
                push = 1.30f;
                break;
            case HYPERDRIVE:
                cooldown = 6.5f;
                duration = 3.3f;
                acceleration = 0.38f;
                grip = 0.16f;
                steering = 0.08f;
                recoil = 0.65f;
                push = 1.25f;
                launchSpeedRatio = 0.22f;
                launchTargetSpeed = 0.54f;
                break;
            case GHOST_CLOAK:
                cooldown = 10f;
                duration = 3f;
                acceleration = 0f;
                grip = 0f;
                steering = 0f;
                break;
            case PHANTOM_CLOAK:
                cooldown = 10f;
                duration = 4f;
                acceleration = 0f;
                grip = 0f;
                steering = 0f;
                break;
            case VOID_CLOAK:
                cooldown = 10f;
                duration = 5f;
                acceleration = 0f;
                grip = 0f;
                steering = 0f;
                break;
            default:
                throw new IllegalArgumentException("Unsupported powerup card: " + cardId);
        }
        cooldownSeconds = cooldown;
        durationSeconds = duration;
        accelerationBonus = acceleration;
        gripBonus = grip;
        steeringBonus = steering;
        recoilMultiplier = recoil;
        pushMultiplier = push;
        forwardLaunchSpeedRatio = launchSpeedRatio;
        launchTargetSpeedRatio = launchTargetSpeed;
        invisibility = cardId == RogueliteCardId.GHOST_CLOAK
                || cardId == RogueliteCardId.PHANTOM_CLOAK
                || cardId == RogueliteCardId.VOID_CLOAK;
        float phase = RogueliteEffectMath.clamp(cycleOffset, 0f, 1f);
        cooldownTimer = cooldownSeconds * (0.18f + phase * 0.42f);
    }

    @Override
    boolean isActive() {
        return activeTimer > 0f || invisibilityExitHeld;
    }

    @Override
    boolean isReady() {
        return raceStarted && cooldownTimer <= 0f && !isActive();
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
    float activeTimeRemainingSeconds() {
        return activeTimer;
    }

    @Override
    float cooldownTimeRemainingSeconds() {
        return cooldownTimer;
    }

    @Override
    void onLoadedByRandomCard() {
        loadedByRandomCard = true;
        activeTimer = 0f;
        cooldownTimer = cooldownSeconds;
        pendingForwardLaunchSpeedRatio = 0f;
        invisibilityExitHeld = false;
        deferInvisibilityExit = false;
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
        if (!invisibility || !isActive()) {
            cooldownTimer = Math.max(0f, cooldownTimer - timerDelta);
        }
        if (isActive()) {
            activeTimer = Math.max(0f, activeTimer - timerDelta);
            boolean wasHeld = invisibilityExitHeld;
            invisibilityExitHeld = invisibility && activeTimer <= 0f && deferInvisibilityExit;
            deferInvisibilityExit = false;
            if (invisibility && wasHeld && !invisibilityExitHeld) {
                cooldownTimer = cooldownSeconds;
            } else if (invisibility && activeTimer <= 0f && !invisibilityExitHeld) {
                cooldownTimer = cooldownSeconds;
            }
            return;
        }
        deferInvisibilityExit = false;
        if (cooldownTimer <= 0f
                && (loadedByRandomCard || shouldActivate(frame))) {
            activeTimer = durationSeconds;
            loadedByRandomCard = false;
            cooldownTimer = invisibility ? 0f : cooldownSeconds;
            pendingForwardLaunchSpeedRatio =
                    Math.min(
                            forwardLaunchSpeedRatio,
                            Math.max(0f, launchTargetSpeedRatio - frame.speedRatio));
        }
    }

    private boolean shouldActivate(RogueliteDrivingFrame frame) {
        if (getCardId() == RogueliteCardId.ACE_HOTLINE
                || TimeDilationPowerupSpec.isTimeDilationCard(getCardId())) {
            return true;
        }
        if (!frame.onRoad) {
            return false;
        }
        boolean longStraight =
                frame.throttle > 0.05f
                        && frame.speedRatio >= 0.24f
                        && frame.cornerSeverity <= 0.06f
                        && (frame.nextCornerSeverity <= 0.13f
                                || frame.nextCornerDistance >= 0.85f);
        boolean stableLaunchStraight = longStraight && frame.slip <= 0.025f;
        boolean mirrorStraight = frame.cornerSeverity <= 0.08f;
        boolean cornerDemand =
                frame.cornerSeverity >= 0.13f
                        || (frame.nextCornerSeverity >= 0.18f
                                && frame.nextCornerDistance <= 0.56f)
                        || frame.slip >= 0.04f;
        switch (getCardId()) {
            case NITRO_PULSE:
                return stableLaunchStraight
                        && frame.speedRatio <= 0.48f
                        && !frame.forwardLaneBlocked;
            case ROCKET_EXHAUST:
                return stableLaunchStraight
                        && frame.speedRatio <= 0.50f
                        && !frame.forwardLaneBlocked;
            case OVERDRIVE_COIL:
            case MIRROR_DUO:
            case MIRROR_TRIO:
                return mirrorStraight && frame.nearbyOpponentProximity > 0f;
            case BULK_FIELD:
            case TITAN_FIELD:
            case COLOSSUS_FIELD:
                return frame.nearbyOpponentProximity > 0f;
            case HYPERDRIVE:
                return stableLaunchStraight
                        && frame.speedRatio <= 0.52f
                        && !frame.forwardLaneBlocked;
            case GRIP_FAN:
                return cornerDemand;
            case PHASE_SHIELD:
                return frame.nearbyOpponentProximity >= 0.35f
                        || frame.recentlyImpacted
                        || (cornerDemand && frame.speedRatio >= 0.38f);
            case GRAVITY_WELL:
                return cornerDemand || frame.nearbyOpponentProximity >= 0.40f;
            case GHOST_CLOAK:
            case PHANTOM_CLOAK:
            case VOID_CLOAK:
                return frame.nearbyOpponentProximity > 0f;
            default:
                return false;
        }
    }

    @Override
    boolean isInvisible() {
        return invisibility && isActive();
    }

    @Override
    boolean usesBestDriver() {
        return isActive() && getCardId() == RogueliteCardId.ACE_HOTLINE;
    }

    @Override
    boolean acceleratesOwnDecisions() {
        return isActive() && TimeDilationPowerupSpec.isTimeDilationCard(getCardId());
    }

    @Override
    void deferInvisibilityExpiration() {
        if (isInvisible()) {
            deferInvisibilityExit = true;
        }
    }

    @Override
    float accelerationBonus() {
        return isActive() ? accelerationBonus : 0f;
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
    float massMultiplier() {
        return isActive() && CollisionFieldPowerupSpec.isCollisionFieldCard(getCardId())
                ? CollisionFieldPowerupSpec.MASS_MULTIPLIER
                : 1f;
    }

    @Override
    float carCollisionAreaMultiplier() {
        return isActive() ? CollisionFieldPowerupSpec.collisionAreaMultiplier(getCardId()) : 1f;
    }

    @Override
    float carCollisionMassMultiplier() {
        return isActive()
                ? CollisionFieldPowerupSpec.collisionMassMultiplier(getCardId())
                : 1f;
    }

    @Override
    float consumeForwardLaunchSpeedRatio() {
        float launchSpeedRatio = pendingForwardLaunchSpeedRatio;
        pendingForwardLaunchSpeedRatio = 0f;
        return launchSpeedRatio;
    }

    @Override
    boolean isRamChargeActive() {
        return false;
    }

    @Override
    void consumeRamCharge() {
        if (isRamChargeActive()) {
            activeTimer = 0f;
        }
    }
}
