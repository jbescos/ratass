package com.github.jbescos.gameplay.roguelite;

/** Repeating antenna effect; shared statistics are resolved at race level. */
final class AntennaPowerupEffect extends RepeatingPowerupEffect {
    AntennaPowerupEffect(RogueliteCardId cardId) {
        super(cardId);
        if (!AntennaPowerupSpec.isAntennaCard(cardId)) {
            throw new IllegalArgumentException("Unsupported antenna card: " + cardId);
        }
    }

    @Override
    int activeDisplayPriority() {
        return 1;
    }
}
