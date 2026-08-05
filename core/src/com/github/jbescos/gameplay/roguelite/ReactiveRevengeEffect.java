package com.github.jbescos.gameplay.roguelite;

/** Timed retaliation effects armed by a qualified hit from another car. */
final class ReactiveRevengeEffect extends RogueliteUpgradeEffect {
    private static final float REVENGE_WINDOW_SECONDS = 12f;

    private enum ActivationCondition {
        COLLISION,
        NEARBY_OPPONENT
    }

    private final float durationSeconds;
    private final float accelerationBonus;
    private final float maxSpeedBonus;
    private final float gripBonus;
    private final float steeringBonus;
    private final float recoilMultiplier;
    private final float pushMultiplier;
    private final float slipstreamRangeMultiplier;
    private final float slipstreamStrengthMultiplier;
    private final float draftMagnetRangeMultiplier;
    private final float draftMagnetForceMultiplier;
    private final ActivationCondition activationCondition;
    private final float activationProximity;
    private final boolean draftMagnet;
    private final boolean ramCharge;
    private final boolean impactCounter;

    private float activeTimer;
    private float armedTimer;

    ReactiveRevengeEffect(RogueliteCardId cardId) {
        super(cardId);
        switch (cardId) {
            case DRAFT_MAGNET:
                durationSeconds = 1.2f;
                accelerationBonus = 0.12f;
                maxSpeedBonus = 0.05f;
                gripBonus = 0.05f;
                steeringBonus = 0f;
                recoilMultiplier = 1f;
                pushMultiplier = 1f;
                slipstreamRangeMultiplier = 1.50f;
                slipstreamStrengthMultiplier = 1.35f;
                draftMagnetRangeMultiplier = 1f;
                draftMagnetForceMultiplier = 1f;
                activationCondition = ActivationCondition.NEARBY_OPPONENT;
                activationProximity = 0.20f;
                draftMagnet = true;
                ramCharge = false;
                impactCounter = false;
                break;
            case RAM_REACTOR:
                durationSeconds = 0.7f;
                accelerationBonus = 0f;
                maxSpeedBonus = 0f;
                gripBonus = 0f;
                steeringBonus = 0f;
                recoilMultiplier = 1f;
                pushMultiplier = 1f;
                slipstreamRangeMultiplier = 1f;
                slipstreamStrengthMultiplier = 1f;
                draftMagnetRangeMultiplier = 1f;
                draftMagnetForceMultiplier = 1f;
                activationCondition = ActivationCondition.COLLISION;
                activationProximity = 0f;
                draftMagnet = false;
                ramCharge = false;
                impactCounter = true;
                break;
            case REPULSOR_SURGE:
                durationSeconds = 1.8f;
                accelerationBonus = 0.22f;
                maxSpeedBonus = 0.08f;
                gripBonus = 0.16f;
                steeringBonus = 0.08f;
                recoilMultiplier = 0.40f;
                pushMultiplier = 1.35f;
                slipstreamRangeMultiplier = 1.25f;
                slipstreamStrengthMultiplier = 1.20f;
                draftMagnetRangeMultiplier = 1.35f;
                draftMagnetForceMultiplier = 1.55f;
                activationCondition = ActivationCondition.NEARBY_OPPONENT;
                activationProximity = 0.16f;
                draftMagnet = true;
                ramCharge = false;
                impactCounter = false;
                break;
            default:
                throw new IllegalArgumentException("Unsupported revenge card: " + cardId);
        }
    }

    @Override
    boolean isActive() {
        return activeTimer > 0f;
    }

    @Override
    boolean isReady() {
        return armedTimer > 0f
                && activeTimer <= 0f;
    }

    @Override
    boolean isArmed() {
        return armedTimer > 0f;
    }

    @Override
    float readiness() {
        return 1f;
    }

    @Override
    float activeTimeRemainingSeconds() {
        return activeTimer;
    }

    @Override
    int activeDisplayPriority() {
        return 4;
    }

    @Override
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        if (activeTimer > 0f) {
            activeTimer = Math.max(0f, activeTimer - timerDelta);
            return;
        }
        if (!impactCounter) {
            armedTimer = Math.max(0f, armedTimer - timerDelta);
        }
        if (armedTimer > 0f
                && shouldActivate(frame)) {
            activeTimer = durationSeconds;
            armedTimer = 0f;
        }
    }

    @Override
    void onHitBy(int vehicleId, float impactStrength) {
        if (vehicleId >= 0 && impactStrength > 0f && activeTimer <= 0f) {
            armedTimer = REVENGE_WINDOW_SECONDS;
        }
    }

    private boolean shouldActivate(RogueliteDrivingFrame frame) {
        if (!frame.onRoad) {
            return false;
        }
        switch (activationCondition) {
            case COLLISION:
                return false;
            case NEARBY_OPPONENT:
            default:
                return frame.nearbyOpponentProximity >= activationProximity;
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
    boolean isDraftMagnetActive() {
        return draftMagnet && isActive();
    }

    @Override
    float draftMagnetRangeMultiplier() {
        return isDraftMagnetActive() ? draftMagnetRangeMultiplier : 1f;
    }

    @Override
    float draftMagnetForceMultiplier() {
        return isDraftMagnetActive() ? draftMagnetForceMultiplier : 1f;
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
        return ramCharge && isActive();
    }

    @Override
    void consumeRamCharge() {
        if (isRamChargeActive()) {
            activeTimer = 0f;
        }
    }

    @Override
    boolean isImpactCounterReady() {
        return impactCounter && isReady();
    }

    @Override
    void consumeImpactCounter() {
        if (!isImpactCounterReady()) {
            return;
        }
        armedTimer = 0f;
        activeTimer = durationSeconds;
    }
}
