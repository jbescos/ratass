package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;

import com.github.jbescos.RatassGame;
import org.junit.Test;

public class RlTrainingConfigTest {
    @Test
    public void pedalChangePenaltyIsConfigurableAndCannotBecomeAReward() {
        RatassGame.RlTrainingConfig config = new RatassGame.RlTrainingConfig();

        config.withPedalChangePenalty(0.01f);
        assertEquals(0.01f, config.pedalChangePenalty, 0.0001f);

        config.withPedalChangePenalty(-1f);
        assertEquals(0f, config.pedalChangePenalty, 0f);
    }

    @Test
    public void stationaryPenaltyIsConfigurableAndCannotBecomeAReward() {
        RatassGame.RlTrainingConfig config = new RatassGame.RlTrainingConfig();

        config.withStationaryPenalty(12, 0.75f);
        assertEquals(12, config.stationaryGraceActionSteps);
        assertEquals(0.75f, config.stationaryPenalty, 0.0001f);

        config.withStationaryPenalty(-1, -1f);
        assertEquals(0, config.stationaryGraceActionSteps);
        assertEquals(0f, config.stationaryPenalty, 0f);
    }
}
