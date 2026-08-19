package com.github.jbescos.gameplay.roguelite.strategy;

import com.github.jbescos.gameplay.roguelite.RogueliteCardOffer;

/** Selects one offered card, or returns {@code null} to skip the reward. */
public interface CardStrategy {
    String getProfileId();

    default String getDisplayName() {
        return getProfileId();
    }

    RogueliteCardOffer choose(
            CardStrategyDecision decision,
            CardStrategyRandom random);
}
