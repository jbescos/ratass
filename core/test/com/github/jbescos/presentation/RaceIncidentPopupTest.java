package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RaceIncidentPopupTest {
    @Test
    public void revengeBecomesTheLatestCameraIncident() {
        RaceIncidentPopup popup = new RaceIncidentPopup();

        popup.recordHit(1, 4);
        long hitSequence = popup.getDisplaySequence();
        popup.showRevenge(1, 4, "Vendetta Hook");

        assertTrue(popup.isVisible());
        assertTrue(popup.getDisplaySequence() > hitSequence);
        assertEquals(1, popup.getPrimaryVehicleId());
        assertEquals(2, popup.getIncidentCount());
    }

    @Test
    public void repeatedFramesDoNotRepeatTheSameRevengeIncident() {
        RaceIncidentPopup popup = new RaceIncidentPopup();

        popup.recordHit(1, 2);
        popup.showRevenge(1, 2, "Draft Magnet");
        long sequence = popup.getDisplaySequence();
        popup.showRevenge(1, 2, "Draft Magnet");

        assertEquals(sequence, popup.getDisplaySequence());
        assertEquals(2, popup.getIncidentCount());
    }

    @Test
    public void resetClearsVisibleAndPendingIncidents() {
        RaceIncidentPopup popup = new RaceIncidentPopup();
        popup.recordHit(1, 2);
        popup.showRevenge(1, 2, "Draft Magnet");

        popup.reset();

        assertFalse(popup.isVisible());
        assertEquals(-1, popup.getPrimaryVehicleId());
        assertEquals(0, popup.getIncidentCount());
    }

    @Test
    public void overtakeSelectsTheOvertakerForTheEventCamera() {
        RaceIncidentPopup popup = new RaceIncidentPopup();

        popup.showOvertake(4, 2, 2);

        assertTrue(popup.isVisible());
        assertEquals(4, popup.getPrimaryVehicleId());
    }

    @Test
    public void finishingVehicleRemovesAndSuppressesItsIncidents() {
        RaceIncidentPopup popup = new RaceIncidentPopup();
        popup.recordHit(1, 2);
        popup.showRevenge(1, 2, "Draft Magnet");
        popup.showOvertake(3, 4, 1);

        popup.discardVehicle(1);
        popup.showRevenge(1, 5, "EMP Snare");

        assertTrue(popup.isVisible());
        assertEquals(1, popup.getIncidentCount());
        assertEquals(3, popup.getPrimaryVehicleId());
    }

    @Test
    public void keepsOnlyTheSixMostRecentCameraIncidents() {
        RaceIncidentPopup popup = new RaceIncidentPopup();

        for (int i = 0; i < 7; i++) {
            popup.showOvertake(i, i + 10, 1);
            popup.update(1f);
        }

        assertEquals(6, popup.getIncidentCount());
        assertEquals(6, popup.getPrimaryVehicleId());
    }
}
