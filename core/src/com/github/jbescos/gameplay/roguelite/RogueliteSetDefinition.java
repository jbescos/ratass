package com.github.jbescos.gameplay.roguelite;

/** One immutable set recipe and the existing card effect granted by completing it. */
public final class RogueliteSetDefinition {
    private final RogueliteSetId id;
    private final String displayName;
    private final int tier;
    private final RogueliteCardId tuningCardId;
    private final RogueliteCardId techniqueCardId;
    private final RogueliteCardId powerupCardId;
    private final RogueliteCardId revengeCardId;
    private final RogueliteCardId bonusCardId;
    private final String bonusEffectText;
    private final boolean setScopedBonusEffect;
    private final int iconIndex;

    RogueliteSetDefinition(
            RogueliteSetId id,
            String displayName,
            int tier,
            RogueliteCardId tuningCardId,
            RogueliteCardId techniqueCardId,
            RogueliteCardId powerupCardId,
            RogueliteCardId revengeCardId,
            RogueliteCardId bonusCardId,
            String bonusEffectText,
            boolean setScopedBonusEffect,
            int iconIndex) {
        this.id = id;
        this.displayName = displayName;
        this.tier = tier;
        this.tuningCardId = tuningCardId;
        this.techniqueCardId = techniqueCardId;
        this.powerupCardId = powerupCardId;
        this.revengeCardId = revengeCardId;
        this.bonusCardId = bonusCardId;
        this.bonusEffectText = bonusEffectText;
        this.setScopedBonusEffect = setScopedBonusEffect;
        this.iconIndex = iconIndex;
    }

    public RogueliteSetId getId() {
        return id;
    }

    public String getDisplayName() {
        return displayName;
    }

    public int getTier() {
        return tier;
    }

    public RogueliteCardId getTuningCardId() {
        return tuningCardId;
    }

    public RogueliteCardId getTechniqueCardId() {
        return techniqueCardId;
    }

    public RogueliteCardId getPowerupCardId() {
        return powerupCardId;
    }

    public RogueliteCardId getRevengeCardId() {
        return revengeCardId;
    }

    public RogueliteCardId getBonusCardId() {
        return bonusCardId;
    }

    public String getBonusEffectText() {
        return bonusEffectText;
    }

    public boolean usesSetScopedBonusEffect() {
        return setScopedBonusEffect;
    }

    public int getIconIndex() {
        return iconIndex;
    }

    public RogueliteCardId getRequiredCard(RogueliteSlotType slotType) {
        if (slotType == RogueliteSlotType.TUNING) {
            return tuningCardId;
        }
        if (slotType == RogueliteSlotType.TECHNIQUE) {
            return techniqueCardId;
        }
        if (slotType == RogueliteSlotType.POWERUP) {
            return powerupCardId;
        }
        if (slotType == RogueliteSlotType.REVENGE) {
            return revengeCardId;
        }
        return null;
    }

    public boolean contains(RogueliteCardId cardId) {
        return cardId != null
                && (cardId == tuningCardId
                        || cardId == techniqueCardId
                        || cardId == powerupCardId
                        || cardId == revengeCardId);
    }

    public boolean isCompletedBy(RogueliteLoadout loadout) {
        return loadout != null
                && loadout.get(RogueliteSlotType.TUNING) == tuningCardId
                && loadout.get(RogueliteSlotType.TECHNIQUE) == techniqueCardId
                && loadout.get(RogueliteSlotType.POWERUP) == powerupCardId
                && loadout.get(RogueliteSlotType.REVENGE) == revengeCardId;
    }

    public int matchingCardCount(RogueliteLoadout loadout) {
        if (loadout == null) {
            return 0;
        }
        int count = 0;
        for (RogueliteSlotType slotType : RogueliteSlotType.modificationSlots()) {
            count += loadout.get(slotType) == getRequiredCard(slotType) ? 1 : 0;
        }
        return count;
    }
}
