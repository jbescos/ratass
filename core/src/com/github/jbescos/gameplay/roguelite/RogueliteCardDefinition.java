package com.github.jbescos.gameplay.roguelite;

public final class RogueliteCardDefinition {
    private final RogueliteCardId id;
    private final String title;
    private final String description;
    private final String effectText;
    private final RogueliteCardId synergyCardId;
    private final int tier;

    RogueliteCardDefinition(
            RogueliteCardId id,
            String title,
            String description,
            String effectText,
            RogueliteCardId synergyCardId,
            int tier) {
        if (effectText == null || effectText.length() == 0) {
            throw new IllegalArgumentException("A card requires an effect.");
        }
        if (tier < 1 || tier > DriverProfileCatalog.MAX_TIER) {
            throw new IllegalArgumentException("Card tier is out of range.");
        }
        this.id = id;
        this.title = title;
        this.description = description;
        this.effectText = effectText;
        this.synergyCardId = synergyCardId;
        this.tier = tier;
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

    public RogueliteCardId getSynergyCardId() {
        return synergyCardId;
    }

    public int getTier() {
        return tier;
    }
}
