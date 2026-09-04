package com.github.jbescos.gameplay.roguelite;

/** Multiplies both pending lap XP capacity and its transfer into global XP. */
final class LapExperienceTechniqueEffect extends RogueliteUpgradeEffect {
    private final float bankMultiplier;

    LapExperienceTechniqueEffect(RogueliteCardId cardId) {
        super(cardId);
        bankMultiplier = multiplierFor(cardId);
    }

    @Override
    float lapExperienceBankMultiplier() {
        return bankMultiplier;
    }

    static boolean isLapExperienceCard(RogueliteCardId cardId) {
        return cardId == RogueliteCardId.LAP_DIVIDEND
                || cardId == RogueliteCardId.LAP_BOOSTER
                || cardId == RogueliteCardId.LAP_DOUBLER;
    }

    static float multiplierFor(RogueliteCardId cardId) {
        if (cardId == RogueliteCardId.LAP_DIVIDEND) {
            return 2f;
        }
        if (cardId == RogueliteCardId.LAP_BOOSTER) {
            return 3f;
        }
        if (cardId == RogueliteCardId.LAP_DOUBLER) {
            return 4f;
        }
        return 1f;
    }
}
