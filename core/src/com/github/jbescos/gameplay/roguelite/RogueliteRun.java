package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RogueliteRun {
    private static final float UPGRADE_OFFER_CHANCE = 0.45f;
    private static final int MAX_OFFERS_WITHOUT_UPGRADE = 2;

    private final RogueliteCardInventory playerInventory = new RogueliteCardInventory();
    private final Map<Integer, RogueliteCardInventory> rivalInventories =
            new LinkedHashMap<Integer, RogueliteCardInventory>();
    private final RogueliteRandom random;
    private int offersSinceUpgrade;

    public RogueliteRun() {
        this(new RogueliteRandom());
    }

    public RogueliteRun(long seed) {
        this(new RogueliteRandom(seed));
    }

    private RogueliteRun(RogueliteRandom random) {
        this.random = random;
    }

    public void reset() {
        playerInventory.clear();
        rivalInventories.clear();
        offersSinceUpgrade = 0;
    }

    public RogueliteCardInventory getPlayerInventory() {
        return playerInventory;
    }

    public RogueliteCardInventory getRivalInventory(int vehicleId) {
        Integer key = Integer.valueOf(vehicleId);
        RogueliteCardInventory inventory = rivalInventories.get(key);
        if (inventory == null) {
            inventory = new RogueliteCardInventory();
            rivalInventories.put(key, inventory);
        }
        return inventory;
    }

    public List<RogueliteCardOffer> createOffers(int count) {
        if (count <= 0) {
            return Collections.emptyList();
        }

        List<RogueliteCardDefinition> eligible = playerInventory.getEligibleCards();
        List<RogueliteCardDefinition> upgrades = new ArrayList<RogueliteCardDefinition>();
        for (int i = 0; i < eligible.size(); i++) {
            RogueliteCardDefinition card = eligible.get(i);
            if (playerInventory.has(card.getId())) {
                upgrades.add(card);
            }
        }

        List<RogueliteCardDefinition> selected = new ArrayList<RogueliteCardDefinition>();
        boolean includeUpgrade =
                !upgrades.isEmpty()
                        && (offersSinceUpgrade >= MAX_OFFERS_WITHOUT_UPGRADE
                                || random.nextFloat() < UPGRADE_OFFER_CHANCE);
        if (includeUpgrade) {
            RogueliteCardDefinition upgrade = takeRandom(upgrades);
            selected.add(upgrade);
            eligible.remove(upgrade);
        }
        while (selected.size() < count && !eligible.isEmpty()) {
            selected.add(takeRandom(eligible));
        }
        shuffle(selected);

        boolean upgradeOffered = false;
        List<RogueliteCardOffer> offers = new ArrayList<RogueliteCardOffer>(selected.size());
        for (int i = 0; i < selected.size(); i++) {
            RogueliteCardDefinition card = selected.get(i);
            int targetLevel = playerInventory.getLevel(card.getId()) + 1;
            offers.add(new RogueliteCardOffer(card, targetLevel));
            upgradeOffered |= targetLevel > 0;
        }
        offersSinceUpgrade = upgradeOffered ? 0 : offersSinceUpgrade + 1;
        return Collections.unmodifiableList(offers);
    }

    public boolean select(RogueliteCardOffer offer) {
        if (offer == null) {
            return false;
        }
        int expectedLevel = playerInventory.getLevel(offer.getCard().getId()) + 1;
        return expectedLevel == offer.getTargetLevel()
                && playerInventory.acquire(offer.getCard());
    }

    public void advanceRivals(Iterable<Integer> vehicleIds) {
        if (vehicleIds == null) {
            return;
        }
        for (Integer vehicleId : vehicleIds) {
            if (vehicleId == null) {
                continue;
            }
            RogueliteCardInventory inventory = getRivalInventory(vehicleId.intValue());
            List<RogueliteCardDefinition> eligible = inventory.getEligibleCards();
            if (!eligible.isEmpty()) {
                inventory.acquire(takeRandom(eligible));
            }
        }
    }

    public Snapshot snapshot() {
        Snapshot snapshot = new Snapshot();
        snapshot.randomState = random.getState();
        snapshot.offersSinceUpgrade = offersSinceUpgrade;
        snapshot.player = snapshotInventory(playerInventory);
        for (Map.Entry<Integer, RogueliteCardInventory> entry : rivalInventories.entrySet()) {
            RivalSnapshot rival = new RivalSnapshot();
            rival.vehicleId = entry.getKey().intValue();
            rival.inventory = snapshotInventory(entry.getValue());
            snapshot.rivals.add(rival);
        }
        return snapshot;
    }

    public boolean restore(Snapshot snapshot) {
        if (snapshot == null
                || snapshot.player == null
                || snapshot.offersSinceUpgrade < 0) {
            return false;
        }

        RogueliteCardInventory restoredPlayer = restoreInventory(snapshot.player);
        if (restoredPlayer == null) {
            return false;
        }
        Map<Integer, RogueliteCardInventory> restoredRivals =
                new LinkedHashMap<Integer, RogueliteCardInventory>();
        if (snapshot.rivals != null) {
            for (int i = 0; i < snapshot.rivals.size(); i++) {
                RivalSnapshot rival = snapshot.rivals.get(i);
                if (rival == null || rival.vehicleId < 0 || rival.inventory == null) {
                    return false;
                }
                Integer vehicleId = Integer.valueOf(rival.vehicleId);
                RogueliteCardInventory inventory = restoreInventory(rival.inventory);
                if (inventory == null || restoredRivals.put(vehicleId, inventory) != null) {
                    return false;
                }
            }
        }

        playerInventory.copyFrom(restoredPlayer);
        rivalInventories.clear();
        rivalInventories.putAll(restoredRivals);
        offersSinceUpgrade = snapshot.offersSinceUpgrade;
        random.setState(snapshot.randomState);
        return true;
    }

    public List<RogueliteCardOffer> restoreOffers(Iterable<String> cardIds) {
        if (cardIds == null) {
            return Collections.emptyList();
        }
        List<RogueliteCardOffer> offers = new ArrayList<RogueliteCardOffer>();
        for (String cardId : cardIds) {
            RogueliteCardId id;
            try {
                id = RogueliteCardId.valueOf(cardId);
            } catch (RuntimeException exception) {
                return Collections.emptyList();
            }
            RogueliteCardDefinition card = RogueliteCardCatalog.get(id);
            if (!playerInventory.canAcquire(card)) {
                return Collections.emptyList();
            }
            offers.add(new RogueliteCardOffer(card, playerInventory.getLevel(id) + 1));
        }
        return Collections.unmodifiableList(offers);
    }

    private RogueliteCardDefinition takeRandom(List<RogueliteCardDefinition> cards) {
        return cards.remove(random.nextInt(cards.size()));
    }

    private void shuffle(List<RogueliteCardDefinition> cards) {
        for (int i = cards.size() - 1; i > 0; i--) {
            int swapIndex = random.nextInt(i + 1);
            RogueliteCardDefinition value = cards.get(i);
            cards.set(i, cards.get(swapIndex));
            cards.set(swapIndex, value);
        }
    }

    private static InventorySnapshot snapshotInventory(RogueliteCardInventory inventory) {
        InventorySnapshot snapshot = new InventorySnapshot();
        List<RogueliteCardDefinition> cards = RogueliteCardCatalog.all();
        for (int i = 0; i < cards.size(); i++) {
            RogueliteCardDefinition card = cards.get(i);
            int level = inventory.getLevel(card.getId());
            if (level < 0) {
                continue;
            }
            CardLevelSnapshot cardLevel = new CardLevelSnapshot();
            cardLevel.cardId = card.getId().name();
            cardLevel.level = level;
            snapshot.cards.add(cardLevel);
        }
        return snapshot;
    }

    private static RogueliteCardInventory restoreInventory(InventorySnapshot snapshot) {
        if (snapshot == null || snapshot.cards == null) {
            return null;
        }
        RogueliteCardInventory inventory = new RogueliteCardInventory();
        Map<RogueliteCardId, Boolean> restored =
                new LinkedHashMap<RogueliteCardId, Boolean>();
        for (int i = 0; i < snapshot.cards.size(); i++) {
            CardLevelSnapshot cardLevel = snapshot.cards.get(i);
            if (cardLevel == null || cardLevel.cardId == null) {
                return null;
            }
            RogueliteCardId id;
            try {
                id = RogueliteCardId.valueOf(cardLevel.cardId);
            } catch (RuntimeException exception) {
                return null;
            }
            RogueliteCardDefinition card = RogueliteCardCatalog.get(id);
            if (cardLevel.level < 0
                    || cardLevel.level > card.getMaxLevel()
                    || restored.put(id, Boolean.TRUE) != null) {
                return null;
            }
            inventory.restoreLevel(card, cardLevel.level);
        }
        return inventory;
    }

    public static final class Snapshot {
        public int randomState;
        public int offersSinceUpgrade;
        public InventorySnapshot player = new InventorySnapshot();
        public List<RivalSnapshot> rivals = new ArrayList<RivalSnapshot>();
    }

    public static final class InventorySnapshot {
        public List<CardLevelSnapshot> cards = new ArrayList<CardLevelSnapshot>();
    }

    public static final class CardLevelSnapshot {
        public String cardId = "";
        public int level;
    }

    public static final class RivalSnapshot {
        public int vehicleId;
        public InventorySnapshot inventory = new InventorySnapshot();
    }
}
