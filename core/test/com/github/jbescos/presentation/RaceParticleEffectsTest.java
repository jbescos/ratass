package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class RaceParticleEffectsTest {
    @Test
    public void driftSmokeUsesThresholdAndHysteresis() {
        RaceParticleEffects effects = new RaceParticleEffects();

        updateDrift(effects, 0.33f);
        assertEquals(0, effects.getSmokeCount());

        updateDrift(effects, 0.60f);
        assertEquals(2, effects.getSmokeCount());

        effects.update(0.12f);
        updateDrift(effects, 0.29f);
        assertEquals(4, effects.getSmokeCount());

        updateDrift(effects, 0.20f);
        effects.update(0.12f);
        updateDrift(effects, 0.29f);
        assertEquals(4, effects.getSmokeCount());
    }

    @Test
    public void collisionBurstIsRateLimitedAndExpires() {
        RaceParticleEffects effects = new RaceParticleEffects();

        effects.emitImpact(2f, 3f, 0f, 1f, 24f, 0.2f, 0.4f, 0.8f);
        int firstBurstCount = effects.getActiveCount();
        assertTrue(effects.getSparkCount() >= 6);
        assertEquals(1, effects.getFlashCount());

        effects.emitImpact(2f, 3f, 0f, 1f, 24f, 0.2f, 0.4f, 0.8f);
        assertEquals(firstBurstCount, effects.getActiveCount());

        effects.update(2f);
        assertEquals(0, effects.getActiveCount());
    }

    @Test
    public void offRoadEmitterThrowsBoundedDebrisOnlyWhileMovingOffRoad() {
        RaceParticleEffects effects = new RaceParticleEffects();

        updateOffRoad(effects, false, 0.7f);
        assertEquals(0, effects.getSurfaceDebrisCount());

        updateOffRoad(effects, true, 0.7f);
        int firstBurst = effects.getSurfaceDebrisCount();
        assertTrue(firstBurst >= 4);

        updateOffRoad(effects, true, 0.02f);
        assertEquals(firstBurst, effects.getSurfaceDebrisCount());

        effects.update(2f);
        assertEquals(0, effects.getSurfaceDebrisCount());
    }

    @Test
    public void particleStorageStaysBoundedAndResetClearsIt() {
        RaceParticleEffects effects = new RaceParticleEffects();
        for (int i = 0; i < 1000; i++) {
            effects.update(0.06f);
            effects.emitImpact(i, 0f, 1f, 0f, 40f, 1f, 0f, 0f);
        }

        assertTrue(effects.getActiveCount() <= RaceParticleEffects.MAX_PARTICLES);
        effects.reset();
        assertEquals(0, effects.getActiveCount());
    }

    private static void updateDrift(RaceParticleEffects effects, float slip) {
        effects.updateDriftEmitter(
                7,
                0.12f,
                0f,
                0f,
                0f,
                1.14f,
                1.58f,
                3f,
                0f,
                0.6f,
                slip);
    }

    private static void updateOffRoad(
            RaceParticleEffects effects,
            boolean offRoad,
            float speedRatio) {
        effects.updateOffRoadEmitter(
                7,
                0.12f,
                0f,
                0f,
                0f,
                1.14f,
                1.58f,
                3f,
                0f,
                speedRatio,
                offRoad);
    }
}
