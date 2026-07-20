package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class RogueliteRun {
    private static final float UPGRADE_OFFER_CHANCE = 0.45f;
    private static final int MAX_OFFERS_WITHOUT_UPGRADE = 2;

    private final RogueliteCardInventory playerInventory = new RogueliteCardInventory();
    private final Map<Integer, RogueliteCardInventory> rivalInventories =
            new LinkedHashMap<Integer, RogueliteCardInventory>();
    private final Random random;
    private int offersSinceUpgrade;

    public RogueliteRun() {
        this(new Random());
    }

    public RogueliteRun(long seed) {
        this(new Random(seed));
    }

    private RogueliteRun(Random random) {
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
        Collections.shuffle(selected, random);

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

    private RogueliteCardDefinition takeRandom(List<RogueliteCardDefinition> cards) {
        return cards.remove(random.nextInt(cards.size()));
    }
}
