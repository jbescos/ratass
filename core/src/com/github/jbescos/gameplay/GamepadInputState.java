package com.github.jbescos.gameplay;

import com.badlogic.gdx.Application.ApplicationType;
import com.badlogic.gdx.controllers.Controller;
import com.badlogic.gdx.controllers.ControllerMapping;
import com.badlogic.gdx.controllers.Controllers;
import com.badlogic.gdx.utils.Array;

/** Polls one controller and exposes stable driving axes plus edge-triggered UI actions. */
public final class GamepadInputState {
    private static final float DRIVING_DEAD_ZONE = 0.16f;
    private static final float NAVIGATION_THRESHOLD = 0.62f;
    private static final int DESKTOP_LEFT_TRIGGER_AXIS = 4;
    private static final int DESKTOP_RIGHT_TRIGGER_AXIS = 5;

    private final boolean[] pressed = new boolean[Action.values().length];
    private final boolean[] previous = new boolean[Action.values().length];
    private float throttle;
    private float turn;
    private boolean connected;

    public void update(boolean enabled, ApplicationType applicationType) {
        System.arraycopy(pressed, 0, previous, 0, pressed.length);
        clearCurrent();
        if (!enabled) {
            return;
        }

        Controller controller;
        try {
            controller = findController();
        } catch (RuntimeException exception) {
            return;
        }
        if (controller == null) {
            return;
        }

        connected = true;
        ControllerMapping mapping = controller.getMapping();
        float leftX = mappedAxis(controller, mapping.axisLeftX);
        float leftY = mappedAxis(controller, mapping.axisLeftY);
        boolean dpadLeft = mappedButton(controller, mapping.buttonDpadLeft);
        boolean dpadRight = mappedButton(controller, mapping.buttonDpadRight);
        boolean dpadUp = mappedButton(controller, mapping.buttonDpadUp);
        boolean dpadDown = mappedButton(controller, mapping.buttonDpadDown);

        turn = steering(leftX, dpadLeft, dpadRight);
        float accelerator = mappedButton(controller, mapping.buttonA) ? 1f : 0f;
        float brake = mappedButton(controller, mapping.buttonB) ? 1f : 0f;
        accelerator = Math.max(
                accelerator,
                mappedButton(controller, mapping.buttonR2) ? 1f : 0f);
        brake = Math.max(brake, mappedButton(controller, mapping.buttonL2) ? 1f : 0f);
        if (applicationType == ApplicationType.Desktop) {
            accelerator = Math.max(
                    accelerator,
                    rawTrigger(controller, DESKTOP_RIGHT_TRIGGER_AXIS));
            brake = Math.max(brake, rawTrigger(controller, DESKTOP_LEFT_TRIGGER_AXIS));
        }
        throttle = Math.max(0f, accelerator) - Math.max(0f, brake);

        set(Action.UP, dpadUp || leftY <= -NAVIGATION_THRESHOLD);
        set(Action.DOWN, dpadDown || leftY >= NAVIGATION_THRESHOLD);
        set(Action.LEFT, dpadLeft || leftX <= -NAVIGATION_THRESHOLD);
        set(Action.RIGHT, dpadRight || leftX >= NAVIGATION_THRESHOLD);
        set(Action.CONFIRM, mappedButton(controller, mapping.buttonA));
        set(Action.CANCEL, mappedButton(controller, mapping.buttonB));
        set(Action.CARDS, mappedButton(controller, mapping.buttonX));
        set(Action.TV_CAMERA, mappedButton(controller, mapping.buttonY));
        set(Action.PAUSE, mappedButton(controller, mapping.buttonStart));
        set(Action.TOGGLE_PANELS, mappedButton(controller, mapping.buttonBack));
        set(Action.PREVIOUS_CAR, mappedButton(controller, mapping.buttonL1));
        set(Action.NEXT_CAR, mappedButton(controller, mapping.buttonR1));
        set(Action.POWERUP, mappedButton(controller, mapping.buttonRightStick));
    }

    public boolean isConnected() {
        return connected;
    }

    public float getThrottle() {
        return throttle;
    }

    public float getTurn() {
        return turn;
    }

    public boolean isPressed(Action action) {
        return action != null && pressed[action.ordinal()];
    }

    public boolean isJustPressed(Action action) {
        return action != null
                && pressed[action.ordinal()]
                && !previous[action.ordinal()];
    }

    static float applyDeadZone(float value) {
        float magnitude = Math.abs(value);
        if (magnitude <= DRIVING_DEAD_ZONE) {
            return 0f;
        }
        float scaled = (magnitude - DRIVING_DEAD_ZONE) / (1f - DRIVING_DEAD_ZONE);
        return Math.copySign(Math.min(1f, scaled), value);
    }

    static float steering(float leftX, boolean dpadLeft, boolean dpadRight) {
        float digital = HybridPlayerControl.digitalAxis(dpadLeft, dpadRight);
        if (dpadLeft || dpadRight) {
            return digital;
        }
        // Game steering uses positive values for left turns.
        return -applyDeadZone(leftX);
    }

    public static float strongest(float first, float second) {
        return Math.abs(second) > Math.abs(first) ? second : first;
    }

    private static Controller findController() {
        Controller current = Controllers.getCurrent();
        if (current != null && current.isConnected()) {
            return current;
        }
        Array<Controller> controllers = Controllers.getControllers();
        for (int i = 0; i < controllers.size; i++) {
            Controller candidate = controllers.get(i);
            if (candidate != null && candidate.isConnected()) {
                return candidate;
            }
        }
        return null;
    }

    private static float mappedAxis(Controller controller, int axis) {
        return validAxis(controller, axis) ? controller.getAxis(axis) : 0f;
    }

    private static float rawTrigger(Controller controller, int axis) {
        if (!validAxis(controller, axis)) {
            return 0f;
        }
        return Math.max(0f, Math.min(1f, controller.getAxis(axis)));
    }

    private static boolean mappedButton(Controller controller, int button) {
        return button != ControllerMapping.UNDEFINED
                && button >= controller.getMinButtonIndex()
                && button <= controller.getMaxButtonIndex()
                && controller.getButton(button);
    }

    private static boolean validAxis(Controller controller, int axis) {
        return axis != ControllerMapping.UNDEFINED
                && axis >= 0
                && axis < controller.getAxisCount();
    }

    private void clearCurrent() {
        for (int i = 0; i < pressed.length; i++) {
            pressed[i] = false;
        }
        connected = false;
        throttle = 0f;
        turn = 0f;
    }

    private void set(Action action, boolean value) {
        pressed[action.ordinal()] = value;
    }

    public enum Action {
        UP,
        DOWN,
        LEFT,
        RIGHT,
        CONFIRM,
        CANCEL,
        CARDS,
        TV_CAMERA,
        PAUSE,
        TOGGLE_PANELS,
        PREVIOUS_CAR,
        NEXT_CAR,
        POWERUP
    }
}
