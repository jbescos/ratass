package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public final class SandboxLoadoutConfigurationTest {
    @Test
    public void startsAutomaticWithEveryDriverAndModificationAvailable() {
        DriverProfileCatalog drivers = DriverProfileCatalog.fallback();
        SandboxLoadoutConfiguration configuration =
                new SandboxLoadoutConfiguration(drivers);

        assertTrue(configuration.isAutomatic());
        assertEquals(
                drivers.getWorst().getProfileId(),
                configuration.getLoadout().getDriverProfileId());
        assertEquals(
                drivers.all().size() + RogueliteCardCatalog.all().size(),
                configuration.getAvailableChoices().size());

        configuration.cycleControlMode();
        assertFalse(configuration.isAutomatic());
        configuration.cycleControlMode();
        assertTrue(configuration.isAutomatic());
    }

    @Test
    public void driverSelectionChangesTheAutomaticPolicy() {
        DriverProfileCatalog drivers = DriverProfileCatalog.fallback();
        SandboxLoadoutConfiguration configuration =
                new SandboxLoadoutConfiguration(drivers);
        DriverProfileMetadata fastest = drivers.all().get(0);
        RogueliteCardOffer offer = findDriver(configuration, fastest.getProfileId());

        assertTrue(configuration.select(offer));
        assertEquals(fastest.getProfileId(), configuration.getLoadout().getDriverProfileId());
        assertTrue(configuration.isEquipped(offer));
        assertFalse(configuration.select(offer));
    }

    @Test
    public void modificationSelectionReplacesAndTogglesItsSlot() {
        SandboxLoadoutConfiguration configuration =
                new SandboxLoadoutConfiguration(DriverProfileCatalog.fallback());
        RogueliteCardOffer clubTune =
                findCard(configuration, RogueliteCardId.CLUB_TUNE);
        RogueliteCardOffer sportTune =
                findCard(configuration, RogueliteCardId.SPORT_TUNE);

        assertTrue(configuration.select(clubTune));
        assertTrue(configuration.isEquipped(clubTune));
        assertTrue(configuration.select(sportTune));
        assertFalse(configuration.isEquipped(clubTune));
        assertTrue(configuration.isEquipped(sportTune));

        assertTrue(configuration.select(sportTune));
        assertNull(configuration.getLoadout().get(RogueliteSlotType.TUNING));
    }

    @Test
    public void oneCardCanBeSelectedForEachOfTheFourLoadoutSlots() {
        DriverProfileCatalog drivers = DriverProfileCatalog.fallback();
        SandboxLoadoutConfiguration configuration =
                new SandboxLoadoutConfiguration(drivers);
        DriverProfileMetadata replacementDriver = drivers.all().get(0);

        assertTrue(
                configuration.select(
                        findDriver(
                                configuration,
                                replacementDriver.getProfileId())));
        assertTrue(
                configuration.select(
                        findCard(configuration, RogueliteCardId.CLUB_TUNE)));
        assertTrue(
                configuration.select(
                        findCard(configuration, RogueliteCardId.CORNER_EXIT)));
        assertTrue(
                configuration.select(
                        findCard(configuration, RogueliteCardId.NITRO_PULSE)));

        assertTrue(configuration.getLoadout().isFull());
        assertEquals(3, configuration.getLoadout().getModifications().size());
    }

    private static RogueliteCardOffer findDriver(
            SandboxLoadoutConfiguration configuration,
            String profileId) {
        for (RogueliteCardOffer offer : configuration.getAvailableChoices()) {
            if (offer.isDriver() && offer.getDriver().getProfileId().equals(profileId)) {
                return offer;
            }
        }
        throw new AssertionError("Missing driver " + profileId);
    }

    private static RogueliteCardOffer findCard(
            SandboxLoadoutConfiguration configuration,
            RogueliteCardId cardId) {
        for (RogueliteCardOffer offer : configuration.getAvailableChoices()) {
            if (!offer.isDriver() && offer.getCard().getId() == cardId) {
                return offer;
            }
        }
        throw new AssertionError("Missing card " + cardId);
    }
}
