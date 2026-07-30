package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class RogueliteCarUpgradesTest {
    private static final float EPSILON = 0.0001f;

    @Test
    public void nullLoadoutKeepsEveryModifierNeutral() {
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
        List<RogueliteCardDefinition> cards = RogueliteCardCatalog.all();
        for (int i = 0; i < cards.size(); i++) {
            RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
            upgrades.configure(loadout(cards.get(i).getId()));
            assertTrue(upgrades.isEnabled());
        }
    }

    @Test
    public void turboAndAerodynamicsSynergyRaisesRedlineAndReducesDrag() {
        RogueliteLoadout loadout =
                loadout(
                        RogueliteCardId.TURBOCHARGER,
                        RogueliteCardId.AERODYNAMIC_KIT);
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        upgrades.configure(loadout);

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

        assertEquals(1.10f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(1.055f, upgrades.getMaxSpeedMultiplier(), EPSILON);
        assertEquals(0.91f, upgrades.getDragMultiplier(), EPSILON);
    }

    @Test
    public void stormTiresRecoverHalfOfWeatherGripLoss() {
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        upgrades.configure(loadout(RogueliteCardId.STORM_TIRES));

        assertEquals(0.79f, upgrades.adjustSurfaceGrip(0.58f), EPSILON);
    }

    @Test
    public void overtakingCreatesThenExpiresAnAccelerationBurst() {
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        upgrades.configure(loadout(RogueliteCardId.OVERTAKE_INJECTOR));

        assertTrue(upgrades.hasOvertakeInjector());
        upgrades.onRacePositionImproved(1, 0f);
        assertEquals(1.11f, upgrades.getAccelerationMultiplier(), EPSILON);
        assertEquals(
                RogueliteCardId.OVERTAKE_INJECTOR,
                upgrades.getActiveCardIds().get(0));

        upgrades.update(
                1.4f,
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
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        upgrades.configure(loadout(RogueliteCardId.DRAFT_RECEIVER));

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
        assertEquals(
                RogueliteCardId.DRAFT_RECEIVER,
                upgrades.getActiveCardIds().get(0));

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

    private static RogueliteLoadout loadout(RogueliteCardId... cards) {
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        for (int i = 0; i < cards.length; i++) {
            assertTrue(loadout.equip(cards[i], -1));
        }
        return loadout;
    }
}
