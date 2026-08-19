package com.github.jbescos.presentation;

import com.badlogic.gdx.math.Vector2;

/** Keeps a follow camera attached to the same car across an instantaneous relocation. */
public final class CameraTeleportFollow {
    private CameraTeleportFollow() {
    }

    public static boolean snapTarget(
            boolean presentationEnabled,
            boolean freeCamera,
            boolean followedCarMoved,
            Vector2 output,
            Vector2 carPosition,
            Vector2 carVelocity,
            float lookAheadSeconds,
            float maximumLookAhead) {
        if (!presentationEnabled
                || freeCamera
                || !followedCarMoved
                || output == null
                || !isFinite(carPosition)
                || !isFinite(carVelocity)) {
            return false;
        }

        float lookAheadX = carVelocity.x * Math.max(0f, lookAheadSeconds);
        float lookAheadY = carVelocity.y * Math.max(0f, lookAheadSeconds);
        float lookAheadLengthSquared = lookAheadX * lookAheadX + lookAheadY * lookAheadY;
        float safeMaximumLookAhead = Math.max(0f, maximumLookAhead);
        if (lookAheadLengthSquared > safeMaximumLookAhead * safeMaximumLookAhead
                && lookAheadLengthSquared > 0f) {
            float scale = safeMaximumLookAhead / (float) Math.sqrt(lookAheadLengthSquared);
            lookAheadX *= scale;
            lookAheadY *= scale;
        }

        output.set(carPosition.x + lookAheadX, carPosition.y + lookAheadY);
        return isFinite(output);
    }

    private static boolean isFinite(Vector2 value) {
        return value != null && Float.isFinite(value.x) && Float.isFinite(value.y);
    }
}
