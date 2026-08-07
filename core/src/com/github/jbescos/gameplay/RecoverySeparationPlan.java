package com.github.jbescos.gameplay;

/** A short, fixed escape path away from a car blocking automatic recovery. */
public final class RecoverySeparationPlan {
    private boolean active;
    private float startX;
    private float startY;
    private float directionX;
    private float directionY;
    private float targetX;
    private float targetY;

    public void begin(
            float carX,
            float carY,
            float blockerX,
            float blockerY,
            float fallbackForwardX,
            float fallbackForwardY,
            float targetDistance) {
        float awayX = carX - blockerX;
        float awayY = carY - blockerY;
        float awayLength = length(awayX, awayY);
        if (awayLength <= 0.0001f) {
            awayX = -fallbackForwardX;
            awayY = -fallbackForwardY;
            awayLength = length(awayX, awayY);
        }
        if (awayLength <= 0.0001f) {
            awayX = 0f;
            awayY = -1f;
            awayLength = 1f;
        }

        directionX = awayX / awayLength;
        directionY = awayY / awayLength;
        startX = carX;
        startY = carY;
        float safeTargetDistance = Math.max(0f, targetDistance);
        targetX = carX + directionX * safeTargetDistance;
        targetY = carY + directionY * safeTargetDistance;
        active = true;
    }

    public void reset() {
        active = false;
        startX = 0f;
        startY = 0f;
        directionX = 0f;
        directionY = 0f;
        targetX = 0f;
        targetY = 0f;
    }

    public boolean isActive() {
        return active;
    }

    public float getTargetX() {
        return targetX;
    }

    public float getTargetY() {
        return targetY;
    }

    public boolean hasMoved(float carX, float carY, float requiredDistance) {
        float movementX = carX - startX;
        float movementY = carY - startY;
        float projectedMovement = movementX * directionX + movementY * directionY;
        return projectedMovement >= Math.max(0f, requiredDistance);
    }

    public boolean hasClearance(
            float carX,
            float carY,
            float blockerX,
            float blockerY,
            float requiredDistance) {
        float separationX = carX - blockerX;
        float separationY = carY - blockerY;
        float safeDistance = Math.max(0f, requiredDistance);
        return separationX * separationX + separationY * separationY
                >= safeDistance * safeDistance;
    }

    private static float length(float x, float y) {
        return (float) Math.sqrt(x * x + y * y);
    }
}
