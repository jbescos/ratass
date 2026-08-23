package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class FinalReckoningEffectTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void oneHitActivatesAllThreeTierThreePowerups() {
        FinalReckoningEffect effect = new FinalReckoningEffect();

        assertTrue(effect.onHitBy(42, 10f));

        assertTrue(effect.isActive());
        assertEquals(42, effect.revengeTargetVehicleId());
        assertEquals(RogueliteCardId.OVERDRIVE_COIL, effect.activePowerupCardId());
        assertTrue(effect.isCardEffectActive(RogueliteCardId.OVERDRIVE_COIL));
        assertTrue(effect.isCardEffectActive(RogueliteCardId.COLOSSUS_FIELD));
        assertTrue(effect.isCardEffectActive(RogueliteCardId.TEMPORAL_DOMINION));
        assertTrue(effect.acceleratesOwnDecisions());
        assertEquals(1.20f, effect.massMultiplier(), EPSILON);
        assertEquals(0.05f, effect.gripBonus(0f), EPSILON);
        assertEquals(4f, effect.carCollisionAreaMultiplier(), EPSILON);
        assertEquals(12f, effect.carCollisionMassMultiplier(), EPSILON);
    }

    @Test
    public void revengeAmplifierScalesDurationsCopiesAndCollisionStrengthOnce() {
        FinalReckoningEffect effect = new FinalReckoningEffect();
        effect.onHitBy(42, 10f);

        effect.amplifyActiveRevenge(2f);
        effect.amplifyActiveRevenge(2f);

        assertEquals(10f, effect.cardEffectActiveTimeRemainingSeconds(
                RogueliteCardId.OVERDRIVE_COIL), EPSILON);
        assertEquals(20f, effect.cardEffectActiveTimeRemainingSeconds(
                RogueliteCardId.COLOSSUS_FIELD), EPSILON);
        assertEquals(4f, effect.cardEffectActiveTimeRemainingSeconds(
                RogueliteCardId.TEMPORAL_DOMINION), EPSILON);
        assertEquals(2f, effect.nestedPowerupEffectMultiplier(
                RogueliteCardId.OVERDRIVE_COIL), EPSILON);
        assertEquals(1.40f, effect.massMultiplier(), EPSILON);
        assertEquals(0.10f, effect.gripBonus(0f), EPSILON);
        assertEquals(8f, effect.carCollisionAreaMultiplier(), EPSILON);
        assertEquals(24f, effect.carCollisionMassMultiplier(), EPSILON);
    }

    @Test
    public void eachPowerupExpiresOnItsOwnDuration() {
        FinalReckoningEffect effect = new FinalReckoningEffect();
        RogueliteDrivingFrame frame = new RogueliteDrivingFrame();
        effect.onHitBy(42, 10f);

        effect.advance(2.1f, 2.1f, frame);
        assertFalse(effect.acceleratesOwnDecisions());
        assertTrue(effect.isCardEffectActive(RogueliteCardId.OVERDRIVE_COIL));
        assertTrue(effect.isCardEffectActive(RogueliteCardId.COLOSSUS_FIELD));

        effect.advance(3f, 3f, frame);
        assertFalse(effect.isCardEffectActive(RogueliteCardId.OVERDRIVE_COIL));
        assertTrue(effect.isCardEffectActive(RogueliteCardId.COLOSSUS_FIELD));

        effect.advance(5f, 5f, frame);
        assertFalse(effect.isActive());
        assertEquals(-1, effect.revengeTargetVehicleId());
    }
}
