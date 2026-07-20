package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class RogueliteCardInventory {
    private final Map<RogueliteCardId, Integer> levels =
            new EnumMap<RogueliteCardId, Integer>(RogueliteCardId.class);
    private int selections;

    public void clear() {
        levels.clear();
        selections = 0;
    }

    public int getLevel(RogueliteCardId id) {
        Integer level = levels.get(id);
        return level == null ? -1 : level.intValue();
    }

    public boolean has(RogueliteCardId id) {
        return getLevel(id) >= 0;
    }

    public boolean canAcquire(RogueliteCardDefinition card) {
        return card != null && getLevel(card.getId()) < card.getMaxLevel();
    }

    public boolean acquire(RogueliteCardDefinition card) {
        if (!canAcquire(card)) {
            return false;
        }
        levels.put(card.getId(), Integer.valueOf(getLevel(card.getId()) + 1));
        selections++;
        return true;
    }

    public int getSelectionCount() {
        return selections;
    }

    public List<RogueliteCardDefinition> getOwnedCards() {
        List<RogueliteCardDefinition> owned = new ArrayList<RogueliteCardDefinition>();
        List<RogueliteCardDefinition> catalog = RogueliteCardCatalog.all();
        for (int i = 0; i < catalog.size(); i++) {
            RogueliteCardDefinition card = catalog.get(i);
            if (has(card.getId())) {
                owned.add(card);
            }
        }
        return owned;
    }

    public List<RogueliteCardDefinition> getEligibleCards() {
        List<RogueliteCardDefinition> eligible = new ArrayList<RogueliteCardDefinition>();
        List<RogueliteCardDefinition> catalog = RogueliteCardCatalog.all();
        for (int i = 0; i < catalog.size(); i++) {
            RogueliteCardDefinition card = catalog.get(i);
            if (canAcquire(card)) {
                eligible.add(card);
            }
        }
        return eligible;
    }
}
