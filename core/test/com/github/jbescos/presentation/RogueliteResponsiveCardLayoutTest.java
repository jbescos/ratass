package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RogueliteResponsiveCardLayoutTest {
    @Test
    public void rewardSectionsUseMatchingGridDensityOnConstrainedScreens() {
        assertEquals(
                3,
                RogueliteResponsiveCardLayout.rewardSectionColumns(1280f, 720f, 3, 3));
        assertEquals(
                5,
                RogueliteResponsiveCardLayout.rewardSectionColumns(1280f, 720f, 5, 5));
        assertEquals(
                3,
                RogueliteResponsiveCardLayout.rewardSectionColumns(390f, 844f, 3, 3));
        assertEquals(
                3,
                RogueliteResponsiveCardLayout.rewardSectionColumns(390f, 844f, 5, 5));
        assertEquals(
                3,
                RogueliteResponsiveCardLayout.rewardSectionColumns(844f, 390f, 5, 5));
        assertEquals(
                0,
                RogueliteResponsiveCardLayout.rewardSectionColumns(1280f, 720f, 0, 5));
    }

    @Test
    public void cardBrowserShowsThreeOffersPerPageOnPhones() {
        assertEquals(
                3,
                RogueliteResponsiveCardLayout.collectionPageCapacity(390f, 844f, 5));
        assertEquals(
                3,
                RogueliteResponsiveCardLayout.collectionPageCapacity(844f, 390f, 5));
    }

    @Test
    public void cardBrowserShowsAtMostThreeOffersPerPage() {
        assertEquals(
                3,
                RogueliteResponsiveCardLayout.collectionPageCapacity(1280f, 720f, 5));
        assertEquals(
                2,
                RogueliteResponsiveCardLayout.collectionPageCapacity(720f, 1280f, 2));
        assertEquals(
                0,
                RogueliteResponsiveCardLayout.collectionPageCapacity(720f, 1280f, 0));
    }

    @Test
    public void equippedLoadoutShowsAllFiveSlotsTogether() {
        assertEquals(5, RogueliteResponsiveCardLayout.equippedLoadoutPageCapacity(5));
        assertEquals(0, RogueliteResponsiveCardLayout.equippedLoadoutPageCapacity(0));
        assertEquals(
                5,
                RogueliteResponsiveCardLayout.equippedLoadoutColumns(1280f, 720f, 5));
        assertEquals(
                3,
                RogueliteResponsiveCardLayout.equippedLoadoutColumns(844f, 390f, 5));
        assertEquals(
                1,
                RogueliteResponsiveCardLayout.equippedLoadoutColumns(390f, 844f, 5));
        assertEquals(
                2,
                RogueliteResponsiveCardLayout.equippedLoadoutColumns(720f, 1280f, 5));
    }

    @Test
    public void carStatsYieldSpaceOnlyOnShortScreens() {
        assertTrue(RogueliteResponsiveCardLayout.showCarStats(720f));
        assertTrue(RogueliteResponsiveCardLayout.showCarStats(844f));
        assertFalse(RogueliteResponsiveCardLayout.showCarStats(390f));
    }

    @Test
    public void allButtonsUseLargeTouchTargets() {
        assertEquals(
                56f,
                RogueliteResponsiveCardLayout.minimumTouchTarget(390f, 844f),
                0.001f);
        assertEquals(
                56f,
                RogueliteResponsiveCardLayout.minimumTouchTarget(844f, 390f),
                0.001f);
        assertEquals(
                61.2f,
                RogueliteResponsiveCardLayout.minimumTouchTarget(1280f, 720f),
                0.001f);
        assertEquals(
                72f,
                RogueliteResponsiveCardLayout.minimumTouchTarget(1920f, 1080f),
                0.001f);
    }

    @Test
    public void cardsButtonUsesAnExtraLargeTouchTarget() {
        assertEquals(
                88f,
                RogueliteResponsiveCardLayout.cardsButtonSize(390f, 844f),
                0.001f);
        assertEquals(
                88f,
                RogueliteResponsiveCardLayout.cardsButtonSize(844f, 390f),
                0.001f);
        assertEquals(
                100.8f,
                RogueliteResponsiveCardLayout.cardsButtonSize(1280f, 720f),
                0.001f);
        assertEquals(
                120f,
                RogueliteResponsiveCardLayout.cardsButtonSize(1920f, 1080f),
                0.001f);
    }

    @Test
    public void mainMenuButtonsScaleUpWithoutBreakingShortLandscapeScreens() {
        assertEquals(
                60f,
                RogueliteResponsiveCardLayout.mainMenuButtonHeight(390f),
                0.001f);
        assertEquals(
                75.6f,
                RogueliteResponsiveCardLayout.mainMenuButtonHeight(720f),
                0.001f);
        assertEquals(
                84f,
                RogueliteResponsiveCardLayout.mainMenuButtonHeight(1080f),
                0.001f);
    }

    @Test
    public void cardSelectionActionsUseLargeResponsiveButtons() {
        assertEquals(
                72f,
                RogueliteResponsiveCardLayout.rewardActionButtonHeight(390f, 844f),
                0.001f);
        assertEquals(
                72f,
                RogueliteResponsiveCardLayout.rewardActionButtonHeight(844f, 390f),
                0.001f);
        assertEquals(
                93.6f,
                RogueliteResponsiveCardLayout.rewardActionButtonHeight(1280f, 720f),
                0.001f);
        assertEquals(
                96f,
                RogueliteResponsiveCardLayout.rewardActionButtonHeight(1920f, 1080f),
                0.001f);
        assertEquals(
                200f,
                RogueliteResponsiveCardLayout.rewardActionButtonMaximumWidth(844f),
                0.001f);
        assertEquals(
                260f,
                RogueliteResponsiveCardLayout.rewardActionButtonMaximumWidth(1920f),
                0.001f);
    }

    @Test
    public void cardBandTextUsesItsCapHeightForVerticalCentering() {
        assertEquals(
                117f,
                RogueliteResponsiveCardLayout.centeredTextBaseline(100f, 24f, 10f),
                0.001f);
        assertEquals(
                100f,
                RogueliteResponsiveCardLayout.centeredTextBaseline(100f, -2f, -3f),
                0.001f);
    }

}
