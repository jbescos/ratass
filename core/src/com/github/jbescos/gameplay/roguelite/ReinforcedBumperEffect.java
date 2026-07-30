package com.github.jbescos.gameplay.roguelite;

final class ReinforcedBumperEffect extends RogueliteUpgradeEffect {
    private static final float ACTIVATION_SECONDS = 0.75f;
    private float activationTimer;

    ReinforcedBumperEffect() {
        super(RogueliteCardId.REINFORCED_BUMPER);
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
        return 0.75f;
    }

    @Override
    float frontCollisionPushMultiplier() {
        return 1.18f;
    }
}
