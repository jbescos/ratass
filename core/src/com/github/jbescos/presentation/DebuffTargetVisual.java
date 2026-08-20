package com.github.jbescos.presentation;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;

/** Presentation-only state for card-specific effects currently applied to a car. */
public final class DebuffTargetVisual {
    private static final float PERSISTENT = -1f;
    private static final RogueliteCardId[] CARD_IDS = RogueliteCardId.values();

    private final float[] remainingSeconds = new float[CARD_IDS.length];
    private final long[] activationOrder = new long[CARD_IDS.length];
    private long nextActivationOrder;
    private RogueliteCardId activeCardId;
    private float activeAge;

    public void activateTimed(RogueliteCardId cardId, float durationSeconds) {
        if (cardId == null || durationSeconds <= 0f || !isFinite(durationSeconds)) {
            return;
        }
        int index = cardId.ordinal();
        remainingSeconds[index] = Math.max(remainingSeconds[index], durationSeconds);
        activationOrder[index] = ++nextActivationOrder;
        selectActiveCard();
    }

    public void activatePersistent(RogueliteCardId cardId) {
        if (cardId == null) {
            return;
        }
        int index = cardId.ordinal();
        remainingSeconds[index] = PERSISTENT;
        activationOrder[index] = ++nextActivationOrder;
        selectActiveCard();
    }

    public void clear(RogueliteCardId cardId) {
        if (cardId == null) {
            return;
        }
        int index = cardId.ordinal();
        remainingSeconds[index] = 0f;
        activationOrder[index] = 0L;
        selectActiveCard();
    }

    public void update(float deltaSeconds) {
        float delta = sanitizeDelta(deltaSeconds);
        for (int i = 0; i < remainingSeconds.length; i++) {
            float remaining = remainingSeconds[i];
            if (remaining > 0f) {
                remainingSeconds[i] = remaining <= delta + 0.0001f
                        ? 0f
                        : remaining - delta;
                if (remainingSeconds[i] == 0f) {
                    activationOrder[i] = 0L;
                }
            }
        }
        RogueliteCardId previous = activeCardId;
        selectActiveCard();
        if (activeCardId == null) {
            activeAge = 0f;
        } else if (activeCardId != previous) {
            activeAge = 0f;
        } else {
            activeAge += delta;
        }
    }

    public void reset() {
        for (int i = 0; i < remainingSeconds.length; i++) {
            remainingSeconds[i] = 0f;
            activationOrder[i] = 0L;
        }
        activeCardId = null;
        activeAge = 0f;
        nextActivationOrder = 0L;
    }

    public boolean isActive() {
        return activeCardId != null;
    }

    public RogueliteCardId getActiveCardId() {
        return activeCardId;
    }

    public float getActiveRemainingSeconds() {
        if (activeCardId == null) {
            return 0f;
        }
        return Math.max(0f, remainingSeconds[activeCardId.ordinal()]);
    }

    public float getPulse() {
        return isActive()
                ? 0.5f + 0.5f * (float) Math.sin(activeAge * 7.5f)
                : 0f;
    }

    private void selectActiveCard() {
        RogueliteCardId selected = null;
        long selectedOrder = 0L;
        for (int i = 0; i < remainingSeconds.length; i++) {
            if (remainingSeconds[i] != 0f && activationOrder[i] >= selectedOrder) {
                selected = CARD_IDS[i];
                selectedOrder = activationOrder[i];
            }
        }
        activeCardId = selected;
    }

    private static float sanitizeDelta(float deltaSeconds) {
        if (deltaSeconds <= 0f || !isFinite(deltaSeconds)) {
            return 0f;
        }
        return Math.min(deltaSeconds, 1f);
    }

    private static boolean isFinite(float value) {
        return !Float.isNaN(value) && !Float.isInfinite(value);
    }
}
