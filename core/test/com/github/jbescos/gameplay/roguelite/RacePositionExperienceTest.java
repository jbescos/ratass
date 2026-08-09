package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class RacePositionExperienceTest {
    @Test
    public void stablePassProducesOneEventForThePassingDriver() {
        RacePositionExperience tracker = new RacePositionExperience(0.5f);
        tracker.reset(order(1, 2, 3));

        assertTrue(tracker.update(order(2, 1, 3), 0.3f).isEmpty());
        List<RacePositionExperience.Overtake> overtakes =
                tracker.update(order(2, 1, 3), 0.2f);

        assertEquals(1, overtakes.size());
        assertEquals(2, overtakes.get(0).getVehicleId());
        assertEquals(1, overtakes.get(0).getPassedVehicleId());
        assertEquals(1, overtakes.get(0).getRivalsPassed());
        assertTrue(tracker.update(order(2, 1, 3), 2f).isEmpty());
    }

    @Test
    public void transientPositionFlappingNeverAwardsExperience() {
        RacePositionExperience tracker = new RacePositionExperience(0.5f);
        tracker.reset(order(1, 2, 3));

        assertTrue(tracker.update(order(2, 1, 3), 0.3f).isEmpty());
        assertTrue(tracker.update(order(1, 2, 3), 0.3f).isEmpty());
        assertTrue(tracker.update(order(2, 1, 3), 0.3f).isEmpty());
        assertTrue(tracker.update(order(1, 2, 3), 0.3f).isEmpty());
    }

    @Test
    public void passingSeveralCarsProducesOneAggregatedEvent() {
        RacePositionExperience tracker = new RacePositionExperience(0f);
        tracker.reset(order(1, 2, 3, 4));

        List<RacePositionExperience.Overtake> overtakes =
                tracker.update(order(4, 1, 2, 3), 0f);

        assertEquals(1, overtakes.size());
        assertEquals(4, overtakes.get(0).getVehicleId());
        assertEquals(1, overtakes.get(0).getPassedVehicleId());
        assertEquals(3, overtakes.get(0).getRivalsPassed());
    }

    @Test
    public void invalidOrChangedFieldResetsWithoutAwards() {
        RacePositionExperience tracker = new RacePositionExperience(0f);
        tracker.reset(order(1, 2));

        assertTrue(tracker.update(order(1, 1), 1f).isEmpty());
        assertTrue(tracker.update(order(1, 2, 3), 1f).isEmpty());
    }

    @Test
    public void listedAheadUsesTheExactCurrentRaceOrder() {
        List<Integer> raceOrder = order(8, 4, 2, 9);

        assertTrue(RacePositionExperience.isListedAhead(raceOrder, 4, 9));
        assertFalse(RacePositionExperience.isListedAhead(raceOrder, 9, 4));
        assertFalse(RacePositionExperience.isListedAhead(raceOrder, 4, 4));
        assertFalse(RacePositionExperience.isListedAhead(raceOrder, 7, 9));
        assertFalse(RacePositionExperience.isListedAhead(null, 4, 9));
    }

    private static List<Integer> order(Integer... vehicleIds) {
        return Arrays.asList(vehicleIds);
    }
}
