package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class VendettaHookBarrierTest {
    @Test
    public void blocksCrossingThroughTheLiveHookSegment() {
        VendettaHookBarrier barrier = new VendettaHookBarrier();
        barrier.start(7, 5f);
        barrier.rememberSide(3, 0f, 0f, 10f, 0f, 5f, 2f);

        assertEquals(1, barrier.getRememberedSide(3));
        assertFalse(barrier.blocksCrossing(3, 0f, 0f, 10f, 0f, 5f, 1f));
        assertTrue(barrier.blocksCrossing(3, 0f, 0f, 10f, 0f, 5f, -0.1f));
        assertTrue(barrier.blocksCrossing(3, 0f, 0f, 10f, 0f, 5f, 0f));
    }

    @Test
    public void allowsCarsToDriveAroundEitherHookEndpoint() {
        VendettaHookBarrier barrier = new VendettaHookBarrier();
        barrier.start(7, 5f);
        barrier.rememberSide(3, 0f, 0f, 10f, 0f, 5f, 2f);

        assertFalse(barrier.blocksCrossing(3, 0f, 0f, 10f, 0f, -1f, -1f));
        assertFalse(barrier.blocksCrossing(3, 0f, 0f, 10f, 0f, 11f, -1f));
    }

    @Test
    public void expiresAndClearsRecordedSides() {
        VendettaHookBarrier barrier = new VendettaHookBarrier();
        barrier.start(7, 5f);
        barrier.rememberSide(3, 0f, 0f, 10f, 0f, 5f, 2f);

        barrier.advance(5.1f);

        assertFalse(barrier.isActive());
        assertEquals(-1, barrier.getTargetVehicleId());
        assertEquals(0, barrier.getRememberedSide(3));
    }
}
