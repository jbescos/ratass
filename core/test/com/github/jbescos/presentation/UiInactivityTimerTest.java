package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class UiInactivityTimerTest {
    @Test
    public void expiresAfterConfiguredActiveTime() {
        UiInactivityTimer timer = new UiInactivityTimer(30f);

        assertEquals(30, timer.getRemainingWholeSeconds());
        assertFalse(timer.update(29.1f));
        assertEquals(1, timer.getRemainingWholeSeconds());
        assertTrue(timer.update(0.9f));
        assertEquals(0, timer.getRemainingWholeSeconds());
    }

    @Test
    public void resetRestoresTheFullDecisionWindow() {
        UiInactivityTimer timer = new UiInactivityTimer(30f);
        timer.update(18f);

        timer.reset();

        assertEquals(30, timer.getRemainingWholeSeconds());
        assertFalse(timer.update(-4f));
        assertEquals(30, timer.getRemainingWholeSeconds());
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsNonPositiveTimeouts() {
        new UiInactivityTimer(0f);
    }
}
