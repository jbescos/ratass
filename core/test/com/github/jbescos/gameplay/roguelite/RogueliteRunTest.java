package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class RogueliteRunTest {
    @Test
    public void duplicateSelectionsUpgradeFromLevelZeroToMaximum() {
        RogueliteCardInventory inventory = new RogueliteCardInventory();
        RogueliteCardDefinition turbo =
                RogueliteCardCatalog.get(RogueliteCardId.TURBOCHARGER);

        assertTrue(inventory.acquire(turbo));
        assertEquals(0, inventory.getLevel(turbo.getId()));
        assertTrue(inventory.acquire(turbo));
        assertEquals(1, inventory.getLevel(turbo.getId()));
        assertTrue(inventory.acquire(turbo));
        assertEquals(2, inventory.getLevel(turbo.getId()));
        assertFalse(inventory.acquire(turbo));
        assertEquals(3, inventory.getSelectionCount());
    }

    @Test
    public void offersContainUniqueCardsWithTheNextLevel() {
        RogueliteRun run = new RogueliteRun(17L);
        RogueliteCardInventory inventory = run.getPlayerInventory();
        RogueliteCardDefinition turbo =
                RogueliteCardCatalog.get(RogueliteCardId.TURBOCHARGER);
        inventory.acquire(turbo);

        List<RogueliteCardOffer> offers = run.createOffers(3);
        Set<RogueliteCardId> ids = new HashSet<RogueliteCardId>();
        for (int i = 0; i < offers.size(); i++) {
            RogueliteCardOffer offer = offers.get(i);
            assertTrue(ids.add(offer.getCard().getId()));
            assertEquals(
                    inventory.getLevel(offer.getCard().getId()) + 1,
                    offer.getTargetLevel());
        }
    }

    @Test
    public void anUpgradeIsOfferedWithinThreeRewardScreens() {
        RogueliteRun run = new RogueliteRun(31L);
        run.getPlayerInventory().acquire(
                RogueliteCardCatalog.get(RogueliteCardId.TURBOCHARGER));

        boolean upgradeSeen = false;
        for (int screen = 0; screen < 3; screen++) {
            List<RogueliteCardOffer> offers = run.createOffers(3);
            for (int i = 0; i < offers.size(); i++) {
                upgradeSeen |= offers.get(i).isUpgrade();
            }
        }

        assertTrue(upgradeSeen);
    }

    @Test
    public void rivalInventoriesAdvanceIndependentlyAndResetWithTheRun() {
        RogueliteRun run = new RogueliteRun(43L);
        run.advanceRivals(Arrays.asList(Integer.valueOf(2), Integer.valueOf(7)));

        RogueliteCardInventory first = run.getRivalInventory(2);
        RogueliteCardInventory second = run.getRivalInventory(7);
        assertEquals(1, first.getSelectionCount());
        assertEquals(1, second.getSelectionCount());
        assertNotEquals(first, second);
        assertEquals(0, run.getPlayerInventory().getSelectionCount());

        run.reset();

        assertEquals(0, run.getPlayerInventory().getSelectionCount());
        assertEquals(0, run.getRivalInventory(2).getSelectionCount());
    }
}
