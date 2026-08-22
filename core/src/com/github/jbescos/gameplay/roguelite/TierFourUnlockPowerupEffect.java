package com.github.jbescos.gameplay.roguelite;

/** Displays the repeating finite signal for the persistent Tier 4 unlock card. */
final class TierFourUnlockPowerupEffect extends RepeatingPowerupEffect {
    TierFourUnlockPowerupEffect() {
        super(RogueliteCardId.TIER_FOUR_SIGNAL);
    }

    @Override
    int activeDisplayPriority() {
        return 2;
    }
}
