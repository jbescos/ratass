package com.github.jbescos.gameplay.roguelite;

public final class RogueliteCardDefinition {
    public static final int ARTWORK_CAPACITY = 36;
    private final RogueliteCardId id;
    private final String title;
    private final String description;
    private final String effectText;
    private final int tier;
    private final RogueliteSlotType slotType;
    private final RogueliteGadgetVisualStyle gadgetVisualStyle;
    private final int artworkIndex;

    RogueliteCardDefinition(
            RogueliteCardId id,
            String title,
            String description,
            String effectText,
            int tier,
            RogueliteSlotType slotType,
            RogueliteGadgetVisualStyle gadgetVisualStyle,
            int artworkIndex) {
        if (effectText == null || effectText.length() == 0) {
            throw new IllegalArgumentException("A card requires an effect.");
        }
        if (tier < 1 || tier > DriverProfileCatalog.MAX_TIER) {
            throw new IllegalArgumentException("Card tier is out of range.");
        }
        if (slotType == null || slotType.isDriver()) {
            throw new IllegalArgumentException("A modification card requires a modification slot.");
        }
        if ((slotType == RogueliteSlotType.GADGET) != (gadgetVisualStyle != null)) {
            throw new IllegalArgumentException("Only gadget cards require a visual style.");
        }
        if (artworkIndex < 0 || artworkIndex >= ARTWORK_CAPACITY) {
            throw new IllegalArgumentException("Card artwork index is out of range.");
        }
        this.id = id;
        this.title = title;
        this.description = description;
        this.effectText = effectText;
        this.tier = tier;
        this.slotType = slotType;
        this.gadgetVisualStyle = gadgetVisualStyle;
        this.artworkIndex = artworkIndex;
    }

    public RogueliteCardId getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getEffectText() {
        return effectText;
    }

    public int getTier() {
        return tier;
    }

    public RogueliteSlotType getSlotType() {
        return slotType;
    }

    public RogueliteGadgetVisualStyle getGadgetVisualStyle() {
        return gadgetVisualStyle;
    }

    public int getArtworkIndex() {
        return artworkIndex;
    }
}
