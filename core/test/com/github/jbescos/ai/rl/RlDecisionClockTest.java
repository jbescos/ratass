package com.github.jbescos.ai.rl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.jbescos.gameplay.TimeDilationDecisionCadence;
import org.junit.Test;

public class RlDecisionClockTest {
    private static final float STEP = 1f / 60f;

    @Test
    public void fourStepCadenceNeverSlipsToFive() {
        assertCadence(4);
    }

    @Test
    public void supportsOtherConfiguredRepeats() {
        assertCadence(1);
        assertCadence(2);
        assertCadence(5);
        assertCadence(8);
    }

    private void assertCadence(int repeat) {
        RlDecisionClock clock = new RlDecisionClock(STEP);
        for (int tick = 0; tick < 10000; tick++) {
            boolean due = clock.advance(STEP);
            assertEquals("tick " + tick, tick % repeat == 0, due);
            if (due) {
                clock.schedule(STEP * repeat);
            }
        }
    }

    @Test
    public void batchedPhysicsStepsAdvanceTheWholeInterval() {
        RlDecisionClock clock = new RlDecisionClock(STEP);
        clock.schedule(STEP * 4);
        assertTrue(clock.advance(STEP * 4));
        clock.schedule(STEP * 4);
        assertFalse(clock.advance(STEP * 2));
        assertTrue(clock.advance(STEP * 2));
    }

    @Test
    public void policyChangeCanDecideImmediatelyWithoutElapsedTime() {
        RlDecisionClock clock = new RlDecisionClock(STEP);
        clock.schedule(STEP * 4);
        assertFalse(clock.advance(0f));
        clock.reset();
        assertTrue(clock.advance(0f));
    }

    @Test
    public void timePowerupBringsDecisionForwardWithoutDelayingItOnExpiry() {
        RlDecisionClock clock = new RlDecisionClock(STEP);
        float normal = STEP * 4;
        float accelerated = TimeDilationDecisionCadence.intervalSeconds(normal, true, 2f);
        clock.schedule(normal);
        clock.limitDelay(accelerated);
        assertFalse(clock.advance(STEP));
        assertTrue(clock.advance(STEP));
        clock.schedule(accelerated);
        assertFalse(clock.advance(STEP));
        clock.limitDelay(normal);
        assertTrue(clock.advance(STEP));
        clock.schedule(normal);
        assertFalse(clock.advance(STEP * 3));
        assertTrue(clock.advance(STEP));
    }

    @Test
    public void independentCarsKeepIndependentCountdowns() {
        RlDecisionClock first = new RlDecisionClock(STEP);
        RlDecisionClock second = new RlDecisionClock(STEP);
        first.schedule(STEP * 4);
        second.schedule(STEP * 2);
        assertFalse(first.advance(STEP * 2));
        assertTrue(second.advance(STEP * 2));
        assertTrue(first.advance(STEP * 2));
    }
}
