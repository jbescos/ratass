package com.github.jbescos.presentation;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;

/** Presentation-only timing and geometry for the level-up card reveal. */
public final class RogueliteRewardReveal {
    static final float DEAL_DURATION = 0.46f;
    static final float FACE_DOWN_HOLD = 0.42f;
    static final float FLIP_DURATION = 0.48f;
    static final float FLIP_STAGGER = 0.14f;
    static final float SETTLE_DURATION = 0.14f;
    private static final float MINIMUM_FLIP_WIDTH_SCALE = 0.045f;

    private RogueliteRewardReveal() {}

    public static float initialElapsed(boolean presentationEnabled, int cardCount) {
        return presentationEnabled ? 0f : completedElapsed(cardCount);
    }

    public static float update(float elapsed, float deltaSeconds) {
        return Math.max(0f, elapsed) + Math.max(0f, deltaSeconds);
    }

    public static float dealProgress(float elapsed) {
        return smoothStep(MathUtils.clamp(elapsed / DEAL_DURATION, 0f, 1f));
    }

    public static float flipProgress(float elapsed, int cardIndex) {
        float start = DEAL_DURATION + FACE_DOWN_HOLD + Math.max(0, cardIndex) * FLIP_STAGGER;
        return smoothStep(MathUtils.clamp((elapsed - start) / FLIP_DURATION, 0f, 1f));
    }

    public static boolean isFaceUp(float elapsed, int cardIndex) {
        return flipProgress(elapsed, cardIndex) >= 0.5f;
    }

    public static boolean isInteractionReady(float elapsed, int cardCount) {
        return elapsed >= completedElapsed(cardCount);
    }

    public static float completedElapsed(int cardCount) {
        int lastIndex = Math.max(0, cardCount - 1);
        return DEAL_DURATION
                + FACE_DOWN_HOLD
                + lastIndex * FLIP_STAGGER
                + FLIP_DURATION
                + SETTLE_DURATION;
    }

    public static void transform(
            Rectangle destination,
            Rectangle finalBounds,
            float elapsed,
            int cardIndex,
            float originX,
            float originY) {
        float deal = dealProgress(elapsed);
        float flip = flipProgress(elapsed, cardIndex);
        float centerX = MathUtils.lerp(originX, finalBounds.x + finalBounds.width * 0.5f, deal);
        float centerY = MathUtils.lerp(originY, finalBounds.y + finalBounds.height * 0.5f, deal);
        float dealScale = MathUtils.lerp(0.78f, 1f, deal);
        float flipScale = Math.max(MINIMUM_FLIP_WIDTH_SCALE, Math.abs(1f - flip * 2f));
        if (flip >= 1f) {
            flipScale = 1f;
        }
        float width = finalBounds.width * dealScale * flipScale;
        float height = finalBounds.height * dealScale;
        destination.set(centerX - width * 0.5f, centerY - height * 0.5f, width, height);
    }

    private static float smoothStep(float value) {
        return value * value * (3f - 2f * value);
    }
}
