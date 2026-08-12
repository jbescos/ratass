package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class HunterBarrageRevengeEffectTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void retargetsBeforeTheFirstShotThenFinishesThatSequence() {
        HunterBarrageRevengeEffect effect = new HunterBarrageRevengeEffect();
        RogueliteDrivingFrame frame = new RogueliteDrivingFrame();

        effect.onHitBy(42, 12f);
        effect.onHitBy(7, 20f);

        assertTrue(effect.isArmed());
        assertFalse(effect.isReady());
        assertEquals(7, effect.revengeTargetVehicleId());
        assertEquals(3f, effect.activeTimeRemainingSeconds(), EPSILON);
        assertNull(effect.tryActivateOffenderStrike(42, 5000f, false));

        for (int shot = 1; shot <= HunterBarrageRevengeEffect.SHOT_COUNT; shot++) {
            effect.advance(1f, 1f, frame);
            assertTrue(effect.isReady());
            assertNull(effect.tryActivateOffenderStrike(42, 0f, true));

            RogueliteRevengeStrike strike =
                    effect.tryActivateOffenderStrike(7, 5000f, false);
            assertNotNull(strike);
            assertEquals(RogueliteCardId.HUNTER_BARRAGE, strike.getCardId());
            assertEquals(RogueliteRevengeStrike.Action.PUSH_SHOT, strike.getAction());
            assertEquals(shot, strike.getStrikeIndex());
            assertEquals(shot == 1, strike.isOpeningStrike());
            if (shot == 1) {
                effect.onHitBy(9, 20f);
                assertEquals(7, effect.revengeTargetVehicleId());
            }
        }

        assertFalse(effect.isArmed());
        assertFalse(effect.isActive());
        assertEquals(-1, effect.revengeTargetVehicleId());
    }

    @Test
    public void canStrikeAcrossTheMapAndWhileEitherCarIsOffRoad() {
        HunterBarrageRevengeEffect effect = new HunterBarrageRevengeEffect();

        assertTrue(effect.allowsOffRoadOffenderStrike());
        effect.onHitBy(42, 12f);
        effect.advance(1f, 1f, new RogueliteDrivingFrame());

        assertNotNull(effect.tryActivateOffenderStrike(42, Float.MAX_VALUE, false));
    }

    @Test
    public void tierThreeStormFiresTwoShotsPerSecondForThreeSeconds() {
        HunterBarrageRevengeEffect effect =
                new HunterBarrageRevengeEffect(RogueliteCardId.HUNTER_STORM);
        RogueliteDrivingFrame frame = new RogueliteDrivingFrame();

        effect.onHitBy(42, 12f);

        assertEquals(3f, effect.activeTimeRemainingSeconds(), EPSILON);
        for (int shot = 1; shot <= HunterBarrageRevengeEffect.STORM_SHOT_COUNT; shot++) {
            effect.advance(
                    HunterBarrageRevengeEffect.STORM_SHOT_INTERVAL_SECONDS,
                    HunterBarrageRevengeEffect.STORM_SHOT_INTERVAL_SECONDS,
                    frame);
            RogueliteRevengeStrike strike =
                    effect.tryActivateOffenderStrike(42, Float.MAX_VALUE, false);
            assertNotNull("missed Tier 3 shot " + shot, strike);
            assertEquals(RogueliteCardId.HUNTER_STORM, strike.getCardId());
            assertEquals(shot, strike.getStrikeIndex());
        }

        assertFalse(effect.isArmed());
        assertFalse(effect.isActive());
    }

    @Test
    public void amplificationScalesStormRateAndDuration() {
        HunterBarrageRevengeEffect effect =
                new HunterBarrageRevengeEffect(RogueliteCardId.HUNTER_STORM);
        RogueliteDrivingFrame frame = new RogueliteDrivingFrame();

        effect.onHitBy(42, 12f);
        effect.amplifyActiveRevenge(2f);

        assertEquals(6f, effect.activeTimeRemainingSeconds(), EPSILON);
        float amplifiedInterval = HunterBarrageRevengeEffect.STORM_SHOT_INTERVAL_SECONDS / 2f;
        int amplifiedShots = HunterBarrageRevengeEffect.STORM_SHOT_COUNT * 4;
        for (int shot = 1; shot <= amplifiedShots; shot++) {
            effect.advance(amplifiedInterval, amplifiedInterval, frame);
            assertNotNull(
                    "missed amplified Tier 3 shot " + shot,
                    effect.tryActivateOffenderStrike(42, Float.MAX_VALUE, false));
        }

        assertFalse(effect.isArmed());
    }
}
