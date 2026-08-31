package com.github.jbescos.gameplay;

/** Tracks a manual driver's last trusted on-road route position. */
public final class ManualShortcutGuard {
    public static final float MAX_REENTRY_PROGRESS_FRACTION = 0.03f;
    private static final float LARGE_ROUTE_WRAP_FRACTION = 0.5f;

    private boolean initialized;
    private boolean offRoad;
    private float trustedProgress;

    public boolean update(
            boolean manualControl,
            boolean onRoad,
            float currentProgress,
            float routeLength) {
        if (!isFinite(currentProgress) || routeLength <= 0f) {
            reset();
            return false;
        }

        if (!initialized) {
            if (!manualControl) {
                return false;
            }
            initialized = true;
            trustedProgress = currentProgress;
        }

        if (offRoad) {
            if (!onRoad) {
                return false;
            }
            offRoad = false;
            float progressDelta = routeProgressDelta(trustedProgress, currentProgress, routeLength);
            if (Math.abs(progressDelta) > routeLength * MAX_REENTRY_PROGRESS_FRACTION) {
                return true;
            }
            trustedProgress = currentProgress;
            return false;
        }

        if (!manualControl) {
            reset();
            return false;
        }

        if (!onRoad) {
            offRoad = true;
            return false;
        }

        trustedProgress = currentProgress;
        return false;
    }

    public float getTrustedProgress() {
        return trustedProgress;
    }

    public boolean isReentryPending() {
        return initialized && offRoad;
    }

    public void reset() {
        initialized = false;
        offRoad = false;
        trustedProgress = 0f;
    }

    /**
     * Detects the large wrapped progress jump produced by crossing the race start backwards.
     */
    public static boolean isLargeBackwardStartCrossing(
            float previousProgress,
            float currentProgress,
            float startProgress,
            float routeLength) {
        if (!isFinite(previousProgress)
                || !isFinite(currentProgress)
                || !isFinite(startProgress)
                || routeLength <= 0f) {
            return false;
        }
        float previousRelative = wrap(previousProgress - startProgress, routeLength);
        float currentRelative = wrap(currentProgress - startProgress, routeLength);
        return currentRelative - previousRelative > routeLength * LARGE_ROUTE_WRAP_FRACTION;
    }

    private static float routeProgressDelta(float from, float to, float routeLength) {
        float delta = wrap(to, routeLength) - wrap(from, routeLength);
        float halfLength = routeLength * 0.5f;
        if (delta < -halfLength) {
            delta += routeLength;
        } else if (delta > halfLength) {
            delta -= routeLength;
        }
        return delta;
    }

    private static float wrap(float progress, float routeLength) {
        float wrapped = progress % routeLength;
        return wrapped < 0f ? wrapped + routeLength : wrapped;
    }

    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }
}
