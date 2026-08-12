package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;

import com.github.jbescos.gameplay.roguelite.RogueliteCardDefinition;
import org.junit.Test;

public class RogueliteCardArtworkAtlasTest {
    @Test
    public void usesThemeScopedArtworkPath() {
        assertEquals(
                "roguelite/cards/card_art_atlas_v3.png",
                RogueliteCardArtworkAtlas.THEMED_RELATIVE_PATH);
    }

    @Test
    public void gridFitsEveryCardArtworkIndex() {
        assertEquals(
                RogueliteCardDefinition.ARTWORK_CAPACITY,
                RogueliteCardArtworkAtlas.COLUMNS * RogueliteCardArtworkAtlas.ROWS);
    }
}
