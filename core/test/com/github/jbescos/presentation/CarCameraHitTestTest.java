package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class CarCameraHitTestTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void detectsPointsInsideRotatedCar() {
        float score = CarCameraHitTest.hitScore(
                4f,
                6.5f,
                4f,
                5f,
                (float) (Math.PI * 0.5),
                2f,
                1f,
                0f);

        assertEquals(2.25f, score, EPSILON);
    }

    @Test
    public void touchPaddingExpandsHitAreaWithoutSelectingDistantPoints() {
        assertEquals(
                1.21f,
                CarCameraHitTest.hitScore(1.1f, 0f, 0f, 0f, 0f, 1f, 2f, 0.2f),
                EPSILON);
        assertTrue(Float.isInfinite(
                CarCameraHitTest.hitScore(1.3f, 0f, 0f, 0f, 0f, 1f, 2f, 0.2f)));
    }
}
