package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;

import com.github.jbescos.gameplay.roguelite.RogueliteSetCatalog;
import com.github.jbescos.gameplay.roguelite.RogueliteSetDefinition;
import com.github.jbescos.gameplay.roguelite.RogueliteSetId;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class RogueliteSetAtlasTest {
    @Test
    public void everySetHasOneUniqueIconAndArtworkCell() {
        Set<Integer> indexes = new HashSet<Integer>();
        for (RogueliteSetId id : RogueliteSetId.values()) {
            RogueliteSetDefinition set = RogueliteSetCatalog.get(id);
            assertEquals(set.getIconIndex(), RogueliteSetIconAtlas.indexFor(set));
            assertEquals(set.getIconIndex(), RogueliteSetArtworkAtlas.indexFor(set));
            indexes.add(Integer.valueOf(set.getIconIndex()));
        }

        assertEquals(
                RogueliteSetId.values().length,
                indexes.size());
        assertEquals(
                RogueliteSetId.values().length,
                RogueliteSetIconAtlas.COLUMNS * RogueliteSetIconAtlas.ROWS);
        assertEquals(
                RogueliteSetId.values().length,
                RogueliteSetArtworkAtlas.COLUMNS * RogueliteSetArtworkAtlas.ROWS);
        assertEquals(
                "roguelite/cards/set_art_atlas.png",
                RogueliteSetArtworkAtlas.THEMED_RELATIVE_PATH);
        assertEquals(
                "roguelite/cards/set_card_shell.png",
                RogueliteSetCardSkin.ASSET_PATH);
    }

    @Test
    public void setShellDistinguishesEmptySelectedAndNormalStates() {
        assertEquals(0.78f, RogueliteSetCardSkin.brightness(true, false), 0f);
        assertEquals(1f, RogueliteSetCardSkin.brightness(false, true), 0f);
        assertEquals(0.94f, RogueliteSetCardSkin.brightness(false, false), 0f);
    }
}
