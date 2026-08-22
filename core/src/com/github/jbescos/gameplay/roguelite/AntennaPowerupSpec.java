package com.github.jbescos.gameplay.roguelite;

/** Rules for passive antenna cards that link car upgrades across the field. */
public final class AntennaPowerupSpec {
    private AntennaPowerupSpec() {
    }

    public static boolean isAntennaCard(RogueliteCardId cardId) {
        return cardId == RogueliteCardId.TUNE_LINK
                || cardId == RogueliteCardId.TECHNIQUE_LINK
                || cardId == RogueliteCardId.GRID_LINK;
    }

    public static boolean sharesTuning(RogueliteCardId cardId) {
        return cardId == RogueliteCardId.TUNE_LINK
                || cardId == RogueliteCardId.GRID_LINK;
    }

    public static boolean sharesTechnique(RogueliteCardId cardId) {
        return cardId == RogueliteCardId.TECHNIQUE_LINK
                || cardId == RogueliteCardId.GRID_LINK;
    }
}
