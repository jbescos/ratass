package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RogueliteTournament {
    public static final int CHAMPIONSHIP_COUNT = 3;
    public static final int ELIMINATIONS_PER_CHAMPIONSHIP = 3;
    public static final int FINALIST_COUNT = 4;

    public Outcome resolve(
            int championshipNumber,
            List<Standing> standings) {
        if (championshipNumber < 1
                || standings == null
                || standings.isEmpty()) {
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

        if (championshipNumber >= CHAMPIONSHIP_COUNT) {
            return Outcome.finalWinner(ordered.get(0).getVehicleId());
        }

        int eliminationCount =
                Math.min(
                        ELIMINATIONS_PER_CHAMPIONSHIP,
                        Math.max(0, ordered.size() - FINALIST_COUNT));
        List<Integer> eliminatedVehicleIds =
                new ArrayList<Integer>(eliminationCount);
        for (int i = 0; i < eliminationCount; i++) {
            eliminatedVehicleIds.add(
                    Integer.valueOf(
                            ordered.get(ordered.size() - 1 - i)
                                    .getVehicleId()));
        }
        return Outcome.eliminations(eliminatedVehicleIds);
    }

    public boolean isFinalChampionship(int championshipNumber) {
        return championshipNumber >= CHAMPIONSHIP_COUNT;
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
        private final List<Integer> eliminatedVehicleIds;

        private Outcome(
                boolean finalChampionship,
                int winningVehicleId,
                List<Integer> eliminatedVehicleIds) {
            this.finalChampionship = finalChampionship;
            this.winningVehicleId = winningVehicleId;
            this.eliminatedVehicleIds =
                    Collections.unmodifiableList(
                            new ArrayList<Integer>(
                                    eliminatedVehicleIds));
        }

        private static Outcome finalWinner(int winningVehicleId) {
            return new Outcome(
                    true,
                    winningVehicleId,
                    Collections.<Integer>emptyList());
        }

        private static Outcome eliminations(
                List<Integer> eliminatedVehicleIds) {
            return new Outcome(false, -1, eliminatedVehicleIds);
        }

        public boolean isFinalChampionship() {
            return finalChampionship;
        }

        public int getWinningVehicleId() {
            return winningVehicleId;
        }

        public List<Integer> getEliminatedVehicleIds() {
            return eliminatedVehicleIds;
        }
    }
}
