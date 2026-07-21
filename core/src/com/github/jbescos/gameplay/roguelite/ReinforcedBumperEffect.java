package com.github.jbescos.gameplay.roguelite;

final class ReinforcedBumperEffect extends RogueliteUpgradeEffect {
    private static final float ACTIVATION_SECONDS = 0.75f;
    private float activationTimer;

    ReinforcedBumperEffect(int level) {
        super(RogueliteCardId.REINFORCED_BUMPER, level);
    }

    @Override
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        activationTimer = Math.max(0f, activationTimer - delta);
    }

    @Override
    boolean isActive() {
        return activationTimer > 0f;
    }

    @Override
    int activeDisplayPriority() {
        return 2;
    }

    @Override
    void onCollision(float impactStrength) {
        if (impactStrength > 0f) {
            activationTimer = Math.max(activationTimer, ACTIVATION_SECONDS);
        }
    }

    @Override
    float frontCollisionRecoilMultiplier() {
        return 1f - RogueliteEffectMath.levelValue(level, 0.15f, 0.25f, 0.35f);
    }

    @Override
    float frontCollisionPushMultiplier() {
        return 1f + RogueliteEffectMath.levelValue(level, 0.10f, 0.18f, 0.26f);
    }
}
