package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public final class GamepadInputStateTest {
    @Test
    public void deadZoneRemovesDriftAndRescalesUsefulStickTravel() {
        assertEquals(0f, GamepadInputState.applyDeadZone(0.15f), 0.0001f);
        assertEquals(0f, GamepadInputState.applyDeadZone(-0.16f), 0.0001f);
        assertEquals(1f, GamepadInputState.applyDeadZone(1f), 0.0001f);
        assertEquals(-1f, GamepadInputState.applyDeadZone(-1f), 0.0001f);
        assertEquals(0.5f, GamepadInputState.applyDeadZone(0.58f), 0.0001f);
    }

    @Test
    public void steeringMatchesTheGamesLeftPositiveConvention() {
        assertEquals(1f, GamepadInputState.steering(-1f, false, false), 0.0001f);
        assertEquals(-1f, GamepadInputState.steering(1f, false, false), 0.0001f);
        assertEquals(1f, GamepadInputState.steering(0f, true, false), 0.0001f);
        assertEquals(0f, GamepadInputState.steering(0.5f, true, true), 0.0001f);
    }

    @Test
    public void strongestInputAllowsKeyboardOrControllerToTakeAuthority() {
        assertEquals(0.8f, GamepadInputState.strongest(0.8f, 0.4f), 0.0001f);
        assertEquals(-0.9f, GamepadInputState.strongest(0.2f, -0.9f), 0.0001f);
    }
}
