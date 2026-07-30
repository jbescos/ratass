package com.github.jbescos.gameplay.roguelite;

final class OvertakeInjectorEffect extends RogueliteUpgradeEffect {
    private final boolean synergy;
    private float boostTimer;

    OvertakeInjectorEffect(boolean synergy) {
        super(RogueliteCardId.OVERTAKE_INJECTOR);
        this.synergy = synergy;
    }

    @Override
    boolean isActive() {
        return boostTimer > 0f;
    }

    @Override
    int activeDisplayPriority() {
        return 2;
    }

    @Override
    boolean tracksRacePosition() {
        return true;
    }

    @Override
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        boostTimer = Math.max(0f, boostTimer - timerDelta);
    }

    @Override
    void onRacePositionImproved(int positionsGained, float slipstreamBoost) {
        if (positionsGained <= 0) {
            return;
        }
        float duration = 1.3f;
        if (synergy && slipstreamBoost > 0.05f) {
            duration *= 1.6f;
        }
        boostTimer = Math.max(boostTimer, duration * Math.min(2, positionsGained));
    }

    @Override
    float accelerationBonus() {
        return boostTimer > 0f
                ? 0.11f
                : 0f;
    }

    @Override
    float maxSpeedBonus() {
        return boostTimer > 0f
                ? 0.11f * 0.45f
                : 0f;
    }
}
