package com.github.jbescos.presentation;

/** Presentation-only state for the warning shown while a car is debuffed. */
public final class DebuffWarningVisual {
    public enum Reason {
        NONE(""),
        BRAKED("BRAKED"),
        NO_GRIP("NO GRIP"),
        BLIND_ENEMIES("BLIND ENEMIES"),
        SLOWED("SLOWED"),
        FULL_THROTTLE("FULL THROTTLE");

        private final String label;

        Reason(String label) {
            this.label = label;
        }

        public String getLabel() {
            return label;
        }
    }

    private Reason reason = Reason.NONE;
    private float activeAge;

    public void update(float deltaSeconds, Reason currentReason) {
        Reason nextReason = currentReason == null ? Reason.NONE : currentReason;
        if (nextReason != reason) {
            reason = nextReason;
            activeAge = 0f;
            return;
        }
        if (isActive()) {
            activeAge += sanitizeDelta(deltaSeconds);
        }
    }

    public void reset() {
        reason = Reason.NONE;
        activeAge = 0f;
    }

    public boolean isActive() {
        return reason != Reason.NONE;
    }

    public String getReasonLabel() {
        return reason.getLabel();
    }

    public float getPulse() {
        return isActive()
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
