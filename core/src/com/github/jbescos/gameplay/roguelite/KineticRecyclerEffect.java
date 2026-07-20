package com.github.jbescos.gameplay.roguelite;

final class KineticRecyclerEffect extends RogueliteUpgradeEffect {
    private final boolean synergy;
    private float recoveryTimer;
    private float recoveryStrength;

    KineticRecyclerEffect(int level, boolean synergy) {
        super(level);
        this.synergy = synergy;
    }

    @Override
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        recoveryTimer = Math.max(0f, recoveryTimer - timerDelta);
        if (recoveryTimer == 0f) {
            recoveryStrength = 0f;
        }
    }

    @Override
    void onCollision(float impactStrength) {
        float impactFactor = RogueliteEffectMath.clamp(impactStrength / 18f, 0.35f, 1f);
        float strength =
                RogueliteEffectMath.levelValue(level, 0.06f, 0.10f, 0.14f)
                        * impactFactor;
        recoveryStrength = Math.max(
                recoveryStrength,
                strength * (synergy ? 1.25f : 1f));
        recoveryTimer = Math.max(recoveryTimer, 1.2f);
    }

    @Override
    float accelerationBonus() {
        return recoveryTimer > 0f ? recoveryStrength : 0f;
    }
}
