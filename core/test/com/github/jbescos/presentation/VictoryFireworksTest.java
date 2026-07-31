package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VictoryFireworksTest {
    @Test
    public void resetLaunchesTheFirstDeterministicBurst() {
        VictoryFireworks fireworks = new VictoryFireworks();
        float firstX = fireworks.getX(0);
        float firstY = fireworks.getY(0);

        fireworks.update(0.8f);
        assertNotEquals(firstY, fireworks.getY(0), 0.0001f);
        assertTrue(fireworks.getAlpha(0) < 1f);

        fireworks.reset();
        assertEquals(1, fireworks.getLaunchedBurstCount());
        assertEquals(VictoryFireworks.PARTICLES_PER_BURST, fireworks.getActiveCount());
        assertEquals(firstX, fireworks.getX(0), 0.0001f);
        assertEquals(firstY, fireworks.getY(0), 0.0001f);
    }

    @Test
    public void animationLaunchesMoreBurstsWithoutExceedingCapacity() {
        VictoryFireworks fireworks = new VictoryFireworks();

        fireworks.update(0.8f);

        assertTrue(fireworks.getLaunchedBurstCount() >= 2);
        assertTrue(fireworks.getActiveCount() <= fireworks.getCapacity());
    }
}
