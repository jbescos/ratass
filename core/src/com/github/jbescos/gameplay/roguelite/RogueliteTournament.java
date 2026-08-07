package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RogueliteTournament {
    public static final int CHAMPIONSHIP_COUNT = 1;

    public Outcome resolve(
            int championshipNumber,
            List<Standing> standings) {
        return resolve(
                championshipNumber,
                standings,
                CHAMPIONSHIP_COUNT);
    }

    public Outcome resolve(
            int championshipNumber,
            List<Standing> standings,
            int championshipCount) {
        if (championshipNumber < 1
                || standings == null
                || standings.isEmpty()
                || championshipCount < 1) {
            throw new IllegalArgumentException(
                    "A championship requires at least one standing.");
        }

        List<Standing> ordered =
                new ArrayList<Standing>(standings);
        validateUniqueVehicles(ordered);
        Collections.sort(
                ordered,
                new Comparator<Standing>() {
                    @Override
                    public int compare(Standing left, Standing right) {
                        int points =
                                Integer.compare(
                                        right.getTotalPoints(),
                                        left.getTotalPoints());
                        if (points != 0) {
                            return points;
                        }
                        int finish =
                                compareFinishPosition(
                                        left.getLatestFinishPosition(),
                                        right.getLatestFinishPosition());
                        if (finish != 0) {
                            return finish;
                        }
                        return Integer.compare(
                                left.getVehicleId(),
                                right.getVehicleId());
                    }
                });

        return Outcome.winner(
                championshipNumber >= championshipCount,
                ordered.get(0).getVehicleId());
    }

    public boolean isFinalChampionship(int championshipNumber) {
        return isFinalChampionship(championshipNumber, CHAMPIONSHIP_COUNT);
    }

    public boolean isFinalChampionship(
            int championshipNumber,
            int championshipCount) {
        return championshipNumber >= Math.max(1, championshipCount);
    }

    public boolean isLosingPosition(
            int championshipNumber,
            int position,
            int competitorCount) {
        return isLosingPosition(
                championshipNumber,
                position,
                competitorCount,
                CHAMPIONSHIP_COUNT);
    }

    public boolean isLosingPosition(
            int championshipNumber,
            int position,
            int competitorCount,
            int championshipCount) {
        if (position <= 1 || position > competitorCount) {
            return false;
        }
        return isFinalChampionship(championshipNumber, championshipCount);
    }

    private void validateUniqueVehicles(List<Standing> standings) {
        Set<Integer> vehicleIds = new HashSet<Integer>();
        for (int i = 0; i < standings.size(); i++) {
            Standing standing = standings.get(i);
            if (standing == null
                    || standing.getVehicleId() < 0
                    || !vehicleIds.add(
                            Integer.valueOf(standing.getVehicleId()))) {
                throw new IllegalArgumentException(
                        "Championship standings must have unique vehicles.");
            }
        }
    }

    private static int compareFinishPosition(
            int leftPosition,
            int rightPosition) {
        if (leftPosition <= 0 || rightPosition <= 0) {
            if (leftPosition <= 0 && rightPosition <= 0) {
                return 0;
            }
            return leftPosition <= 0 ? 1 : -1;
        }
        return Integer.compare(leftPosition, rightPosition);
    }

    public static final class Standing {
        private final int vehicleId;
        private final int totalPoints;
        private final int latestFinishPosition;

        public Standing(
                int vehicleId,
                int totalPoints,
                int latestFinishPosition) {
            this.vehicleId = vehicleId;
            this.totalPoints = totalPoints;
            this.latestFinishPosition = latestFinishPosition;
        }

        public int getVehicleId() {
            return vehicleId;
        }

        public int getTotalPoints() {
            return totalPoints;
        }

        public int getLatestFinishPosition() {
            return latestFinishPosition;
        }
    }

    public static final class Outcome {
        private final boolean finalChampionship;
        private final int winningVehicleId;

        private Outcome(
                boolean finalChampionship,
                int winningVehicleId) {
            this.finalChampionship = finalChampionship;
            this.winningVehicleId = winningVehicleId;
        }

        private static Outcome winner(
                boolean finalChampionship,
                int winningVehicleId) {
            return new Outcome(finalChampionship, winningVehicleId);
        }

        public boolean isFinalChampionship() {
            return finalChampionship;
        }

        public int getWinningVehicleId() {
            return winningVehicleId;
        }
    }
}
