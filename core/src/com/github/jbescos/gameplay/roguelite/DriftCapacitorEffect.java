package com.github.jbescos.gameplay.roguelite;

final class DriftCapacitorEffect extends RogueliteUpgradeEffect {
    private final boolean synergy;
    private float charge;
    private float boostTimer;
    private float boostStrength;
    private boolean drifting;

    DriftCapacitorEffect(int level, boolean synergy) {
        super(level);
        this.synergy = synergy;
    }

    @Override
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        boostTimer = Math.max(0f, boostTimer - timerDelta);
        if (boostTimer == 0f) {
            boostStrength = 0f;
        }
        boolean canCharge =
                frame.onRoad
                        && frame.speedRatio >= RogueliteEffectTuning.DRIFT_MIN_SPEED_RATIO
                        && frame.slip >= RogueliteEffectTuning.DRIFT_START_SLIP;
        if (canCharge) {
            drifting = true;
            charge =
                    Math.min(
                            RogueliteEffectTuning.DRIFT_MAX_CHARGE_SECONDS,
                            charge + delta * (synergy ? 1.25f : 1f));
            return;
        }
        if (drifting
                && (!frame.onRoad
                        || frame.slip <= RogueliteEffectTuning.DRIFT_END_SLIP)) {
            if (frame.onRoad && charge >= 0.25f) {
                float normalizedCharge =
                        RogueliteEffectMath.clamp(
                                charge / RogueliteEffectTuning.DRIFT_MAX_CHARGE_SECONDS,
                                0f,
                                1f);
                boostStrength =
                        normalizedCharge
                                * RogueliteEffectMath.levelValue(
                                        level, 0.06f, 0.10f, 0.14f);
                boostTimer =
                        Math.max(
                                boostTimer,
                                RogueliteEffectMath.lerp(
                                        0.55f, 1.60f, normalizedCharge));
            }
            drifting = false;
            charge = 0f;
        }
    }

    @Override
    float accelerationBonus() {
        return boostTimer > 0f ? boostStrength : 0f;
    }

    @Override
    float maxSpeedBonus() {
        return boostTimer > 0f ? boostStrength * 0.35f : 0f;
    }
}
