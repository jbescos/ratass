package com.github.jbescos.presentation;

/** Tracks touch-driving controls from pointer down until pointer release. */
public final class TouchDrivingPointerCapture {
    private static final byte CONTROL_NONE = 0;
    private static final byte CONTROL_STEER = 1;
    private static final byte CONTROL_THROTTLE = 2;
    private static final byte CONTROL_BRAKE = 3;
    private static final byte CONTROL_POWERUP = 4;

    private final byte[] pointerControls;
    private final boolean[] pointerDown;
    private float turn;
    private boolean steering;
    private boolean throttling;
    private boolean braking;
    private boolean powerupPressed;
    private boolean powerupJustPressed;

    public TouchDrivingPointerCapture(int maxPointers) {
        if (maxPointers <= 0) {
            throw new IllegalArgumentException("maxPointers must be positive");
        }
        pointerControls = new byte[maxPointers];
        pointerDown = new boolean[maxPointers];
    }

    public int getMaxPointers() {
        return pointerControls.length;
    }

    public void beginFrame() {
        turn = 0f;
        steering = false;
        throttling = false;
        braking = false;
        powerupPressed = false;
        powerupJustPressed = false;
    }

    public void updatePointer(
            int pointer,
            boolean down,
            boolean overSteering,
            boolean overThrottle,
            boolean overBrake,
            boolean pedalsEnabled,
            float steeringValue) {
        updatePointer(
                pointer,
                down,
                overSteering,
                overThrottle,
                overBrake,
                false,
                pedalsEnabled,
                false,
                steeringValue);
    }

    public void updatePointer(
            int pointer,
            boolean down,
            boolean overSteering,
            boolean overThrottle,
            boolean overBrake,
            boolean overPowerup,
            boolean pedalsEnabled,
            boolean powerupEnabled,
            float steeringValue) {
        if (pointer < 0 || pointer >= pointerControls.length) {
            return;
        }
        if (!down) {
            pointerDown[pointer] = false;
            pointerControls[pointer] = CONTROL_NONE;
            return;
        }

        boolean newlyPressed = !pointerDown[pointer];
        if (newlyPressed) {
            pointerControls[pointer] = captureControl(
                    overSteering,
                    pedalsEnabled && overThrottle,
                    pedalsEnabled && overBrake,
                    powerupEnabled && overPowerup);
        }
        pointerDown[pointer] = true;

        switch (pointerControls[pointer]) {
            case CONTROL_STEER:
                steering = true;
                turn += clamp(steeringValue, -1f, 1f);
                break;
            case CONTROL_THROTTLE:
                throttling = true;
                break;
            case CONTROL_BRAKE:
                braking = true;
                break;
            case CONTROL_POWERUP:
                powerupPressed = true;
                powerupJustPressed |= newlyPressed;
                break;
            default:
                break;
        }
    }

    public void reset() {
        beginFrame();
        for (int pointer = 0; pointer < pointerControls.length; pointer++) {
            pointerControls[pointer] = CONTROL_NONE;
            pointerDown[pointer] = false;
        }
    }

    public float getTurn() {
        return clamp(turn, -1f, 1f);
    }

    public boolean isSteering() {
        return steering;
    }

    public boolean isThrottling() {
        return throttling;
    }

    public boolean isBraking() {
        return braking;
    }

    public boolean isPowerupPressed() {
        return powerupPressed;
    }

    public boolean isPowerupJustPressed() {
        return powerupJustPressed;
    }

    public boolean isCaptured(int pointer) {
        return pointer >= 0
                && pointer < pointerControls.length
                && pointerDown[pointer]
                && pointerControls[pointer] != CONTROL_NONE;
    }

    private static byte captureControl(
            boolean overSteering,
            boolean overThrottle,
            boolean overBrake,
            boolean overPowerup) {
        if (overSteering) {
            return CONTROL_STEER;
        }
        if (overThrottle) {
            return CONTROL_THROTTLE;
        }
        if (overBrake) {
            return CONTROL_BRAKE;
        }
        if (overPowerup) {
            return CONTROL_POWERUP;
        }
        return CONTROL_NONE;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
