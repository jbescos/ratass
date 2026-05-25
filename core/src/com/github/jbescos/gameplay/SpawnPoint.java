package com.github.jbescos.gameplay;

import com.badlogic.gdx.math.MathUtils;

public final class SpawnPoint {
    public final float x;
    public final float y;
    public final float angleRad;
    public final boolean hasGate;
    public final float gateStartX;
    public final float gateStartY;
    public final float gateEndX;
    public final float gateEndY;

    public SpawnPoint(float x, float y, float angleRad) {
        this(x, y, angleRad, false, x, y, x, y);
    }

    private SpawnPoint(
            float x,
            float y,
            float angleRad,
            boolean hasGate,
            float gateStartX,
            float gateStartY,
            float gateEndX,
            float gateEndY) {
        this.x = x;
        this.y = y;
        this.angleRad = angleRad;
        this.hasGate = hasGate;
        this.gateStartX = gateStartX;
        this.gateStartY = gateStartY;
        this.gateEndX = gateEndX;
        this.gateEndY = gateEndY;
    }

    public SpawnPoint scale(float factor) {
        return new SpawnPoint(
                x * factor,
                y * factor,
                angleRad,
                hasGate,
                gateStartX * factor,
                gateStartY * factor,
                gateEndX * factor,
                gateEndY * factor);
    }

    public static SpawnPoint facingPoint(float x, float y, float lookAtX, float lookAtY) {
        return new SpawnPoint(
                x,
                y,
                MathUtils.atan2(lookAtY - y, lookAtX - x) - MathUtils.HALF_PI);
    }

    public static SpawnPoint facingGate(
            float x,
            float y,
            float lookAtX,
            float lookAtY,
            float gateStartX,
            float gateStartY,
            float gateEndX,
            float gateEndY) {
        float gateDx = gateEndX - gateStartX;
        float gateDy = gateEndY - gateStartY;
        return new SpawnPoint(
                x,
                y,
                MathUtils.atan2(lookAtY - y, lookAtX - x) - MathUtils.HALF_PI,
                gateDx * gateDx + gateDy * gateDy > 0.01f,
                gateStartX,
                gateStartY,
                gateEndX,
                gateEndY);
    }
}
