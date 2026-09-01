package com.github.jbescos.presentation;

public final class RacingHudLayout {
    private static final float MIN_STANDINGS_PANEL_WIDTH = 180f;
    private static final float MIN_PLAYFIELD_WIDTH = 320f;
    private static final float MIN_BOTTOM_PANEL_HEIGHT = 130f;
    private static final float MAX_BOTTOM_PANEL_HEIGHT = 194f;
    private static final float MIN_RACE_SUMMARY_HEIGHT = 44f;
    private static final float MAX_RACE_SUMMARY_HEIGHT = 58f;
    private static final int CAR_STAT_ROWS = 6;
    private static final int TELEMETRY_ROWS = 6;
    private static final float CAR_STATS_SECTION_RATIO = 0.27f;
    private static final float TELEMETRY_SECTION_RATIO = 0.40f;
    private static final float SIDEBAR_TEXT_SCALE = 1.05f;
    private static final float SIDEBAR_ROW_SCALE = 1.4f;
    private static final float SIDEBAR_TIMING_HEADER_SCALE = 0.82f;

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

    public static int carStatBenefitTone(
            float multiplier,
            boolean lowerIsBetter) {
        int displayedPercent = Math.round(multiplier * 100f);
        if (displayedPercent == 100) {
            return 0;
        }
        boolean beneficial = displayedPercent > 100;
        if (lowerIsBetter) {
            beneficial = !beneficial;
        }
        return beneficial ? 1 : -1;
    }

    public static int cardRows(int loadoutSlotCount) {
        return Math.max(1, loadoutSlotCount + 1);
    }

    public static float bottomPanelHeight(float screenHeight) {
        return clamp(
                screenHeight * 0.21f,
                MIN_BOTTOM_PANEL_HEIGHT,
                MAX_BOTTOM_PANEL_HEIGHT);
    }

    public static float raceSummaryPanelHeight(float screenHeight) {
        return clamp(
                screenHeight * 0.068f,
                MIN_RACE_SUMMARY_HEIGHT,
                MAX_RACE_SUMMARY_HEIGHT);
    }

    public static float raceControlBarPadding(float screenWidth, float screenHeight) {
        return clamp(inGameMenuButtonSize(screenWidth, screenHeight) * 0.10f, 4f, 6f);
    }

    public static float raceControlBarHeight(float screenWidth, float screenHeight) {
        return inGameMenuButtonSize(screenWidth, screenHeight)
                + raceControlBarPadding(screenWidth, screenHeight) * 2f;
    }

    public static float raceSummaryRowGap(float buttonSize) {
        return clamp(buttonSize * 0.06f, 2f, 4f);
    }

    public static float raceSummaryRowHeight(float buttonSize) {
        return Math.max(1f, (buttonSize - raceSummaryRowGap(buttonSize) * 2f) / 3f);
    }

    public static float bottomPanelMinimapWidth(
            float screenWidth,
            float preferredSidebarWidth) {
        return standingsPanelWidth(screenWidth, preferredSidebarWidth);
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

    public static float infoIconVisualSize(
            float sectionTitleHeight,
            float hitWidth,
            float hitHeight) {
        return Math.max(
                0f,
                Math.min(
                        Math.max(0f, sectionTitleHeight) * 0.80f,
                        Math.min(Math.max(0f, hitWidth), Math.max(0f, hitHeight))));
    }

    public static float infoIconVisualOffsetY(float sectionTitleHeight) {
        return clamp(Math.max(0f, sectionTitleHeight) * 0.10f, 1.5f, 2f);
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

    public static float carStatsLabelWidth(float contentWidth) {
        return clamp(contentWidth * 0.48f, 64f, 124f);
    }

    public static float telemetryLabelWidth(float contentWidth) {
        return clamp(contentWidth * 0.38f, 62f, 120f);
    }

    public static float cardStatusIconSize(float slotHeight) {
        return clamp(slotHeight * 0.68f, 11f, 20f);
    }

    public static float carNameBaseline(
            float normalBaseline,
            float lineHeight,
            float minimumBaseline,
            float maximumBaseline,
            boolean warningActive) {
        if (!warningActive) {
            return normalBaseline;
        }
        float clearance = Math.max(18f, Math.max(0f, lineHeight) + 6f);
        float raisedBaseline = normalBaseline + clearance;
        if (raisedBaseline <= maximumBaseline) {
            return raisedBaseline;
        }
        return Math.max(minimumBaseline, normalBaseline - clearance);
    }

    public static String carRaceLabel(int racePosition, String carName) {
        if (racePosition <= 0) {
            return carName;
        }
        return racePosition + "-" + carName;
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
            float rowY = viewportTop - contentOffset - row * rowStep + scrollOffset;
            if (touchY >= rowY - rowStep + 2f && touchY <= rowY + 4f) {
                return row;
            }
        }
        return -1;
    }

    public static float sidebarTextScale() {
        return SIDEBAR_TEXT_SCALE;
    }

    public static float sidebarRowScale() {
        return SIDEBAR_ROW_SCALE;
    }

    public static float sidebarTimingHeaderScale() {
        return SIDEBAR_TIMING_HEADER_SCALE;
    }

    public static float sidebarContentWidth(
            float positionWidth,
            float positionGap,
            float nameWidth,
            float columnGap,
            float bestLapWidth,
            float currentLapWidth,
            float championshipChangeWidth,
            float championshipPointsWidth,
            float titleWidth) {
        float raceWidth =
                positive(positionWidth)
                        + positive(positionGap)
                        + positive(nameWidth)
                        + positive(columnGap) * 2f
                        + positive(bestLapWidth)
                        + positive(currentLapWidth);
        float championshipWidth =
                positive(positionWidth)
                        + positive(positionGap)
                        + positive(nameWidth)
                        + positive(columnGap) * 2f
                        + positive(championshipChangeWidth)
                        + positive(championshipPointsWidth);
        return Math.max(Math.max(raceWidth, championshipWidth), positive(titleWidth));
    }

    public static float sidebarLineHeight(float unscaledLineHeight) {
        return Math.max(0f, unscaledLineHeight) * SIDEBAR_TEXT_SCALE;
    }

    public static float sidebarTableRowStep(float unscaledLineHeight) {
        return Math.max(22f, Math.max(0f, unscaledLineHeight) * SIDEBAR_ROW_SCALE + 2f);
    }

    public static float inGameMenuButtonSize(float screenWidth, float screenHeight) {
        return clamp(Math.min(screenWidth, screenHeight) * 0.09f, 52f, 72f);
    }

    public static float inGameMenuButtonMargin(float screenWidth, float screenHeight) {
        return clamp(Math.min(screenWidth, screenHeight) * 0.025f, 10f, 18f);
    }

    public static float inGameButtonGap(float screenWidth, float screenHeight) {
        return Math.max(7f, inGameMenuButtonMargin(screenWidth, screenHeight) * 0.55f);
    }

    public static float inGameControlStripWidth(
            float screenWidth,
            float screenHeight,
            int buttonCount) {
        int count = Math.max(0, buttonCount);
        if (count == 0) {
            return 0f;
        }
        return inGameMenuButtonSize(screenWidth, screenHeight) * count
                + inGameButtonGap(screenWidth, screenHeight) * (count - 1);
    }

    private static float clamp(float value, float minimum, float maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static float positive(float value) {
        return Math.max(0f, value);
    }
}
