package com.github.jbescos.gameplay.roguelite;

/** Calculates deterministic movement for a Vendetta Hook pull. */
public final class VendettaHookPull {
    public static final float DURATION_SECONDS = 1f;

    private VendettaHookPull() {
    }

    public static float stepFraction(
            float elapsedSeconds,
            float deltaSeconds,
            float durationSeconds) {
        if (!Float.isFinite(elapsedSeconds)
                || !Float.isFinite(deltaSeconds)
                || !Float.isFinite(durationSeconds)
                || durationSeconds <= 0f) {
            return durationSeconds <= 0f ? 1f : 0f;
        }
        float safeElapsed = Math.max(0f, elapsedSeconds);
        float safeDelta = Math.max(0f, deltaSeconds);
        if (safeElapsed + safeDelta >= durationSeconds) {
            return 1f;
        }
        float remainingSeconds = durationSeconds - safeElapsed;
        return Math.max(0f, Math.min(1f, safeDelta / remainingSeconds));
    }

    public static boolean isComplete(float elapsedSeconds, float durationSeconds) {
        return Float.isFinite(elapsedSeconds)
                && Float.isFinite(durationSeconds)
                && durationSeconds > 0f
                && elapsedSeconds >= durationSeconds;
    }

    public static float alignedHeading(
            float sourceAngleRadians,
            float targetAngleRadians) {
        if (Float.isFinite(targetAngleRadians)) {
            return targetAngleRadians;
        }
        return Float.isFinite(sourceAngleRadians) ? sourceAngleRadians : 0f;
    }

    public static float desiredContactDistance(
            float currentDistance,
            float contactDistance,
            float contactOverlap) {
        float safeCurrentDistance = sanitizeDimension(currentDistance);
        float overlappingContactDistance =
                Math.max(
                        0f,
                        sanitizeDimension(contactDistance)
                                - sanitizeDimension(contactOverlap));
        return Math.min(safeCurrentDistance, overlappingContactDistance);
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
