package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;

import java.util.Collections;
import org.junit.Test;

public class RogueliteRewardPreviewTest {
    @Test
    public void pendingModificationAppearsOnlyInItsMatchingSlot() {
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        loadout.equip(RogueliteCardId.CLUB_TUNE);
        loadout.equip(RogueliteCardId.CORNER_FOCUS);
        RogueliteCardOffer preview =
                RogueliteCardOffer.modification(
                        RogueliteCardCatalog.get(RogueliteCardId.RACE_TUNE));

        assertEquals(
                RogueliteCardId.RACE_TUNE,
                RogueliteRewardPreview.resolveCard(
                        loadout,
                        RogueliteSlotType.TUNING,
                        preview));
        assertEquals(
                RogueliteCardId.CORNER_FOCUS,
                RogueliteRewardPreview.resolveCard(
                        loadout,
                        RogueliteSlotType.TECHNIQUE,
                        preview));
    }

    @Test
    public void cancellingPreviewRestoresEquippedCard() {
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        loadout.equip(RogueliteCardId.CLUB_TUNE);

        assertEquals(
                RogueliteCardId.CLUB_TUNE,
                RogueliteRewardPreview.resolveCard(
                        loadout,
                        RogueliteSlotType.TUNING,
                        null));
    }

    @Test
    public void pendingDriverAppearsWithoutChangingModificationSlots() {
        DriverProfileMetadata equipped = driver("profile00");
        DriverProfileMetadata offered = driver("profile01");
        RogueliteCardOffer preview = RogueliteCardOffer.driver(offered, 1);

        assertSame(
                offered,
                RogueliteRewardPreview.resolveDriver(equipped, preview));
        assertSame(
                equipped,
                RogueliteRewardPreview.resolveDriver(equipped, null));
        assertNull(
                RogueliteRewardPreview.resolveCard(
                        new RogueliteLoadout("profile00"),
                RogueliteSlotType.DRIVER,
                preview));
    }

    @Test
    public void pendingFinalComponentPreviewsSetUntilOfferIsCancelled() {
        RogueliteSetDefinition set =
                RogueliteSetCatalog.get(RogueliteSetId.CHAOS_CIRCUIT);
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        loadout.equip(set.getTuningCardId());
        loadout.equip(set.getTechniqueCardId());
        loadout.equip(set.getPowerupCardId());
        RogueliteCardOffer preview = RogueliteCardOffer.modification(
                RogueliteCardCatalog.get(set.getRevengeCardId()));

        assertSame(
                set,
                RogueliteRewardPreview.resolveCompletedSet(
                        loadout,
                        preview,
                        Collections.singletonList(set.getId())));
        assertNull(
                RogueliteRewardPreview.resolveCompletedSet(
                        loadout,
                        null,
                        Collections.singletonList(set.getId())));
    }

    @Test
    public void previewReplacingASetComponentHidesCompletedSet() {
        RogueliteSetDefinition set =
                RogueliteSetCatalog.get(RogueliteSetId.CHAOS_CIRCUIT);
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        loadout.equip(set.getTuningCardId());
        loadout.equip(set.getTechniqueCardId());
        loadout.equip(set.getPowerupCardId());
        loadout.equip(set.getRevengeCardId());
        RogueliteCardOffer preview = RogueliteCardOffer.modification(
                RogueliteCardCatalog.get(RogueliteCardId.HUNTER_STORM));

        assertNull(
                RogueliteRewardPreview.resolveCompletedSet(
                        loadout,
                        preview,
                        Collections.singletonList(set.getId())));
        assertSame(
                set,
                RogueliteRewardPreview.resolveCompletedSet(
                        loadout,
                        null,
                        Collections.singletonList(set.getId())));
    }

    private static DriverProfileMetadata driver(String profileId) {
        return new DriverProfileMetadata(
                profileId,
                "sha",
                "benchmark",
                50f,
                50f,
                50f,
                1f,
                40f,
                42f,
                0f);
    }
}
