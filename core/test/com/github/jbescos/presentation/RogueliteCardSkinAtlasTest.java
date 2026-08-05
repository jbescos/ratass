package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;

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
        assertEquals(147f, RogueliteCardSkinAtlas.preferredArtworkSize(420f, false), 0f);
        assertEquals(147f, RogueliteCardSkinAtlas.preferredArtworkSize(420f, true), 0f);
        assertEquals(75f, RogueliteCardSkinAtlas.preferredArtworkSize(250f, false), 0f);
        assertEquals(75f, RogueliteCardSkinAtlas.preferredArtworkSize(250f, true), 0f);
        assertEquals(0f, RogueliteCardSkinAtlas.preferredArtworkSize(0f, false), 0f);
    }

    @Test
    public void exposesNonOverlappingCardSafeAreas() {
        float height = 400f;

        assertEquals(30f, RogueliteCardSkinAtlas.informationPanelBottom(height), 0.001f);
        assertEquals(156f, RogueliteCardSkinAtlas.informationPanelTop(height), 0.001f);
        assertEquals(164f, RogueliteCardSkinAtlas.artworkWindowBottom(height), 0.001f);
        assertEquals(316f, RogueliteCardSkinAtlas.artworkWindowTop(height), 0.001f);
    }

    @Test
    public void footerLabelMatchesTheCenteredShellSocket() {
        assertEquals(119.04f, RogueliteCardSkinAtlas.footerLabelLeft(384f), 0.001f);
        assertEquals(138.24f, RogueliteCardSkinAtlas.footerLabelWidth(384f), 0.001f);
        assertEquals(18.944f, RogueliteCardSkinAtlas.footerLabelBottom(512f), 0.001f);
        assertEquals(18.944f, RogueliteCardSkinAtlas.footerLabelHeight(512f), 0.001f);
    }
}
