package com.github.jbescos.gameplay.roguelite.strategy;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable live-race values used when a rival makes a strategic card decision. */
public final class CardStrategyRaceState {
    private static final CardStrategyRaceState EMPTY = new Builder().build();

    private final int circuitIndex;
    private final int circuitCount;
    private final int lapCount;
    private final int playerVehicleId;
    private final Map<Integer, Competitor> competitors;

    private CardStrategyRaceState(Builder builder) {
        circuitIndex = builder.circuitIndex;
        circuitCount = builder.circuitCount;
        lapCount = builder.lapCount;
        playerVehicleId = builder.playerVehicleId;
        competitors = Collections.unmodifiableMap(
                new LinkedHashMap<Integer, Competitor>(builder.competitors));
    }

    public static CardStrategyRaceState empty() {
        return EMPTY;
    }

    public int getCircuitIndex() {
        return circuitIndex;
    }

    public int getCircuitCount() {
        return circuitCount;
    }

    public int getLapCount() {
        return lapCount;
    }

    public Competitor get(int vehicleId) {
        return competitors.get(Integer.valueOf(vehicleId));
    }

    public Competitor getPlayer() {
        return get(playerVehicleId);
    }

    public static final class Builder {
        private int circuitIndex;
        private int circuitCount;
        private int lapCount;
        private int playerVehicleId = -1;
        private final Map<Integer, Competitor> competitors =
                new LinkedHashMap<Integer, Competitor>();

        public Builder race(
                int circuitIndex,
                int circuitCount,
                int lapCount) {
            this.circuitIndex = Math.max(0, circuitIndex);
            this.circuitCount = Math.max(0, circuitCount);
            this.lapCount = Math.max(0, lapCount);
            return this;
        }

        public Builder competitor(
                int vehicleId,
                boolean player,
                int lap,
                int racePosition,
                int championshipPosition) {
            competitors.put(
                    Integer.valueOf(vehicleId),
                    new Competitor(lap, racePosition, championshipPosition));
            if (player) {
                playerVehicleId = vehicleId;
            }
            return this;
        }

        public CardStrategyRaceState build() {
            return new CardStrategyRaceState(this);
        }
    }

    public static final class Competitor {
        private final int lap;
        private final int racePosition;
        private final int championshipPosition;

        private Competitor(int lap, int racePosition, int championshipPosition) {
            this.lap = Math.max(0, lap);
            this.racePosition = Math.max(0, racePosition);
            this.championshipPosition = Math.max(0, championshipPosition);
        }

        public int getLap() {
            return lap;
        }

        public int getRacePosition() {
            return racePosition;
        }

        public int getChampionshipPosition() {
            return championshipPosition;
        }
    }
}
