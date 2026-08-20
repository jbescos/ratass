package com.github.jbescos.presentation;

/** Hit-tests a world-space pointer against a rotated car sprite. */
public final class CarCameraHitTest {
    private CarCameraHitTest() {
    }

    public static float hitScore(
            float pointerX,
            float pointerY,
            float centerX,
            float centerY,
            float angleRadians,
            float halfWidth,
            float halfHeight,
            float padding) {
        float offsetX = pointerX - centerX;
        float offsetY = pointerY - centerY;
        float cosine = (float) Math.cos(angleRadians);
        float sine = (float) Math.sin(angleRadians);
        float localX = cosine * offsetX + sine * offsetY;
        float localY = -sine * offsetX + cosine * offsetY;
        float safePadding = Math.max(0f, padding);
        if (Math.abs(localX) > Math.max(0f, halfWidth) + safePadding
                || Math.abs(localY) > Math.max(0f, halfHeight) + safePadding) {
            return Float.POSITIVE_INFINITY;
        }
        return offsetX * offsetX + offsetY * offsetY;
    }
}
