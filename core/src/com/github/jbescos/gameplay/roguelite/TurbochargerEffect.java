package com.github.jbescos.gameplay.roguelite;

final class TurbochargerEffect extends RogueliteUpgradeEffect {
    private final boolean synergy;
    private float fullThrottleTimer;

    TurbochargerEffect(boolean synergy) {
        super(RogueliteCardId.TURBOCHARGER);
        this.synergy = synergy;
    }

    @Override
    boolean isActive() {
        return latestFrame != null && latestFrame.onRoad && latestFrame.throttle > 0.05f;
    }

    @Override
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        if (frame.onRoad && frame.throttle >= 0.92f) {
            fullThrottleTimer = Math.min(3f, fullThrottleTimer + delta);
        } else {
            fullThrottleTimer = Math.max(0f, fullThrottleTimer - delta * 2f);
        }
    }

    @Override
    float accelerationBonus() {
        return 0.10f;
    }

    @Override
    float maxSpeedBonus() {
        if (fullThrottleTimer < 1.5f) {
            return 0f;
        }
        return synergy ? 0.055f : 0.04f;
    }
}
