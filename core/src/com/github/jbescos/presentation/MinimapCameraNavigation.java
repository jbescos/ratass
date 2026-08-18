package com.github.jbescos.presentation;

import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;

/** Aspect-fit minimap geometry and HUD-to-world navigation. */
public final class MinimapCameraNavigation {
    private MinimapCameraNavigation() {}

    public static boolean fitMap(
            Rectangle panel,
            float padding,
            Rectangle worldBounds,
            Rectangle out) {
        if (panel == null
                || worldBounds == null
                || out == null
                || panel.width <= 0f
                || panel.height <= 0f
                || worldBounds.width <= 0f
                || worldBounds.height <= 0f) {
            if (out != null) {
                out.set(0f, 0f, 0f, 0f);
            }
            return false;
        }

        float safePadding = Math.max(0f, padding);
        float innerX = panel.x + safePadding;
        float innerY = panel.y + safePadding;
        float innerWidth = panel.width - safePadding * 2f;
        float innerHeight = panel.height - safePadding * 2f;
        if (innerWidth <= 0f || innerHeight <= 0f) {
            out.set(0f, 0f, 0f, 0f);
            return false;
        }

        float scale = Math.min(
                innerWidth / worldBounds.width,
                innerHeight / worldBounds.height);
        if (!Float.isFinite(scale) || scale <= 0f) {
            out.set(0f, 0f, 0f, 0f);
            return false;
        }

        float width = worldBounds.width * scale;
        float height = worldBounds.height * scale;
        out.set(
                innerX + (innerWidth - width) * 0.5f,
                innerY + (innerHeight - height) * 0.5f,
                width,
                height);
        return true;
    }

    public static boolean worldPositionAt(
            float hudX,
            float hudY,
            Rectangle fittedMapBounds,
            Rectangle worldBounds,
            Vector2 out) {
        if (fittedMapBounds == null
                || worldBounds == null
                || out == null
                || fittedMapBounds.width <= 0f
                || fittedMapBounds.height <= 0f
                || worldBounds.width <= 0f
                || worldBounds.height <= 0f
                || !fittedMapBounds.contains(hudX, hudY)) {
            return false;
        }

        float normalizedX = (hudX - fittedMapBounds.x) / fittedMapBounds.width;
        float normalizedY = (hudY - fittedMapBounds.y) / fittedMapBounds.height;
        out.set(
                worldBounds.x + normalizedX * worldBounds.width,
                worldBounds.y + normalizedY * worldBounds.height);
        return true;
    }
}
