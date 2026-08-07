package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RogueliteTierIconAtlasTest {
    @Test
    public void mapsSupportedTiersToAtlasColumns() {
        assertEquals(0, RogueliteTierIconAtlas.indexForTier(1));
        assertEquals(1, RogueliteTierIconAtlas.indexForTier(2));
        assertEquals(2, RogueliteTierIconAtlas.indexForTier(3));
    }

    @Test
    public void rejectsUnsupportedTiers() {
        assertEquals(-1, RogueliteTierIconAtlas.indexForTier(0));
        assertEquals(-1, RogueliteTierIconAtlas.indexForTier(4));
    }
}
