package com.github.jbescos.gameplay.roguelite;

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
}
