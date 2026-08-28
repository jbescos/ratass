package com.github.jbescos.gameplay.roguelite;

import java.util.Collections;

/** Resolves the loadout shown while a level-up reward is awaiting confirmation. */
public final class RogueliteRewardPreview {
    private RogueliteRewardPreview() {
    }

    public static DriverProfileMetadata resolveDriver(
            DriverProfileMetadata equippedDriver,
            RogueliteCardOffer pendingOffer) {
        return pendingOffer != null && pendingOffer.isDriver()
                ? pendingOffer.getDriver()
                : equippedDriver;
    }

    public static RogueliteCardId resolveCard(
            RogueliteLoadout loadout,
            RogueliteSlotType slotType,
            RogueliteCardOffer pendingOffer) {
        if (slotType == null || slotType.isDriver()) {
            return null;
        }
        if (pendingOffer != null
                && !pendingOffer.isDriver()
                && pendingOffer.getSlotType() == slotType) {
            return pendingOffer.getCard().getId();
        }
        return loadout == null ? null : loadout.get(slotType);
    }

    public static RogueliteSetDefinition resolveCompletedSet(
            RogueliteLoadout loadout,
            RogueliteCardOffer pendingOffer,
            Iterable<RogueliteSetId> enabledSetIds) {
        if (pendingOffer == null || pendingOffer.isDriver()) {
            return RogueliteSetCatalog.completedSet(loadout, enabledSetIds);
        }
        RogueliteCardId candidate = pendingOffer.getCard().getId();
        Iterable<RogueliteSetId> availableSetIds = enabledSetIds == null
                ? Collections.<RogueliteSetId>emptyList()
                : enabledSetIds;
        for (RogueliteSetId setId : availableSetIds) {
            RogueliteSetDefinition set = RogueliteSetCatalog.get(setId);
            if (set != null
                    && RogueliteSetCatalog.matchingCardCountAfter(
                                    loadout,
                                    candidate,
                                    set)
                            == RogueliteLoadout.MODIFICATION_SLOT_COUNT) {
                return set;
            }
        }
        return null;
    }
}
