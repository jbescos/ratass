package com.github.jbescos.presentation;

/** Rendering-agnostic latest local peaks for the telemetry panel. */
public final class TelemetryPeakTracker {
    private static final float PEAK_REVERSAL_THRESHOLD = 0.005f;

    private final LatestPeak speedRatio = new LatestPeak();
    private final LatestPeak drive = new LatestPeak();
    private final LatestPeak brake = new LatestPeak();
    private final LatestPeak drift = new LatestPeak();
    private final LatestPeak slipstream = new LatestPeak();

    public void update(
            float currentSpeedRatio,
            float currentDrive,
            float currentBrake,
            float currentDrift,
            float currentSlipstream) {
        speedRatio.update(sanitizeUnit(currentSpeedRatio));
        drive.update(sanitizeUnit(currentDrive));
        brake.update(sanitizeUnit(currentBrake));
        drift.update(sanitizeUnit(currentDrift));
        slipstream.update(sanitizeUnit(currentSlipstream));
    }

    public void reset() {
        speedRatio.reset();
        drive.reset();
        brake.reset();
        drift.reset();
        slipstream.reset();
    }

    public float getSpeedRatio() {
        return speedRatio.getValue();
    }

    public float getDrive() {
        return drive.getValue();
    }

    public float getBrake() {
        return brake.getValue();
    }

    public float getDrift() {
        return drift.getValue();
    }

    public float getSlipstream() {
        return slipstream.getValue();
    }

    private static float sanitizeNonNegative(float value) {
        return isFinite(value) ? Math.max(0f, value) : 0f;
    }

    private static float sanitizeUnit(float value) {
        return Math.max(0f, Math.min(1f, sanitizeNonNegative(value)));
    }

    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }

    private static final class LatestPeak {
        private float value;
        private float peakCandidate;
        private float valleyCandidate;
        private boolean initialized;
        private boolean seekingPeak = true;

        private void update(float current) {
            if (!initialized) {
                initialized = true;
                peakCandidate = current;
                valleyCandidate = current;
                return;
            }

            if (seekingPeak) {
                peakCandidate = Math.max(peakCandidate, current);
                if (peakCandidate - current >= PEAK_REVERSAL_THRESHOLD) {
                    value = peakCandidate;
                    valleyCandidate = current;
                    seekingPeak = false;
                }
                return;
            }

            valleyCandidate = Math.min(valleyCandidate, current);
            if (current - valleyCandidate >= PEAK_REVERSAL_THRESHOLD) {
                peakCandidate = current;
                seekingPeak = true;
            }
        }

        private float getValue() {
            return value;
        }

        private void reset() {
            value = 0f;
            peakCandidate = 0f;
            valleyCandidate = 0f;
            initialized = false;
            seekingPeak = true;
        }
    }
}
