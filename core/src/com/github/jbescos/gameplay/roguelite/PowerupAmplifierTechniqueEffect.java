package com.github.jbescos.gameplay.roguelite;

/** Passive Technique that strengthens Powerups and accelerates their cooldowns. */
final class PowerupAmplifierTechniqueEffect extends RogueliteUpgradeEffect {
    private final float multiplier;

    PowerupAmplifierTechniqueEffect(RogueliteCardId cardId) {
        super(cardId);
        switch (cardId) {
            case POWERUP_LINK:
                multiplier = 1.25f;
                break;
            case POWERUP_MATRIX:
                multiplier = 1.50f;
                break;
            case POWERUP_NEXUS:
                multiplier = 2f;
                break;
            default:
                throw new IllegalArgumentException(
                        "Unsupported Powerup amplifier Technique: " + cardId);
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
    float powerupEffectMultiplier() {
        return multiplier;
    }

    @Override
    float powerupCooldownRateMultiplier() {
        return multiplier;
    }
}
