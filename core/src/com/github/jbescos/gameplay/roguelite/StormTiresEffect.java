package com.github.jbescos.gameplay.roguelite;

final class StormTiresEffect extends RogueliteUpgradeEffect {
    StormTiresEffect() {
        super(RogueliteCardId.STORM_TIRES);
    }

    @Override
    boolean isActive() {
        return latestFrame != null && latestFrame.adverseWeather;
    }

    @Override
    int activeDisplayPriority() {
        return 1;
    }

    @Override
    float adjustSurfaceGrip(float baseGripMultiplier) {
        if (baseGripMultiplier >= 1f) {
            return baseGripMultiplier;
        }
        float retainedLoss = 0.50f;
        return 1f - (1f - baseGripMultiplier) * (1f - retainedLoss);
    }
}
