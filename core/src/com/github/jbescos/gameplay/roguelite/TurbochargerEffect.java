package com.github.jbescos.gameplay.roguelite;

final class TurbochargerEffect extends RogueliteUpgradeEffect {
    private final boolean synergy;
    private float fullThrottleTimer;

    TurbochargerEffect(int level, boolean synergy) {
        super(level);
        this.synergy = synergy;
    }

    @Override
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        if (frame.onRoad && frame.throttle >= 0.92f) {
            fullThrottleTimer = Math.min(3f, fullThrottleTimer + delta);
        } else {
            fullThrottleTimer = Math.max(0f, fullThrottleTimer - delta * 2f);
        }
    }

    @Override
    float accelerationBonus() {
        return RogueliteEffectMath.levelValue(level, 0.06f, 0.10f, 0.14f);
    }

    @Override
    float maxSpeedBonus() {
        if (level < 2 || fullThrottleTimer < 1.5f) {
            return 0f;
        }
        return synergy ? 0.055f : 0.04f;
    }
}
