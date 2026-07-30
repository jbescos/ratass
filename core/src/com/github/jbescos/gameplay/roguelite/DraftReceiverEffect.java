package com.github.jbescos.gameplay.roguelite;

final class DraftReceiverEffect extends RogueliteUpgradeEffect {
    private final boolean synergy;

    DraftReceiverEffect(boolean synergy) {
        super(RogueliteCardId.DRAFT_RECEIVER);
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
        return 1.20f;
    }

    @Override
    float slipstreamStrengthMultiplier() {
        return 1.20f + (synergy ? 0.03f : 0f);
    }

    @Override
    float slipstreamReleaseLerp(float baseReleaseLerp) {
        return synergy ? 2.5f : baseReleaseLerp;
    }
}
