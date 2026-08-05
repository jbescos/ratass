package com.github.jbescos.presentation;

/** Presentation-only timing for an offender-targeting revenge projectile. */
public final class RevengeProjectileVisual {
    private static final float PROJECTILE_DURATION_SECONDS = 0.55f;

    private float remainingSeconds;
    private float durationSeconds = PROJECTILE_DURATION_SECONDS;
    private int tier;
    private boolean tether;

    public void start(int tier) {
        start(tier, PROJECTILE_DURATION_SECONDS, false);
    }

    public void startTether(int tier, float durationSeconds) {
        start(tier, durationSeconds, true);
    }

    private void start(int tier, float durationSeconds, boolean tether) {
        this.tier = Math.max(1, Math.min(3, tier));
        this.durationSeconds = Math.max(0.01f, durationSeconds);
        this.tether = tether;
        remainingSeconds = this.durationSeconds;
    }

    public void update(float delta) {
        remainingSeconds = Math.max(0f, remainingSeconds - Math.max(0f, delta));
    }

    public void reset() {
        remainingSeconds = 0f;
        durationSeconds = PROJECTILE_DURATION_SECONDS;
        tier = 0;
        tether = false;
    }

    public boolean isActive() {
        return remainingSeconds > 0f;
    }

    public float getProgress() {
        if (!isActive()) {
            return 1f;
        }
        return 1f - remainingSeconds / durationSeconds;
    }

    public float getAlpha() {
        if (!isActive()) {
            return 0f;
        }
        float progress = getProgress();
        return Math.min(1f, Math.min(progress * 8f, (1f - progress) * 8f));
    }

    public int getTier() {
        return tier;
    }

    public boolean isTether() {
        return tether;
    }

    public float getTetherReach() {
        if (!tether) {
            return getProgress();
        }
        float reach = Math.min(1f, getProgress() * 4f);
        return reach * reach * (3f - 2f * reach);
    }
}
