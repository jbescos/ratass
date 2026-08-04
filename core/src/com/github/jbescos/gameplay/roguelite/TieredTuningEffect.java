package com.github.jbescos.gameplay.roguelite;

/** Permanent race-performance tuning with distinct handling tradeoffs. */
final class TieredTuningEffect extends RogueliteUpgradeEffect {
    private final float accelerationBonus;
    private final float maxSpeedBonus;
    private final float dragMultiplier;
    private final float massMultiplier;
    private final float gripBonus;
    private final float slidingGripLoss;
    private final float steeringBonus;
    private final float slidingSteeringBonus;
    private final float recoilMultiplier;
    private final float pushMultiplier;

    TieredTuningEffect(RogueliteCardId cardId) {
        super(cardId);
        float acceleration;
        float speed;
        float drag;
        float mass;
        float grip;
        float slidingGrip = 0f;
        float steering;
        float slidingSteering = 0f;
        float recoil;
        float push;
        switch (cardId) {
            case CLUB_TUNE:
                acceleration = 0.06f;
                speed = 0.02f;
                drag = 1f;
                mass = 1f;
                grip = 0.04f;
                steering = 0.02f;
                recoil = 1f;
                push = 1f;
                break;
            case SPORT_TUNE:
                acceleration = 0.08f;
                speed = 0.03f;
                drag = 1f;
                mass = 0.98f;
                grip = 0.02f;
                steering = 0.04f;
                recoil = 1f;
                push = 1f;
                break;
            case AERO_TRIM:
                acceleration = 0.06f;
                speed = 0.07f;
                drag = 0.92f;
                mass = 1f;
                grip = 0.02f;
                steering = 0f;
                recoil = 1f;
                push = 1f;
                break;
            case SHORT_GEARING:
                acceleration = 0.12f;
                speed = -0.04f;
                drag = 1.01f;
                mass = 1f;
                grip = 0.06f;
                steering = 0.04f;
                recoil = 0.98f;
                push = 1.02f;
                break;
            case RACE_TUNE:
                acceleration = 0.34f;
                speed = 0.14f;
                drag = 0.93f;
                mass = 1.06f;
                grip = 0.16f;
                steering = 0.05f;
                recoil = 0.90f;
                push = 1.09f;
                break;
            case HEAVYWEIGHT_TUNE:
                acceleration = 0.40f;
                speed = 0.14f;
                drag = 0.92f;
                mass = 1.16f;
                grip = 0.18f;
                steering = 0.06f;
                recoil = 0.82f;
                push = 1.18f;
                break;
            case LOW_DRAG_BODY:
                acceleration = 0.38f;
                speed = 0.20f;
                drag = 0.78f;
                mass = 1.08f;
                grip = 0.16f;
                steering = 0.04f;
                recoil = 0.95f;
                push = 1.03f;
                break;
            case DRIFT_DIFFERENTIAL:
                acceleration = 0.48f;
                speed = 0.20f;
                drag = 0.88f;
                mass = 1.12f;
                grip = 0.20f;
                slidingGrip = 0.04f;
                steering = 0.03f;
                recoil = 1f;
                push = 1.08f;
                break;
            case CHAMPIONSHIP_TUNE:
                acceleration = 0.56f;
                speed = 0.24f;
                drag = 0.80f;
                mass = 1.10f;
                grip = 0.22f;
                steering = 0.06f;
                recoil = 0.76f;
                push = 1.25f;
                break;
            case GROUND_EFFECT:
                acceleration = 0.62f;
                speed = 0.24f;
                drag = 0.90f;
                mass = 1.14f;
                grip = 0.24f;
                steering = 0.06f;
                recoil = 0.70f;
                push = 1.20f;
                break;
            case VELOCITY_SHELL:
                acceleration = 0.68f;
                speed = 0.32f;
                drag = 0.68f;
                mass = 1.10f;
                grip = 0.18f;
                steering = 0.04f;
                recoil = 0.90f;
                push = 1.10f;
                break;
            case TORQUE_VECTORING:
                acceleration = 0.68f;
                speed = 0.32f;
                drag = 0.68f;
                mass = 1.10f;
                grip = 0.18f;
                steering = 0.04f;
                recoil = 0.84f;
                push = 1.22f;
                break;
            default:
                throw new IllegalArgumentException("Unsupported tuning card: " + cardId);
        }
        accelerationBonus = acceleration;
        maxSpeedBonus = speed;
        dragMultiplier = drag;
        massMultiplier = mass;
        gripBonus = grip;
        slidingGripLoss = slidingGrip;
        steeringBonus = steering;
        slidingSteeringBonus = slidingSteering;
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
        return gripBonus - slidingGripLoss * slideProgress(slip);
    }

    @Override
    float steeringBonus(float slip) {
        return steeringBonus + slidingSteeringBonus * slideProgress(slip);
    }

    @Override
    float frontCollisionRecoilMultiplier() {
        return recoilMultiplier;
    }

    @Override
    float frontCollisionPushMultiplier() {
        return pushMultiplier;
    }

    private static float slideProgress(float slip) {
        float slide = RogueliteEffectMath.clamp((slip - 0.34f) / 0.16f, 0f, 1f);
        return slide * slide * (3f - 2f * slide);
    }
}
