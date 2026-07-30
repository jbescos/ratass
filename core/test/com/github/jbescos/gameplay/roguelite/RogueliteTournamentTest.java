package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class RogueliteTournamentTest {
    @Test
    public void bottomTwoLeaveEachOfTheFirstFourChampionships() {
        RogueliteTournament tournament = new RogueliteTournament();
        List<Integer> survivingVehicleIds =
                new ArrayList<Integer>(
                        Arrays.asList(
                                0, 1, 2, 3, 4, 5, 6, 7, 8, 9));

        for (int championship = 1; championship <= 4; championship++) {
            RogueliteTournament.Outcome outcome =
                    tournament.resolve(
                            championship,
                            standingsFor(survivingVehicleIds));

            assertFalse(outcome.isFinalChampionship());
            assertEquals(
                    RogueliteTournament.ELIMINATIONS_PER_CHAMPIONSHIP,
                    outcome.getEliminatedVehicleIds().size());
            survivingVehicleIds.removeAll(
                    outcome.getEliminatedVehicleIds());
            assertEquals(
                    10
                            - championship
                                    * RogueliteTournament
                                            .ELIMINATIONS_PER_CHAMPIONSHIP,
                    survivingVehicleIds.size());
        }

        assertEquals(Arrays.asList(0, 1), survivingVehicleIds);
    }

    @Test
    public void championshipFiveAwardsTheLeaderInsteadOfEliminating() {
        RogueliteTournament tournament = new RogueliteTournament();
        List<RogueliteTournament.Standing> standings =
                Arrays.asList(
                        new RogueliteTournament.Standing(7, 30, 2),
                        new RogueliteTournament.Standing(3, 40, 1));

        RogueliteTournament.Outcome outcome =
                tournament.resolve(5, standings);

        assertTrue(outcome.isFinalChampionship());
        assertEquals(3, outcome.getWinningVehicleId());
        assertTrue(outcome.getEliminatedVehicleIds().isEmpty());
    }

    @Test
    public void latestFinishBreaksEqualPointsBeforeVehicleId() {
        RogueliteTournament tournament = new RogueliteTournament();
        List<RogueliteTournament.Standing> standings =
                Arrays.asList(
                        new RogueliteTournament.Standing(4, 20, 2),
                        new RogueliteTournament.Standing(2, 20, 1),
                        new RogueliteTournament.Standing(8, 10, 3),
                        new RogueliteTournament.Standing(6, 10, 4));

        RogueliteTournament.Outcome outcome =
                tournament.resolve(1, standings);

        assertEquals(
                Arrays.asList(Integer.valueOf(6), Integer.valueOf(8)),
                outcome.getEliminatedVehicleIds());
    }

    private static List<RogueliteTournament.Standing> standingsFor(
            List<Integer> vehicleIds) {
        List<RogueliteTournament.Standing> standings =
                new ArrayList<RogueliteTournament.Standing>();
        for (int i = 0; i < vehicleIds.size(); i++) {
            int vehicleId = vehicleIds.get(i).intValue();
            standings.add(
                    new RogueliteTournament.Standing(
                            vehicleId,
                            100 - vehicleId,
                            i + 1));
        }
        return standings;
    }
}
