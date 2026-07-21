package com.github.jbescos.gameplay.roguelite;

final class RecoveryDifferentialEffect extends RogueliteUpgradeEffect {
    private final boolean synergy;
    private float boostTimer;
    private float offRoadStartProgress;
    private boolean roadStateInitialized;
    private boolean wasOnRoad;

    RecoveryDifferentialEffect(int level, boolean synergy) {
        super(RogueliteCardId.RECOVERY_DIFFERENTIAL, level);
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
                float duration =
                        RogueliteEffectMath.levelValue(level, 0.8f, 1.1f, 1.4f);
                if (level >= 2 && frame.speedRatio < 0.18f) {
                    duration *= 1.5f;
                }
                boostTimer = Math.max(boostTimer, duration);
            }
        }
        wasOnRoad = frame.onRoad;
    }

    @Override
    float accelerationBonus() {
        return boostTimer > 0f
                ? RogueliteEffectMath.levelValue(level, 0f, 0.06f, 0.10f)
                : 0f;
    }

    @Override
    float maxSpeedBonus() {
        return boostTimer > 0f && synergy ? 0.025f : 0f;
    }

    @Override
    float gripBonus(float slip) {
        return boostTimer > 0f
                ? RogueliteEffectMath.levelValue(level, 0.08f, 0.12f, 0.16f)
                : 0f;
    }
}
