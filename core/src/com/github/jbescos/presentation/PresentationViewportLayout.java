package com.github.jbescos.presentation;

public final class PresentationViewportLayout {
    public static final int ANDROID_LOGICAL_WIDTH = 1280;
    public static final int ANDROID_LOGICAL_HEIGHT = 720;
    private static final float TARGET_ASPECT_RATIO = 16f / 9f;

    private PresentationViewportLayout() {}

    public static Layout fit(int screenWidth, int screenHeight, boolean androidScale) {
        int safeWidth = Math.max(1, screenWidth);
        int safeHeight = Math.max(1, screenHeight);
        float screenAspectRatio = safeWidth / (float) safeHeight;

        int contentWidth;
        int contentHeight;
        if (screenAspectRatio > TARGET_ASPECT_RATIO) {
            contentHeight = safeHeight;
            contentWidth = Math.max(1, Math.round(contentHeight * TARGET_ASPECT_RATIO));
        } else {
            contentWidth = safeWidth;
            contentHeight = Math.max(1, Math.round(contentWidth / TARGET_ASPECT_RATIO));
        }
        contentWidth = Math.min(safeWidth, contentWidth);
        contentHeight = Math.min(safeHeight, contentHeight);

        int screenX = (safeWidth - contentWidth) / 2;
        int screenY = (safeHeight - contentHeight) / 2;
        float logicalWidth = androidScale ? ANDROID_LOGICAL_WIDTH : contentWidth;
        float logicalHeight = androidScale ? ANDROID_LOGICAL_HEIGHT : contentHeight;
        return new Layout(
                screenX,
                screenY,
                contentWidth,
                contentHeight,
                logicalWidth,
                logicalHeight);
    }

    public static final class Layout {
        public final int screenX;
        public final int screenY;
        public final int screenWidth;
        public final int screenHeight;
        public final float logicalWidth;
        public final float logicalHeight;

        private Layout(
                int screenX,
                int screenY,
                int screenWidth,
                int screenHeight,
                float logicalWidth,
                float logicalHeight) {
            this.screenX = screenX;
            this.screenY = screenY;
            this.screenWidth = screenWidth;
            this.screenHeight = screenHeight;
            this.logicalWidth = logicalWidth;
            this.logicalHeight = logicalHeight;
        }
    }
}
