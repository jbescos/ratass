package com.github.jbescos.gameplay.roguelite;

import java.util.Arrays;

/** Tracks the side of a live Vendetta Hook occupied by each race vehicle. */
public final class VendettaHookBarrier {
    private static final float SIDE_EPSILON = 0.0001f;

    private int targetVehicleId = -1;
    private float remainingSeconds;
    private int[] vehicleSides = new int[0];

    public void start(int targetVehicleId, float durationSeconds) {
        this.targetVehicleId = targetVehicleId;
        remainingSeconds = Math.max(0f, durationSeconds);
        Arrays.fill(vehicleSides, 0);
    }

    public void reset() {
        targetVehicleId = -1;
        remainingSeconds = 0f;
        Arrays.fill(vehicleSides, 0);
    }

    public void advance(float delta) {
        remainingSeconds = Math.max(0f, remainingSeconds - Math.max(0f, delta));
        if (remainingSeconds <= 0f) {
            reset();
        }
    }

    public boolean isActive() {
        return targetVehicleId >= 0 && remainingSeconds > 0f;
    }

    public int getTargetVehicleId() {
        return targetVehicleId;
    }

    public int getRememberedSide(int vehicleId) {
        return vehicleId >= 0 && vehicleId < vehicleSides.length
                ? vehicleSides[vehicleId]
                : 0;
    }

    public void rememberSide(
            int vehicleId,
            float startX,
            float startY,
            float endX,
            float endY,
            float vehicleX,
            float vehicleY) {
        if (vehicleId < 0) {
            return;
        }
        ensureVehicleCapacity(vehicleId);
        int side = classifySide(startX, startY, endX, endY, vehicleX, vehicleY);
        if (side != 0) {
            vehicleSides[vehicleId] = side;
        }
    }

    public boolean blocksCrossing(
            int vehicleId,
            float startX,
            float startY,
            float endX,
            float endY,
            float vehicleX,
            float vehicleY) {
        int rememberedSide = getRememberedSide(vehicleId);
        if (!isActive() || rememberedSide == 0) {
            return false;
        }
        float projection = projectionRatio(
                startX,
                startY,
                endX,
                endY,
                vehicleX,
                vehicleY);
        if (projection <= 0f || projection >= 1f) {
            return false;
        }
        int currentSide = classifySide(
                startX,
                startY,
                endX,
                endY,
                vehicleX,
                vehicleY);
        return currentSide == 0 || currentSide != rememberedSide;
    }

    public static float projectionRatio(
            float startX,
            float startY,
            float endX,
            float endY,
            float pointX,
            float pointY) {
        float dx = endX - startX;
        float dy = endY - startY;
        float lengthSquared = dx * dx + dy * dy;
        if (lengthSquared <= SIDE_EPSILON) {
            return 0f;
        }
        return ((pointX - startX) * dx + (pointY - startY) * dy) / lengthSquared;
    }

    private static int classifySide(
            float startX,
            float startY,
            float endX,
            float endY,
            float pointX,
            float pointY) {
        float side = (endX - startX) * (pointY - startY)
                - (endY - startY) * (pointX - startX);
        if (side > SIDE_EPSILON) {
            return 1;
        }
        if (side < -SIDE_EPSILON) {
            return -1;
        }
        return 0;
    }

    private void ensureVehicleCapacity(int vehicleId) {
        if (vehicleId < vehicleSides.length) {
            return;
        }
        int newLength = Math.max(vehicleId + 1, Math.max(8, vehicleSides.length * 2));
        vehicleSides = Arrays.copyOf(vehicleSides, newLength);
    }
}
