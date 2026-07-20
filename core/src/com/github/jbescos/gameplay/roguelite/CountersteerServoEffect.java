package com.github.jbescos.gameplay.roguelite;

final class CountersteerServoEffect extends RogueliteUpgradeEffect {
    private final boolean synergy;
    private float exitGripTimer;
    private boolean sliding;

    CountersteerServoEffect(int level, boolean synergy) {
        super(level);
        this.synergy = synergy;
    }

    @Override
    void update(float delta, float timerDelta, RogueliteDrivingFrame frame) {
        exitGripTimer = Math.max(0f, exitGripTimer - timerDelta);
        if (level < 2) {
            return;
        }
        if (frame.onRoad
                && frame.speedRatio >= RogueliteEffectTuning.DRIFT_MIN_SPEED_RATIO
                && frame.slip >= RogueliteEffectTuning.DRIFT_START_SLIP) {
            sliding = true;
        } else if (sliding
                && frame.onRoad
                && frame.slip <= RogueliteEffectTuning.DRIFT_END_SLIP) {
            sliding = false;
            exitGripTimer = Math.max(exitGripTimer, 0.8f);
        } else if (!frame.onRoad) {
            sliding = false;
            exitGripTimer = 0f;
        }
    }

    @Override
    float gripBonus(float slip) {
        float bonus = 0f;
        if (slip >= RogueliteEffectTuning.DRIFT_END_SLIP) {
            bonus += RogueliteEffectMath.levelValue(level, 0.08f, 0.13f, 0.18f);
            if (synergy) {
                bonus += 0.03f;
            }
        }
        if (exitGripTimer > 0f) {
            bonus += 0.08f;
        }
        return bonus;
    }

    @Override
    float steeringBonus(float slip) {
        if (slip < RogueliteEffectTuning.DRIFT_END_SLIP) {
            return 0f;
        }
        return RogueliteEffectMath.levelValue(level, 0.08f, 0.13f, 0.18f)
                + (synergy ? 0.03f : 0f);
    }
}
