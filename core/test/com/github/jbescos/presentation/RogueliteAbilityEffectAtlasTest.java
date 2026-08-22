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
        assertEquals(17, RogueliteAbilityEffectAtlas.indexFor(
                RogueliteAbilityVisualStyle.HOTLINE_T1));
        assertEquals(18, RogueliteAbilityEffectAtlas.indexFor(
                RogueliteAbilityVisualStyle.HOTLINE_T2));
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
    public void nitroArtworkIsAnchoredBehindTheRearExhaust() {
        float carHeight = 1.58f;
        float effectSize = 3f;

        for (RogueliteAbilityVisualStyle style : new RogueliteAbilityVisualStyle[] {
                RogueliteAbilityVisualStyle.NITRO_T1,
                RogueliteAbilityVisualStyle.NITRO_T2,
                RogueliteAbilityVisualStyle.NITRO_T3
        }) {
            assertTrue(RogueliteAbilityEffectAtlas.usesRearExhaustAnchor(style));
            assertTrue(RogueliteAbilityEffectAtlas.localCenterOffsetY(
                    style,
                    carHeight,
                    effectSize) < -carHeight * 0.5f);
        }
        assertEquals(
                0f,
                RogueliteAbilityEffectAtlas.localCenterOffsetY(
                        RogueliteAbilityVisualStyle.GRIP_T1,
                        carHeight,
                        effectSize),
                0.0001f);
    }

    @Test
    public void timeArtworkRotatesClockwiseMoreClearlyAtHigherTiers() {
        float elapsedSeconds = 0.5f;
        float tierOne = RogueliteAbilityEffectAtlas.timeRotationDegrees(
                RogueliteAbilityVisualStyle.TIME_T1,
                elapsedSeconds);
        float tierTwo = RogueliteAbilityEffectAtlas.timeRotationDegrees(
                RogueliteAbilityVisualStyle.TIME_T2,
                elapsedSeconds);
        float tierThree = RogueliteAbilityEffectAtlas.timeRotationDegrees(
                RogueliteAbilityVisualStyle.TIME_T3,
                elapsedSeconds);

        assertTrue(tierOne > 0f);
        assertTrue(tierTwo > tierOne);
        assertTrue(tierThree > tierTwo);
        assertEquals(
                0f,
                RogueliteAbilityEffectAtlas.timeRotationDegrees(
                        RogueliteAbilityVisualStyle.NITRO_T1,
                        elapsedSeconds),
                0f);
    }

    @Test
    public void gripSpiralsRotateTogetherWithStrongerMotionAtHigherTiers() {
        float elapsedSeconds = 0.5f;
        float tierOne = RogueliteAbilityEffectAtlas.gripRotationDegrees(
                RogueliteAbilityVisualStyle.GRIP_T1,
                elapsedSeconds);
        float tierTwo = RogueliteAbilityEffectAtlas.gripRotationDegrees(
                RogueliteAbilityVisualStyle.GRIP_T2,
                elapsedSeconds);
        float tierThree = RogueliteAbilityEffectAtlas.gripRotationDegrees(
                RogueliteAbilityVisualStyle.GRIP_T3,
                elapsedSeconds);

        assertTrue(tierOne > 0f);
        assertTrue(tierTwo > tierOne);
        assertTrue(tierThree > tierTwo);
        assertEquals(
                0f,
                RogueliteAbilityEffectAtlas.gripRotationDegrees(
                        RogueliteAbilityVisualStyle.TIME_T1,
                        elapsedSeconds),
                0f);
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
        assertEquals(
                RogueliteAbilityVisualStyle.HOTLINE_T1,
                RogueliteCardCatalog.get(RogueliteCardId.ACE_HOTLINE)
                        .getAbilityVisualStyle());
        assertEquals(
                RogueliteAbilityVisualStyle.HOTLINE_T2,
                RogueliteCardCatalog.get(RogueliteCardId.PRIORITY_HOTLINE)
                        .getAbilityVisualStyle());
    }

    @Test
    public void antennaCardsUseDistinctIllustratedAtlasCells() {
        RogueliteCardId[] cards = {
                RogueliteCardId.TUNE_LINK,
                RogueliteCardId.TECHNIQUE_LINK,
                RogueliteCardId.GRID_LINK
        };
        RogueliteAbilityVisualStyle[] styles = {
                RogueliteAbilityVisualStyle.ANTENNA_T1,
                RogueliteAbilityVisualStyle.ANTENNA_T2,
                RogueliteAbilityVisualStyle.ANTENNA_T3
        };

        for (int i = 0; i < cards.length; i++) {
            assertEquals(styles[i], RogueliteCardCatalog.get(cards[i]).getAbilityVisualStyle());
            assertEquals(19 + i, RogueliteAbilityEffectAtlas.indexFor(styles[i]));
        }
        assertTrue(RogueliteAbilityEffectAtlas.sizeScale(styles[1])
                > RogueliteAbilityEffectAtlas.sizeScale(styles[0]));
        assertTrue(RogueliteAbilityEffectAtlas.sizeScale(styles[2])
                > RogueliteAbilityEffectAtlas.sizeScale(styles[1]));
    }

    @Test
    public void tierFourUnlockUsesAKeyInsteadOfAnAntennaIdentity() {
        assertEquals(
                "Apex Key",
                RogueliteCardCatalog.get(RogueliteCardId.TIER_FOUR_SIGNAL).getTitle());
        assertEquals(
                126,
                RogueliteCardCatalog.get(RogueliteCardId.TIER_FOUR_SIGNAL)
                        .getArtworkIndex());
        assertEquals(
                22,
                RogueliteAbilityEffectAtlas.indexFor(
                        RogueliteAbilityVisualStyle.TIER_FOUR_SIGNAL));
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
