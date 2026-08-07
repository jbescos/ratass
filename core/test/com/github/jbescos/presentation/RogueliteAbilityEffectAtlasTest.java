package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.github.jbescos.gameplay.roguelite.RogueliteAbilityVisualStyle;
import org.junit.Test;

public class RogueliteAbilityEffectAtlasTest {
    @Test
    public void mapsEveryAbilityStyleToOneUniqueCell() {
        boolean[] used = new boolean[RogueliteAbilityEffectAtlas.COLUMNS];
        for (RogueliteAbilityVisualStyle style : RogueliteAbilityVisualStyle.values()) {
            int index = RogueliteAbilityEffectAtlas.indexFor(style);
            assertTrue(index >= 0 && index < used.length);
            assertTrue(!used[index]);
            used[index] = true;
            assertTrue(RogueliteAbilityEffectAtlas.sizeScale(style) > 0f);
        }
    }

    @Test
    public void rejectsMissingStyle() {
        assertEquals(-1, RogueliteAbilityEffectAtlas.indexFor(null));
        assertEquals(0f, RogueliteAbilityEffectAtlas.sizeScale(null), 0f);
    }
}
