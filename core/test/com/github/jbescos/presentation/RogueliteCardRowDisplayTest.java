package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import com.github.jbescos.gameplay.roguelite.RogueliteCarUpgrades;
import com.github.jbescos.gameplay.roguelite.RogueliteLoadout;
import com.github.jbescos.gameplay.roguelite.RogueliteSlotType;
import org.junit.Test;

public class RogueliteCardRowDisplayTest {
    @Test
    public void regularSlotDisplaysItsEquippedCard() {
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        loadout.equip(RogueliteCardId.NITRO_PULSE);
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        upgrades.configure(loadout);

        assertEquals(
                RogueliteCardId.NITRO_PULSE,
                RogueliteCardRowDisplay.resolveCardId(
                        loadout,
                        upgrades,
                        RogueliteSlotType.POWERUP));
    }

    @Test
    public void randomSlotDisplaysItsCurrentlyLoadedChild() {
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        loadout.equip(RogueliteCardId.LUCKY_SPARK);
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        upgrades.configure(loadout);

        RogueliteCardId loadedCardId =
                upgrades.getLoadedCardId(RogueliteSlotType.POWERUP);

        assertNotNull(loadedCardId);
        assertEquals(
                loadedCardId,
                RogueliteCardRowDisplay.resolveCardId(
                        loadout,
                        upgrades,
                        RogueliteSlotType.POWERUP));
    }

    @Test
    public void driverAndEmptySlotsDoNotResolveCards() {
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");

        assertNull(
                RogueliteCardRowDisplay.resolveCardId(
                        loadout,
                        null,
                        RogueliteSlotType.DRIVER));
        assertNull(
                RogueliteCardRowDisplay.resolveCardId(
                        loadout,
                        null,
                        RogueliteSlotType.TECHNIQUE));
    }

    @Test
    public void activeTechniqueDisplaysItsRemainingTime() {
        assertEquals(
                "2.4s",
                RogueliteCardRowDisplay.statusText(
                        RogueliteSlotType.TECHNIQUE,
                        2.36f,
                        0f,
                        false));
    }

    @Test
    public void inactiveTechniqueDoesNotDisplayReadinessOrCooldown() {
        assertEquals(
                "",
                RogueliteCardRowDisplay.statusText(
                        RogueliteSlotType.TECHNIQUE,
                        0f,
                        8f,
                        false));
    }

    @Test
    public void existingPowerupAndRevengeStatusesArePreserved() {
        assertEquals(
                "CD 4.5s",
                RogueliteCardRowDisplay.statusText(
                        RogueliteSlotType.POWERUP,
                        0f,
                        4.47f,
                        false));
        assertEquals(
                "READY",
                RogueliteCardRowDisplay.statusText(
                        RogueliteSlotType.REVENGE,
                        0f,
                        0f,
                        true));
        assertEquals(
                "WAIT",
                RogueliteCardRowDisplay.statusText(
                        RogueliteSlotType.REVENGE,
                        0f,
                        0f,
                        false));
    }
}
