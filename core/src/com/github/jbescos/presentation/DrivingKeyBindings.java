package com.github.jbescos.presentation;

import com.badlogic.gdx.Input;

/** Persistent desktop bindings for the player's per-axis driving takeover. */
public final class DrivingKeyBindings {
    public enum Action {
        FORWARD,
        BACKWARD,
        LEFT,
        RIGHT,
        POWERUP
    }

    private final int[] keycodes = new int[Action.values().length];

    public DrivingKeyBindings() {
        resetToDefaults();
    }

    public void resetToDefaults() {
        keycodes[Action.FORWARD.ordinal()] = Input.Keys.W;
        keycodes[Action.BACKWARD.ordinal()] = Input.Keys.S;
        keycodes[Action.LEFT.ordinal()] = Input.Keys.A;
        keycodes[Action.RIGHT.ordinal()] = Input.Keys.D;
        keycodes[Action.POWERUP.ordinal()] = Input.Keys.SPACE;
    }

    public int get(Action action) {
        return keycodes[action.ordinal()];
    }

    public void set(Action action, int keycode) {
        if (action == null || keycode < 0) {
            return;
        }
        keycodes[action.ordinal()] = keycode;
    }

    /** Rebinds an action and swaps an existing owner so every action remains reachable. */
    public void rebind(Action action, int keycode) {
        if (action == null || keycode < 0) {
            return;
        }
        int actionIndex = action.ordinal();
        int previousKeycode = keycodes[actionIndex];
        for (int i = 0; i < keycodes.length; i++) {
            if (i != actionIndex && keycodes[i] == keycode) {
                keycodes[i] = previousKeycode;
                break;
            }
        }
        keycodes[actionIndex] = keycode;
    }

    public String displayName(Action action) {
        String name = Input.Keys.toString(get(action));
        return name == null || name.length() == 0 ? "?" : name;
    }
}
