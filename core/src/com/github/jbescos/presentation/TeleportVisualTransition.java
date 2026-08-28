package com.github.jbescos.presentation;

/** Presentation-only offset that makes an instantaneous relocation visibly travel. */
public final class TeleportVisualTransition {
    public static final float DURATION_SECONDS = 0.40f;

    private float offsetX;
    private float offsetY;
    private float angleOffset;
    private float elapsedSeconds;
    private boolean active;

    public void start(
            float sourceX,
            float sourceY,
            float sourceAngle,
            float destinationX,
            float destinationY,
            float destinationAngle) {
        if (!areFinite(
                sourceX,
                sourceY,
                sourceAngle,
                destinationX,
                destinationY,
                destinationAngle)) {
            reset();
            return;
        }
        offsetX = sourceX - destinationX;
        offsetY = sourceY - destinationY;
        angleOffset = normalizedAngle(sourceAngle - destinationAngle);
        elapsedSeconds = 0f;
        active = offsetX * offsetX + offsetY * offsetY > 0.0001f
                || Math.abs(angleOffset) > 0.0001f;
    }

    public void update(float deltaSeconds) {
        if (!active) {
            return;
        }
        float delta = Float.isFinite(deltaSeconds)
                ? Math.max(0f, deltaSeconds)
                : 0f;
        elapsedSeconds = Math.min(DURATION_SECONDS, elapsedSeconds + delta);
        if (elapsedSeconds >= DURATION_SECONDS) {
            active = false;
        }
    }

    public boolean isActive() {
        return active;
    }

    public float getRenderX(float physicalX) {
        return physicalX + offsetX * remainingOffsetScale();
    }

    public float getRenderY(float physicalY) {
        return physicalY + offsetY * remainingOffsetScale();
    }

    public float getRenderAngle(float physicalAngle) {
        return physicalAngle + angleOffset * remainingOffsetScale();
    }

    public void reset() {
        offsetX = 0f;
        offsetY = 0f;
        angleOffset = 0f;
        elapsedSeconds = 0f;
        active = false;
    }

    private float remainingOffsetScale() {
        if (!active) {
            return 0f;
        }
        float progress = Math.min(1f, elapsedSeconds / DURATION_SECONDS);
        float eased = progress * progress * (3f - 2f * progress);
        return 1f - eased;
    }

    private static float normalizedAngle(float angle) {
        return (float) Math.atan2(Math.sin(angle), Math.cos(angle));
    }

    private static boolean areFinite(float... values) {
        for (int i = 0; i < values.length; i++) {
            if (!Float.isFinite(values[i])) {
                return false;
            }
        }
        return true;
    }
}
