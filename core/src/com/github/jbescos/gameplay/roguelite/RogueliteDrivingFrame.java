package com.github.jbescos.gameplay.roguelite;

final class RogueliteDrivingFrame {
    float throttle;
    boolean onRoad;
    boolean adverseWeather;
    boolean recentlyImpacted;
    float slip;
    float speedRatio;
    float slipstreamBoost;
    float routeProgress;
    float routeLength;
    float safeRecoveryRouteGain;
    float cornerSeverity;
    float nextCornerDistance;
    float nextCornerSeverity;
    float opponentAheadProximity;
    float nearbyOpponentProximity;

    void set(
            float throttle,
            boolean onRoad,
            boolean adverseWeather,
            boolean recentlyImpacted,
            float slip,
            float speedRatio,
            float slipstreamBoost,
            float routeProgress,
            float routeLength,
            float safeRecoveryRouteGain,
            float cornerSeverity,
            float nextCornerDistance,
            float nextCornerSeverity,
            float opponentAheadProximity,
            float nearbyOpponentProximity) {
        this.throttle = throttle;
        this.onRoad = onRoad;
        this.adverseWeather = adverseWeather;
        this.recentlyImpacted = recentlyImpacted;
        this.slip = slip;
        this.speedRatio = speedRatio;
        this.slipstreamBoost = slipstreamBoost;
        this.routeProgress = routeProgress;
        this.routeLength = routeLength;
        this.safeRecoveryRouteGain = safeRecoveryRouteGain;
        this.cornerSeverity = cornerSeverity;
        this.nextCornerDistance = nextCornerDistance;
        this.nextCornerSeverity = nextCornerSeverity;
        this.opponentAheadProximity = opponentAheadProximity;
        this.nearbyOpponentProximity = nearbyOpponentProximity;
    }

    void clear() {
        set(
                0f,
                true,
                false,
                false,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                0f,
                1f,
                0f,
                0f,
                0f);
    }
}
