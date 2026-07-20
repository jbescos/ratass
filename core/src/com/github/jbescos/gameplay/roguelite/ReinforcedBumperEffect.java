package com.github.jbescos.gameplay.roguelite;

final class ReinforcedBumperEffect extends RogueliteUpgradeEffect {
    ReinforcedBumperEffect(int level) {
        super(level);
    }

    @Override
    float frontCollisionRecoilMultiplier() {
        return 1f - RogueliteEffectMath.levelValue(level, 0.15f, 0.25f, 0.35f);
    }

    @Override
    float frontCollisionPushMultiplier() {
        return 1f + RogueliteEffectMath.levelValue(level, 0.10f, 0.18f, 0.26f);
    }
}
