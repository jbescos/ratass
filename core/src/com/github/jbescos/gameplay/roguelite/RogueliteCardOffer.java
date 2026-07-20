package com.github.jbescos.gameplay.roguelite;

public final class RogueliteCardOffer {
    private final RogueliteCardDefinition card;
    private final int targetLevel;

    RogueliteCardOffer(RogueliteCardDefinition card, int targetLevel) {
        this.card = card;
        this.targetLevel = targetLevel;
    }

    public RogueliteCardDefinition getCard() {
        return card;
    }

    public int getTargetLevel() {
        return targetLevel;
    }

    public boolean isUpgrade() {
        return targetLevel > 0;
    }
}
