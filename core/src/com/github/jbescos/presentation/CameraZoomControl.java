package com.github.jbescos.presentation;

/** Allocation-free camera zoom calculations shared by mouse and touch controls. */
public final class CameraZoomControl {
    private CameraZoomControl() {}

    public static float step(
            float current,
            float direction,
            float step,
            float minimum,
            float maximum) {
        return clamp(current + direction * step, minimum, maximum);
    }

    public static float scale(
            float current,
            float pinchScale,
            float minimum,
            float maximum) {
        if (!Float.isFinite(pinchScale) || pinchScale <= 0f) {
            return clamp(current, minimum, maximum);
        }
        return clamp(current * pinchScale, minimum, maximum);
    }

    public static float limitToMap(float requestedZoom, float mapFitZoom) {
        return Math.min(requestedZoom, mapFitZoom);
    }

    public static boolean reachesWholeMap(float requestedZoom, float mapFitZoom) {
        return requestedZoom >= mapFitZoom - 0.001f;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
