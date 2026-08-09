package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RacingHudLayoutTest {
    @Test
    public void telemetryUsesOneCompactColumnAtEveryResolution() {
        assertEquals(1, RacingHudLayout.telemetryColumns());
        assertEquals(6, RacingHudLayout.telemetryRows());
        assertEquals(6, RacingHudLayout.carStatRows());
        assertEquals(6, RacingHudLayout.cardRows(5));
        float phoneStandingsWidth =
                RacingHudLayout.standingsPanelWidth(720f, 260f);
        assertEquals(260f, phoneStandingsWidth, 0.001f);
        assertEquals(
                460f,
                720f - phoneStandingsWidth,
                0.001f);
        assertEquals(160f, RacingHudLayout.standingsPanelWidth(480f, 260f), 0.001f);
        assertEquals(420f, RacingHudLayout.standingsPanelWidth(1920f, 420f), 0.001f);
    }

    @Test
    public void bottomPanelKeepsReadableRowsOnShortLandscapeScreens() {
        assertEquals(130f, RacingHudLayout.bottomPanelHeight(390f), 0.001f);
        assertEquals(6f, RacingHudLayout.bottomPanelContentMargin(390f), 0.001f);
        assertEquals(16f, RacingHudLayout.bottomPanelSectionTitleHeight(390f), 0.001f);
        assertEquals(16.333f, RacingHudLayout.bottomPanelMetricRowHeight(720f, 390f), 0.001f);
    }

    @Test
    public void bottomPanelStopsGrowingOnTallDisplays() {
        assertEquals(151.2f, RacingHudLayout.bottomPanelHeight(720f), 0.001f);
        assertEquals(194f, RacingHudLayout.bottomPanelHeight(1080f), 0.001f);
        assertEquals(16f, RacingHudLayout.bottomPanelSectionGap(1920f), 0.001f);
        assertEquals(24f, RacingHudLayout.bottomPanelMetricRowHeight(1920f, 1080f), 0.001f);
    }

    @Test
    public void sidebarHitTestingAccountsForHeadersAndScroll() {
        assertEquals(
                0,
                RacingHudLayout.sidebarTableRowAt(
                        170f,
                        200f,
                        20f,
                        0f,
                        18f,
                        3));
        assertEquals(
                2,
                RacingHudLayout.sidebarTableRowAt(
                        134f,
                        200f,
                        20f,
                        10f,
                        18f,
                        4));
        assertEquals(
                -1,
                RacingHudLayout.sidebarTableRowAt(
                        90f,
                        200f,
                        20f,
                        0f,
                        18f,
                        3));
    }

    @Test
    public void eventCameraRowIsLargeAndOffsetsDriverRows() {
        float eventRowStep = RacingHudLayout.eventCameraRowStep(18f);
        assertEquals(30f, eventRowStep, 0.001f);
        assertEquals(
                0,
                RacingHudLayout.sidebarTableRowAt(
                        178f,
                        200f,
                        0f,
                        0f,
                        eventRowStep,
                        1));
        assertEquals(
                0,
                RacingHudLayout.sidebarTableRowAt(
                        146f,
                        200f,
                        eventRowStep + 20f,
                        0f,
                        18f,
                        3));
    }

    @Test
    public void cardsOccupyTheRightThirdOfTheBottomPanel() {
        assertEquals(300f, RacingHudLayout.bottomPanelCarStatsWidth(1000f), 0.001f);
        assertEquals(400f, RacingHudLayout.bottomPanelTelemetryWidth(1000f), 0.001f);
        assertEquals(300f, RacingHudLayout.bottomPanelCardsWidth(1000f), 0.001f);
        assertEquals(11f, RacingHudLayout.cardStatusIconSize(12f), 0.001f);
        assertEquals(13.6f, RacingHudLayout.cardStatusIconSize(20f), 0.001f);
        assertEquals(20f, RacingHudLayout.cardStatusIconSize(40f), 0.001f);
    }

    @Test
    public void carNameMovesAwayFromAnActiveWarning() {
        assertEquals(
                100f,
                RacingHudLayout.carNameBaseline(100f, 16f, 20f, 180f, false),
                0.001f);
        assertEquals(
                122f,
                RacingHudLayout.carNameBaseline(100f, 16f, 20f, 180f, true),
                0.001f);
        assertEquals(
                138f,
                RacingHudLayout.carNameBaseline(160f, 16f, 20f, 180f, true),
                0.001f);
    }
}
