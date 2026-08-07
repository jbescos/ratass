package com.github.jbescos.gameplay.roguelite;

/** Immediately blinds every rival when this revenge card's owner is hit. */
final class RaceBlackoutRevengeEffect extends RogueliteUpgradeEffect {
    private final float durationSeconds;
    private float activeTimer;
    private float pendingDurationSeconds;

    RaceBlackoutRevengeEffect(RogueliteCardId cardId) {
        super(cardId);
        switch (cardId) {
            case SENSOR_JAMMER:
                durationSeconds = 10f;
                break;
            case GRID_BLACKOUT:
                durationSeconds = 20f;
                break;
            case TOTAL_BLACKOUT:
                durationSeconds = 30f;
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported race blackout card: " + cardId);
        }
    }

    @Override
    boolean isActive() {
        return activeTimer > 0f;
    }

    @Override
    float activeTimeRemainingSeconds() {
        return activeTimer;
    }

    @Override
    int activeDisplayPriority() {
        return 5;
    }

    @Override
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        activeTimer = Math.max(0f, activeTimer - timerDelta);
    }

    @Override
    void onHitBy(int vehicleId, float impactStrength) {
        if (vehicleId < 0 || impactStrength <= 0f) {
            return;
        }
        activeTimer = durationSeconds;
        pendingDurationSeconds = durationSeconds;
    }

    @Override
    float consumeRaceBlackoutSeconds() {
        float pending = pendingDurationSeconds;
        pendingDurationSeconds = 0f;
        return pending;
    }
}
