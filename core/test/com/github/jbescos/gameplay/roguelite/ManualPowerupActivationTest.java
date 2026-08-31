package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class ManualPowerupActivationTest {
    @Test
    public void manualModeWaitsForRequestAndThenBypassesContextCondition() {
        CooldownPowerupEffect effect =
                new CooldownPowerupEffect(RogueliteCardId.NITRO_PULSE, 0f);
        RogueliteDrivingFrame frame = new RogueliteDrivingFrame();
        frame.throttle = 1f;
        frame.onRoad = false;
        frame.speedRatio = 0.1f;

        effect.setAutomaticPowerupActivationAllowed(false);
        effect.advance(0.1f, 0.1f, frame);
        effect.advance(10f, 10f, frame);

        assertTrue(effect.isReady());
        assertFalse(effect.isActive());
        assertTrue(effect.requestManualPowerupActivation());

        effect.advance(0.1f, 0.1f, frame);

        assertTrue(effect.isActive());
        assertFalse(effect.requestManualPowerupActivation());
    }
}
