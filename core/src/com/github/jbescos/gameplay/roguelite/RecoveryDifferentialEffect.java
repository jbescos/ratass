package com.github.jbescos.gameplay.roguelite;

final class RecoveryDifferentialEffect extends RogueliteUpgradeEffect {
    private final boolean synergy;
    private float boostTimer;
    private float offRoadStartProgress;
    private boolean roadStateInitialized;
    private boolean wasOnRoad;

    RecoveryDifferentialEffect(boolean synergy) {
        super(RogueliteCardId.RECOVERY_DIFFERENTIAL);
        this.synergy = synergy;
    }

    @Override
    boolean isActive() {
        return boostTimer > 0f;
    }

    @Override
    int activeDisplayPriority() {
        return 2;
    }

    @Override
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        boostTimer = Math.max(0f, boostTimer - timerDelta);
        if (frame.routeLength <= 0f) {
            return;
        }
        if (!roadStateInitialized) {
            roadStateInitialized = true;
            wasOnRoad = frame.onRoad;
            return;
        }
        if (wasOnRoad && !frame.onRoad) {
            offRoadStartProgress = frame.routeProgress;
        } else if (!wasOnRoad && frame.onRoad) {
            float routeGain =
                    RogueliteEffectMath.circularDelta(
                            offRoadStartProgress,
                            frame.routeProgress,
                            frame.routeLength);
            if (Math.abs(routeGain) <= frame.safeRecoveryRouteGain) {
                float duration = 1.1f;
                if (frame.speedRatio < 0.18f) {
                    duration *= 1.25f;
                }
                boostTimer = Math.max(boostTimer, duration);
            }
        }
        wasOnRoad = frame.onRoad;
    }

    @Override
    float accelerationBonus() {
        return boostTimer > 0f
                ? 0.06f
                : 0f;
    }

    @Override
    float maxSpeedBonus() {
        return boostTimer > 0f && synergy ? 0.025f : 0f;
    }

    @Override
    float gripBonus(float slip) {
        return boostTimer > 0f
                ? 0.12f
                : 0f;
    }
}
