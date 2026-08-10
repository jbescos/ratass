package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import org.junit.Test;

public class CrownBreakerLinkVisualTest {
    @Test
    public void linkOnlyShowsForAnArmedCrownBreakerWithBothCars() {
        assertTrue(CrownBreakerLinkVisual.shouldDraw(
                RogueliteCardId.CROWN_ENGINE, true, true, true));
        assertFalse(CrownBreakerLinkVisual.shouldDraw(
                RogueliteCardId.CROWN_ENGINE, false, true, true));
        assertFalse(CrownBreakerLinkVisual.shouldDraw(
                RogueliteCardId.CROWN_ENGINE, true, true, false));
        assertFalse(CrownBreakerLinkVisual.shouldDraw(
                RogueliteCardId.RECOVERY_BEACON, true, true, true));
    }

    @Test
    public void linkFadesOnlyDuringItsLastFiveSeconds() {
        assertEquals(1f, CrownBreakerLinkVisual.timeoutAlpha(30f), 0.0001f);
        assertEquals(1f, CrownBreakerLinkVisual.timeoutAlpha(5f), 0.0001f);
        assertEquals(0.5f, CrownBreakerLinkVisual.timeoutAlpha(2.5f), 0.0001f);
        assertEquals(0f, CrownBreakerLinkVisual.timeoutAlpha(0f), 0.0001f);
    }
}
