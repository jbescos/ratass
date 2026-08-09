package com.github.jbescos.gameplay.roguelite;

/** Calculates deterministic movement for a Vendetta Hook pull. */
public final class VendettaHookPull {
    private VendettaHookPull() {
    }

    public static float remainingGap(
            float initialGap,
            float elapsedSeconds,
            float durationSeconds) {
        if (!Float.isFinite(initialGap)
                || !Float.isFinite(elapsedSeconds)
                || !Float.isFinite(durationSeconds)
                || durationSeconds <= 0f) {
            return 0f;
        }
        float progress = Math.max(0f, Math.min(1f, elapsedSeconds / durationSeconds));
        return Math.max(0f, initialGap) * (1f - progress);
    }

    public static boolean isComplete(float elapsedSeconds, float durationSeconds) {
        return Float.isFinite(elapsedSeconds)
                && Float.isFinite(durationSeconds)
                && durationSeconds > 0f
                && elapsedSeconds >= durationSeconds;
    }

    public static float pullDistance(
            float initialDistance,
            float contactDistance,
            float contactOverlap,
            float elapsedSeconds,
            float durationSeconds) {
        float safeInitialDistance = sanitizeDimension(initialDistance);
        float finalDistance =
                Math.min(
                        safeInitialDistance,
                        Math.max(
                                0f,
                                sanitizeDimension(contactDistance)
                                        - sanitizeDimension(contactOverlap)));
        return finalDistance
                + remainingGap(
                        safeInitialDistance - finalDistance,
                        elapsedSeconds,
                        durationSeconds);
    }

    public static float contactDistance(
            float directionX,
            float directionY,
            float sourceHalfWidth,
            float sourceHalfHeight,
            float sourceAngleRadians,
            float targetHalfWidth,
            float targetHalfHeight,
            float targetAngleRadians,
            float margin) {
        float length = (float) Math.sqrt(directionX * directionX + directionY * directionY);
        if (!Float.isFinite(length) || length <= 0.0001f) {
            return 0f;
        }
        float normalX = directionX / length;
        float normalY = directionY / length;
        return projectedRadius(
                        normalX,
                        normalY,
                        sourceHalfWidth,
                        sourceHalfHeight,
                        sourceAngleRadians)
                + projectedRadius(
                        normalX,
                        normalY,
                        targetHalfWidth,
                        targetHalfHeight,
                        targetAngleRadians)
                + sanitizeDimension(margin);
    }

    private static float projectedRadius(
            float normalX,
            float normalY,
            float halfWidth,
            float halfHeight,
            float angleRadians) {
        if (!Float.isFinite(angleRadians)) {
            return 0f;
        }
        float cosine = (float) Math.cos(angleRadians);
        float sine = (float) Math.sin(angleRadians);
        float widthProjection = Math.abs(normalX * cosine + normalY * sine);
        float heightProjection = Math.abs(-normalX * sine + normalY * cosine);
        return sanitizeDimension(halfWidth) * widthProjection
                + sanitizeDimension(halfHeight) * heightProjection;
    }

    private static float sanitizeDimension(float value) {
        return Float.isFinite(value) ? Math.max(0f, value) : 0f;
    }
}
