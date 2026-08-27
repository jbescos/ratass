package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RivalBuildLeechRevengeEffectTest {
    private static final float EPSILON = 0.001f;

    @Test
    public void tiersUseTheirConfiguredDurations() {
        assertDuration(RogueliteCardId.TELEMETRY_THEFT, 5f);
        assertDuration(RogueliteCardId.BUILD_HEIST, 10f);
        assertDuration(RogueliteCardId.APEX_PLUNDER, 15f);
    }

    @Test
    public void aLaterHitRetargetsAndRestartsTheLink() {
        RivalBuildLeechRevengeEffect effect =
                new RivalBuildLeechRevengeEffect(RogueliteCardId.TELEMETRY_THEFT);
        assertTrue(effect.onHitBy(3, 4f));
        effect.advance(4f, 4f, new RogueliteDrivingFrame());
        assertEquals(1f, effect.activeTimeRemainingSeconds(), EPSILON);

        assertTrue(effect.onHitBy(7, 4f));

        assertEquals(7, effect.revengeTargetVehicleId());
        assertEquals(5f, effect.activeTimeRemainingSeconds(), EPSILON);
        assertEquals(0f, effect.revengeTargetAgeSeconds(), EPSILON);
    }

    @Test
    public void expiryClearsTheTargetAndSuppressionState() {
        RivalBuildLeechRevengeEffect effect =
                new RivalBuildLeechRevengeEffect(RogueliteCardId.TELEMETRY_THEFT);
        effect.onHitBy(3, 4f);

        effect.advance(5.1f, 5.1f, new RogueliteDrivingFrame());

        assertFalse(effect.isActive());
        assertFalse(effect.suppressesOffenderBuildAndTransfersLapExperience());
        assertEquals(-1, effect.revengeTargetVehicleId());
    }

    @Test
    public void revengeAmplificationExtendsTheActiveWindow() {
        RivalBuildLeechRevengeEffect effect =
                new RivalBuildLeechRevengeEffect(RogueliteCardId.BUILD_HEIST);
        effect.onHitBy(3, 4f);

        effect.amplifyActiveRevenge(2f);

        assertEquals(20f, effect.activeTimeRemainingSeconds(), EPSILON);
    }

    private static void assertDuration(RogueliteCardId cardId, float expected) {
        RivalBuildLeechRevengeEffect effect = new RivalBuildLeechRevengeEffect(cardId);
        effect.onHitBy(4, 3f);
        assertTrue(effect.isActive());
        assertEquals(expected, effect.activeTimeRemainingSeconds(), EPSILON);
    }
}
