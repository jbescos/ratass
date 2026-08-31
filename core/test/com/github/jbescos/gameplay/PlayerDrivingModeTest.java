package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PlayerDrivingModeTest {
    @Test
    public void togglesBetweenAutomaticAndManual() {
        assertTrue(PlayerDrivingMode.AUTOMATIC.isAutomatic());
        assertEquals(PlayerDrivingMode.MANUAL, PlayerDrivingMode.AUTOMATIC.toggle());
        assertFalse(PlayerDrivingMode.MANUAL.isAutomatic());
        assertEquals(PlayerDrivingMode.AUTOMATIC, PlayerDrivingMode.MANUAL.toggle());
    }

    @Test
    public void loadsStoredModesAndDefaultsUnknownValuesToAutomatic() {
        assertEquals(PlayerDrivingMode.MANUAL, PlayerDrivingMode.fromStoredValue("manual"));
        assertEquals(PlayerDrivingMode.AUTOMATIC, PlayerDrivingMode.fromStoredValue("unknown"));
        assertEquals(PlayerDrivingMode.AUTOMATIC, PlayerDrivingMode.fromStoredValue(null));
    }
}
