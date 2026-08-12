package com.github.jbescos.gameplay.roguelite;

/** Calculates the physical impulse applied by each Hunter Barrage shot. */
public final class HunterBarragePush {
    static final float PUSH_SPEED_RATIO = 0.14f;
    static final float MAX_COLLISION_IMPULSE_MULTIPLIER = 8f;

    private HunterBarragePush() {
    }

    public static float pushSpeed(float maxForwardSpeed) {
        return sanitize(maxForwardSpeed) * PUSH_SPEED_RATIO;
    }

    public static float impulse(
            float bodyMass,
            float maxForwardSpeed,
            float maxCollisionImpulse) {
        float desiredImpulse = sanitize(bodyMass) * pushSpeed(maxForwardSpeed);
        float impulseLimit = sanitize(maxCollisionImpulse) * MAX_COLLISION_IMPULSE_MULTIPLIER;
        return Math.min(desiredImpulse, impulseLimit);
    }

    private static float sanitize(float value) {
        return Float.isFinite(value) ? Math.max(0f, value) : 0f;
    }
}
