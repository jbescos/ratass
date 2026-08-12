package com.github.jbescos.presentation;

/** Runtime world-camera behavior. Free mode is entered by dragging the circuit. */
public enum CameraViewMode {
    TOP_DOWN,
    FREE;

    public boolean isFree() {
        return this == FREE;
    }
}
