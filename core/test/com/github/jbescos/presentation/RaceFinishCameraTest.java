package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RaceFinishCameraTest {
    @Test
    public void eventCameraHoldsWinnerForThreeSecondsThenReleasesEvents() {
        assertTrue(RaceFinishCamera.shouldFocusWinner(true, true, false, true, 0f));
        assertTrue(RaceFinishCamera.shouldFocusWinner(true, true, false, true, 2.99f));
        assertFalse(RaceFinishCamera.shouldFocusWinner(true, true, false, true, 3f));
        assertTrue(RaceFinishCamera.shouldFocusWinner(true, false, true, true, 3f));
        assertFalse(RaceFinishCamera.shouldFocusWinner(false, true, false, true, 0f));
        assertFalse(RaceFinishCamera.shouldFocusWinner(true, false, false, true, 0f));
        assertFalse(RaceFinishCamera.shouldFocusWinner(true, true, false, false, 0f));
    }

    @Test
    public void finishShotZoomsInWithoutPassingMinimum() {
        assertTrue(RaceFinishCamera.focusedZoom(1.2f, 0.6f) < 1.2f);
        assertEquals(0.6f, RaceFinishCamera.focusedZoom(0.7f, 0.6f), 0f);
    }
}
