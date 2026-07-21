package com.github.jbescos.gameplay.roguelite;

abstract class RogueliteUpgradeEffect {
    private final RogueliteCardId cardId;
    protected final int level;
    protected RogueliteDrivingFrame latestFrame;

    RogueliteUpgradeEffect(RogueliteCardId cardId, int level) {
        this.cardId = cardId;
        this.level = level;
    }

    final void advance(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        latestFrame = frame;
        update(delta, timerDelta, frame);
    }

    final RogueliteCardId getCardId() {
        return cardId;
    }

    boolean isActive() {
        return false;
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

    void onRacePositionImproved(int positionsGained, float slipstreamBoost) {
    }

    void onCollision(float impactStrength) {
    }
}
