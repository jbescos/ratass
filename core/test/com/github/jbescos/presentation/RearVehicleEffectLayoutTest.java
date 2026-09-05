package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RearVehicleEffectLayoutTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void centeredLayoutUsesOneEmitterOnTheCenterLine() {
        assertEquals(1, RearVehicleEffectLayout.emitterCount(true));
        assertEquals(0f, RearVehicleEffectLayout.lateralSign(true, 0), EPSILON);
    }

    @Test
    public void pairedLayoutRetainsLeftAndRightEmitters() {
        assertEquals(2, RearVehicleEffectLayout.emitterCount(false));
        assertEquals(-1f, RearVehicleEffectLayout.lateralSign(false, 0), EPSILON);
        assertEquals(1f, RearVehicleEffectLayout.lateralSign(false, 1), EPSILON);
    }
}
