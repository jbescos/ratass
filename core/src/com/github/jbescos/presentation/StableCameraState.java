package com.github.jbescos.presentation;

/** Validates the values used to build the world-camera projection matrix. */
public final class StableCameraState {
    private static final double MIN_DIRECTION_LENGTH_SQUARED = 0.00000001;

    private StableCameraState() {
    }

    public static boolean isUsable(
            float positionX,
            float positionY,
            float directionX,
            float directionY,
            float zoom) {
        if (!isFinite(positionX)
                || !isFinite(positionY)
                || !isFinite(directionX)
                || !isFinite(directionY)
                || !isFinite(zoom)
                || zoom <= 0f) {
            return false;
        }
        double directionLengthSquared =
                (double) directionX * directionX + (double) directionY * directionY;
        return !Double.isInfinite(directionLengthSquared)
                && !Double.isNaN(directionLengthSquared)
                && directionLengthSquared >= MIN_DIRECTION_LENGTH_SQUARED;
    }

    public static boolean shouldHoldLastTransform(
            boolean wholeMapMode,
            boolean sameMap,
            boolean targetAvailable,
            boolean initialized,
            float positionX,
            float positionY,
            float directionX,
            float directionY,
            float zoom) {
        return !wholeMapMode
                && sameMap
                && !targetAvailable
                && initialized
                && isUsable(positionX, positionY, directionX, directionY, zoom);
    }

    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }
}
