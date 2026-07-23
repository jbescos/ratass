package com.github.jbescos;

final class CarLongitudinalModel {
    static final float KPH_PER_MPS = 3.6f;
    static final float DEFAULT_TOP_SPEED_KPH = 280f;
    static final float DEFAULT_AERO_DRAG = 0.0030f;

    private static final float MIN_ENGINE_FORCE_RATIO_AT_LIMIT = 0.18f;
    private static final float ENGINE_FORCE_CURVE_EXPONENT = 2.35f;

    private CarLongitudinalModel() {
    }

    static float engineForceRatio(float speedRatio) {
        float clamped = Math.max(0f, Math.min(speedRatio, 1f));
        float highSpeedBlend = (float) Math.pow(clamped, ENGINE_FORCE_CURVE_EXPONENT);
        return 1f - (1f - MIN_ENGINE_FORCE_RATIO_AT_LIMIT) * highSpeedBlend;
    }
}
