package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class RogueliteCarUpgradesTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void nullInventoryKeepsEveryModifierNeutral() {
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        upgrades.configure(null);

        assertFalse(upgrades.isEnabled());
        assertEquals(1f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getMaxSpeedMultiplier(), EPSILON);
        assertEquals(1f, upgrades.getDragMultiplier(), EPSILON);
        assertEquals(0.58f, upgrades.adjustSurfaceGrip(0.58f), EPSILON);
        assertTrue(upgrades.getActiveCardIds().isEmpty());
    }

    @Test
    public void everyCatalogCardHasARuntimeEffectRegistration() {
        RogueliteCardInventory inventory = new RogueliteCardInventory();
        List<RogueliteCardDefinition> cards = RogueliteCardCatalog.all();
        for (int i = 0; i < cards.size(); i++) {
            assertTrue(inventory.acquire(cards.get(i)));
        }

        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        upgrades.configure(inventory);

        assertTrue(upgrades.isEnabled());
        assertTrue(upgrades.hasOvertakeInjector());
    }

    @Test
    public void turboAndAerodynamicsSynergyRaisesRedlineAndReducesDrag() {
        RogueliteCardInventory inventory = new RogueliteCardInventory();
        acquire(inventory, RogueliteCardId.TURBOCHARGER, 3);
        acquire(inventory, RogueliteCardId.AERODYNAMIC_KIT, 1);
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        upgrades.configure(inventory);

        upgrades.update(
                1.6f,
                1f,
                true,
                false,
                false,
                0f,
                0.8f,
                0f,
                100f,
                2f);

        assertEquals(1.14f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.055f, upgrades.getMaxSpeedMultiplier(), EPSILON);
        assertEquals(0.95f, upgrades.getDragMultiplier(), EPSILON);
    }

    @Test
    public void stormTiresRecoverPartOfWeatherGripLoss() {
        RogueliteCardInventory inventory = new RogueliteCardInventory();
        acquire(inventory, RogueliteCardId.STORM_TIRES, 1);
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        upgrades.configure(inventory);

        assertEquals(0.706f, upgrades.adjustSurfaceGrip(0.58f), EPSILON);
    }

    @Test
    public void overtakingCreatesThenExpiresAnAccelerationBurst() {
        RogueliteCardInventory inventory = new RogueliteCardInventory();
        acquire(inventory, RogueliteCardId.OVERTAKE_INJECTOR, 1);
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        upgrades.configure(inventory);

        assertTrue(upgrades.hasOvertakeInjector());
        upgrades.onRacePositionImproved(1, 0f);
        assertEquals(1.07f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(
                RogueliteCardId.OVERTAKE_INJECTOR,
                upgrades.getActiveCardIds().get(0));

        upgrades.update(
                1.1f,
                1f,
                true,
                false,
                false,
                0f,
                0.8f,
                0f,
                100f,
                2f);
        assertEquals(1f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertTrue(upgrades.getActiveCardIds().isEmpty());
    }

    @Test
    public void draftReceiverIsReportedOnlyWhileSlipstreamIsActive() {
        RogueliteCardInventory inventory = new RogueliteCardInventory();
        acquire(inventory, RogueliteCardId.DRAFT_RECEIVER, 1);
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        upgrades.configure(inventory);

        upgrades.update(
                0.1f,
                1f,
                true,
                false,
                false,
                0f,
                0.8f,
                0.45f,
                0f,
                100f,
                2f);
        assertEquals(RogueliteCardId.DRAFT_RECEIVER, upgrades.getActiveCardIds().get(0));

        upgrades.update(
                0.1f,
                1f,
                true,
                false,
                false,
                0f,
                0.8f,
                0f,
                0f,
                100f,
                2f);
        assertTrue(upgrades.getActiveCardIds().isEmpty());
    }

    private static void acquire(
            RogueliteCardInventory inventory,
            RogueliteCardId id,
            int copies) {
        RogueliteCardDefinition card = RogueliteCardCatalog.get(id);
        for (int i = 0; i < copies; i++) {
            assertTrue(inventory.acquire(card));
        }
    }
}
