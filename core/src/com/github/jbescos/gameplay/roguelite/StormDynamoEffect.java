package com.github.jbescos.gameplay.roguelite;

final class StormDynamoEffect extends RogueliteUpgradeEffect {
    private final boolean synergy;
    private float charge;
    private float retentionTimer;

    StormDynamoEffect(boolean synergy) {
        super(RogueliteCardId.STORM_DYNAMO);
        this.synergy = synergy;
    }

    @Override
    boolean isActive() {
        return charge > 0.01f;
    }

    @Override
    int activeDisplayPriority() {
        return 1;
    }

    @Override
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        if (frame.adverseWeather) {
            charge = Math.min(1f, charge + delta / (synergy ? 3.5f : 5f));
            retentionTimer = 2.5f;
        } else if (retentionTimer > 0f) {
            retentionTimer = Math.max(0f, retentionTimer - timerDelta);
        } else {
            charge = Math.max(0f, charge - delta * 0.5f);
        }
    }

    @Override
    float accelerationBonus() {
        float bonus = 0.08f * charge;
        return synergy ? bonus * 1.15f : bonus;
    }
}
