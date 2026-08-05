package com.github.jbescos.presentation;

import com.badlogic.gdx.math.Rectangle;

/** Responsive placement for the outcome artwork shown above championship results. */
public final class RogueliteEndArtworkLayout {
    private RogueliteEndArtworkLayout() {}

    public static Rectangle updateDefeatArtworkBounds(
            Rectangle output,
            Rectangle panel,
            int textureWidth,
            int textureHeight) {
        float maxWidth = panel.width * 0.22f;
        float maxHeight = panel.height * 0.20f;
        float aspect =
                Math.max(1, textureWidth)
                        / (float) Math.max(1, textureHeight);
        float width = maxWidth;
        float height = width / aspect;
        if (height > maxHeight) {
            height = maxHeight;
            width = height * aspect;
        }
        output.set(
                panel.x + (panel.width - width) * 0.5f,
                panel.y + panel.height * 0.88f - height * 0.5f,
                width,
                height);
        return output;
    }
}
