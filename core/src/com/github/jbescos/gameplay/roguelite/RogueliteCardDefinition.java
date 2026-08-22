package com.github.jbescos.gameplay.roguelite;

public final class RogueliteCardDefinition {
    public static final int ARTWORK_CAPACITY = 132;
    private final RogueliteCardId id;
    private final String title;
    private final String description;
    private final String effectText;
    private final int tier;
    private final RogueliteSlotType slotType;
    private final RogueliteAbilityVisualStyle abilityVisualStyle;
    private final int artworkIndex;

    RogueliteCardDefinition(
            RogueliteCardId id,
            String title,
            String description,
            String effectText,
            int tier,
            RogueliteSlotType slotType,
            RogueliteAbilityVisualStyle abilityVisualStyle,
            int artworkIndex) {
        if (effectText == null || effectText.length() == 0) {
            throw new IllegalArgumentException("A card requires an effect.");
        }
        if (tier < 1 || tier > RogueliteCardCatalog.MAX_CARD_TIER) {
            throw new IllegalArgumentException("Card tier is out of range.");
        }
        if (slotType == null || slotType.isDriver()) {
            throw new IllegalArgumentException("A modification card requires a modification slot.");
        }
        boolean activeAbility =
                slotType == RogueliteSlotType.POWERUP
                        || slotType == RogueliteSlotType.REVENGE;
        if (activeAbility != (abilityVisualStyle != null)) {
            throw new IllegalArgumentException(
                    "Only powerup and revenge cards require a visual style.");
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
        this.abilityVisualStyle = abilityVisualStyle;
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

    public RogueliteAbilityVisualStyle getAbilityVisualStyle() {
        return abilityVisualStyle;
    }

    public int getArtworkIndex() {
        return artworkIndex;
    }
}
