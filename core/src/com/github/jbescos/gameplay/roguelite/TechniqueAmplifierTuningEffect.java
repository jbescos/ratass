package com.github.jbescos.gameplay.roguelite;

/** Tuning hardware that strengthens the equipped Technique card. */
final class TechniqueAmplifierTuningEffect extends RogueliteUpgradeEffect {
    private final float multiplier;

    TechniqueAmplifierTuningEffect(RogueliteCardId cardId) {
        super(cardId);
        switch (cardId) {
            case TECHNIQUE_COUPLER:
                multiplier = 1.25f;
                break;
            case TECHNIQUE_MATRIX:
                multiplier = 1.50f;
                break;
            case TECHNIQUE_SINGULARITY:
                multiplier = 2f;
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported Technique amplifier Tuning: " + cardId);
        }
    }

    @Override
    boolean isActive() {
        return true;
    }

    @Override
    int activeDisplayPriority() {
        return 1;
    }

    @Override
    float techniqueEffectMultiplier() {
        return multiplier;
    }
}
