package com.github.jbescos.gameplay.roguelite;

abstract class RogueliteUpgradeEffect {
    protected final int level;

    RogueliteUpgradeEffect(int level) {
        this.level = level;
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
