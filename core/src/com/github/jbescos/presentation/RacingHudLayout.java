package com.github.jbescos.presentation;

public final class RacingHudLayout {
    private static final float MIN_STANDINGS_PANEL_WIDTH = 180f;
    private static final float MIN_PLAYFIELD_WIDTH = 320f;
    private static final float MIN_BOTTOM_PANEL_HEIGHT = 130f;
    private static final float MAX_BOTTOM_PANEL_HEIGHT = 194f;
    private static final int CAR_STAT_ROWS = 6;
    private static final int TELEMETRY_ROWS = 6;
    private static final float CAR_STATS_SECTION_RATIO = 0.30f;
    private static final float TELEMETRY_SECTION_RATIO = 0.40f;

    private RacingHudLayout() {}

    public static float standingsPanelWidth(
            float screenWidth,
            float preferredWidth) {
        float maximum =
                Math.max(
                        0f,
                        screenWidth - MIN_PLAYFIELD_WIDTH);
        return Math.min(
                Math.max(MIN_STANDINGS_PANEL_WIDTH, preferredWidth),
                maximum);
    }

    public static int telemetryColumns() {
        return 1;
    }

    public static int telemetryRows() {
        return TELEMETRY_ROWS;
    }

    public static int carStatRows() {
        return CAR_STAT_ROWS;
    }

    public static float bottomPanelHeight(float screenHeight) {
        return clamp(
                screenHeight * 0.21f,
                MIN_BOTTOM_PANEL_HEIGHT,
                MAX_BOTTOM_PANEL_HEIGHT);
    }

    public static float bottomPanelContentMargin(float screenHeight) {
        return clamp(screenHeight * 0.015f, 6f, 12f);
    }

    public static float bottomPanelSectionGap(float screenWidth) {
        return clamp(screenWidth * 0.012f, 8f, 16f);
    }

    public static float bottomPanelSectionTitleHeight(float screenHeight) {
        return clamp(screenHeight * 0.028f, 16f, 22f);
    }

    public static float bottomPanelCarStatsWidth(float availableWidth) {
        return Math.max(1f, availableWidth * CAR_STATS_SECTION_RATIO);
    }

    public static float bottomPanelTelemetryWidth(float availableWidth) {
        return Math.max(1f, availableWidth * TELEMETRY_SECTION_RATIO);
    }

    public static float bottomPanelCardsWidth(float availableWidth) {
        return Math.max(
                1f,
                availableWidth
                        - bottomPanelCarStatsWidth(availableWidth)
                        - bottomPanelTelemetryWidth(availableWidth));
    }

    public static float cardActivationMarkerRadius(float slotHeight) {
        return clamp(slotHeight * 0.18f, 2.5f, 5f);
    }

    public static float bottomPanelMetricRowHeight(float screenWidth, float screenHeight) {
        float panelHeight = bottomPanelHeight(screenHeight);
        float margin = bottomPanelContentMargin(screenHeight);
        float availableHeight =
                panelHeight
                        - margin * 2f
                        - bottomPanelSectionTitleHeight(screenHeight)
                        - 4f;
        return Math.max(1f, availableHeight / Math.max(CAR_STAT_ROWS, TELEMETRY_ROWS));
    }

    public static int sidebarTableRowAt(
            float touchY,
            float viewportTop,
            float contentOffset,
            float scrollOffset,
            float rowStep,
            int rowCount) {
        if (rowStep <= 0f || rowCount <= 0) {
            return -1;
        }
        for (int row = 0; row < rowCount; row++) {
            float rowY = viewportTop - contentOffset - row * rowStep - scrollOffset;
            if (touchY >= rowY - rowStep + 2f && touchY <= rowY + 4f) {
                return row;
            }
        }
        return -1;
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }
}
