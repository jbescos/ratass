package com.github.jbescos.gameplay.roguelite;

/** Independently timed blindness curses whose strongest active penalties apply together. */
public final class OffenderCurseState {
    private static final int MAX_STACKED_CURSES = 3;

    private final float[] massMultipliers = new float[MAX_STACKED_CURSES];
    private final float[] performanceMultipliers = new float[MAX_STACKED_CURSES];
    private final float[] remainingSeconds = new float[MAX_STACKED_CURSES];

    public OffenderCurseState() {
        reset();
    }

    public boolean apply(
            float nextMassMultiplier,
            float nextPerformanceMultiplier,
            float durationSeconds) {
        float previousMassMultiplier = getMassMultiplier();
        float safeMass = RogueliteEffectMath.clamp(nextMassMultiplier, 1f, 2f);
        float safePerformance = RogueliteEffectMath.clamp(nextPerformanceMultiplier, 0f, 1f);
        float safeDuration = Float.isFinite(durationSeconds)
                ? Math.max(0f, durationSeconds)
                : 0f;
        int slot = findMatchingOrEmptySlot(safeMass, safePerformance);
        massMultipliers[slot] = safeMass;
        performanceMultipliers[slot] = safePerformance;
        remainingSeconds[slot] = Math.max(remainingSeconds[slot], safeDuration);
        return Math.abs(previousMassMultiplier - getMassMultiplier()) > 0.0001f;
    }

    /** Returns true when expiry changes the effective collision mass. */
    public boolean advance(float deltaSeconds) {
        float previousMassMultiplier = getMassMultiplier();
        float safeDelta = Float.isFinite(deltaSeconds) ? Math.max(0f, deltaSeconds) : 0f;
        for (int index = 0; index < remainingSeconds.length; index++) {
            remainingSeconds[index] = Math.max(0f, remainingSeconds[index] - safeDelta);
        }
        return Math.abs(previousMassMultiplier - getMassMultiplier()) > 0.0001f;
    }

    public void reset() {
        for (int index = 0; index < remainingSeconds.length; index++) {
            massMultipliers[index] = 1f;
            performanceMultipliers[index] = 1f;
            remainingSeconds[index] = 0f;
        }
    }

    public boolean isActive() {
        for (int index = 0; index < remainingSeconds.length; index++) {
            if (remainingSeconds[index] > 0f) {
                return true;
            }
        }
        return false;
    }

    public boolean isBlind() {
        return isActive();
    }

    public float getMassMultiplier() {
        float multiplier = 1f;
        for (int index = 0; index < remainingSeconds.length; index++) {
            if (remainingSeconds[index] > 0f) {
                multiplier = Math.max(multiplier, massMultipliers[index]);
            }
        }
        return multiplier;
    }

    public float getGripMultiplier() {
        return getPerformanceMultiplier();
    }

    public float getPowerMultiplier() {
        return getPerformanceMultiplier();
    }

    public float getAerodynamicEfficiencyMultiplier() {
        return getPerformanceMultiplier();
    }

    public float getRemainingSeconds() {
        float maximum = 0f;
        for (int index = 0; index < remainingSeconds.length; index++) {
            maximum = Math.max(maximum, remainingSeconds[index]);
        }
        return maximum;
    }

    private float getPerformanceMultiplier() {
        float multiplier = 1f;
        for (int index = 0; index < remainingSeconds.length; index++) {
            if (remainingSeconds[index] > 0f) {
                multiplier = Math.min(multiplier, performanceMultipliers[index]);
            }
        }
        return multiplier;
    }

    private int findMatchingOrEmptySlot(float mass, float performance) {
        int emptySlot = -1;
        for (int index = 0; index < remainingSeconds.length; index++) {
            if (Math.abs(massMultipliers[index] - mass) < 0.0001f
                    && Math.abs(performanceMultipliers[index] - performance) < 0.0001f) {
                return index;
            }
            if (emptySlot < 0 && remainingSeconds[index] <= 0f) {
                emptySlot = index;
            }
        }
        if (emptySlot >= 0) {
            return emptySlot;
        }
        int shortestSlot = 0;
        for (int index = 1; index < remainingSeconds.length; index++) {
            if (remainingSeconds[index] < remainingSeconds[shortestSlot]) {
                shortestSlot = index;
            }
        }
        return shortestSlot;
    }
}
