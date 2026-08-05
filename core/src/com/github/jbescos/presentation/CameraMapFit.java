package com.github.jbescos.presentation;

/** Allocation-free map bounds fitting for an orthographic camera. */
public final class CameraMapFit {
    private CameraMapFit() {}

    public static float calculateZoom(
            float mapWidth,
            float mapHeight,
            float viewportWidth,
            float viewportHeight,
            float padding) {
        float safeViewportWidth = Math.max(1f, viewportWidth);
        float safeViewportHeight = Math.max(1f, viewportHeight);
        float safePadding = Math.max(1f, padding);
        float zoomX = Math.max(0f, mapWidth) / safeViewportWidth;
        float zoomY = Math.max(0f, mapHeight) / safeViewportHeight;
        return Math.max(0.01f, Math.max(zoomX, zoomY) * safePadding);
    }
}
