package com.github.jbescos.gameplay.roguelite;

/** Rules for passive antenna cards that link car upgrades across the field. */
public final class AntennaPowerupSpec {
    private AntennaPowerupSpec() {
    }

    public static boolean isAntennaCard(RogueliteCardId cardId) {
        return cardId == RogueliteCardId.TUNE_LINK
                || cardId == RogueliteCardId.TECHNIQUE_LINK;
    }

    public static boolean sharesTuning(RogueliteCardId cardId) {
        return cardId == RogueliteCardId.TUNE_LINK
                || cardId == RogueliteCardId.TECHNIQUE_LINK;
    }

    public static boolean sharesTechnique(RogueliteCardId cardId) {
        return false;
    }

    public static int sharedTuningAttributeCount(RogueliteCardId cardId) {
        if (cardId == RogueliteCardId.TUNE_LINK) {
            return 1;
        }
        if (cardId == RogueliteCardId.TECHNIQUE_LINK) {
            return 2;
        }
        return 0;
    }
}
