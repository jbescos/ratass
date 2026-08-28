package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import org.junit.Test;

public class CrownBreakerStarVisualTest {
    @Test
    public void onlyArmedOrActiveCrownBreakerShowsTheStar() {
        assertTrue(
                CrownBreakerStarVisual.isVisible(
                        RogueliteCardId.CROWN_ENGINE,
                        null,
                        true));
        assertTrue(
                CrownBreakerStarVisual.isVisible(
                        RogueliteCardId.CROWN_ENGINE,
                        RogueliteCardId.CROWN_ENGINE,
                        false));
        assertFalse(
                CrownBreakerStarVisual.isVisible(
                        RogueliteCardId.DRAFT_MAGNET,
                        RogueliteCardId.DRAFT_MAGNET,
                        true));
        assertTrue(
                CrownBreakerStarVisual.isVisible(
                        RogueliteCardId.FINAL_RECKONING,
                        RogueliteCardId.FINAL_RECKONING,
                        true));
    }

    @Test
    public void alternatingPointsCreateARegularFivePointStar() {
        float outer = radius(0);
        float inner = radius(1);

        assertEquals(1f, outer, 0.0001f);
        assertEquals(0.46f, inner, 0.0001f);
        assertEquals(1.06f, CrownBreakerStarVisual.radiusScale(1f), 0.0001f);
    }

    private static float radius(int point) {
        float x = CrownBreakerStarVisual.pointX(point, 0f);
        float y = CrownBreakerStarVisual.pointY(point, 0f);
        return (float) Math.sqrt(x * x + y * y);
    }
}
