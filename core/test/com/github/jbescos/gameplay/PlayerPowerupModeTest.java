package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class PlayerPowerupModeTest {
    @Test
    public void automaticAndManualModesToggle() {
        assertTrue(PlayerPowerupMode.AUTOMATIC.isAutomatic());
        assertFalse(PlayerPowerupMode.MANUAL.isAutomatic());
        assertEquals(PlayerPowerupMode.MANUAL, PlayerPowerupMode.AUTOMATIC.toggle());
        assertEquals(PlayerPowerupMode.AUTOMATIC, PlayerPowerupMode.MANUAL.toggle());
    }

    @Test
    public void invalidStoredValueFallsBackToAutomatic() {
        assertEquals(
                PlayerPowerupMode.MANUAL,
                PlayerPowerupMode.fromStoredValue("manual"));
        assertEquals(
                PlayerPowerupMode.AUTOMATIC,
                PlayerPowerupMode.fromStoredValue("unknown"));
    }
}
