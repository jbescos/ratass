package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class CarSizeTransitionTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void growsAndShrinksWithoutJumpingToTheTarget() {
        float grown = CarSizeTransition.update(1f, 2f, 1f / 60f);
        float shrunk = CarSizeTransition.update(2f, 1f, 1f / 60f);

        assertTrue(grown > 1f && grown < 2f);
        assertTrue(shrunk > 1f && shrunk < 2f);
    }

    @Test
    public void reachesEitherTargetQuicklyAndNeverDropsBelowNormalSize() {
        float scale = 1f;
        for (int frame = 0; frame < 15; frame++) {
            scale = CarSizeTransition.update(scale, 2f, 1f / 60f);
        }
        assertTrue(scale > 1.99f);

        for (int frame = 0; frame < 15; frame++) {
            scale = CarSizeTransition.update(scale, 1f, 1f / 60f);
        }
        assertEquals(1f, scale, 0.01f);
        assertEquals(1f, CarSizeTransition.update(0f, 0f, 1f), EPSILON);
    }
}
