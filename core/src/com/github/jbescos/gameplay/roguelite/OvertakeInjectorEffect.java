package com.github.jbescos.gameplay.roguelite;

final class OvertakeInjectorEffect extends RogueliteUpgradeEffect {
    private final boolean synergy;
    private float boostTimer;

    OvertakeInjectorEffect(int level, boolean synergy) {
        super(level);
        this.synergy = synergy;
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
        float duration = RogueliteEffectMath.levelValue(level, 1.0f, 1.3f, 1.6f);
        if (level >= 2 && synergy && slipstreamBoost > 0.05f) {
            duration *= 2f;
        }
        boostTimer = Math.max(boostTimer, duration * Math.min(2, positionsGained));
    }

    @Override
    float accelerationBonus() {
        return boostTimer > 0f
                ? RogueliteEffectMath.levelValue(level, 0.07f, 0.11f, 0.15f)
                : 0f;
    }

    @Override
    float maxSpeedBonus() {
        return boostTimer > 0f
                ? RogueliteEffectMath.levelValue(level, 0.07f, 0.11f, 0.15f) * 0.45f
                : 0f;
    }
}
