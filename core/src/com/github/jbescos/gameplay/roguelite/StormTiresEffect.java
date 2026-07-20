package com.github.jbescos.gameplay.roguelite;

final class StormTiresEffect extends RogueliteUpgradeEffect {
    StormTiresEffect(int level) {
        super(level);
    }

    @Override
    float adjustSurfaceGrip(float baseGripMultiplier) {
        if (baseGripMultiplier >= 1f) {
            return baseGripMultiplier;
        }
        float retainedLoss =
                RogueliteEffectMath.levelValue(level, 0.30f, 0.50f, 0.70f);
        return 1f - (1f - baseGripMultiplier) * (1f - retainedLoss);
    }
}
