package com.github.jbescos.presentation;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;

/** Deterministic, rendering-agnostic state for active-card feedback. */
public final class AbilityActivationVisual {
    private RogueliteCardId activeCardId;
    private float activeAge;
    private boolean revengeArmed;
    private float revengeArmedAge;
    private boolean techniqueActive;
    private float techniqueActiveAge;
    private boolean powerupActive;
    private float powerupActiveAge;

    public void reset() {
        activeCardId = null;
        activeAge = 0f;
        revengeArmed = false;
        revengeArmedAge = 0f;
        techniqueActive = false;
        techniqueActiveAge = 0f;
        powerupActive = false;
        powerupActiveAge = 0f;
    }

    public void update(float deltaSeconds, RogueliteCardId currentActiveCardId) {
        update(deltaSeconds, currentActiveCardId, false, false, false);
    }

    public void update(
            float deltaSeconds,
            RogueliteCardId currentActiveCardId,
            boolean currentRevengeArmed) {
        update(
                deltaSeconds,
                currentActiveCardId,
                currentRevengeArmed,
                false,
                false);
    }

    public void update(
            float deltaSeconds,
            RogueliteCardId currentActiveCardId,
            boolean currentRevengeArmed,
            boolean currentTechniqueActive,
            boolean currentPowerupActive) {
        float delta = sanitizeDelta(deltaSeconds);
        if (currentRevengeArmed != revengeArmed) {
            revengeArmed = currentRevengeArmed;
            revengeArmedAge = 0f;
        } else if (revengeArmed) {
            revengeArmedAge += delta;
        }
        if (currentTechniqueActive != techniqueActive) {
            techniqueActive = currentTechniqueActive;
            techniqueActiveAge = 0f;
        } else if (techniqueActive) {
            techniqueActiveAge += delta;
        }
        if (currentPowerupActive != powerupActive) {
            powerupActive = currentPowerupActive;
            powerupActiveAge = 0f;
        } else if (powerupActive) {
            powerupActiveAge += delta;
        }
        if (currentActiveCardId != activeCardId) {
            activeCardId = currentActiveCardId;
            activeAge = 0f;
            return;
        }
        if (activeCardId != null) {
            activeAge += delta;
        }
    }

    public RogueliteCardId getActiveCardId() {
        return activeCardId;
    }

    public boolean isActive() {
        return activeCardId != null;
    }

    public boolean hasCarCenteredEffect() {
        return activeCardId != null
                && activeCardId != RogueliteCardId.DRAFT_VENDETTA
                && activeCardId != RogueliteCardId.TAR_TETHER
                && activeCardId != RogueliteCardId.EMP_SNARE;
    }

    public float getPulse() {
        return isActive()
                ? 0.5f + 0.5f * (float) Math.sin(activeAge * 12f)
                : 0f;
    }

    public float getActivationFlash() {
        return isActive() ? Math.max(0f, 1f - activeAge / 0.42f) : 0f;
    }

    public boolean isRevengeArmed() {
        return revengeArmed;
    }

    public float getRevengeArmedPulse() {
        return revengeArmed
                ? 0.5f + 0.5f * (float) Math.sin(revengeArmedAge * 5.5f)
                : 0f;
    }

    public boolean isTechniqueActive() {
        return techniqueActive;
    }

    public float getTechniquePulse() {
        return techniqueActive
                ? 0.5f + 0.5f * (float) Math.sin(techniqueActiveAge * 7f)
                : 0f;
    }

    public boolean isPowerupActive() {
        return powerupActive;
    }

    public float getPowerupPulse() {
        return powerupActive
                ? 0.5f + 0.5f * (float) Math.sin(powerupActiveAge * 8.5f)
                : 0f;
    }

    private static float sanitizeDelta(float deltaSeconds) {
        if (deltaSeconds <= 0f
                || Float.isNaN(deltaSeconds)
                || Float.isInfinite(deltaSeconds)) {
            return 0f;
        }
        return Math.min(deltaSeconds, 1f);
    }
}
