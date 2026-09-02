package com.github.jbescos.gameplay;

/** Detects sustained low-speed, nose-to-nose car contacts. */
public final class FaceToFaceDeadlockDetector {
    private static final float MAX_OPPOSING_DIRECTION_DOT = -0.55f;
    private static final float MIN_NOSE_ALIGNMENT = 0.55f;

    private final float maximumSpeed;
    private final float triggerSeconds;
    private final float cooldownSeconds;
    private int trackedOpponentId = -1;
    private float deadlockSeconds;
    private float cooldownRemaining;

    public FaceToFaceDeadlockDetector(
            float maximumSpeed,
            float triggerSeconds,
            float cooldownSeconds) {
        this.maximumSpeed = Math.max(0f, maximumSpeed);
        this.triggerSeconds = Math.max(0f, triggerSeconds);
        this.cooldownSeconds = Math.max(0f, cooldownSeconds);
    }

    public boolean update(float delta, int opponentId, boolean deadlocked) {
        float elapsed = Float.isFinite(delta) ? Math.max(0f, delta) : 0f;
        boolean coolingDown = cooldownRemaining > 0f;
        cooldownRemaining = Math.max(0f, cooldownRemaining - elapsed);
        if (!deadlocked || opponentId < 0 || coolingDown) {
            clearCandidate();
            return false;
        }
        if (trackedOpponentId != opponentId) {
            trackedOpponentId = opponentId;
            deadlockSeconds = 0f;
        }
        deadlockSeconds += elapsed;
        if (deadlockSeconds < triggerSeconds) {
            return false;
        }
        cooldownRemaining = cooldownSeconds;
        clearCandidate();
        return true;
    }

    public void reset() {
        clearCandidate();
        cooldownRemaining = 0f;
    }

    public boolean isDeadlocked(
            float ownSpeed,
            float opponentSpeed,
            float directionDot,
            float ownNoseAlignment,
            float opponentNoseAlignment) {
        return finiteAtMost(ownSpeed, maximumSpeed)
                && finiteAtMost(opponentSpeed, maximumSpeed)
                && Float.isFinite(directionDot)
                && directionDot <= MAX_OPPOSING_DIRECTION_DOT
                && Float.isFinite(ownNoseAlignment)
                && ownNoseAlignment >= MIN_NOSE_ALIGNMENT
                && Float.isFinite(opponentNoseAlignment)
                && opponentNoseAlignment >= MIN_NOSE_ALIGNMENT;
    }

    private void clearCandidate() {
        trackedOpponentId = -1;
        deadlockSeconds = 0f;
    }

    private static boolean finiteAtMost(float value, float maximum) {
        return Float.isFinite(value) && value >= 0f && value <= maximum;
    }
}
