package com.github.jbescos.gameplay.roguelite;

final class BestDriverOverrideEffect extends RepeatingPowerupEffect {
    BestDriverOverrideEffect(RogueliteCardId cardId) {
        super(cardId);
        if (cardId != RogueliteCardId.PRIORITY_HOTLINE) {
            throw new IllegalArgumentException("Unsupported best-driver override: " + cardId);
        }
    }

    @Override
    int activeDisplayPriority() {
        return 3;
    }

    @Override
    boolean usesBestDriver() {
        return isActive();
    }
}
