package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EventCameraDirectorTest {
    @Test
    public void followsNewlyArmedRevengeForAtMostThreeSeconds() {
        EventCameraDirector director = new EventCameraDirector();

        director.observeRevengeArmed(4, true);

        assertEquals(4, director.consumeRequestedVehicleId());
        assertTrue(director.isLocked());
        director.update(2.99f);
        assertTrue(director.isLocked());
        director.update(0.01f);
        assertFalse(director.isLocked());

        director.observeRevengeArmed(4, true);
        assertEquals(-1, director.consumeRequestedVehicleId());
    }

    @Test
    public void revengeExecutionSwitchesToTargetForTwoSeconds() {
        EventCameraDirector director = new EventCameraDirector();
        director.observeRevengeArmed(4, true);
        assertEquals(4, director.consumeRequestedVehicleId());

        assertTrue(director.revengeExecuted(4, 7, 1L));

        assertEquals(-1, director.consumeRequestedVehicleId());
        director.update(1.99f);
        assertEquals(-1, director.consumeRequestedVehicleId());
        director.update(0.01f);
        assertEquals(7, director.consumeRequestedVehicleId());
        director.observeRevengeArmed(4, false);
        director.update(1.99f);
        assertTrue(director.isLocked());
        director.update(0.01f);
        assertFalse(director.isLocked());
    }

    @Test
    public void eventsDuringAnotherRevengeLockAreIgnoredRatherThanQueued() {
        EventCameraDirector director = new EventCameraDirector();
        director.observeRevengeArmed(2, true);
        assertEquals(2, director.consumeRequestedVehicleId());

        director.observeRevengeArmed(5, true);
        assertFalse(director.revengeExecuted(5, 8, 1L));
        assertEquals(-1, director.consumeRequestedVehicleId());

        director.update(3f);
        assertFalse(director.isLocked());
        director.observeRevengeArmed(5, true);
        assertEquals(-1, director.consumeRequestedVehicleId());
    }

    @Test
    public void repeatedFramesFromOneActivationDoNotExtendTargetLock() {
        EventCameraDirector director = new EventCameraDirector();

        assertTrue(director.revengeExecuted(4, 7, 12L));
        assertEquals(7, director.consumeRequestedVehicleId());
        director.update(1.5f);
        assertFalse(director.revengeExecuted(4, 7, 12L));
        director.update(0.5f);

        assertFalse(director.isLocked());
        assertEquals(-1, director.consumeRequestedVehicleId());
    }

    @Test
    public void completingPreparationWithoutExecutionReleasesCameraEarly() {
        EventCameraDirector director = new EventCameraDirector();
        director.observeRevengeArmed(3, true);
        director.consumeRequestedVehicleId();

        director.observeRevengeArmed(3, false);

        assertFalse(director.isLocked());
    }

    @Test
    public void requestsPlayerOnceAfterFiveSecondsWithoutEvents() {
        EventCameraDirector director = new EventCameraDirector();

        director.update(4.99f);
        assertFalse(director.consumePlayerFallbackRequested());
        director.update(0.01f);
        assertTrue(director.consumePlayerFallbackRequested());
        assertFalse(director.consumePlayerFallbackRequested());

        director.update(20f);
        assertFalse(director.consumePlayerFallbackRequested());
    }

    @Test
    public void aNewIncidentRestartsThePlayerFallbackTimer() {
        EventCameraDirector director = new EventCameraDirector();
        director.update(4f);

        director.observeIncident();
        director.update(4.99f);
        assertFalse(director.consumePlayerFallbackRequested());
        director.update(0.01f);
        assertTrue(director.consumePlayerFallbackRequested());
    }

    @Test
    public void ordinaryIncidentsCannotMoveCameraMoreOftenThanEveryTwoSeconds() {
        EventCameraDirector director = new EventCameraDirector();

        director.observeIncident(2);
        assertEquals(2, director.consumeRequestedVehicleId());

        director.update(1f);
        director.observeIncident(5);
        assertEquals(-1, director.consumeRequestedVehicleId());

        director.update(1f);
        assertEquals(-1, director.consumeRequestedVehicleId());
        director.observeIncident(8);
        assertEquals(8, director.consumeRequestedVehicleId());
    }
}
