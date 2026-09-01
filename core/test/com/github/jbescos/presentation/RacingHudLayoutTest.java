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
    public void infoIconMatchesHeaderHeightInsideLargerTouchTarget() {
        assertEquals(12.8f, RacingHudLayout.infoIconVisualSize(16f, 24f, 24f), 0.001f);
        assertEquals(17.6f, RacingHudLayout.infoIconVisualSize(22f, 28f, 28f), 0.001f);
        assertEquals(14f, RacingHudLayout.infoIconVisualSize(22f, 14f, 24f), 0.001f);
    }

    @Test
    public void infoIconMovesUpToMatchTheVisibleHeaderGlyphs() {
        assertEquals(1.6f, RacingHudLayout.infoIconVisualOffsetY(16f), 0.001f);
        assertEquals(2f, RacingHudLayout.infoIconVisualOffsetY(22f), 0.001f);
    }

    @Test
    public void bottomPanelStopsGrowingOnTallDisplays() {
        assertEquals(151.2f, RacingHudLayout.bottomPanelHeight(720f), 0.001f);
        assertEquals(194f, RacingHudLayout.bottomPanelHeight(1080f), 0.001f);
        assertEquals(48.96f, RacingHudLayout.raceSummaryPanelHeight(720f), 0.001f);
        assertEquals(58f, RacingHudLayout.raceSummaryPanelHeight(1080f), 0.001f);
        assertEquals(16f, RacingHudLayout.bottomPanelSectionGap(1920f), 0.001f);
        assertEquals(24f, RacingHudLayout.bottomPanelMetricRowHeight(1920f, 1080f), 0.001f);
    }

    @Test
    public void bottomPanelReservesTheSidebarWidthForItsMinimap() {
        assertEquals(
                260f,
                RacingHudLayout.bottomPanelMinimapWidth(720f, 260f),
                0.001f);
        assertEquals(
                420f,
                RacingHudLayout.bottomPanelMinimapWidth(1920f, 420f),
                0.001f);
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
                        154f,
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
    public void sidebarUsesCompactTextAndKeepsLargeClickableRows() {
        assertEquals(1.05f, RacingHudLayout.sidebarTextScale(), 0.001f);
        assertEquals(1.4f, RacingHudLayout.sidebarRowScale(), 0.001f);
        assertEquals(0.82f, RacingHudLayout.sidebarTimingHeaderScale(), 0.001f);
        assertEquals(12.6f, RacingHudLayout.sidebarLineHeight(12f), 0.001f);
        assertEquals(22f, RacingHudLayout.sidebarTableRowStep(12f), 0.001f);
        assertEquals(30f, RacingHudLayout.sidebarTableRowStep(20f), 0.001f);
    }

    @Test
    public void sidebarContentWidthUsesTheWidestMeasuredTable() {
        assertEquals(
                170f,
                RacingHudLayout.sidebarContentWidth(
                        16f,
                        4f,
                        52f,
                        7f,
                        42f,
                        42f,
                        22f,
                        22f,
                        120f),
                0.001f);
        assertEquals(
                190f,
                RacingHudLayout.sidebarContentWidth(
                        16f,
                        4f,
                        52f,
                        7f,
                        42f,
                        42f,
                        22f,
                        22f,
                        190f),
                0.001f);
    }

    @Test
    public void inGameMenuButtonKeepsAMobileFriendlyTouchTarget() {
        assertEquals(52f, RacingHudLayout.inGameMenuButtonSize(720f, 390f), 0.001f);
        assertEquals(64.8f, RacingHudLayout.inGameMenuButtonSize(1280f, 720f), 0.001f);
        assertEquals(72f, RacingHudLayout.inGameMenuButtonSize(1920f, 1080f), 0.001f);
        assertEquals(10f, RacingHudLayout.inGameMenuButtonMargin(720f, 390f), 0.001f);
        assertEquals(18f, RacingHudLayout.inGameMenuButtonMargin(1920f, 1080f), 0.001f);
        assertEquals(7f, RacingHudLayout.inGameButtonGap(720f, 390f), 0.001f);
        assertEquals(288f, RacingHudLayout.inGameControlStripWidth(720f, 390f, 5), 0.001f);
        assertEquals(0f, RacingHudLayout.inGameControlStripWidth(720f, 390f, 0), 0.001f);
        assertEquals(5.2f, RacingHudLayout.raceControlBarPadding(720f, 390f), 0.001f);
        assertEquals(62.4f, RacingHudLayout.raceControlBarHeight(720f, 390f), 0.001f);
        assertEquals(84f, RacingHudLayout.raceControlBarHeight(1920f, 1080f), 0.001f);
        assertEquals(3.12f, RacingHudLayout.raceSummaryRowGap(52f), 0.001f);
        assertEquals(15.253f, RacingHudLayout.raceSummaryRowHeight(52f), 0.001f);
        assertEquals(4f, RacingHudLayout.raceSummaryRowGap(72f), 0.001f);
        assertEquals(21.333f, RacingHudLayout.raceSummaryRowHeight(72f), 0.001f);
    }

    @Test
    public void compactStatsLeaveMoreWidthForCards() {
        assertEquals(270f, RacingHudLayout.bottomPanelCarStatsWidth(1000f), 0.001f);
        assertEquals(400f, RacingHudLayout.bottomPanelTelemetryWidth(1000f), 0.001f);
        assertEquals(330f, RacingHudLayout.bottomPanelCardsWidth(1000f), 0.001f);
        assertEquals(94.5f, RacingHudLayout.bottomPanelCarStatsWidth(350f), 0.001f);
        assertEquals(115.5f, RacingHudLayout.bottomPanelCardsWidth(350f), 0.001f);
        assertEquals(64f, RacingHudLayout.carStatsLabelWidth(100f), 0.001f);
        assertEquals(96f, RacingHudLayout.carStatsLabelWidth(200f), 0.001f);
        assertEquals(62f, RacingHudLayout.telemetryLabelWidth(150f), 0.001f);
        assertEquals(114f, RacingHudLayout.telemetryLabelWidth(300f), 0.001f);
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

    @Test
    public void carLabelIncludesItsCurrentRacePosition() {
        assertEquals("4-Torque", RacingHudLayout.carRaceLabel(4, "Torque"));
        assertEquals("Torque", RacingHudLayout.carRaceLabel(0, "Torque"));
    }

    @Test
    public void carStatToneTreatsLowerMassAsBeneficial() {
        assertEquals(1, RacingHudLayout.carStatBenefitTone(1.08f, false));
        assertEquals(-1, RacingHudLayout.carStatBenefitTone(0.92f, false));
        assertEquals(1, RacingHudLayout.carStatBenefitTone(0.92f, true));
        assertEquals(-1, RacingHudLayout.carStatBenefitTone(1.08f, true));
        assertEquals(0, RacingHudLayout.carStatBenefitTone(1.004f, false));
    }
}
