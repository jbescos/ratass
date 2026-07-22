package com.github.jbescos;

final class CarLongitudinalModel {
    static final float KPH_PER_MPS = 3.6f;
    static final float DEFAULT_TOP_SPEED_KPH = 300f;
    static final float DEFAULT_AERO_DRAG = 0.006f;
    static final float DEFAULT_LINEAR_DAMPING = 0.03f;

    // Balanced for the default 550 hp / 1300 kg car without increasing launch acceleration.
    private static final float HORSEPOWER_TO_FORCE = 0.12f;
    private static final float DRIVE_TRACTION_MULTIPLIER = 1.45f;
    private static final float MIN_ENGINE_FORCE_RATIO_AT_LIMIT = 0.75f;
    private static final float ENGINE_FORCE_CURVE_EXPONENT = 2.35f;

    private CarLongitudinalModel() {
    }

    static float engineForce(float horsePower) {
        return horsePower * HORSEPOWER_TO_FORCE;
    }

    static float engineForceRatio(float speedRatio) {
        float clamped = Math.max(0f, Math.min(speedRatio, 1f));
        float highSpeedBlend = (float) Math.pow(clamped, ENGINE_FORCE_CURVE_EXPONENT);
        return 1f - (1f - MIN_ENGINE_FORCE_RATIO_AT_LIMIT) * highSpeedBlend;
    }

    static float driveTractionLimit(
            float mass,
            float lateralGripPerSecond,
            float wheelGrip,
            float surfaceGripMultiplier) {
        return mass
                * lateralGripPerSecond
                * wheelGrip
                * surfaceGripMultiplier
                * DRIVE_TRACTION_MULTIPLIER;
    }
}
