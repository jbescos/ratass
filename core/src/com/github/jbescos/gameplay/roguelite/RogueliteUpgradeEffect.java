package com.github.jbescos.gameplay.roguelite;

abstract class RogueliteUpgradeEffect {
    private final RogueliteCardId cardId;
    protected RogueliteDrivingFrame latestFrame;

    RogueliteUpgradeEffect(RogueliteCardId cardId) {
        this.cardId = cardId;
    }

    final void advance(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        latestFrame = frame;
        update(delta, timerDelta, frame);
    }

    final RogueliteCardId getCardId() {
        return cardId;
    }

    RogueliteCardId behaviorCardId() {
        return cardId;
    }

    RogueliteCardId activeDisplayCardId() {
        return cardId;
    }

    boolean isActive() {
        return false;
    }

    boolean isReady() {
        return false;
    }

    boolean isArmed() {
        return false;
    }

    float readiness() {
        return 0f;
    }

    float activeTimeRemainingSeconds() {
        return 0f;
    }

    float cooldownTimeRemainingSeconds() {
        return 0f;
    }

    int activeDisplayPriority() {
        return 0;
    }

    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
    }

    float timedEffectDecay() {
        return 1f;
    }

    boolean tracksRacePosition() {
        return false;
    }

    float adjustSurfaceGrip(float baseGripMultiplier) {
        return baseGripMultiplier;
    }

    float accelerationBonus() {
        return 0f;
    }

    float maxSpeedBonus() {
        return 0f;
    }

    float dragMultiplier() {
        return 1f;
    }

    float massMultiplier() {
        return 1f;
    }

    float gripBonus(float slip) {
        return 0f;
    }

    float steeringBonus(float slip) {
        return 0f;
    }

    float slipstreamRangeMultiplier() {
        return 1f;
    }

    float slipstreamStrengthMultiplier() {
        return 1f;
    }

    float slipstreamReleaseLerp(float baseReleaseLerp) {
        return baseReleaseLerp;
    }

    float frontCollisionRecoilMultiplier() {
        return 1f;
    }

    float frontCollisionPushMultiplier() {
        return 1f;
    }

    float consumeForwardLaunchSpeedRatio() {
        return 0f;
    }

    boolean isDraftMagnetActive() {
        return false;
    }

    float draftMagnetRangeMultiplier() {
        return 1f;
    }

    float draftMagnetForceMultiplier() {
        return 1f;
    }

    boolean isRamChargeActive() {
        return false;
    }

    void consumeRamCharge() {
    }

    boolean isImpactCounterReady() {
        return false;
    }

    void consumeImpactCounter() {
    }

    float consumeRaceBlackoutSeconds() {
        return 0f;
    }

    void onRacePositionImproved(int positionsGained, float slipstreamBoost) {
    }

    void onCollision(float impactStrength) {
    }

    void onHitBy(int vehicleId, float impactStrength) {
    }

    void onContactEnded(int vehicleId) {
    }

    boolean isInvisible() {
        return false;
    }

    void deferInvisibilityExpiration() {
    }

    int revengeTargetVehicleId() {
        return -1;
    }

    RogueliteRevengeStrike tryActivateOffenderStrike(
            int targetVehicleId,
            float distance,
            boolean offenderAhead) {
        return null;
    }
}
