package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import org.junit.Test;

public class RogueliteAbilityEffectLayersTest {
    @Test
    public void keepsOutwardFieldVisibleBelowEachRevengeAmplifierTier() {
        assertEquals(
                RogueliteCardId.DRAFT_MAGNET,
                RogueliteAbilityEffectLayers.underlayFor(
                        RogueliteCardId.GRUDGE_SPARK,
                        RogueliteCardId.GRUDGE_SPARK,
                        RogueliteCardId.DRAFT_MAGNET));
        assertEquals(
                RogueliteCardId.DRAFT_MAGNET,
                RogueliteAbilityEffectLayers.underlayFor(
                        RogueliteCardId.VENGEANCE_CORE,
                        RogueliteCardId.VENGEANCE_CORE,
                        RogueliteCardId.DRAFT_MAGNET));
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
