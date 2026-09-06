package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.badlogic.gdx.math.Rectangle;
import org.junit.Test;

public class RogueliteRewardRevealTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void dealsFaceDownBeforeFlippingCardsInOrder() {
        float afterDeal = RogueliteRewardReveal.DEAL_DURATION + 0.02f;
        assertFalse(RogueliteRewardReveal.isFaceUp(afterDeal, 0));
        assertFalse(RogueliteRewardReveal.isFaceUp(afterDeal, 2));

        float firstRevealed =
                RogueliteRewardReveal.DEAL_DURATION
                        + RogueliteRewardReveal.FACE_DOWN_HOLD
                        + RogueliteRewardReveal.FLIP_DURATION * 0.75f;
        assertTrue(RogueliteRewardReveal.isFaceUp(firstRevealed, 0));
        assertFalse(RogueliteRewardReveal.isFaceUp(firstRevealed, 2));
    }

    @Test
    public void interactionWaitsForEveryCardToSettle() {
        float complete = RogueliteRewardReveal.completedElapsed(3);
        assertFalse(RogueliteRewardReveal.isInteractionReady(complete - 0.01f, 3));
        assertTrue(RogueliteRewardReveal.isInteractionReady(complete, 3));
        assertEquals(complete, RogueliteRewardReveal.initialElapsed(false, 3), EPSILON);
        assertEquals(0f, RogueliteRewardReveal.initialElapsed(true, 3), EPSILON);
    }

    @Test
    public void transformDealsFromCenterAndFinishesAtLayoutBounds() {
        Rectangle finalBounds = new Rectangle(100f, 40f, 80f, 120f);
        Rectangle transformed = new Rectangle();
        RogueliteRewardReveal.transform(transformed, finalBounds, 0f, 0, 320f, 240f);
        assertEquals(320f, transformed.x + transformed.width * 0.5f, EPSILON);
        assertEquals(240f, transformed.y + transformed.height * 0.5f, EPSILON);

        RogueliteRewardReveal.transform(
                transformed,
                finalBounds,
                RogueliteRewardReveal.completedElapsed(3),
                0,
                320f,
                240f);
        assertEquals(finalBounds.x, transformed.x, EPSILON);
        assertEquals(finalBounds.y, transformed.y, EPSILON);
        assertEquals(finalBounds.width, transformed.width, EPSILON);
        assertEquals(finalBounds.height, transformed.height, EPSILON);
    }

    @Test
    public void cardIsEdgeOnHalfwayThroughItsFlip() {
        Rectangle finalBounds = new Rectangle(10f, 20f, 100f, 150f);
        Rectangle transformed = new Rectangle();
        float halfway =
                RogueliteRewardReveal.DEAL_DURATION
                        + RogueliteRewardReveal.FACE_DOWN_HOLD
                        + RogueliteRewardReveal.FLIP_DURATION * 0.5f;
        RogueliteRewardReveal.transform(transformed, finalBounds, halfway, 0, 0f, 0f);
        assertTrue(transformed.width < finalBounds.width * 0.1f);
        assertEquals(finalBounds.height, transformed.height, EPSILON);
    }
}
