package com.github.jbescos.gameplay;

import com.badlogic.gdx.math.MathUtils;

/** Route-relative opponent sectors used by the shared overtaking policy. */
public final class OvertakingSectorSensors {
    public static final int FRONT = 0;
    public static final int FRONT_LEFT = 1;
    public static final int LEFT = 2;
    public static final int REAR_LEFT = 3;
    public static final int REAR = 4;
    public static final int REAR_RIGHT = 5;
    public static final int RIGHT = 6;
    public static final int FRONT_RIGHT = 7;
    public static final int SECTOR_COUNT = 8;

    private OvertakingSectorSensors() {}

    public static int sectorFor(
            float routeForwardDistance,
            float routeSideDistance,
            float alongsideThreshold,
            float sideThreshold) {
        float forwardThreshold = Math.max(0f, alongsideThreshold);
        float lateralThreshold = Math.max(0f, sideThreshold);
        if (routeForwardDistance > forwardThreshold) {
            if (routeSideDistance > lateralThreshold) {
                return FRONT_LEFT;
            }
            if (routeSideDistance < -lateralThreshold) {
                return FRONT_RIGHT;
            }
            return FRONT;
        }
        if (routeForwardDistance < -forwardThreshold) {
            if (routeSideDistance > lateralThreshold) {
                return REAR_LEFT;
            }
            if (routeSideDistance < -lateralThreshold) {
                return REAR_RIGHT;
            }
            return REAR;
        }
        if (routeSideDistance > 0f) {
            return LEFT;
        }
        if (routeSideDistance < 0f) {
            return RIGHT;
        }
        return routeForwardDistance >= 0f ? FRONT : REAR;
    }

    public static float proximity(float distance, float range) {
        if (range <= 0f || distance >= range) {
            return 0f;
        }
        return 1f - MathUtils.clamp(distance / range, 0f, 1f);
    }

    public static float detectionRange(
            float baseRange,
            float maximumRange,
            float closingSpeed,
            float lookaheadSeconds) {
        float minimum = Math.max(0f, baseRange);
        float maximum = Math.max(minimum, maximumRange);
        return MathUtils.clamp(
                minimum + Math.max(0f, closingSpeed) * Math.max(0f, lookaheadSeconds),
                minimum,
                maximum);
    }

    public static float normalizedRelativeSpeed(float relativeSpeed, float fullScaleSpeed) {
        if (fullScaleSpeed <= 0f) {
            return 0f;
        }
        return MathUtils.clamp(relativeSpeed / fullScaleSpeed, -1f, 1f);
    }

    public static float closingCollisionRisk(
            float forwardDistance,
            float sideDistance,
            float closingSpeed,
            float collisionLength,
            float safeSideDistance,
            float timeHorizonSeconds) {
        if (closingSpeed <= 0f
                || forwardDistance <= -Math.max(0f, collisionLength)
                || safeSideDistance <= 0f
                || timeHorizonSeconds <= 0f) {
            return 0f;
        }
        float timeToOverlap = Math.max(0f, forwardDistance - Math.max(0f, collisionLength))
                / closingSpeed;
        float longitudinalRisk = 1f - MathUtils.clamp(
                timeToOverlap / timeHorizonSeconds,
                0f,
                1f);
        float lateralRisk = 1f - MathUtils.clamp(
                Math.abs(sideDistance) / safeSideDistance,
                0f,
                1f);
        return longitudinalRisk * lateralRisk;
    }

    public static boolean closingThreatResolved(
            boolean contactOccurred,
            float collisionRisk,
            float lateralClearance,
            float safeLateralClearance) {
        return !contactOccurred
                && collisionRisk <= 0.02f
                && safeLateralClearance > 0f
                && lateralClearance >= safeLateralClearance;
    }

    public static boolean isRelevantSteeringThreat(
            float forwardDistance,
            float allowedTrailingDistance) {
        return forwardDistance >= -Math.max(0f, allowedTrailingDistance);
    }

}
