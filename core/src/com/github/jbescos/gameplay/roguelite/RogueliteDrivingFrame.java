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
    float revengeNearbyOpponentProximity;
    boolean forwardLaneBlocked;
    boolean longStraight;
    float racePositionFactor;

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
            float nearbyOpponentProximity,
            boolean forwardLaneBlocked,
            float racePositionFactor) {
        set(
                throttle,
                onRoad,
                adverseWeather,
                recentlyImpacted,
                slip,
                speedRatio,
                slipstreamBoost,
                routeProgress,
                routeLength,
                safeRecoveryRouteGain,
                cornerSeverity,
                nextCornerDistance,
                nextCornerSeverity,
                opponentAheadProximity,
                nearbyOpponentProximity,
                forwardLaneBlocked,
                racePositionFactor,
                nearbyOpponentProximity);
    }

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
            float nearbyOpponentProximity,
            boolean forwardLaneBlocked,
            float racePositionFactor,
            float revengeNearbyOpponentProximity) {
        set(
                throttle,
                onRoad,
                adverseWeather,
                recentlyImpacted,
                slip,
                speedRatio,
                slipstreamBoost,
                routeProgress,
                routeLength,
                safeRecoveryRouteGain,
                cornerSeverity,
                nextCornerDistance,
                nextCornerSeverity,
                opponentAheadProximity,
                nearbyOpponentProximity,
                forwardLaneBlocked,
                racePositionFactor,
                revengeNearbyOpponentProximity,
                false);
    }

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
            float nearbyOpponentProximity,
            boolean forwardLaneBlocked,
            float racePositionFactor,
            float revengeNearbyOpponentProximity,
            boolean longStraight) {
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
        this.revengeNearbyOpponentProximity = revengeNearbyOpponentProximity;
        this.forwardLaneBlocked = forwardLaneBlocked;
        this.longStraight = longStraight;
        this.racePositionFactor = racePositionFactor;
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
                0f,
                false,
                0f,
                0f);
    }
}
