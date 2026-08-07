package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public class RogueliteTournamentTest {
    @Test
    public void defaultChampionshipAwardsTheLeaderImmediately() {
        RogueliteTournament tournament = new RogueliteTournament();
        List<Integer> vehicleIds =
                Arrays.asList(0, 1, 2, 3, 4, 5, 6, 7, 8, 9);

        RogueliteTournament.Outcome outcome =
                tournament.resolve(1, standingsFor(vehicleIds));

        assertTrue(outcome.isFinalChampionship());
        assertEquals(0, outcome.getWinningVehicleId());
    }

    @Test
    public void championshipWinnerUsesPointsThenLatestFinish() {
        RogueliteTournament tournament = new RogueliteTournament();
        List<RogueliteTournament.Standing> standings =
                Arrays.asList(
                        new RogueliteTournament.Standing(7, 30, 2),
                        new RogueliteTournament.Standing(3, 40, 1));

        RogueliteTournament.Outcome outcome =
                tournament.resolve(1, standings);

        assertTrue(outcome.isFinalChampionship());
        assertEquals(3, outcome.getWinningVehicleId());
    }

    @Test
    public void finalChampionshipMarksEveryNonWinnerAsLosing() {
        RogueliteTournament tournament = new RogueliteTournament();

        assertFalse(tournament.isLosingPosition(1, 1, 4));
        assertTrue(tournament.isLosingPosition(1, 2, 4));
        assertTrue(tournament.isLosingPosition(1, 3, 4));
        assertTrue(tournament.isLosingPosition(1, 4, 4));
    }

    @Test
    public void customEarlierChampionshipHasNoLosingPositions() {
        RogueliteTournament tournament = new RogueliteTournament();

        assertFalse(tournament.isLosingPosition(1, 2, 10, 3));
        assertFalse(tournament.isLosingPosition(1, 6, 10, 3));
        assertFalse(tournament.isLosingPosition(1, 10, 10, 3));
    }

    @Test
    public void latestFinishBreaksEqualPointsBeforeVehicleId() {
        RogueliteTournament tournament = new RogueliteTournament();
        List<RogueliteTournament.Standing> standings =
                Arrays.asList(
                        new RogueliteTournament.Standing(4, 20, 2),
                        new RogueliteTournament.Standing(2, 20, 1),
                        new RogueliteTournament.Standing(8, 10, 3),
                        new RogueliteTournament.Standing(6, 10, 4),
                        new RogueliteTournament.Standing(9, 5, 5),
                        new RogueliteTournament.Standing(10, 4, 6),
                        new RogueliteTournament.Standing(11, 3, 7));

        RogueliteTournament.Outcome outcome =
                tournament.resolve(1, standings);

        assertEquals(2, outcome.getWinningVehicleId());
    }

    @Test
    public void customTournamentUsesConfiguredLength() {
        RogueliteTournament tournament = new RogueliteTournament();
        List<RogueliteTournament.Standing> standings =
                standingsFor(Arrays.asList(0, 1, 2));

        RogueliteTournament.Outcome first =
                tournament.resolve(1, standings, 4);
        RogueliteTournament.Outcome finalOutcome =
                tournament.resolve(4, standings, 4);

        assertFalse(first.isFinalChampionship());
        assertEquals(0, first.getWinningVehicleId());
        assertTrue(finalOutcome.isFinalChampionship());
        assertEquals(0, finalOutcome.getWinningVehicleId());
    }

    private static List<RogueliteTournament.Standing> standingsFor(
            List<Integer> vehicleIds) {
        List<RogueliteTournament.Standing> standings =
                new java.util.ArrayList<RogueliteTournament.Standing>();
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
