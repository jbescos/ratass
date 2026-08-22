package com.github.jbescos.gameplay;

/** Converts card-specific impact mass into stable collision response factors. */
public final class CarCollisionMassResponse {
    private CarCollisionMassResponse() {
    }

    public static float correctionFraction(float collisionMassMultiplier) {
        float safeMultiplier = Math.max(1f, collisionMassMultiplier);
        return 1f - 1f / safeMultiplier;
    }

    public static float cancellationImpulseX(
            float normalX,
            float normalY,
            float normalImpulse,
            float tangentImpulse,
            float direction) {
        return direction * (normalX * normalImpulse + normalY * tangentImpulse);
    }

    public static float cancellationImpulseY(
            float normalX,
            float normalY,
            float normalImpulse,
            float tangentImpulse,
            float direction) {
        return direction * (normalY * normalImpulse - normalX * tangentImpulse);
    }

    public static float collisionFreeCoordinate(
            float stepStart,
            float postCollisionVelocity,
            float deltaSeconds) {
        return stepStart + postCollisionVelocity * Math.max(0f, deltaSeconds);
    }

    public static boolean protectsFromContact(
            boolean ownCollisionFieldActive,
            boolean otherCollisionFieldActive) {
        return ownCollisionFieldActive && !otherCollisionFieldActive;
    }

    public static float reboundMultiplier(
            float ownCollisionMassMultiplier,
            float otherCollisionMassMultiplier) {
        float ownMass = Math.max(1f, ownCollisionMassMultiplier);
        float otherMass = Math.max(1f, otherCollisionMassMultiplier);
        return otherMass / ownMass;
    }
}
