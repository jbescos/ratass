package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RaceIncidentPopupTest {
    @Test
    public void combinesTheOriginalImpactAndRevenge() {
        RaceIncidentPopup popup = new RaceIncidentPopup();

        popup.recordHit(1, "You", 4, "Car 4", 0.75f);
        popup.showRevenge(1, "You", 4, "Car 4", "Vendetta Hook");
        popup.update(0.2f);

        assertTrue(popup.isVisible());
        assertEquals("CAR 4 SLAMMED YOU", popup.getImpactLine());
        assertEquals("YOU USED VENDETTA HOOK ON CAR 4", popup.getDetailLine());
        assertEquals(2, popup.getLogLineCount());
        assertEquals("HIT: CAR 4 SLAMMED YOU", popup.getLogLine(0));
        assertEquals(
                "REVENGE: YOU USED VENDETTA HOOK ON CAR 4",
                popup.getLogLine(1));
        assertEquals(1f, popup.getAlpha(), 0.0001f);
    }

    @Test
    public void appendsSeparateIncidentsWhileEarlierOnesAreVisible() {
        RaceIncidentPopup popup = new RaceIncidentPopup();

        popup.recordHit(1, "You", 2, "Car 2", 0.4f);
        popup.showRevenge(1, "You", 2, "Car 2", "EMP Snare");
        popup.recordHit(3, "Car 3", 4, "Car 4", 0.1f);
        popup.showRevenge(3, "Car 3", 4, "Car 4", "Tar Tether");

        assertEquals(3, popup.getQueuedCount());
        assertEquals(4, popup.getLogLineCount());
        assertEquals(
                "REVENGE: CAR 3 USED TAR TETHER ON CAR 4",
                popup.getLogLine(3));

        popup.update(4.3f);

        assertTrue(popup.isVisible());
    }

    @Test
    public void repeatedFramesDoNotQueueTheSameActiveRevenge() {
        RaceIncidentPopup popup = new RaceIncidentPopup();

        popup.recordHit(1, "You", 2, "Car 2", 0.5f);
        popup.showRevenge(1, "You", 2, "Car 2", "Draft Magnet");
        popup.showRevenge(1, "You", 2, "Car 2", "Draft Magnet");

        assertEquals(1, popup.getQueuedCount());
        assertEquals(2, popup.getLogLineCount());
        popup.update(4.3f);
        assertTrue(popup.isVisible());
    }

    @Test
    public void resetClearsVisibleAndPendingIncidents() {
        RaceIncidentPopup popup = new RaceIncidentPopup();
        popup.recordHit(1, "You", 2, "Car 2", 1f);
        popup.showRevenge(1, "You", 2, "Car 2", "Impact Reversal");

        popup.reset();

        assertFalse(popup.isVisible());
        assertEquals("", popup.getHeadline());
        assertEquals(0f, popup.getAlpha(), 0.0001f);
    }

    @Test
    public void overtakeNamesBothCarsAndCyclesCameraParticipants() {
        RaceIncidentPopup popup = new RaceIncidentPopup();

        popup.showOvertake(4, "Car 4", 2, "Car 2", 2);
        popup.update(0.2f);

        assertTrue(popup.isVisible());
        assertFalse(popup.isRevenge());
        assertEquals("CAR 4 PASSED CAR 2", popup.getImpactLine());
        assertEquals("2 POSITIONS GAINED", popup.getDetailLine());
        assertEquals(4, popup.nextCameraVehicleId());
        assertEquals(2, popup.nextCameraVehicleId());
        assertEquals(4, popup.nextCameraVehicleId());
    }

    @Test
    public void finishingVehicleRemovesAndSuppressesItsIncidents() {
        RaceIncidentPopup popup = new RaceIncidentPopup();
        popup.recordHit(1, "You", 2, "Car 2", 0.5f);
        popup.showRevenge(1, "You", 2, "Car 2", "Draft Magnet");
        popup.showOvertake(3, "Car 3", 4, "Car 4", 1);

        popup.discardVehicle(1);

        assertTrue(popup.isVisible());
        assertEquals(1, popup.getLogLineCount());
        assertEquals("PASS: CAR 3 PASSED CAR 4", popup.getLogLine(0));
        popup.showRevenge(1, "You", 5, "Car 5", "EMP Snare");
        assertEquals(0, popup.getQueuedCount());
    }

    @Test
    public void revengeBecomesLatestWithoutRemovingEarlierLogLines() {
        RaceIncidentPopup popup = new RaceIncidentPopup();
        popup.showOvertake(4, "Car 4", 2, "Car 2", 1);
        long overtakeSequence = popup.getDisplaySequence();

        popup.recordHit(1, "You", 3, "Car 3", 0.7f);
        popup.showRevenge(1, "You", 3, "Car 3", "Doom Hex");

        assertTrue(popup.isRevenge());
        assertTrue(popup.getDisplaySequence() > overtakeSequence);
        assertEquals(1, popup.getPrimaryVehicleId());
        assertEquals("YOU USED DOOM HEX ON CAR 3", popup.getDetailLine());
        assertEquals(3, popup.getLogLineCount());
        assertEquals("PASS: CAR 4 PASSED CAR 2", popup.getLogLine(0));
    }

    @Test
    public void keepsOnlyTheSixMostRecentLogLines() {
        RaceIncidentPopup popup = new RaceIncidentPopup();

        for (int i = 0; i < 7; i++) {
            popup.showOvertake(i, "Car " + i, i + 10, "Car " + (i + 10), 1);
            popup.update(1f);
        }

        assertEquals(6, popup.getLogLineCount());
        assertEquals("PASS: CAR 1 PASSED CAR 11", popup.getLogLine(0));
        assertEquals("PASS: CAR 6 PASSED CAR 16", popup.getLogLine(5));
    }

    @Test
    public void clickingAChosenLogLineCyclesItsParticipants() {
        RaceIncidentPopup popup = new RaceIncidentPopup();
        popup.showOvertake(4, "Car 4", 2, "Car 2", 1);
        popup.showOvertake(7, "Car 7", 5, "Car 5", 1);

        assertEquals(4, popup.nextCameraVehicleId(0));
        assertEquals(2, popup.nextCameraVehicleId(0));
        assertEquals(7, popup.nextCameraVehicleId(1));
        assertEquals(5, popup.nextCameraVehicleId(1));
    }
}
