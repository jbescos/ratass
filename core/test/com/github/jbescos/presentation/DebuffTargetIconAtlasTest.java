package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import org.junit.Test;

public class DebuffTargetIconAtlasTest {
    @Test
    public void mapsEveryTargetDebuffToItsGeneratedCell() {
        assertEquals(0, DebuffTargetIconAtlas.indexFor(RogueliteCardId.DRAFT_VENDETTA));
        assertEquals(1, DebuffTargetIconAtlas.indexFor(RogueliteCardId.TAR_TETHER));
        assertEquals(2, DebuffTargetIconAtlas.indexFor(RogueliteCardId.SENSOR_JAMMER));
        assertEquals(3, DebuffTargetIconAtlas.indexFor(RogueliteCardId.RECOVERY_BEACON));
        assertEquals(4, DebuffTargetIconAtlas.indexFor(RogueliteCardId.EMP_SNARE));
        assertEquals(5, DebuffTargetIconAtlas.indexFor(RogueliteCardId.GRID_BLACKOUT));
        assertEquals(6, DebuffTargetIconAtlas.indexFor(RogueliteCardId.VOID_ANCHOR));
        assertEquals(7, DebuffTargetIconAtlas.indexFor(RogueliteCardId.TOTAL_BLACKOUT));
        assertEquals(8, DebuffTargetIconAtlas.indexFor(RogueliteCardId.PAYBACK_SHIELD));
    }

    @Test
    public void rejectsCardsWithoutTargetDebuffArtwork() {
        assertEquals(-1, DebuffTargetIconAtlas.indexFor(RogueliteCardId.NITRO_PULSE));
        assertEquals(-1, DebuffTargetIconAtlas.indexFor(null));
    }
}
