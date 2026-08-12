package com.github.jbescos.gameplay.roguelite;

/** Passive Powerup that becomes visible and amplifies each Revenge activation. */
final class RevengeAmplifierPowerupEffect extends RogueliteUpgradeEffect {
    private static final float MINIMUM_VISIBLE_SECONDS = 1.5f;

    private final float multiplier;
    private float activeTimer;

    RevengeAmplifierPowerupEffect(RogueliteCardId cardId) {
        super(cardId);
        switch (cardId) {
            case GRUDGE_SPARK:
                multiplier = 1.25f;
                break;
            case VENGEANCE_CORE:
                multiplier = 1.50f;
                break;
            case NEMESIS_ENGINE:
                multiplier = 2f;
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported Revenge amplifier Powerup: " + cardId);
        }
    }

    @Override
    boolean isActive() {
        return activeTimer > 0f;
    }

    @Override
    boolean isReady() {
        return !isActive();
    }

    @Override
    float readiness() {
        return 1f;
    }

    @Override
    float activeTimeRemainingSeconds() {
        return activeTimer;
    }

    @Override
    int activeDisplayPriority() {
        return 8;
    }

    @Override
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        activeTimer = Math.max(0f, activeTimer - Math.max(0f, timerDelta));
    }

    @Override
    float revengeEffectMultiplier() {
        return multiplier;
    }

    @Override
    void onRevengeActivated(float durationSeconds) {
        activeTimer = Math.max(
                activeTimer,
                Math.max(MINIMUM_VISIBLE_SECONDS, Math.max(0f, durationSeconds)));
    }

    @Override
    void onRevengeFinished() {
        activeTimer = Math.min(activeTimer, MINIMUM_VISIBLE_SECONDS);
    }
}
