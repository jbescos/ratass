package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RogueliteLoadout {
    public static final int MODIFICATION_SLOT_COUNT = 3;

    private String driverProfileId;
    private final List<RogueliteCardId> modifications =
            new ArrayList<RogueliteCardId>(MODIFICATION_SLOT_COUNT);
    private final List<RogueliteCardId> readOnlyModifications =
            Collections.unmodifiableList(modifications);

    public RogueliteLoadout(String driverProfileId) {
        setDriverProfileId(driverProfileId);
    }

    public String getDriverProfileId() {
        return driverProfileId;
    }

    public void setDriverProfileId(String driverProfileId) {
        if (driverProfileId == null || driverProfileId.trim().length() == 0) {
            throw new IllegalArgumentException("Driver profile ID is required.");
        }
        this.driverProfileId = driverProfileId.trim();
    }

    public List<RogueliteCardId> getModifications() {
        return readOnlyModifications;
    }

    public boolean has(RogueliteCardId cardId) {
        return modifications.contains(cardId);
    }

    public boolean isFull() {
        return modifications.size() >= MODIFICATION_SLOT_COUNT;
    }

    public boolean equip(RogueliteCardId cardId, int replacementSlot) {
        if (cardId == null || has(cardId)) {
            return false;
        }
        if (!isFull()) {
            modifications.add(cardId);
            return true;
        }
        if (replacementSlot < 0 || replacementSlot >= modifications.size()) {
            return false;
        }
        modifications.set(replacementSlot, cardId);
        return true;
    }

    void restoreModification(RogueliteCardId cardId) {
        if (cardId == null || has(cardId) || isFull()) {
            throw new IllegalArgumentException("Invalid roguelite modification.");
        }
        modifications.add(cardId);
    }

}
