package com.github.jbescos.presentation;

/** Presentation-only emitter placement for exhaust flames and brake lights. */
public final class RearVehicleEffectLayout {
    private RearVehicleEffectLayout() {}

    public static int emitterCount(boolean centered) {
        return centered ? 1 : 2;
    }

    public static float lateralSign(boolean centered, int emitterIndex) {
        if (centered) {
            if (emitterIndex != 0) {
                throw new IndexOutOfBoundsException("Centered layout has one emitter");
            }
            return 0f;
        }
        if (emitterIndex == 0) {
            return -1f;
        }
        if (emitterIndex == 1) {
            return 1f;
        }
        throw new IndexOutOfBoundsException("Paired layout has two emitters");
    }
}
