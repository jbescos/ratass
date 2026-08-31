package com.github.jbescos.presentation;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;

/** Presentation-only camera emphasis used while the player drives manually. */
public final class ManualDrivingCamera {
    public static final float MAXIMUM_ZOOM = 1.88f;
    public static final float MAXIMUM_LOOK_AHEAD = 30.00f;

    private static final float EXTRA_ZOOM = 0.30f;
    private static final float BASE_LOOK_AHEAD = 12.00f;
    private static final float SPEED_LOOK_AHEAD = 12.00f;
    private static final float FORWARD_FRAMING_RATIO = 0.74f;
    private static final float ROUTE_DIRECTION_SPEED_THRESHOLD = 0.5f;
    private static final float LOOK_AHEAD_LERP_SPEED = 4f;

    private ManualDrivingCamera() {
    }

    public static float additionalZoom(boolean active, float speedFactor) {
        return active ? EXTRA_ZOOM * speedEmphasis(speedFactor) : 0f;
    }

    public static float additionalLookAhead(boolean active, float speedFactor) {
        return active
                ? BASE_LOOK_AHEAD + SPEED_LOOK_AHEAD * speedEmphasis(speedFactor)
                : 0f;
    }

    public static boolean aimAtRoutePoint(
            Vector2 output,
            Vector2 carPosition,
            Vector2 routePoint,
            float distance) {
        if (output == null || carPosition == null || routePoint == null) {
            return false;
        }
        output.set(routePoint).sub(carPosition);
        if (output.isZero(0.0001f)) {
            output.setZero();
            return false;
        }
        output.setLength(Math.max(0f, distance));
        return true;
    }

    public static float routeLookAheadSign(
            float velocityAlongRoute,
            float carForwardRouteAlignment) {
        if (velocityAlongRoute > ROUTE_DIRECTION_SPEED_THRESHOLD) {
            return 1f;
        }
        if (velocityAlongRoute < -ROUTE_DIRECTION_SPEED_THRESHOLD) {
            return -1f;
        }
        return carForwardRouteAlignment < 0f ? -1f : 1f;
    }

    public static void smoothLookAhead(
            Vector2 current,
            Vector2 target,
            float deltaSeconds) {
        if (current == null || target == null) {
            return;
        }
        float safeDelta = Math.max(0f, deltaSeconds);
        float alpha = 1f - (float) Math.exp(-LOOK_AHEAD_LERP_SPEED * safeDelta);
        current.lerp(target, MathUtils.clamp(alpha, 0f, 1f));
    }

    public static float maximumLookAhead(
            boolean active,
            float defaultMaximum,
            float directionX,
            float directionY,
            float viewportWidth,
            float viewportHeight,
            float zoom) {
        return maximumLookAhead(
                active,
                defaultMaximum,
                directionX,
                directionY,
                viewportWidth,
                viewportHeight,
                zoom,
                0f);
    }

    public static float maximumLookAhead(
            boolean active,
            float defaultMaximum,
            float directionX,
            float directionY,
            float viewportWidth,
            float viewportHeight,
            float zoom,
            float minimumRearDistance) {
        float safeDefaultMaximum = Math.max(0f, defaultMaximum);
        if (!active) {
            return safeDefaultMaximum;
        }
        float directionLength = (float) Math.sqrt(
                directionX * directionX + directionY * directionY);
        if (directionLength <= 0.0001f) {
            return safeDefaultMaximum;
        }
        float normalizedX = Math.abs(directionX / directionLength);
        float normalizedY = Math.abs(directionY / directionLength);
        float halfWidth = Math.max(0f, viewportWidth) * Math.max(0f, zoom) * 0.5f;
        float halfHeight = Math.max(0f, viewportHeight) * Math.max(0f, zoom) * 0.5f;
        float horizontalLimit = normalizedX > 0.0001f
                ? halfWidth / normalizedX
                : Float.POSITIVE_INFINITY;
        float verticalLimit = normalizedY > 0.0001f
                ? halfHeight / normalizedY
                : Float.POSITIVE_INFINITY;
        float visibleDistance = Math.min(horizontalLimit, verticalLimit);
        if (!Float.isFinite(visibleDistance)) {
            return safeDefaultMaximum;
        }
        float framedLookAhead = visibleDistance * FORWARD_FRAMING_RATIO;
        float visibleCarLookAhead =
                Math.max(0f, visibleDistance - Math.max(0f, minimumRearDistance));
        return Math.min(
                MAXIMUM_LOOK_AHEAD,
                Math.min(framedLookAhead, visibleCarLookAhead));
    }

    private static float speedEmphasis(float speedFactor) {
        float clamped = MathUtils.clamp(speedFactor, 0f, 1f);
        return clamped * clamped;
    }
}
