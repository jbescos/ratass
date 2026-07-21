package com.github.jbescos.gameplay.roguelite;

final class DraftReceiverEffect extends RogueliteUpgradeEffect {
    private final boolean synergy;

    DraftReceiverEffect(int level, boolean synergy) {
        super(RogueliteCardId.DRAFT_RECEIVER, level);
        this.synergy = synergy;
    }

    @Override
    boolean isActive() {
        return latestFrame != null && latestFrame.slipstreamBoost > 0.01f;
    }

    @Override
    int activeDisplayPriority() {
        return 1;
    }

    @Override
    float slipstreamRangeMultiplier() {
        return 1f + RogueliteEffectMath.levelValue(level, 0.10f, 0.20f, 0.30f);
    }

    @Override
    float slipstreamStrengthMultiplier() {
        return 1f
                + RogueliteEffectMath.levelValue(level, 0.10f, 0.20f, 0.30f)
                + (synergy ? 0.03f : 0f);
    }

    @Override
    float slipstreamReleaseLerp(float baseReleaseLerp) {
        return level >= 2 ? 2.5f : baseReleaseLerp;
    }
}
