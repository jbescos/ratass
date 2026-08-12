package com.github.jbescos.gameplay.roguelite;

/** Timed blindness and performance curse applied to an offender. */
public final class OffenderCurseState {
    private boolean active;
    private float massMultiplier = 1f;
    private float performanceMultiplier = 1f;
    private float remainingSeconds;

    public boolean apply(
            float nextMassMultiplier,
            float nextPerformanceMultiplier,
            float durationSeconds) {
        float previousMassMultiplier = massMultiplier;
        remainingSeconds = Math.max(
                remainingSeconds,
                Float.isFinite(durationSeconds) ? Math.max(0f, durationSeconds) : 0f);
        active = remainingSeconds > 0f;
        massMultiplier = Math.max(
                massMultiplier,
                RogueliteEffectMath.clamp(nextMassMultiplier, 1f, 2f));
        performanceMultiplier = Math.min(
                performanceMultiplier,
                RogueliteEffectMath.clamp(nextPerformanceMultiplier, 0f, 1f));
        return Math.abs(previousMassMultiplier - massMultiplier) > 0.0001f;
    }

    public boolean advance(float deltaSeconds) {
        if (!active) {
            return false;
        }
        float safeDelta = Float.isFinite(deltaSeconds) ? Math.max(0f, deltaSeconds) : 0f;
        remainingSeconds = Math.max(0f, remainingSeconds - safeDelta);
        if (remainingSeconds > 0f) {
            return false;
        }
        reset();
        return true;
    }

    public void reset() {
        active = false;
        massMultiplier = 1f;
        performanceMultiplier = 1f;
        remainingSeconds = 0f;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isBlind() {
        return active;
    }

    public float getMassMultiplier() {
        return massMultiplier;
    }

    public float getGripMultiplier() {
        return performanceMultiplier;
    }

    public float getPowerMultiplier() {
        return performanceMultiplier;
    }

    public float getAerodynamicEfficiencyMultiplier() {
        return performanceMultiplier;
    }

    public float getRemainingSeconds() {
        return remainingSeconds;
    }
}
