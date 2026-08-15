package com.github.jbescos.gameplay.roguelite;

final class BestDriverOverrideEffect extends RogueliteUpgradeEffect {
    BestDriverOverrideEffect(RogueliteCardId cardId) {
        super(cardId);
        if (cardId != RogueliteCardId.PRIORITY_HOTLINE) {
            throw new IllegalArgumentException("Unsupported best-driver override: " + cardId);
        }
    }

    @Override
    boolean isActive() {
        return true;
    }

    @Override
    float readiness() {
        return 1f;
    }

    @Override
    int activeDisplayPriority() {
        return 3;
    }

    @Override
    boolean usesBestDriver() {
        return true;
    }
}
