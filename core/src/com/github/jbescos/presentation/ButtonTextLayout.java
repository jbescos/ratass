package com.github.jbescos.presentation;

public final class ButtonTextLayout {
    private ButtonTextLayout() {}

    public static float horizontalInset(float width, float height) {
        if (width <= 0f || height <= 0f) {
            return 0f;
        }
        return Math.max(8f, Math.min(width * 0.10f, height * 0.42f));
    }

    public static float verticalInset(float height) {
        if (height <= 0f) {
            return 0f;
        }
        return Math.max(6f, Math.min(12f, height * 0.15f));
    }

    public static float contentWidth(float width, float height) {
        if (width <= 0f || height <= 0f) {
            return 0f;
        }
        return Math.max(1f, width - horizontalInset(width, height) * 2f);
    }

    public static float contentHeight(float height) {
        if (height <= 0f) {
            return 0f;
        }
        return Math.max(1f, height - verticalInset(height) * 2f);
    }

    public static float preferredTextScale(float height) {
        if (height <= 0f) {
            return 1f;
        }
        return Math.max(1.05f, Math.min(1.42f, height * 1.30f / 56f));
    }

    public static float centeredBaseline(
            float bottom,
            float height,
            float capHeight,
            float textScale) {
        if (height <= 0f) {
            return bottom;
        }
        return bottom
                + (height + Math.max(0f, capHeight) * Math.max(0f, textScale))
                        * 0.5f;
    }
}
