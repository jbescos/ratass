package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class RogueliteLoadout {
    public static final int MODIFICATION_SLOT_COUNT =
            RogueliteSlotType.modificationSlots().size();

    private String driverProfileId;
    private final Map<RogueliteSlotType, RogueliteCardId> modificationsBySlot =
            new EnumMap<RogueliteSlotType, RogueliteCardId>(RogueliteSlotType.class);
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

    public RogueliteCardId get(RogueliteSlotType slotType) {
        if (slotType == null || slotType.isDriver()) {
            return null;
        }
        return modificationsBySlot.get(slotType);
    }

    public boolean hasCardIn(RogueliteSlotType slotType) {
        return get(slotType) != null;
    }

    public boolean isFull() {
        return modificationsBySlot.size() >= MODIFICATION_SLOT_COUNT;
    }

    public boolean equip(RogueliteCardId cardId) {
        if (cardId == null || has(cardId)) {
            return false;
        }
        RogueliteSlotType slotType =
                RogueliteCardCatalog.get(cardId).getSlotType();
        modificationsBySlot.put(slotType, cardId);
        rebuildModificationList();
        return true;
    }

    public boolean unequip(RogueliteSlotType slotType) {
        if (slotType == null || slotType.isDriver()) {
            return false;
        }
        if (modificationsBySlot.remove(slotType) == null) {
            return false;
        }
        rebuildModificationList();
        return true;
    }

    void restoreModification(RogueliteCardId cardId) {
        if (cardId == null) {
            throw new IllegalArgumentException("Invalid roguelite modification.");
        }
        RogueliteCardDefinition incoming =
                RogueliteCardCatalog.get(cardId);
        RogueliteCardId existingId =
                modificationsBySlot.get(incoming.getSlotType());
        if (existingId != null
                && RogueliteCardCatalog.get(existingId).getTier()
                        > incoming.getTier()) {
            return;
        }
        modificationsBySlot.put(incoming.getSlotType(), cardId);
        rebuildModificationList();
    }

    private void rebuildModificationList() {
        modifications.clear();
        List<RogueliteSlotType> slots =
                RogueliteSlotType.modificationSlots();
        for (int i = 0; i < slots.size(); i++) {
            RogueliteCardId cardId = modificationsBySlot.get(slots.get(i));
            if (cardId != null) {
                modifications.add(cardId);
            }
        }
    }
}
