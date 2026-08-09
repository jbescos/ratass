package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import org.junit.Test;

public class DebuffTargetVisualTest {
    @Test
    public void timedEffectRemainsVisibleUntilItsDurationEnds() {
        DebuffTargetVisual visual = new DebuffTargetVisual();

        visual.activateTimed(RogueliteCardId.EMP_SNARE, 2f);
        visual.update(1f);
        visual.update(0.9f);

        assertTrue(visual.isActive());
        assertEquals(RogueliteCardId.EMP_SNARE, visual.getActiveCardId());

        visual.update(0.1f);

        assertFalse(visual.isActive());
        assertEquals(0f, visual.getPulse(), 0f);
    }

    @Test
    public void newestOverlappingEffectIsShownThenRevealsPreviousEffect() {
        DebuffTargetVisual visual = new DebuffTargetVisual();

        visual.activateTimed(RogueliteCardId.TAR_TETHER, 4f);
        visual.activateTimed(RogueliteCardId.EMP_SNARE, 1f);
        assertEquals(RogueliteCardId.EMP_SNARE, visual.getActiveCardId());

        visual.update(1f);

        assertEquals(RogueliteCardId.TAR_TETHER, visual.getActiveCardId());
    }

    @Test
    public void persistentEffectStaysUntilExplicitlyCleared() {
        DebuffTargetVisual visual = new DebuffTargetVisual();

        visual.activatePersistent(RogueliteCardId.TOTAL_BLACKOUT);
        visual.update(20f);

        assertEquals(RogueliteCardId.TOTAL_BLACKOUT, visual.getActiveCardId());

        visual.clear(RogueliteCardId.TOTAL_BLACKOUT);

        assertFalse(visual.isActive());
    }

    @Test
    public void resetClearsAllPresentationState() {
        DebuffTargetVisual visual = new DebuffTargetVisual();
        visual.activateTimed(RogueliteCardId.DRAFT_VENDETTA, 5f);

        visual.reset();

        assertFalse(visual.isActive());
        assertEquals(null, visual.getActiveCardId());
    }
}
