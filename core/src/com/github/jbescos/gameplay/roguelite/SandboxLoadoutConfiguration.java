package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class SandboxLoadoutConfiguration {
    public enum ControlMode {
        MANUAL("Manual"),
        AUTOMATIC("Automatic");

        private final String displayName;

        ControlMode(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    private ControlMode controlMode;
    private String initialDriverProfileId;
    private final Map<Integer, RogueliteLoadout> loadoutsByVehicleId =
            new HashMap<Integer, RogueliteLoadout>();
    private List<RogueliteCardOffer> availableChoices;

    public SandboxLoadoutConfiguration(DriverProfileCatalog driverCatalog) {
        reset(driverCatalog);
    }

    public void reset(DriverProfileCatalog driverCatalog) {
        if (driverCatalog == null) {
            throw new IllegalArgumentException("Driver catalog is required.");
        }
        reset(driverCatalog, driverCatalog.getWorst().getProfileId());
    }

    public void reset(
            DriverProfileCatalog driverCatalog,
            String initialDriverProfileId) {
        if (driverCatalog == null) {
            throw new IllegalArgumentException("Driver catalog is required.");
        }
        if (driverCatalog.get(initialDriverProfileId) == null) {
            throw new IllegalArgumentException(
                    "Unknown initial driver profile: " + initialDriverProfileId);
        }
        controlMode = ControlMode.AUTOMATIC;
        this.initialDriverProfileId = initialDriverProfileId;
        loadoutsByVehicleId.clear();
        getLoadout(0);
        availableChoices = buildAvailableChoices(driverCatalog);
    }

    public ControlMode getControlMode() {
        return controlMode;
    }

    public boolean isAutomatic() {
        return controlMode == ControlMode.AUTOMATIC;
    }

    public void cycleControlMode() {
        controlMode = isAutomatic() ? ControlMode.MANUAL : ControlMode.AUTOMATIC;
    }

    public RogueliteLoadout getLoadout() {
        return getLoadout(0);
    }

    public RogueliteLoadout getLoadout(int vehicleId) {
        if (vehicleId < 0) {
            throw new IllegalArgumentException("Vehicle ID cannot be negative.");
        }
        RogueliteLoadout loadout = loadoutsByVehicleId.get(Integer.valueOf(vehicleId));
        if (loadout == null) {
            loadout = new RogueliteLoadout(initialDriverProfileId);
            loadoutsByVehicleId.put(Integer.valueOf(vehicleId), loadout);
        }
        return loadout;
    }

    public List<RogueliteCardOffer> getAvailableChoices() {
        return availableChoices;
    }

    public boolean select(RogueliteCardOffer choice) {
        return select(0, choice);
    }

    public boolean select(int vehicleId, RogueliteCardOffer choice) {
        if (choice == null || !containsChoice(choice.getOfferId())) {
            return false;
        }
        RogueliteLoadout loadout = getLoadout(vehicleId);
        if (choice.isDriver()) {
            String profileId = choice.getDriver().getProfileId();
            if (profileId.equals(loadout.getDriverProfileId())) {
                return false;
            }
            loadout.setDriverProfileId(profileId);
            return true;
        }

        RogueliteCardId cardId = choice.getCard().getId();
        RogueliteSlotType slotType = choice.getSlotType();
        if (cardId == loadout.get(slotType)) {
            return loadout.unequip(slotType);
        }
        return loadout.equip(cardId);
    }

    public boolean isEquipped(RogueliteCardOffer choice) {
        return isEquipped(0, choice);
    }

    public boolean isEquipped(int vehicleId, RogueliteCardOffer choice) {
        if (choice == null) {
            return false;
        }
        RogueliteLoadout loadout = getLoadout(vehicleId);
        if (choice.isDriver()) {
            return choice.getDriver().getProfileId().equals(loadout.getDriverProfileId());
        }
        return choice.getCard().getId() == loadout.get(choice.getSlotType());
    }

    public void propagateLoadout(int sourceVehicleId, int vehicleCount) {
        if (vehicleCount < 0) {
            throw new IllegalArgumentException("Vehicle count cannot be negative.");
        }
        RogueliteLoadout source = getLoadout(sourceVehicleId);
        for (int vehicleId = 0; vehicleId < vehicleCount; vehicleId++) {
            if (vehicleId != sourceVehicleId) {
                loadoutsByVehicleId.put(
                        Integer.valueOf(vehicleId),
                        copyLoadout(source));
            }
        }
    }

    private boolean containsChoice(String offerId) {
        for (int i = 0; i < availableChoices.size(); i++) {
            if (availableChoices.get(i).getOfferId().equals(offerId)) {
                return true;
            }
        }
        return false;
    }

    private static RogueliteLoadout copyLoadout(RogueliteLoadout source) {
        RogueliteLoadout copy = new RogueliteLoadout(source.getDriverProfileId());
        List<RogueliteCardId> modifications = source.getModifications();
        for (int i = 0; i < modifications.size(); i++) {
            copy.equip(modifications.get(i));
        }
        return copy;
    }

    private static List<RogueliteCardOffer> buildAvailableChoices(
            DriverProfileCatalog driverCatalog) {
        List<RogueliteCardOffer> choices = new ArrayList<RogueliteCardOffer>();
        List<DriverProfileMetadata> drivers = driverCatalog.all();
        for (int i = 0; i < drivers.size(); i++) {
            DriverProfileMetadata driver = drivers.get(i);
            choices.add(
                    RogueliteCardOffer.driver(
                            driver,
                            driverCatalog.getTier(driver.getProfileId())));
        }

        List<RogueliteCardDefinition> cards = RogueliteCardCatalog.all();
        List<RogueliteSlotType> slots = RogueliteSlotType.modificationSlots();
        for (int slotIndex = 0; slotIndex < slots.size(); slotIndex++) {
            RogueliteSlotType slotType = slots.get(slotIndex);
            for (int tier = 1; tier <= RogueliteCardCatalog.MAX_CARD_TIER; tier++) {
                for (int cardIndex = 0; cardIndex < cards.size(); cardIndex++) {
                    RogueliteCardDefinition card = cards.get(cardIndex);
                    if (card.getSlotType() == slotType && card.getTier() == tier) {
                        choices.add(RogueliteCardOffer.modification(card));
                    }
                }
            }
        }
        return Collections.unmodifiableList(choices);
    }
}
