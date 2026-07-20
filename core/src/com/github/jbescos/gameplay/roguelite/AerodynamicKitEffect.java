package com.github.jbescos.gameplay.roguelite;

final class AerodynamicKitEffect extends RogueliteUpgradeEffect {
    AerodynamicKitEffect(int level) {
        super(level);
    }

    @Override
    float timedEffectDecay() {
        return level >= 2 ? 0.75f : 1f;
    }

    @Override
    float dragMultiplier() {
        return RogueliteEffectMath.levelValue(level, 0.95f, 0.91f, 0.87f);
    }
}
