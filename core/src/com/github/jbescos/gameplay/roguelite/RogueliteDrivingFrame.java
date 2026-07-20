package com.github.jbescos.gameplay.roguelite;

final class RogueliteDrivingFrame {
    float throttle;
    boolean onRoad;
    boolean adverseWeather;
    boolean recentlyImpacted;
    float slip;
    float speedRatio;
    float routeProgress;
    float routeLength;
    float safeRecoveryRouteGain;

    void set(
            float throttle,
            boolean onRoad,
            boolean adverseWeather,
            boolean recentlyImpacted,
            float slip,
            float speedRatio,
            float routeProgress,
            float routeLength,
            float safeRecoveryRouteGain) {
        this.throttle = throttle;
        this.onRoad = onRoad;
        this.adverseWeather = adverseWeather;
        this.recentlyImpacted = recentlyImpacted;
        this.slip = slip;
        this.speedRatio = speedRatio;
        this.routeProgress = routeProgress;
        this.routeLength = routeLength;
        this.safeRecoveryRouteGain = safeRecoveryRouteGain;
    }

    void clear() {
        set(0f, true, false, false, 0f, 0f, 0f, 0f, 0f);
    }
}
