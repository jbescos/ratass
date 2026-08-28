package com.github.jbescos.gameplay.roguelite;

/** Permanent debuff immunity granted while the Iron Giant set remains complete. */
final class IronGiantSetEffect extends RogueliteUpgradeEffect {
    IronGiantSetEffect() {
        // Retain the former bonus card ID so set display and saved state remain stable.
        super(RogueliteCardId.TORQUE_VECTORING);
    }

    @Override
    boolean blocksDebuffs() {
        return true;
    }
}
