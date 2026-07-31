package com.github.jbescos.gameplay.roguelite;

/** Permanent race-performance tuning with distinct handling tradeoffs. */
final class TieredTuningEffect extends RogueliteUpgradeEffect {
    private final float accelerationBonus;
    private final float maxSpeedBonus;
    private final float dragMultiplier;
    private final float massMultiplier;
    private final float gripBonus;
    private final float steeringBonus;
    private final float recoilMultiplier;
    private final float pushMultiplier;

    TieredTuningEffect(RogueliteCardId cardId) {
        super(cardId);
        float acceleration;
        float speed;
        float drag;
        float mass;
        float grip;
        float steering;
        float recoil;
        float push;
        switch (cardId) {
            case CLUB_TUNE:
                acceleration = 0.05f;
                speed = 0.02f;
                drag = 1f;
                mass = 1f;
                grip = 0.04f;
                steering = 0.02f;
                recoil = 1f;
                push = 1f;
                break;
            case SPORT_TUNE:
                acceleration = 0.07f;
                speed = 0.03f;
                drag = 0.99f;
                mass = 0.92f;
                grip = 0.05f;
                steering = 0.05f;
                recoil = 1f;
                push = 1f;
                break;
            case RACE_TUNE:
                acceleration = 0.14f;
                speed = 0.06f;
                drag = 0.98f;
                mass = 1f;
                grip = 0.10f;
                steering = 0.05f;
                recoil = 0.90f;
                push = 1.09f;
                break;
            case HEAVYWEIGHT_TUNE:
                acceleration = 0.24f;
                speed = 0.08f;
                drag = 0.97f;
                mass = 1.16f;
                grip = 0.14f;
                steering = 0.06f;
                recoil = 0.82f;
                push = 1.18f;
                break;
            case CHAMPIONSHIP_TUNE:
                acceleration = 0.27f;
                speed = 0.12f;
                drag = 0.90f;
                mass = 1f;
                grip = 0.18f;
                steering = 0.08f;
                recoil = 0.76f;
                push = 1.25f;
                break;
            default:
                throw new IllegalArgumentException("Unsupported tuning card: " + cardId);
        }
        accelerationBonus = acceleration;
        maxSpeedBonus = speed;
        dragMultiplier = drag;
        massMultiplier = mass;
        gripBonus = grip;
        steeringBonus = steering;
        recoilMultiplier = recoil;
        pushMultiplier = push;
    }

    @Override
    boolean isActive() {
        return true;
    }

    @Override
    float accelerationBonus() {
        return accelerationBonus;
    }

    @Override
    float maxSpeedBonus() {
        return maxSpeedBonus;
    }

    @Override
    float dragMultiplier() {
        return dragMultiplier;
    }

    @Override
    float massMultiplier() {
        return massMultiplier;
    }

    @Override
    float gripBonus(float slip) {
        return gripBonus;
    }

    @Override
    float steeringBonus(float slip) {
        return steeringBonus;
    }

    @Override
    float frontCollisionRecoilMultiplier() {
        return recoilMultiplier;
    }

    @Override
    float frontCollisionPushMultiplier() {
        return pushMultiplier;
    }
}
