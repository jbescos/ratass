package com.github.jbescos.presentation;

import com.badlogic.gdx.math.Vector2;

/** Converts a screen drag into a world-space camera pan. */
public final class FreeCameraPan {
    private FreeCameraPan() {}

    public static void applyDrag(
            Vector2 position,
            float screenDeltaX,
            float screenDeltaY,
            float viewportWidth,
            float viewportHeight,
            float zoom,
            float screenWidth,
            float screenHeight) {
        if (position == null
                || viewportWidth <= 0f
                || viewportHeight <= 0f
                || zoom <= 0f
                || screenWidth <= 0f
                || screenHeight <= 0f) {
            return;
        }
        position.add(
                -screenDeltaX * viewportWidth * zoom / screenWidth,
                screenDeltaY * viewportHeight * zoom / screenHeight);
    }
}
