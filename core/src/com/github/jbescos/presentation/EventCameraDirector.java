package com.github.jbescos.presentation;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/** Selects immediate camera targets without retaining incidents that happen while locked. */
public final class EventCameraDirector {
    static final float REVENGE_PREPARATION_SECONDS = 3f;
    static final float REVENGE_TARGET_SECONDS = 2f;

    private enum Mode {
        IDLE,
        REVENGE_PREPARATION,
        REVENGE_TARGET
    }

    private final Set<Integer> observedArmedVehicles = new HashSet<Integer>();
    private final Map<Integer, Long> observedRevengeExecutions =
            new HashMap<Integer, Long>();
    private Mode mode = Mode.IDLE;
    private int revengeSourceVehicleId = -1;
    private int requestedVehicleId = -1;
    private float lockTimeRemaining;

    public void update(float delta) {
        if (mode == Mode.IDLE) {
            return;
        }
        lockTimeRemaining = Math.max(0f, lockTimeRemaining - Math.max(0f, delta));
        if (lockTimeRemaining == 0f) {
            clearLock();
        }
    }

    public void observeRevengeArmed(int vehicleId, boolean armed) {
        if (vehicleId < 0) {
            return;
        }
        if (!armed) {
            observedArmedVehicles.remove(Integer.valueOf(vehicleId));
            if (mode == Mode.REVENGE_PREPARATION
                    && revengeSourceVehicleId == vehicleId) {
                clearLock();
            }
            return;
        }

        boolean newlyArmed = observedArmedVehicles.add(Integer.valueOf(vehicleId));
        if (newlyArmed && mode == Mode.IDLE) {
            mode = Mode.REVENGE_PREPARATION;
            revengeSourceVehicleId = vehicleId;
            lockTimeRemaining = REVENGE_PREPARATION_SECONDS;
            requestedVehicleId = vehicleId;
        }
    }

    public boolean revengeExecuted(
            int sourceVehicleId,
            int targetVehicleId,
            long activationSequence) {
        if (sourceVehicleId < 0 || targetVehicleId < 0) {
            return false;
        }
        Long previousSequence = observedRevengeExecutions.put(
                Integer.valueOf(sourceVehicleId),
                Long.valueOf(activationSequence));
        if (previousSequence != null
                && previousSequence.longValue() == activationSequence) {
            return false;
        }
        if (mode != Mode.IDLE
                && (mode != Mode.REVENGE_PREPARATION
                        || revengeSourceVehicleId != sourceVehicleId)) {
            return false;
        }
        mode = Mode.REVENGE_TARGET;
        revengeSourceVehicleId = sourceVehicleId;
        lockTimeRemaining = REVENGE_TARGET_SECONDS;
        requestedVehicleId = targetVehicleId;
        return true;
    }

    public int consumeRequestedVehicleId() {
        int requested = requestedVehicleId;
        requestedVehicleId = -1;
        return requested;
    }

    public boolean isLocked() {
        return mode != Mode.IDLE;
    }

    public void reset() {
        observedArmedVehicles.clear();
        observedRevengeExecutions.clear();
        clearLock();
    }

    private void clearLock() {
        mode = Mode.IDLE;
        revengeSourceVehicleId = -1;
        requestedVehicleId = -1;
        lockTimeRemaining = 0f;
    }
}
