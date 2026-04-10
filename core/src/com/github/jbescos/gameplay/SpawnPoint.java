package com.github.jbescos.gameplay;

import com.badlogic.gdx.math.MathUtils;

public final class SpawnPoint {
    public final float x;
    public final float y;
    public final float angleRad;

    public SpawnPoint(float x, float y, float angleRad) {
        this.x = x;
        this.y = y;
        this.angleRad = angleRad;
    }

    public SpawnPoint scale(float factor) {
        return new SpawnPoint(x * factor, y * factor, angleRad);
    }

    public static SpawnPoint facingPoint(float x, float y, float lookAtX, float lookAtY) {
        return new SpawnPoint(
                x,
                y,
                MathUtils.atan2(lookAtY - y, lookAtX - x) - MathUtils.HALF_PI);
    }
}
