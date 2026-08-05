package com.github.jbescos.presentation;

/** Tracks active UI time before an unattended screen advances automatically. */
public final class UiInactivityTimer {
    private final float timeoutSeconds;
    private float elapsedSeconds;

    public UiInactivityTimer(float timeoutSeconds) {
        if (timeoutSeconds <= 0f) {
            throw new IllegalArgumentException("Timeout must be positive.");
        }
        this.timeoutSeconds = timeoutSeconds;
    }

    public void reset() {
        elapsedSeconds = 0f;
    }

    public boolean update(float deltaSeconds) {
        elapsedSeconds =
                Math.min(
                        timeoutSeconds,
                        elapsedSeconds + Math.max(0f, deltaSeconds));
        return elapsedSeconds >= timeoutSeconds;
    }

    public int getRemainingWholeSeconds() {
        return Math.max(
                0,
                (int) Math.ceil(timeoutSeconds - elapsedSeconds));
    }
}
