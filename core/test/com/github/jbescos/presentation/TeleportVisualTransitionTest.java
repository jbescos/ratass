package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class TeleportVisualTransitionTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void startsAtOldPositionAndEndsAtMovingPhysicalDestination() {
        TeleportVisualTransition transition = new TeleportVisualTransition();
        transition.start(2f, 3f, 0f, 12f, 8f, 1f);

        assertTrue(transition.isActive());
        assertEquals(2f, transition.getRenderX(12f), EPSILON);
        assertEquals(3f, transition.getRenderY(8f), EPSILON);

        transition.update(TeleportVisualTransition.DURATION_SECONDS * 0.5f);
        assertEquals(8f, transition.getRenderX(13f), EPSILON);
        assertEquals(6.5f, transition.getRenderY(9f), EPSILON);

        transition.update(TeleportVisualTransition.DURATION_SECONDS * 0.5f);
        assertFalse(transition.isActive());
        assertEquals(14f, transition.getRenderX(14f), EPSILON);
        assertEquals(10f, transition.getRenderY(10f), EPSILON);
    }

    @Test
    public void invalidTransitionDoesNotBecomeActive() {
        TeleportVisualTransition transition = new TeleportVisualTransition();

        transition.start(Float.NaN, 0f, 0f, 1f, 1f, 0f);

        assertFalse(transition.isActive());
    }
}
