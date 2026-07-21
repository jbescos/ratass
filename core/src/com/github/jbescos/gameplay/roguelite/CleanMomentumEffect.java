package com.github.jbescos.gameplay.roguelite;

final class CleanMomentumEffect extends RogueliteUpgradeEffect {
    private float momentum;

    CleanMomentumEffect(int level) {
        super(RogueliteCardId.CLEAN_MOMENTUM, level);
    }

    @Override
    boolean isActive() {
        return momentum > 0.01f;
    }

    @Override
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        if (frame.onRoad && !frame.recentlyImpacted && frame.speedRatio >= 0.20f) {
            momentum =
                    Math.min(
                            1f,
                            momentum
                                    + delta
                                            / RogueliteEffectTuning.CLEAN_MOMENTUM_BUILD_SECONDS);
        } else if (!frame.onRoad) {
            momentum = 0f;
        }
    }

    @Override
    void onCollision(float impactStrength) {
        momentum = level >= 2 ? momentum * 0.65f : 0f;
    }

    @Override
    float maxSpeedBonus() {
        return RogueliteEffectMath.levelValue(level, 0.03f, 0.05f, 0.07f) * momentum;
    }
}
