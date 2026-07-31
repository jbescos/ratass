package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private RogueliteLoadout loadout;
    private List<RogueliteCardOffer> availableChoices;

    public SandboxLoadoutConfiguration(DriverProfileCatalog driverCatalog) {
        reset(driverCatalog);
    }

    public void reset(DriverProfileCatalog driverCatalog) {
        if (driverCatalog == null) {
            throw new IllegalArgumentException("Driver catalog is required.");
        }
        controlMode = ControlMode.AUTOMATIC;
        loadout = new RogueliteLoadout(driverCatalog.getWorst().getProfileId());
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
        return loadout;
    }

    public List<RogueliteCardOffer> getAvailableChoices() {
        return availableChoices;
    }

    public boolean select(RogueliteCardOffer choice) {
        if (choice == null || !containsChoice(choice.getOfferId())) {
            return false;
        }
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
        if (choice == null) {
            return false;
        }
        if (choice.isDriver()) {
            return choice.getDriver().getProfileId().equals(loadout.getDriverProfileId());
        }
        return choice.getCard().getId() == loadout.get(choice.getSlotType());
    }

    private boolean containsChoice(String offerId) {
        for (int i = 0; i < availableChoices.size(); i++) {
            if (availableChoices.get(i).getOfferId().equals(offerId)) {
                return true;
            }
        }
        return false;
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
            for (int tier = 1; tier <= DriverProfileCatalog.MAX_TIER; tier++) {
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
