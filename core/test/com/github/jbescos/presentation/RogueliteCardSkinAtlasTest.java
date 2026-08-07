package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.github.jbescos.gameplay.roguelite.RogueliteSlotType;
import org.junit.Test;

public class RogueliteCardSkinAtlasTest {
    @Test
    public void mapsFilledAndEmptyRowsBySlotType() {
        RogueliteSlotType[] types = RogueliteSlotType.values();
        for (int i = 0; i < types.length; i++) {
            assertEquals(i, RogueliteCardSkinAtlas.indexFor(types[i], false));
            assertEquals(
                    i + RogueliteCardSkinAtlas.COLUMNS,
                    RogueliteCardSkinAtlas.indexFor(types[i], true));
        }
    }

    @Test
    public void rejectsMissingSlotType() {
        assertEquals(-1, RogueliteCardSkinAtlas.indexFor(null, false));
    }

    @Test
    public void fitsSquareArtworkWithoutGrowingEitherConstraint() {
        assertEquals(96f, RogueliteCardSkinAtlas.fitSquareArtwork(180f, 96f), 0f);
        assertEquals(80f, RogueliteCardSkinAtlas.fitSquareArtwork(80f, 120f), 0f);
        assertEquals(0f, RogueliteCardSkinAtlas.fitSquareArtwork(-1f, 120f), 0f);
    }

    @Test
    public void allocatesMoreArtworkSpaceWhilePreservingDriverStatsRoom() {
        assertEquals(159.6f, RogueliteCardSkinAtlas.preferredArtworkSize(420f, false), 0.001f);
        assertEquals(159.6f, RogueliteCardSkinAtlas.preferredArtworkSize(420f, true), 0.001f);
        assertEquals(85f, RogueliteCardSkinAtlas.preferredArtworkSize(250f, false), 0f);
        assertEquals(85f, RogueliteCardSkinAtlas.preferredArtworkSize(250f, true), 0f);
        assertEquals(0f, RogueliteCardSkinAtlas.preferredArtworkSize(0f, false), 0f);
    }

    @Test
    public void alignsHeaderIconsWithShellSockets() {
        assertEquals(57.344f, RogueliteCardSkinAtlas.headerBadgeIconSize(384f, 512f), 0.001f);
        assertEquals(48f, RogueliteCardSkinAtlas.typeIconCenterX(384f), 0.001f);
        assertEquals(336f, RogueliteCardSkinAtlas.tierIconCenterX(384f), 0.001f);
        assertEquals(464f, RogueliteCardSkinAtlas.headerIconCenterY(512f), 0.001f);
    }

    @Test
    public void centersTitleBetweenHeaderIconsWithoutOverlap() {
        float width = 384f;
        float height = 512f;
        float iconHalfSize = RogueliteCardSkinAtlas.headerBadgeIconSize(width, height) * 0.5f;
        float titleLeft = RogueliteCardSkinAtlas.headerTitleLeft(width);
        float titleRight = titleLeft + RogueliteCardSkinAtlas.headerTitleWidth(width);

        assertEquals(88.32f, titleLeft, 0.001f);
        assertEquals(207.36f, RogueliteCardSkinAtlas.headerTitleWidth(width), 0.001f);
        assertEquals(437.76f, RogueliteCardSkinAtlas.headerTitleBottom(height), 0.001f);
        assertEquals(56.32f, RogueliteCardSkinAtlas.headerTitleHeight(height), 0.001f);
        assertTrue(titleLeft > RogueliteCardSkinAtlas.typeIconCenterX(width) + iconHalfSize);
        assertTrue(titleRight < RogueliteCardSkinAtlas.tierIconCenterX(width) - iconHalfSize);
    }

    @Test
    public void exposesNonOverlappingCardSafeAreas() {
        float height = 400f;

        assertEquals(33.2f, RogueliteCardSkinAtlas.informationPanelBottom(height), 0.001f);
        assertEquals(156f, RogueliteCardSkinAtlas.informationPanelTop(height), 0.001f);
        assertEquals(170f, RogueliteCardSkinAtlas.artworkWindowBottom(height), 0.001f);
        assertEquals(330f, RogueliteCardSkinAtlas.artworkWindowTop(height), 0.001f);
        assertTrue(
                RogueliteCardSkinAtlas.informationPanelTop(height)
                        < RogueliteCardSkinAtlas.artworkWindowBottom(height));
        assertTrue(
                RogueliteCardSkinAtlas.artworkWindowTop(height)
                        < RogueliteCardSkinAtlas.headerTitleBottom(height));
    }

    @Test
    public void footerLabelMatchesTheCenteredShellSocket() {
        assertEquals(126f, RogueliteCardSkinAtlas.footerLabelLeft(384f), 0.001f);
        assertEquals(132f, RogueliteCardSkinAtlas.footerLabelWidth(384f), 0.001f);
        assertEquals(9.216f, RogueliteCardSkinAtlas.footerLabelBottom(512f), 0.001f);
        assertEquals(26.624f, RogueliteCardSkinAtlas.footerLabelHeight(512f), 0.001f);
        assertTrue(
                RogueliteCardSkinAtlas.footerLabelBottom(512f)
                                + RogueliteCardSkinAtlas.footerLabelHeight(512f)
                        < RogueliteCardSkinAtlas.informationPanelBottom(512f));
    }
}
