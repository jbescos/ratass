package com.github.jbescos.gameplay.roguelite;

/**
 * A continuously equipped Powerup represented as repeated finite activations.
 * Its cooldown runs concurrently with the activation, matching regular Powerups.
 */
abstract class RepeatingPowerupEffect extends RogueliteUpgradeEffect {
    static final float DURATION_SECONDS = 10f;
    static final float COOLDOWN_SECONDS = DURATION_SECONDS;

    private float activeTimer = DURATION_SECONDS;
    private float cooldownTimer = COOLDOWN_SECONDS;
    private boolean loadedByRandomCard;
    private boolean randomActivationExecuted;

    RepeatingPowerupEffect(RogueliteCardId cardId) {
        super(cardId);
    }

    @Override
    final boolean isActive() {
        return activeTimer > 0f;
    }

    @Override
    final boolean isReady() {
        return !isActive() && cooldownTimer <= 0f;
    }

    @Override
    final float readiness() {
        if (isActive() || isReady()) {
            return 1f;
        }
        return RogueliteEffectMath.clamp(
                1f - cooldownTimer / COOLDOWN_SECONDS,
                0f,
                1f);
    }

    @Override
    final float activeTimeRemainingSeconds() {
        return activeTimer;
    }

    @Override
    final float cooldownTimeRemainingSeconds() {
        return cooldownTimer;
    }

    @Override
    final void onLoadedByRandomCard() {
        loadedByRandomCard = true;
        randomActivationExecuted = false;
        activeTimer = 0f;
        cooldownTimer = COOLDOWN_SECONDS;
    }

    @Override
    final void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        float elapsed = Math.max(0f, timerDelta);
        cooldownTimer = Math.max(0f, cooldownTimer - elapsed);
        if (isActive()) {
            activeTimer = Math.max(0f, activeTimer - elapsed);
            if (activeTimer <= 0f && cooldownTimer <= 0f && !loadedByRandomCard) {
                activeTimer = DURATION_SECONDS;
                cooldownTimer = COOLDOWN_SECONDS;
            }
            return;
        }
        if (cooldownTimer <= 0f
                && (!loadedByRandomCard || !randomActivationExecuted)) {
            activeTimer = DURATION_SECONDS;
            cooldownTimer = COOLDOWN_SECONDS;
            randomActivationExecuted = loadedByRandomCard;
        }
    }
}
