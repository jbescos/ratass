package com.github.jbescos.gameplay;

/** Decides when a live AI car can safely hold full throttle on a long straight. */
public final class LongStraightThrottleAssist {
    private static final float MIN_ROUTE_ALIGNMENT = 0.98f;
    private static final float MAX_ROUTE_CURVATURE = 0.055f;
    private static final float MAX_BRAKE_DEMAND = 0.03f;
    private static final float MAX_LATERAL_SLIP = 0.10f;
    private static final float MIN_RUNWAY_CAR_LENGTHS = 8f;
    private static final float SATURATED_DISTANCE_RATIO = 0.999f;

    private LongStraightThrottleAssist() {
    }

    public static boolean shouldForceFullThrottle(
            float routeAlignment,
            float routeCurvature,
            float nextCornerDistance,
            float brakeDemand,
            float lateralSlip,
            float carLength) {
        float safeCarLength = Math.max(0f, carLength);
        return routeAlignment >= MIN_ROUTE_ALIGNMENT
                && Math.abs(routeCurvature) <= MAX_ROUTE_CURVATURE
                && nextCornerDistance >= safeCarLength * MIN_RUNWAY_CAR_LENGTHS
                && brakeDemand <= MAX_BRAKE_DEMAND
                && lateralSlip <= MAX_LATERAL_SLIP;
    }

    public static float effectiveBrakeDemand(
            float nextCornerDistance,
            float maximumRepresentedDistance,
            float brakeDemand) {
        if (Float.isFinite(nextCornerDistance)
                && Float.isFinite(maximumRepresentedDistance)
                && maximumRepresentedDistance > 0f
                && nextCornerDistance
                        >= maximumRepresentedDistance * SATURATED_DISTANCE_RATIO) {
            return 0f;
        }
        return Float.isFinite(brakeDemand) ? Math.max(0f, brakeDemand) : 1f;
    }
}
