package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import org.junit.Test;

public class RogueliteAbilityEffectLayersTest {
    @Test
    public void outwardFieldRevengeCardsUseTheirCenteredFieldArtwork() {
        assertTrue(RogueliteAbilityEffectLayers.usesCenteredArtwork(
                RogueliteCardId.DRAFT_MAGNET));
        assertTrue(RogueliteAbilityEffectLayers.usesCenteredArtwork(
                RogueliteCardId.REPULSOR_WAVE));
        assertTrue(RogueliteAbilityEffectLayers.usesCenteredArtwork(
                RogueliteCardId.REPULSOR_SURGE));
        assertFalse(RogueliteAbilityEffectLayers.usesCenteredArtwork(
                RogueliteCardId.TELEMETRY_THEFT));
        assertTrue(RogueliteAbilityEffectLayers.usesCenteredArtwork(
                RogueliteCardId.NITRO_PULSE));
    }

    @Test
    public void keepsOutwardFieldVisibleBelowARevengeAmplifier() {
        assertEquals(
                RogueliteCardId.DRAFT_MAGNET,
                RogueliteAbilityEffectLayers.underlayFor(
                        RogueliteCardId.NEMESIS_ENGINE,
                        RogueliteCardId.NEMESIS_ENGINE,
                        RogueliteCardId.DRAFT_MAGNET));
    }

    @Test
    public void doesNotAddASecondLayerWithoutAnAmplifier() {
        assertNull(RogueliteAbilityEffectLayers.underlayFor(
                RogueliteCardId.DRAFT_MAGNET,
                RogueliteCardId.NITRO_PULSE,
                RogueliteCardId.DRAFT_MAGNET));
    }

    @Test
    public void keepsAnActivePowerupVisibleDuringXpTheftRevenge() {
        assertEquals(
                RogueliteCardId.NITRO_PULSE,
                RogueliteAbilityEffectLayers.underlayFor(
                        RogueliteCardId.TELEMETRY_THEFT,
                        RogueliteCardId.NITRO_PULSE,
                        RogueliteCardId.TELEMETRY_THEFT));
    }

    @Test
    public void ignoresRevengeCardsWithDedicatedNonCenteredVisuals() {
        assertNull(RogueliteAbilityEffectLayers.underlayFor(
                RogueliteCardId.GRUDGE_SPARK,
                RogueliteCardId.GRUDGE_SPARK,
                RogueliteCardId.TAR_TETHER));
        assertNull(RogueliteAbilityEffectLayers.underlayFor(
                RogueliteCardId.GRUDGE_SPARK,
                RogueliteCardId.GRUDGE_SPARK,
                RogueliteCardId.EMP_SNARE));
        assertNull(RogueliteAbilityEffectLayers.underlayFor(
                RogueliteCardId.GRUDGE_SPARK,
                RogueliteCardId.GRUDGE_SPARK,
                RogueliteCardId.CROWN_ENGINE));
    }
}
