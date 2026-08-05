package com.github.jbescos.gameplay;

/** Keeps the last finite transform that can be used after an invalid physics step. */
public final class StableCarMotion {
    private boolean anchorAvailable;
    private float anchorX;
    private float anchorY;
    private float anchorAngle;

    public void rememberSpawn(float x, float y, float angle) {
        if (isFiniteTransform(x, y, angle)) {
            setAnchor(x, y, angle);
        }
    }

    public void rememberSafe(float x, float y, float angle, boolean safe) {
        if (safe && isFiniteTransform(x, y, angle)) {
            setAnchor(x, y, angle);
        }
    }

    public boolean hasAnchor() {
        return anchorAvailable;
    }

    public float getAnchorX() {
        return anchorX;
    }

    public float getAnchorY() {
        return anchorY;
    }

    public float getAnchorAngle() {
        return anchorAngle;
    }

    public static boolean isFiniteMotion(
            float x,
            float y,
            float angle,
            float velocityX,
            float velocityY,
            float angularVelocity) {
        return isFiniteTransform(x, y, angle)
                && isFinite(velocityX)
                && isFinite(velocityY)
                && isFinite(angularVelocity);
    }

    public static boolean isFiniteTransform(float x, float y, float angle) {
        return isFinite(x) && isFinite(y) && isFinite(angle);
    }

    public static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }

    private void setAnchor(float x, float y, float angle) {
        anchorX = x;
        anchorY = y;
        anchorAngle = angle;
        anchorAvailable = true;
    }
}
