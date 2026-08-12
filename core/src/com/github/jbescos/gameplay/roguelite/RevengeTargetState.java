package com.github.jbescos.gameplay.roguelite;

/** Mutable target data shared by all offender-targeting Revenge workflows. */
final class RevengeTargetState {
    private int primaryVehicleId = -1;
    private int secondaryVehicleId = -1;
    private boolean secondaryCaptured;
    private float ageSeconds;

    boolean isArmed() {
        return primaryVehicleId >= 0;
    }

    int primaryVehicleId() {
        return primaryVehicleId;
    }

    int secondaryVehicleId() {
        return secondaryVehicleId;
    }

    float ageSeconds() {
        return ageSeconds;
    }

    void restart(int vehicleId) {
        primaryVehicleId = vehicleId;
        secondaryVehicleId = -1;
        secondaryCaptured = false;
        ageSeconds = 0f;
    }

    void advance(float delta) {
        if (isArmed()) {
            ageSeconds += Math.max(0f, delta);
        }
    }

    void captureSecondary(int vehicleId) {
        if (!isArmed() || secondaryCaptured) {
            return;
        }
        secondaryVehicleId = vehicleId == primaryVehicleId ? -1 : vehicleId;
        secondaryCaptured = true;
    }

    boolean clearIfTarget(int vehicleId) {
        if (vehicleId < 0 || primaryVehicleId != vehicleId) {
            return false;
        }
        clear();
        return true;
    }

    void clear() {
        primaryVehicleId = -1;
        secondaryVehicleId = -1;
        secondaryCaptured = false;
        ageSeconds = 0f;
    }
}
