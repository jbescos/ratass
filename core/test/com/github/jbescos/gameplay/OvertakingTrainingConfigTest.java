package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.jbescos.RatassGame;
import org.junit.Test;

public class OvertakingTrainingConfigTest {
    @Test
    public void overtakingTrainingUsesOneLearnerAndDedicatedContract() {
        RatassGame.RlTrainingConfig config =
                new RatassGame.RlTrainingConfig()
                        .withControlledAgentCount(8)
                        .withFieldSize(10)
                        .withRecoveryTraining(true)
                        .withOvertakingTraining(true)
                        .withOvertakingScenario("pack")
                        .withOvertakingOpponentCount(3)
                        .withOvertakingOpponentThrottleScale(0.82f);

        assertTrue(config.overtakingTraining);
        assertFalse(config.recoveryTraining);
        assertEquals(1, config.controlledAgentCount);
        assertEquals(4, config.fieldSize);
        assertEquals(1, config.routeTargets);
        assertEquals("pack", config.overtakingScenario);
        assertEquals(3, config.overtakingOpponentCount);
        assertEquals(0.82f, config.overtakingOpponentThrottleScale, 0f);
        assertEquals(1, RatassGame.RL_OVERTAKING_ACTION_SIZE);
    }
}
