package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RogueliteCompetitionModeTest {
    @Test
    public void infiniteModeRoundTripsThroughItsPersistentId() {
        RogueliteCompetitionMode mode = RogueliteCompetitionMode.INFINITE;

        assertEquals(mode, RogueliteCompetitionMode.fromId(mode.getId()));
        assertTrue(mode.isInfinite());
        assertTrue(RogueliteCompetitionMode.isKnownId(mode.getId()));
    }

    @Test
    public void customModeRoundTripsThroughItsPersistentId() {
        RogueliteCompetitionMode mode = RogueliteCompetitionMode.CUSTOM;

        assertEquals(mode, RogueliteCompetitionMode.fromId(mode.getId()));
        assertTrue(mode.isCustom());
        assertFalse(mode.isInfinite());
        assertTrue(RogueliteCompetitionMode.isKnownId(mode.getId()));
    }

    @Test
    public void unknownPersistentIdFallsBackToChampionship() {
        assertEquals(
                RogueliteCompetitionMode.CHAMPIONSHIP,
                RogueliteCompetitionMode.fromId("unknown"));
        assertFalse(RogueliteCompetitionMode.isKnownId("unknown"));
    }
}
