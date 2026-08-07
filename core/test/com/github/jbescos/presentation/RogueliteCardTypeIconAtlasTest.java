package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;

import com.github.jbescos.gameplay.roguelite.RogueliteSlotType;
import org.junit.Test;

public class RogueliteCardTypeIconAtlasTest {
    @Test
    public void mapsEveryCardCategoryToItsAtlasColumn() {
        assertEquals(0, RogueliteCardTypeIconAtlas.indexFor(RogueliteSlotType.DRIVER));
        assertEquals(1, RogueliteCardTypeIconAtlas.indexFor(RogueliteSlotType.TUNING));
        assertEquals(2, RogueliteCardTypeIconAtlas.indexFor(RogueliteSlotType.TECHNIQUE));
        assertEquals(3, RogueliteCardTypeIconAtlas.indexFor(RogueliteSlotType.POWERUP));
        assertEquals(4, RogueliteCardTypeIconAtlas.indexFor(RogueliteSlotType.REVENGE));
        assertEquals(5, RogueliteCardTypeIconAtlas.WARNING_INDEX);
    }

    @Test
    public void rejectsMissingCardCategory() {
        assertEquals(-1, RogueliteCardTypeIconAtlas.indexFor(null));
    }
}
