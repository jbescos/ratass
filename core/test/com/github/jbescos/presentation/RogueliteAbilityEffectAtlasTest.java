package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.github.jbescos.gameplay.roguelite.RogueliteAbilityVisualStyle;
import com.github.jbescos.gameplay.roguelite.RogueliteCardCatalog;
import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import org.junit.Test;

public class RogueliteAbilityEffectAtlasTest {
    @Test
    public void mapsEveryCenteredAbilityStyleToOneUniqueCell() {
        boolean[] used = new boolean[RogueliteAbilityEffectAtlas.COLUMNS];
        for (RogueliteAbilityVisualStyle style : RogueliteAbilityVisualStyle.values()) {
            int index = RogueliteAbilityEffectAtlas.indexFor(style);
            if (style == RogueliteAbilityVisualStyle.ICON_ONLY) {
                assertEquals(-1, index);
                assertEquals(0f, RogueliteAbilityEffectAtlas.sizeScale(style), 0f);
                continue;
            }
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

    @Test
    public void nitroAndGripTiersUseDifferentArtwork() {
        assertEquals(0, RogueliteAbilityEffectAtlas.indexFor(
                RogueliteAbilityVisualStyle.NITRO_T1));
        assertEquals(10, RogueliteAbilityEffectAtlas.indexFor(
                RogueliteAbilityVisualStyle.NITRO_T2));
        assertEquals(11, RogueliteAbilityEffectAtlas.indexFor(
                RogueliteAbilityVisualStyle.NITRO_T3));
        assertEquals(1, RogueliteAbilityEffectAtlas.indexFor(
                RogueliteAbilityVisualStyle.GRIP_T1));
        assertEquals(12, RogueliteAbilityEffectAtlas.indexFor(
                RogueliteAbilityVisualStyle.GRIP_T2));
        assertEquals(13, RogueliteAbilityEffectAtlas.indexFor(
                RogueliteAbilityVisualStyle.GRIP_T3));
        assertEquals(14, RogueliteAbilityEffectAtlas.indexFor(
                RogueliteAbilityVisualStyle.TIME_T1));
        assertEquals(15, RogueliteAbilityEffectAtlas.indexFor(
                RogueliteAbilityVisualStyle.TIME_T2));
        assertEquals(16, RogueliteAbilityEffectAtlas.indexFor(
                RogueliteAbilityVisualStyle.TIME_T3));
    }

    @Test
    public void nitroVisualScaleEscalatesClearlyByTier() {
        float tierOne = RogueliteAbilityEffectAtlas.sizeScale(
                RogueliteAbilityVisualStyle.NITRO_T1);
        float tierTwo = RogueliteAbilityEffectAtlas.sizeScale(
                RogueliteAbilityVisualStyle.NITRO_T2);
        float tierThree = RogueliteAbilityEffectAtlas.sizeScale(
                RogueliteAbilityVisualStyle.NITRO_T3);

        assertTrue(tierTwo >= tierOne + 0.40f);
        assertTrue(tierThree >= tierTwo + 0.40f);
    }

    @Test
    public void powerupFamiliesSelectTheirTierVisuals() {
        assertEquals(
                RogueliteAbilityVisualStyle.NITRO_T1,
                RogueliteCardCatalog.get(RogueliteCardId.NITRO_PULSE)
                        .getAbilityVisualStyle());
        assertEquals(
                RogueliteAbilityVisualStyle.NITRO_T2,
                RogueliteCardCatalog.get(RogueliteCardId.ROCKET_EXHAUST)
                        .getAbilityVisualStyle());
        assertEquals(
                RogueliteAbilityVisualStyle.NITRO_T3,
                RogueliteCardCatalog.get(RogueliteCardId.HYPERDRIVE)
                        .getAbilityVisualStyle());
        assertEquals(
                RogueliteAbilityVisualStyle.GRIP_T1,
                RogueliteCardCatalog.get(RogueliteCardId.GRIP_FAN)
                        .getAbilityVisualStyle());
        assertEquals(
                RogueliteAbilityVisualStyle.GRIP_T2,
                RogueliteCardCatalog.get(RogueliteCardId.PHASE_SHIELD)
                        .getAbilityVisualStyle());
        assertEquals(
                RogueliteAbilityVisualStyle.GRIP_T3,
                RogueliteCardCatalog.get(RogueliteCardId.GRAVITY_WELL)
                        .getAbilityVisualStyle());
        assertEquals(
                RogueliteAbilityVisualStyle.TIME_T1,
                RogueliteCardCatalog.get(RogueliteCardId.TIME_RIPPLE)
                        .getAbilityVisualStyle());
        assertEquals(
                RogueliteAbilityVisualStyle.TIME_T2,
                RogueliteCardCatalog.get(RogueliteCardId.CHRONO_SHIFT)
                        .getAbilityVisualStyle());
        assertEquals(
                RogueliteAbilityVisualStyle.TIME_T3,
                RogueliteCardCatalog.get(RogueliteCardId.TEMPORAL_DOMINION)
                        .getAbilityVisualStyle());
    }

    @Test
    public void draftFieldUsesItsGameplayDiameter() {
        assertEquals(
                12f,
                RogueliteAbilityEffectAtlas.worldSize(
                        RogueliteAbilityVisualStyle.DRAFT,
                        3f,
                        6f),
                0f);
        assertEquals(
                3f,
                RogueliteAbilityEffectAtlas.worldSize(
                        RogueliteAbilityVisualStyle.NITRO_T1,
                        3f,
                        6f),
                0f);
        assertEquals(
                3f,
                RogueliteAbilityEffectAtlas.worldSize(
                        RogueliteAbilityVisualStyle.DRAFT,
                        3f,
                        Float.NaN),
                0f);
    }
}
