package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;

import com.badlogic.gdx.Input;
import org.junit.Test;

public final class DrivingKeyBindingsTest {
    @Test
    public void defaultsToWasd() {
        DrivingKeyBindings bindings = new DrivingKeyBindings();

        assertEquals(Input.Keys.W, bindings.get(DrivingKeyBindings.Action.FORWARD));
        assertEquals(Input.Keys.S, bindings.get(DrivingKeyBindings.Action.BACKWARD));
        assertEquals(Input.Keys.A, bindings.get(DrivingKeyBindings.Action.LEFT));
        assertEquals(Input.Keys.D, bindings.get(DrivingKeyBindings.Action.RIGHT));
    }

    @Test
    public void rebindingToUsedKeySwapsActions() {
        DrivingKeyBindings bindings = new DrivingKeyBindings();

        bindings.rebind(DrivingKeyBindings.Action.FORWARD, Input.Keys.S);

        assertEquals(Input.Keys.S, bindings.get(DrivingKeyBindings.Action.FORWARD));
        assertEquals(Input.Keys.W, bindings.get(DrivingKeyBindings.Action.BACKWARD));
    }
}
