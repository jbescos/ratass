package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

public class RogueliteSetCatalogTest {
    @Test
    public void recipesUseOneCorrectlyTieredCardFromEveryModificationSlot() {
        Set<RogueliteCardId> components = new HashSet<RogueliteCardId>();
        int recipeCount = 0;
        for (RogueliteSetId id : RogueliteSetId.values()) {
            RogueliteSetDefinition set = RogueliteSetCatalog.get(id);
            recipeCount++;
            for (RogueliteSlotType slot : RogueliteSlotType.modificationSlots()) {
                RogueliteCardId cardId = set.getRequiredCard(slot);
                RogueliteCardDefinition card = RogueliteCardCatalog.get(cardId);
                assertEquals(slot, card.getSlotType());
                assertEquals(set.getTier(), card.getTier());
                assertTrue("Component appears in more than one recipe: " + cardId,
                        components.add(cardId));
            }
        }

        assertEquals(9, recipeCount);
        assertEquals(36, components.size());
        assertEquals(8, RogueliteSetCatalog.tierThreeSets().size());
        assertEquals(4, RogueliteSetCatalog.tierFourSet().getTier());
    }

    @Test
    public void completionRequiresAllFourExactComponents() {
        RogueliteSetDefinition set = RogueliteSetCatalog.tierThreeSets().get(0);
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        loadout.equip(set.getTuningCardId());
        loadout.equip(set.getTechniqueCardId());
        loadout.equip(set.getPowerupCardId());

        assertFalse(set.isCompletedBy(loadout));
        assertNull(RogueliteSetCatalog.completedSet(
                loadout, java.util.Collections.singletonList(set.getId())));

        loadout.equip(set.getRevengeCardId());

        assertTrue(set.isCompletedBy(loadout));
        assertSame(set, RogueliteSetCatalog.completedSet(
                loadout, java.util.Collections.singletonList(set.getId())));
    }

    @Test
    public void ironGiantUsesPermanentDebuffImmunityAsItsSetBonus() {
        RogueliteSetDefinition ironGiant =
                RogueliteSetCatalog.get(RogueliteSetId.IRON_GIANT);

        assertTrue(ironGiant.usesSetScopedBonusEffect());
        assertEquals("Immune to all debuffs", ironGiant.getBonusEffectText());
    }

    @Test
    public void chaosCircuitKeepsItsTechniqueAlwaysActive() {
        RogueliteSetDefinition chaosCircuit =
                RogueliteSetCatalog.get(RogueliteSetId.CHAOS_CIRCUIT);

        assertTrue(chaosCircuit.usesSetScopedBonusEffect());
        assertEquals("Technique always active", chaosCircuit.getBonusEffectText());
    }

    @Test
    public void quantumPackGrantsPowerupNexus() {
        RogueliteSetDefinition quantumPack =
                RogueliteSetCatalog.get(RogueliteSetId.QUANTUM_PACK);

        assertFalse(quantumPack.usesSetScopedBonusEffect());
        assertEquals(RogueliteCardId.POWERUP_NEXUS, quantumPack.getBonusCardId());

        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        loadout.equip(quantumPack.getTuningCardId());
        loadout.equip(quantumPack.getTechniqueCardId());
        loadout.equip(quantumPack.getPowerupCardId());
        loadout.equip(quantumPack.getRevengeCardId());
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        upgrades.configure(loadout, 0f, quantumPack);

        assertTrue(upgrades.isCardEffectActive(RogueliteCardId.POWERUP_NEXUS));
        assertFalse(upgrades.isCardEffectActive(RogueliteCardId.TEMPORAL_DOMINION));
    }

    @Test
    public void doomRallyCopiesThePassedRivalsPowerupOnOvertake() {
        RogueliteSetDefinition doomRally =
                RogueliteSetCatalog.get(RogueliteSetId.DOOM_RALLY);

        assertTrue(doomRally.usesSetScopedBonusEffect());
        assertEquals(
                "Overtake: execute the rival's Powerup",
                doomRally.getBonusEffectText());

        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        loadout.equip(doomRally.getTuningCardId());
        loadout.equip(doomRally.getTechniqueCardId());
        loadout.equip(doomRally.getPowerupCardId());
        loadout.equip(doomRally.getRevengeCardId());
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        upgrades.configure(loadout, 0f, doomRally);

        RogueliteLoadout rivalLoadout = new RogueliteLoadout("profile01");
        rivalLoadout.equip(RogueliteCardId.NITRO_PULSE);
        RogueliteCarUpgrades rival = new RogueliteCarUpgrades();
        rival.configure(rivalLoadout);

        assertEquals(
                RogueliteCardId.NITRO_PULSE,
                rival.getCopyablePowerupCardId());
        assertTrue(upgrades.activateDoomRallyPowerup(
                rival.getCopyablePowerupCardId()));
        assertEquals(
                RogueliteCardId.NITRO_PULSE,
                upgrades.getActivePowerupCardId());
        assertTrue(upgrades.getAccelerationMultiplier() > 1f);
        assertNull(rival.getActivePowerupCardId());
    }

    @Test
    public void selectionGainStronglyRewardsCompletingAnEnabledSet() {
        RogueliteSetDefinition set = RogueliteSetCatalog.tierThreeSets().get(0);
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        loadout.equip(set.getTuningCardId());
        loadout.equip(set.getTechniqueCardId());
        loadout.equip(set.getPowerupCardId());

        float completionGain = RogueliteSetCatalog.selectionGain(
                loadout,
                set.getRevengeCardId(),
                java.util.Collections.singletonList(set.getId()));

        assertTrue(completionGain >= 1f);
        assertEquals(0f, RogueliteSetCatalog.selectionGain(
                loadout,
                set.getRevengeCardId(),
                java.util.Collections.<RogueliteSetId>emptyList()),
                0.0001f);
    }

    @Test
    public void candidatePreviewReportsProgressCompletionAndBreakage() {
        RogueliteSetDefinition set = RogueliteSetCatalog.tierThreeSets().get(0);
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        loadout.equip(set.getTuningCardId());
        loadout.equip(set.getTechniqueCardId());
        loadout.equip(set.getPowerupCardId());
        java.util.List<RogueliteSetId> enabled =
                java.util.Collections.singletonList(set.getId());

        assertEquals(1, RogueliteSetCatalog.selectionProgress(
                loadout, set.getRevengeCardId(), enabled));
        assertTrue(RogueliteSetCatalog.completesSetAfter(
                loadout, set.getRevengeCardId(), enabled));

        loadout.equip(set.getRevengeCardId());
        assertTrue(RogueliteSetCatalog.breaksCompletedSet(
                loadout, RogueliteCardId.HUNTER_STORM, enabled));
        assertEquals(1, RogueliteSetCatalog.selectionRegression(
                loadout, RogueliteCardId.HUNTER_STORM, enabled));
    }
}
