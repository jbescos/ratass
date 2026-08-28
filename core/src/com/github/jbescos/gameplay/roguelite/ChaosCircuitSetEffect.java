package com.github.jbescos.gameplay.roguelite;

/** Keeps the equipped Technique active while the Chaos Circuit set is complete. */
final class ChaosCircuitSetEffect extends RogueliteUpgradeEffect {
    ChaosCircuitSetEffect() {
        // Retain the former bonus card ID so saves and strategy observations stay stable.
        super(RogueliteCardId.DRIFT_MASTER);
    }
}
