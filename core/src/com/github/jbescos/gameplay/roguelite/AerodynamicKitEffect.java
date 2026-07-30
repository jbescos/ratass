package com.github.jbescos.gameplay.roguelite;

final class AerodynamicKitEffect extends RogueliteUpgradeEffect {
    AerodynamicKitEffect() {
        super(RogueliteCardId.AERODYNAMIC_KIT);
    }

    @Override
    boolean isActive() {
        return latestFrame != null && latestFrame.speedRatio > 0.05f;
    }

    @Override
    float timedEffectDecay() {
        return 0.82f;
    }

    @Override
    float dragMultiplier() {
        return 0.91f;
    }
}
