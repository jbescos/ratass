package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.github.jbescos.gameplay.roguelite.RogueliteCardCatalog;
import com.github.jbescos.gameplay.roguelite.RogueliteCardDefinition;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class RogueliteCardArtworkTest {
    @Test
    public void resolvesThemeScopedArtworkFiles() {
        assertEquals("roguelite/cards/artwork/000.png", RogueliteCardArtwork.pathForIndex(0));
        assertEquals("roguelite/cards/artwork/009.png", RogueliteCardArtwork.pathForIndex(9));
        assertEquals("roguelite/cards/artwork/137.png", RogueliteCardArtwork.pathForIndex(137));
    }

    @Test
    public void rejectsUnsupportedArtworkIndexes() {
        assertNull(RogueliteCardArtwork.pathForIndex(-1));
        assertNull(RogueliteCardArtwork.pathForIndex(138));
    }

    @Test
    public void everyCardResolvesOneUniqueArtworkFile() {
        Set<String> paths = new HashSet<String>();
        for (RogueliteCardDefinition card : RogueliteCardCatalog.all()) {
            String path = RogueliteCardArtwork.pathForIndex(card.getArtworkIndex());
            assertNotNull(path);
            paths.add(path);
        }
        assertEquals(RogueliteCardCatalog.all().size(), paths.size());
    }
}
