package com.github.jbescos.gameplay.roguelite;

public final class RogueliteCardOffer {
    public enum Type {
        DRIVER,
        MODIFICATION
    }

    private final Type type;
    private final RogueliteCardDefinition card;
    private final DriverProfileMetadata driver;
    private final int tier;

    private RogueliteCardOffer(
            Type type,
            RogueliteCardDefinition card,
            DriverProfileMetadata driver,
            int tier) {
        this.type = type;
        this.card = card;
        this.driver = driver;
        this.tier = tier;
    }

    static RogueliteCardOffer modification(RogueliteCardDefinition card) {
        return new RogueliteCardOffer(
                Type.MODIFICATION,
                card,
                null,
                card.getTier());
    }

    static RogueliteCardOffer driver(
            DriverProfileMetadata driver,
            int tier) {
        return new RogueliteCardOffer(Type.DRIVER, null, driver, tier);
    }

    public Type getType() {
        return type;
    }

    public boolean isDriver() {
        return type == Type.DRIVER;
    }

    public RogueliteCardDefinition getCard() {
        return card;
    }

    public DriverProfileMetadata getDriver() {
        return driver;
    }

    public int getTier() {
        return tier;
    }

    public RogueliteSlotType getSlotType() {
        return isDriver()
                ? RogueliteSlotType.DRIVER
                : card.getSlotType();
    }

    public String getOfferId() {
        return isDriver()
                ? "driver:" + driver.getProfileId()
                : "card:" + card.getId().name();
    }
}
