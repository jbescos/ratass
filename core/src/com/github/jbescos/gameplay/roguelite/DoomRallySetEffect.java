package com.github.jbescos.gameplay.roguelite;

/** Marker effect for Doom Rally's overtake-triggered borrowed Powerup. */
final class DoomRallySetEffect extends RogueliteUpgradeEffect {
    DoomRallySetEffect() {
        // Retain the former bonus card ID so saves and strategy observations stay stable.
        super(RogueliteCardId.FATES_REVENGE);
    }
}
