package com.github.jbescos.presentation;

/** Timing for a finite camera-target transition. */
public final class CameraTargetTransition {
    private CameraTargetTransition() {
    }

    public static float nextEasedProgress(
            float remainingSeconds,
            float durationSeconds,
            float deltaSeconds) {
        if (durationSeconds <= 0f || remainingSeconds <= 0f) {
            return 1f;
        }
        float nextRemaining = Math.max(0f, remainingSeconds - Math.max(0f, deltaSeconds));
        float linearProgress =
                Math.max(0f, Math.min(1f, 1f - nextRemaining / durationSeconds));
        return linearProgress * linearProgress * (3f - 2f * linearProgress);
    }
}
