package com.github.jbescos.gameplay.roguelite;

/** Marker effect that makes every Web Barrage hook apply a native random debuff. */
final class VenomWebSetEffect extends RogueliteUpgradeEffect {
    VenomWebSetEffect() {
        // Retain the former bonus card ID so saved set state remains compatible.
        super(RogueliteCardId.NEMESIS_ENGINE);
    }
}
