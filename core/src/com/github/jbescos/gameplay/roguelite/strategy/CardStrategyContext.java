package com.github.jbescos.gameplay.roguelite.strategy;

import com.github.jbescos.gameplay.roguelite.DriverProfileMetadata;
import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import com.github.jbescos.gameplay.roguelite.RogueliteCardOffer;
import com.github.jbescos.gameplay.roguelite.RogueliteLoadout;
import com.github.jbescos.gameplay.roguelite.RogueliteSlotType;
import com.github.jbescos.gameplay.roguelite.RogueliteSetId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Race and championship state that can affect the long-term value of a card. */
public final class CardStrategyContext {
    private static final CardStrategyContext EMPTY = new CardStrategyContext(
            0, 0, 0, 0, 0, 0, 0,
            Collections.<Opponent>emptyList(),
            Collections.<RogueliteSetId>emptyList());

    private final int circuitIndex;
    private final int circuitCount;
    private final int lap;
    private final int lapCount;
    private final int racePosition;
    private final int championshipPosition;
    private final int remainingCircuits;
    private final List<Opponent> opponents;
    private final List<RogueliteSetId> enabledSetIds;

    public CardStrategyContext(
            int circuitIndex,
            int circuitCount,
            int lap,
            int lapCount,
            int racePosition,
            int championshipPosition,
            int remainingCircuits,
            List<Opponent> opponents) {
        this(
                circuitIndex,
                circuitCount,
                lap,
                lapCount,
                racePosition,
                championshipPosition,
                remainingCircuits,
                opponents,
                Collections.<RogueliteSetId>emptyList());
    }

    public CardStrategyContext(
            int circuitIndex,
            int circuitCount,
            int lap,
            int lapCount,
            int racePosition,
            int championshipPosition,
            int remainingCircuits,
            List<Opponent> opponents,
            List<RogueliteSetId> enabledSetIds) {
        this.circuitIndex = Math.max(0, circuitIndex);
        this.circuitCount = Math.max(0, circuitCount);
        this.lap = Math.max(0, lap);
        this.lapCount = Math.max(0, lapCount);
        this.racePosition = Math.max(0, racePosition);
        this.championshipPosition = Math.max(0, championshipPosition);
        this.remainingCircuits = Math.max(0, remainingCircuits);
        this.opponents = opponents == null
                ? Collections.<Opponent>emptyList()
                : Collections.unmodifiableList(new ArrayList<Opponent>(opponents));
        this.enabledSetIds = enabledSetIds == null
                ? Collections.<RogueliteSetId>emptyList()
                : Collections.unmodifiableList(
                        new ArrayList<RogueliteSetId>(enabledSetIds));
    }

    public static CardStrategyContext empty() {
        return EMPTY;
    }

    public int getCircuitIndex() {
        return circuitIndex;
    }

    public int getCircuitCount() {
        return circuitCount;
    }

    public int getLap() {
        return lap;
    }

    public int getLapCount() {
        return lapCount;
    }

    public int getRacePosition() {
        return racePosition;
    }

    public int getChampionshipPosition() {
        return championshipPosition;
    }

    public int getRemainingCircuits() {
        return remainingCircuits;
    }

    public List<Opponent> getOpponents() {
        return opponents;
    }

    public List<RogueliteSetId> getEnabledSetIds() {
        return enabledSetIds;
    }

    public float equippedCandidateOverlap(RogueliteCardOffer offer) {
        if (offer == null
                || opponents.isEmpty()
                || offer.isDriver()
                || (offer.getSlotType() != RogueliteSlotType.POWERUP
                        && offer.getSlotType() != RogueliteSlotType.REVENGE)) {
            return 0f;
        }
        return equippedCardOverlap(offer.getSlotType(), offer.getCard().getId());
    }

    float equippedCardOverlap(RogueliteSlotType slot, RogueliteCardId cardId) {
        if ((slot != RogueliteSlotType.POWERUP && slot != RogueliteSlotType.REVENGE)
                || cardId == null
                || opponents.isEmpty()) {
            return 0f;
        }
        int matches = 0;
        for (Opponent opponent : opponents) {
            matches += cardId == opponent.getCard(slot) ? 1 : 0;
        }
        return matches / (float) opponents.size();
    }

    public static final class Opponent {
        private final int level;
        private final int racePosition;
        private final int championshipPosition;
        private final DriverProfileMetadata driver;
        private final Map<RogueliteSlotType, RogueliteCardId> cards;

        public Opponent(
                int level,
                int racePosition,
                int championshipPosition,
                DriverProfileMetadata driver) {
            this(level, racePosition, championshipPosition, driver, null);
        }

        public Opponent(
                int level,
                int racePosition,
                int championshipPosition,
                DriverProfileMetadata driver,
                RogueliteLoadout loadout) {
            this.level = Math.max(1, level);
            this.racePosition = Math.max(0, racePosition);
            this.championshipPosition = Math.max(0, championshipPosition);
            this.driver = driver;
            cards = new EnumMap<RogueliteSlotType, RogueliteCardId>(RogueliteSlotType.class);
            if (loadout != null) {
                RogueliteSlotType[] sharedSlots = {
                    RogueliteSlotType.POWERUP,
                    RogueliteSlotType.REVENGE
                };
                for (RogueliteSlotType slot : sharedSlots) {
                    RogueliteCardId cardId = loadout.get(slot);
                    if (cardId != null) {
                        cards.put(slot, cardId);
                    }
                }
            }
        }

        public int getLevel() {
            return level;
        }

        public int getRacePosition() {
            return racePosition;
        }

        public int getChampionshipPosition() {
            return championshipPosition;
        }

        public DriverProfileMetadata getDriver() {
            return driver;
        }

        public RogueliteCardId getCard(RogueliteSlotType slot) {
            return cards.get(slot);
        }

    }
}
