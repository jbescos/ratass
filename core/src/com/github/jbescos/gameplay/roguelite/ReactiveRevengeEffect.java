package com.github.jbescos.gameplay.roguelite;

/** Timed retaliation effects armed by a qualified hit from another car. */
final class ReactiveRevengeEffect extends RevengeUpgradeEffect {
    private static final float REVENGE_WINDOW_SECONDS = 12f;

    private final float durationSeconds;
    private final float accelerationBonus;
    private final float gripBonus;
    private final float steeringBonus;
    private final float recoilMultiplier;
    private final float pushMultiplier;
    private final float slipstreamRangeMultiplier;
    private final float slipstreamStrengthMultiplier;
    private final float draftMagnetRangeMultiplier;
    private final float draftMagnetForceMultiplier;
    private final float activationProximity;
    private final boolean draftMagnet;
    private final boolean ramCharge;

    private float activeTimer;
    private float armedTimer;
    private float effectMultiplier = 1f;
    private boolean amplificationApplied;

    ReactiveRevengeEffect(RogueliteCardId cardId) {
        super(cardId, RevengeWorkflow.PROXIMITY);
        switch (cardId) {
            case DRAFT_MAGNET:
                durationSeconds = 2f;
                accelerationBonus = 0.12f;
                gripBonus = 0.05f;
                steeringBonus = 0f;
                recoilMultiplier = 1f;
                pushMultiplier = 1f;
                slipstreamRangeMultiplier = 1.50f;
                slipstreamStrengthMultiplier = 1.35f;
                draftMagnetRangeMultiplier = 1f;
                draftMagnetForceMultiplier = 1f;
                activationProximity = 0.20f;
                draftMagnet = true;
                ramCharge = false;
                break;
            case REPULSOR_SURGE:
                durationSeconds = 2f;
                accelerationBonus = 0.22f;
                gripBonus = 0.16f;
                steeringBonus = 0.08f;
                recoilMultiplier = 0.40f;
                pushMultiplier = 1.35f;
                slipstreamRangeMultiplier = 1.25f;
                slipstreamStrengthMultiplier = 1.20f;
                draftMagnetRangeMultiplier = 1.75f;
                draftMagnetForceMultiplier = 1.55f;
                activationProximity = 0.16f;
                draftMagnet = true;
                ramCharge = false;
                break;
            case REPULSOR_WAVE:
                durationSeconds = 2f;
                accelerationBonus = 0.17f;
                gripBonus = 0.105f;
                steeringBonus = 0.04f;
                recoilMultiplier = 0.70f;
                pushMultiplier = 1.175f;
                slipstreamRangeMultiplier = 1.375f;
                slipstreamStrengthMultiplier = 1.275f;
                draftMagnetRangeMultiplier = 1.375f;
                draftMagnetForceMultiplier = 1.275f;
                activationProximity = 0.18f;
                draftMagnet = true;
                ramCharge = false;
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
        return armedTimer > 0f && activeTimer <= 0f;
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
        armedTimer = Math.max(0f, armedTimer - timerDelta);
        if (armedTimer > 0f
                && shouldActivate(frame)) {
            activeTimer = durationSeconds;
            armedTimer = 0f;
        }
    }

    @Override
    protected void prepareFromHit(int vehicleId, float impactStrength) {
        armedTimer = REVENGE_WINDOW_SECONDS;
        effectMultiplier = 1f;
        amplificationApplied = false;
    }

    @Override
    protected boolean isExecutionInProgress() {
        return activeTimer > 0f;
    }

    @Override
    void amplifyActiveRevenge(float multiplier) {
        if (!isActive() || amplificationApplied) {
            return;
        }
        float safeMultiplier = sanitizeAmplifier(multiplier);
        activeTimer *= safeMultiplier;
        effectMultiplier = safeMultiplier;
        amplificationApplied = true;
    }

    private boolean shouldActivate(RogueliteDrivingFrame frame) {
        return frame.onRoad
                && frame.revengeNearbyOpponentProximity >= activationProximity;
    }

    @Override
    float accelerationBonus() {
        return isActive() ? accelerationBonus * effectMultiplier : 0f;
    }

    @Override
    float gripBonus(float slip) {
        return isActive() ? gripBonus * effectMultiplier : 0f;
    }

    @Override
    float steeringBonus(float slip) {
        return isActive() ? steeringBonus * effectMultiplier : 0f;
    }

    @Override
    float frontCollisionRecoilMultiplier() {
        return isActive() ? amplifyDeviation(recoilMultiplier) : 1f;
    }

    @Override
    float frontCollisionPushMultiplier() {
        return isActive() ? amplifyDeviation(pushMultiplier) : 1f;
    }

    @Override
    boolean isDraftMagnetActive() {
        return draftMagnet && isActive();
    }

    @Override
    float draftMagnetRangeMultiplier() {
        return isDraftMagnetActive() ? amplifyDeviation(draftMagnetRangeMultiplier) : 1f;
    }

    @Override
    float draftMagnetForceMultiplier() {
        return isDraftMagnetActive() ? amplifyDeviation(draftMagnetForceMultiplier) : 1f;
    }

    @Override
    float slipstreamRangeMultiplier() {
        return isActive() ? amplifyDeviation(slipstreamRangeMultiplier) : 1f;
    }

    @Override
    float slipstreamStrengthMultiplier() {
        return isActive() ? amplifyDeviation(slipstreamStrengthMultiplier) : 1f;
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

    private float amplifyDeviation(float value) {
        return Math.max(0f, 1f + (value - 1f) * effectMultiplier);
    }

    private static float sanitizeAmplifier(float multiplier) {
        return Float.isFinite(multiplier) ? Math.max(1f, multiplier) : 1f;
    }

}
