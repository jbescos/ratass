package com.github.jbescos.ai.rl;

/** Counts whole physics steps so float subtraction cannot delay a policy decision. */
public final class RlDecisionClock {
    private final float physicsStep;
    private int remainingSteps;

    public RlDecisionClock(float physicsStep) {
        if (!(physicsStep > 0f) || Float.isInfinite(physicsStep)) {
            throw new IllegalArgumentException("Physics step must be finite and positive");
        }
        this.physicsStep = physicsStep;
    }

    /** Elapsed time must represent a whole number of fixed physics steps. */
    public boolean advance(float elapsedSeconds) {
        remainingSteps = Math.max(0, remainingSteps - toSteps(elapsedSeconds));
        return remainingSteps == 0;
    }

    public void schedule(float intervalSeconds) {
        remainingSteps = Math.max(1, toSteps(intervalSeconds));
    }

    public void limitDelay(float intervalSeconds) {
        remainingSteps = Math.min(remainingSteps, Math.max(1, toSteps(intervalSeconds)));
    }

    public void reset() {
        remainingSteps = 0;
    }

    private int toSteps(float seconds) {
        return Math.max(0, Math.round(seconds / physicsStep));
    }
}
