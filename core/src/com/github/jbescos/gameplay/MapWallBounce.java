package com.github.jbescos.gameplay;

/** Calculates the separating impulse used when a car contacts a map barrier. */
public final class MapWallBounce {
    private MapWallBounce() {
    }

    public static float requiredImpulse(
            float outwardSpeed,
            float mass,
            float targetOutwardSpeed,
            float maxImpulse) {
        if (!Float.isFinite(outwardSpeed)
                || !Float.isFinite(mass)
                || !Float.isFinite(targetOutwardSpeed)
                || !Float.isFinite(maxImpulse)
                || mass <= 0f
                || targetOutwardSpeed <= 0f
                || maxImpulse <= 0f) {
            return 0f;
        }
        float missingOutwardSpeed = targetOutwardSpeed - outwardSpeed;
        if (missingOutwardSpeed <= 0f) {
            return 0f;
        }
        return Math.min(maxImpulse, missingOutwardSpeed * mass);
    }
}
