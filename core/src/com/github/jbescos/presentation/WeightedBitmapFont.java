package com.github.jbescos.presentation;

import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.GlyphLayout;

/** Draws the built-in bitmap font with a subtle second pass for stronger strokes. */
public final class WeightedBitmapFont extends BitmapFont {
    private final float strokeOffset;

    public WeightedBitmapFont(float strokeOffset) {
        super();
        this.strokeOffset = Math.max(0f, strokeOffset);
    }

    @Override
    public GlyphLayout draw(Batch batch, CharSequence text, float x, float y) {
        super.draw(batch, text, x + strokeOffset, y);
        return super.draw(batch, text, x, y);
    }

    @Override
    public GlyphLayout draw(
            Batch batch,
            CharSequence text,
            float x,
            float y,
            float targetWidth,
            int horizontalAlignment,
            boolean wrap) {
        super.draw(
                batch,
                text,
                x + strokeOffset,
                y,
                targetWidth,
                horizontalAlignment,
                wrap);
        return super.draw(
                batch,
                text,
                x,
                y,
                targetWidth,
                horizontalAlignment,
                wrap);
    }

    @Override
    public GlyphLayout draw(
            Batch batch,
            CharSequence text,
            float x,
            float y,
            int start,
            int end,
            float targetWidth,
            int horizontalAlignment,
            boolean wrap) {
        super.draw(
                batch,
                text,
                x + strokeOffset,
                y,
                start,
                end,
                targetWidth,
                horizontalAlignment,
                wrap);
        return super.draw(
                batch,
                text,
                x,
                y,
                start,
                end,
                targetWidth,
                horizontalAlignment,
                wrap);
    }

    @Override
    public GlyphLayout draw(
            Batch batch,
            CharSequence text,
            float x,
            float y,
            int start,
            int end,
            float targetWidth,
            int horizontalAlignment,
            boolean wrap,
            String truncate) {
        super.draw(
                batch,
                text,
                x + strokeOffset,
                y,
                start,
                end,
                targetWidth,
                horizontalAlignment,
                wrap,
                truncate);
        return super.draw(
                batch,
                text,
                x,
                y,
                start,
                end,
                targetWidth,
                horizontalAlignment,
                wrap,
                truncate);
    }

    @Override
    public void draw(Batch batch, GlyphLayout layout, float x, float y) {
        super.draw(batch, layout, x + strokeOffset, y);
        super.draw(batch, layout, x, y);
    }
}
