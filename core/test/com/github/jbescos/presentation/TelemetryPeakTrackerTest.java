package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TelemetryPeakTrackerTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void recordsLatestLocalPeakForEveryTelemetrySignal() {
        TelemetryPeakTracker tracker = new TelemetryPeakTracker();

        tracker.update(0.42f, 0.4f, 0.7f, 0.3f, 0.5f);
        tracker.update(0.38f, 0.2f, 0.1f, 0.2f, 0.1f);

        assertEquals(0.42f, tracker.getSpeedRatio(), EPSILON);
        assertEquals(0.4f, tracker.getDrive(), EPSILON);
        assertEquals(0.7f, tracker.getBrake(), EPSILON);
        assertEquals(0.3f, tracker.getDrift(), EPSILON);
        assertEquals(0.5f, tracker.getSlipstream(), EPSILON);
    }

    @Test
    public void replacesAnOlderHigherPeakWithTheNextLocalPeak() {
        TelemetryPeakTracker tracker = new TelemetryPeakTracker();

        tracker.update(0.8f, 0.8f, 0.8f, 0.8f, 0.8f);
        tracker.update(0.5f, 0.5f, 0.5f, 0.5f, 0.5f);
        tracker.update(0.6f, 0.6f, 0.6f, 0.6f, 0.6f);
        tracker.update(0.4f, 0.4f, 0.4f, 0.4f, 0.4f);

        assertEquals(0.6f, tracker.getSpeedRatio(), EPSILON);
        assertEquals(0.6f, tracker.getDrive(), EPSILON);
        assertEquals(0.6f, tracker.getBrake(), EPSILON);
        assertEquals(0.6f, tracker.getDrift(), EPSILON);
        assertEquals(0.6f, tracker.getSlipstream(), EPSILON);
    }

    @Test
    public void retainsTheMeasuredSpeedForTheLatestPeakMarker() {
        TelemetryPeakTracker tracker = new TelemetryPeakTracker();

        tracker.update(0.72f, 214f, 0f, 0f, 0f, 0f);
        tracker.update(0.50f, 149f, 0f, 0f, 0f, 0f);

        assertEquals(0.72f, tracker.getSpeedRatio(), EPSILON);
        assertEquals(214f, tracker.getSpeedKph(), EPSILON);

        tracker.update(0.64f, 191f, 0f, 0f, 0f, 0f);
        tracker.update(0.40f, 119f, 0f, 0f, 0f, 0f);

        assertEquals(0.64f, tracker.getSpeedRatio(), EPSILON);
        assertEquals(191f, tracker.getSpeedKph(), EPSILON);
    }

    @Test
    public void ignoresSmallSignalNoiseWhenDetectingReversals() {
        TelemetryPeakTracker tracker = new TelemetryPeakTracker();

        tracker.update(0.5f, 0f, 0f, 0f, 0f);
        tracker.update(0.497f, 0f, 0f, 0f, 0f);

        assertEquals(0f, tracker.getSpeedRatio(), EPSILON);
        assertEquals(0f, tracker.getSpeedKph(), EPSILON);

        tracker.update(0.49f, 0f, 0f, 0f, 0f);

        assertEquals(0.5f, tracker.getSpeedRatio(), EPSILON);
    }

    @Test
    public void invalidValuesAreIgnoredAndNormalizedSignalsAreClamped() {
        TelemetryPeakTracker tracker = new TelemetryPeakTracker();

        tracker.update(Float.NaN, 3f, Float.POSITIVE_INFINITY, -2f, 1.5f);
        tracker.update(0f, 0f, 0f, 0f, 0f);

        assertEquals(0f, tracker.getSpeedRatio(), EPSILON);
        assertEquals(1f, tracker.getDrive(), EPSILON);
        assertEquals(0f, tracker.getBrake(), EPSILON);
        assertEquals(0f, tracker.getDrift(), EPSILON);
        assertEquals(1f, tracker.getSlipstream(), EPSILON);
    }

    @Test
    public void resetClearsAllMarkers() {
        TelemetryPeakTracker tracker = new TelemetryPeakTracker();
        tracker.update(0.6f, 1f, 1f, 1f, 1f);

        tracker.reset();

        assertEquals(0f, tracker.getSpeedRatio(), EPSILON);
        assertEquals(0f, tracker.getSpeedKph(), EPSILON);
        assertEquals(0f, tracker.getDrive(), EPSILON);
        assertEquals(0f, tracker.getBrake(), EPSILON);
        assertEquals(0f, tracker.getDrift(), EPSILON);
        assertEquals(0f, tracker.getSlipstream(), EPSILON);
    }
}
