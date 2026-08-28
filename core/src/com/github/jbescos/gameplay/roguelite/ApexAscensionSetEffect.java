package com.github.jbescos.gameplay.roguelite;

/** Fixed vehicle-stat bonus granted by completing Apex Ascension. */
final class ApexAscensionSetEffect extends RogueliteUpgradeEffect {
    private static final float STEERING_TORQUE_COMPENSATION = 1f / 0.595f;

    ApexAscensionSetEffect() {
        // Retain the existing internal ID so saves and strategy observations stay stable.
        super(RogueliteCardId.TEMPORAL_DOMINION);
    }

    @Override
    float accelerationBonus() {
        return 0.8f;
    }

    @Override
    float driveForceLimitMultiplier() {
        return 1.8f / 0.7f;
    }

    @Override
    float dragMultiplier() {
        return 0.5f;
    }

    @Override
    float massMultiplier() {
        return 0.7f;
    }

    @Override
    float gripBonus(float slip) {
        return 0.2f;
    }

    @Override
    float steeringTorqueMultiplier() {
        return STEERING_TORQUE_COMPENSATION;
    }
}
