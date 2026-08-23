package com.github.jbescos.gameplay.roguelite;

/** Multiplies capped lap XP only when it is banked into global XP. */
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
            return 1.25f;
        }
        if (cardId == RogueliteCardId.LAP_BOOSTER) {
            return 1.50f;
        }
        if (cardId == RogueliteCardId.LAP_DOUBLER) {
            return 2f;
        }
        return 1f;
    }
}
