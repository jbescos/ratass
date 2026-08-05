package com.github.jbescos.presentation;

/** Presentation-only state for the warning shown while a car is debuffed. */
public final class DebuffWarningVisual {
    private boolean active;
    private float activeAge;

    public void update(float deltaSeconds, boolean currentlyActive) {
        if (currentlyActive != active) {
            active = currentlyActive;
            activeAge = 0f;
            return;
        }
        if (active) {
            activeAge += sanitizeDelta(deltaSeconds);
        }
    }

    public void reset() {
        active = false;
        activeAge = 0f;
    }

    public boolean isActive() {
        return active;
    }

    public float getPulse() {
        return active
                ? 0.5f + 0.5f * (float) Math.sin(activeAge * 7.5f)
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
