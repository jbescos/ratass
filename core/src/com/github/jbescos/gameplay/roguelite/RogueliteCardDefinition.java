package com.github.jbescos.gameplay.roguelite;

public final class RogueliteCardDefinition {
    private final RogueliteCardId id;
    private final String title;
    private final String description;
    private final String[] effectTexts;
    private final RogueliteCardId synergyCardId;

    RogueliteCardDefinition(
            RogueliteCardId id,
            String title,
            String description,
            String[] effectTexts,
            RogueliteCardId synergyCardId) {
        if (effectTexts == null || effectTexts.length == 0) {
            throw new IllegalArgumentException("A card requires at least one level");
        }
        this.id = id;
        this.title = title;
        this.description = description;
        this.effectTexts = effectTexts.clone();
        this.synergyCardId = synergyCardId;
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

    public int getMaxLevel() {
        return effectTexts.length - 1;
    }

    public String getEffectText(int level) {
        int clampedLevel = Math.max(0, Math.min(level, getMaxLevel()));
        return effectTexts[clampedLevel];
    }

    public RogueliteCardId getSynergyCardId() {
        return synergyCardId;
    }
}
