package com.github.jbescos.presentation;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;

/** Deterministic, rendering-agnostic state for periodic gadget feedback. */
public final class GadgetActivationVisual {
    private static final float CALLOUT_SECONDS = 1.15f;

    private RogueliteCardId activeCardId;
    private float activeAge;
    private float calloutTimer;
    private int activationCount;

    public void reset() {
        activeCardId = null;
        activeAge = 0f;
        calloutTimer = 0f;
        activationCount = 0;
    }

    public void update(float deltaSeconds, RogueliteCardId currentActiveCardId) {
        float delta = sanitizeDelta(deltaSeconds);
        calloutTimer = Math.max(0f, calloutTimer - delta);
        if (currentActiveCardId != activeCardId) {
            activeCardId = currentActiveCardId;
            activeAge = 0f;
            if (activeCardId != null) {
                calloutTimer = CALLOUT_SECONDS;
                activationCount++;
            }
            return;
        }
        if (activeCardId != null) {
            activeAge += delta;
        }
    }

    public RogueliteCardId getActiveCardId() {
        return activeCardId;
    }

    public boolean isActive() {
        return activeCardId != null;
    }

    public float getPulse() {
        return isActive()
                ? 0.5f + 0.5f * (float) Math.sin(activeAge * 12f)
                : 0f;
    }

    public float getActivationFlash() {
        return isActive() ? Math.max(0f, 1f - activeAge / 0.42f) : 0f;
    }

    public boolean isCalloutVisible() {
        return activeCardId != null && calloutTimer > 0f;
    }

    public float getCalloutAlpha() {
        return isCalloutVisible()
                ? Math.min(1f, calloutTimer / 0.25f)
                : 0f;
    }

    public int getActivationCount() {
        return activationCount;
    }

    private static float sanitizeDelta(float deltaSeconds) {
        if (deltaSeconds <= 0f
                || Float.isNaN(deltaSeconds)
                || Float.isInfinite(deltaSeconds)) {
            return 0f;
        }
        return Math.min(deltaSeconds, 1f);
    }
}
