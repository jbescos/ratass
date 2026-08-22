package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.jbescos.RatassGame;
import org.junit.Test;

public class RecoveryTrainingConfigTest {
    @Test
    public void recoveryTrainingAlwaysUsesOneControlledCar() {
        RatassGame.RlTrainingConfig config =
                new RatassGame.RlTrainingConfig()
                        .withControlledAgentCount(8)
                        .withFieldSize(20)
                        .withRouteTargets(5)
                        .withRandomRaceSpawns(true)
                        .withRecoveryTraining(true)
                        .withRecoveryScenario("offroad_shallow");

        assertTrue(config.recoveryTraining);
        assertEquals(1, config.controlledAgentCount);
        assertEquals(1, config.fieldSize);
        assertEquals(1, config.routeTargets);
        assertTrue(config.randomRaceSpawns);
        assertEquals("offroad_shallow", config.recoveryScenario);
    }

    @Test
    public void normalTrainingConfigurationRemainsUnchanged() {
        RatassGame.RlTrainingConfig config =
                new RatassGame.RlTrainingConfig()
                        .withControlledAgentCount(4)
                        .withFieldSize(10)
                        .withRouteTargets(3)
                        .withRecoveryTraining(false);

        assertFalse(config.recoveryTraining);
        assertEquals(4, config.controlledAgentCount);
        assertEquals(10, config.fieldSize);
        assertEquals(3, config.routeTargets);
    }

    @Test
    public void noseToNoseRecoveryScenarioIsRetained() {
        RatassGame.RlTrainingConfig config =
                new RatassGame.RlTrainingConfig()
                        .withRecoveryTraining(true)
                        .withRecoveryScenario("nose_to_nose");

        assertEquals("nose_to_nose", config.recoveryScenario);
        assertEquals(1, config.controlledAgentCount);
        assertEquals(1, config.fieldSize);
    }

    @Test
    public void map014InflectionRecoveryScenarioIsRetained() {
        RatassGame.RlTrainingConfig config =
                new RatassGame.RlTrainingConfig()
                        .withRecoveryTraining(true)
                        .withRecoveryScenario("map014_inflection");

        assertEquals("map014_inflection", config.recoveryScenario);
        assertEquals(1, config.controlledAgentCount);
        assertEquals(1, config.fieldSize);
    }
}
