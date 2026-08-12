package com.github.jbescos.presentation;

/** Presentation-only state for the restraint shown on a Void Anchor target. */
public final class VoidAnchorVisual {
    private float remainingSeconds;
    private float activeAge;
    private float appearanceDelaySeconds;

    public void start(float durationSeconds) {
        start(durationSeconds, 0f);
    }

    public void start(float durationSeconds, float appearanceDelaySeconds) {
        float duration = sanitizeDuration(durationSeconds);
        if (duration <= 0f) {
            reset();
            return;
        }
        remainingSeconds = duration;
        activeAge = 0f;
        this.appearanceDelaySeconds = Math.min(
                duration,
                sanitizeDuration(appearanceDelaySeconds));
    }

    public void update(float deltaSeconds) {
        if (!isActive()) {
            return;
        }
        float delta = sanitizeDelta(deltaSeconds);
        activeAge += delta;
        remainingSeconds = Math.max(0f, remainingSeconds - delta);
    }

    public void reset() {
        remainingSeconds = 0f;
        activeAge = 0f;
        appearanceDelaySeconds = 0f;
    }

    public boolean isActive() {
        return remainingSeconds > 0f;
    }

    public boolean isVisible() {
        return isActive() && activeAge >= appearanceDelaySeconds;
    }

    public float getDeployment() {
        return isVisible()
                ? Math.min(1f, (activeAge - appearanceDelaySeconds) / 0.18f)
                : 0f;
    }

    public float getPulse() {
        return isVisible()
                ? 0.5f
                        + 0.5f
                                * (float) Math.sin(
                                        (activeAge - appearanceDelaySeconds) * 8f)
                : 0f;
    }

    public float getEndFade() {
        return isActive() ? Math.min(1f, remainingSeconds / 0.18f) : 0f;
    }

    private static float sanitizeDuration(float durationSeconds) {
        if (durationSeconds <= 0f
                || Float.isNaN(durationSeconds)
                || Float.isInfinite(durationSeconds)) {
            return 0f;
        }
        return durationSeconds;
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
