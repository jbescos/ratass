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
                drivers.all().size()
                        + RogueliteCardCatalog.all().size()
                        + RogueliteSetCatalog.allSets().size(),
                configuration.getAvailableChoices().size()
                        + configuration.getAvailableSets().size());

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
    public void sandboxCanStartWithTheRunsRandomTierOneDriver() {
        DriverProfileCatalog drivers = DriverProfileCatalog.fallback();
        RogueliteRun run = new RogueliteRun(13L, drivers);
        String startingDriver =
                run.getPlayerLoadout().getDriverProfileId();
        SandboxLoadoutConfiguration configuration =
                new SandboxLoadoutConfiguration(drivers);

        configuration.reset(drivers, startingDriver);

        assertEquals(1, drivers.getTier(startingDriver));
        assertEquals(
                startingDriver,
                configuration.getLoadout().getDriverProfileId());
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
    public void oneCardCanBeSelectedForEachOfTheFiveLoadoutSlots() {
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
                        findCard(configuration, RogueliteCardId.CORNER_FOCUS)));
        assertTrue(
                configuration.select(
                        findCard(configuration, RogueliteCardId.NITRO_PULSE)));
        assertTrue(
                configuration.select(
                        findCard(configuration, RogueliteCardId.DRAFT_MAGNET)));

        assertTrue(configuration.getLoadout().isFull());
        assertEquals(4, configuration.getLoadout().getModifications().size());
    }

    @Test
    public void setSelectionEquipsAndTogglesAllFourRecipeCards() {
        SandboxLoadoutConfiguration configuration =
                new SandboxLoadoutConfiguration(DriverProfileCatalog.fallback());
        RogueliteSetDefinition set = configuration.getAvailableSets().get(0);

        assertTrue(configuration.selectSet(0, set));
        assertTrue(configuration.isSetEquipped(0, set));
        for (RogueliteSlotType slotType : RogueliteSlotType.modificationSlots()) {
            assertEquals(
                    set.getRequiredCard(slotType),
                    configuration.getLoadout().get(slotType));
        }

        assertTrue(configuration.selectSet(0, set));
        assertFalse(configuration.isSetEquipped(0, set));
        for (RogueliteSlotType slotType : RogueliteSlotType.modificationSlots()) {
            assertNull(configuration.getLoadout().get(slotType));
        }
    }

    @Test
    public void selectionOnlyChangesTheTargetVehicleLoadout() {
        SandboxLoadoutConfiguration configuration =
                new SandboxLoadoutConfiguration(DriverProfileCatalog.fallback());
        RogueliteCardOffer clubTune =
                findCard(configuration, RogueliteCardId.CLUB_TUNE);

        assertTrue(configuration.select(2, clubTune));
        assertEquals(
                RogueliteCardId.CLUB_TUNE,
                configuration.getLoadout(2).get(RogueliteSlotType.TUNING));
        assertNull(configuration.getLoadout(0).get(RogueliteSlotType.TUNING));
        assertNull(configuration.getLoadout(1).get(RogueliteSlotType.TUNING));
        assertTrue(configuration.isEquipped(2, clubTune));
        assertFalse(configuration.isEquipped(0, clubTune));
    }

    @Test
    public void propagationCopiesTheSourceLoadoutWithoutSharingMutableState() {
        DriverProfileCatalog drivers = DriverProfileCatalog.fallback();
        SandboxLoadoutConfiguration configuration =
                new SandboxLoadoutConfiguration(drivers);
        DriverProfileMetadata fastest = drivers.all().get(0);
        RogueliteCardOffer fastestDriver =
                findDriver(configuration, fastest.getProfileId());
        RogueliteCardOffer clubTune =
                findCard(configuration, RogueliteCardId.CLUB_TUNE);
        RogueliteCardOffer sportTune =
                findCard(configuration, RogueliteCardId.SPORT_TUNE);

        assertTrue(configuration.select(2, fastestDriver));
        assertTrue(configuration.select(2, clubTune));
        configuration.propagateLoadout(2, 4);

        for (int vehicleId = 0; vehicleId < 4; vehicleId++) {
            assertEquals(
                    fastest.getProfileId(),
                    configuration.getLoadout(vehicleId).getDriverProfileId());
            assertEquals(
                    RogueliteCardId.CLUB_TUNE,
                    configuration.getLoadout(vehicleId).get(RogueliteSlotType.TUNING));
        }

        assertTrue(configuration.select(1, sportTune));
        assertEquals(
                RogueliteCardId.SPORT_TUNE,
                configuration.getLoadout(1).get(RogueliteSlotType.TUNING));
        assertEquals(
                RogueliteCardId.CLUB_TUNE,
                configuration.getLoadout(2).get(RogueliteSlotType.TUNING));
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
