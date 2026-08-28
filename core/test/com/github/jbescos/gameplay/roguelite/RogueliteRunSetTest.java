package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class RogueliteRunSetTest {
    @Test
    public void everyRunUsesTheSameStaticRecipes() {
        RogueliteRun first = new RogueliteRun(481L);
        RogueliteRun second = new RogueliteRun(999L);

        assertEquals(RogueliteSetCatalog.allSetIds(), first.getEnabledSetIds());
        assertEquals(first.getEnabledSetIds(), second.getEnabledSetIds());
    }

    @Test
    public void staticSetsSurviveSaveRestoreAndDriveAutomaticCompletion() {
        RogueliteRun original = new RogueliteRun(482L);
        RogueliteSetDefinition set = RogueliteSetCatalog.get(
                original.getEnabledSetIds().get(0));
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        loadout.equip(set.getTuningCardId());
        loadout.equip(set.getTechniqueCardId());
        loadout.equip(set.getPowerupCardId());
        loadout.equip(set.getRevengeCardId());

        assertEquals(set.getId(), original.getCompletedSet(loadout).getId());

        RogueliteRun restored = new RogueliteRun(999L);
        assertTrue(restored.restore(original.snapshot()));
        assertEquals(original.getEnabledSetIds(), restored.getEnabledSetIds());
        assertNotNull(restored.getCompletedSet(loadout));
        assertEquals(set.getId(), restored.getCompletedSet(loadout).getId());
    }

    @Test
    public void restoreUpgradesLegacyRandomSubsetToStaticRecipes() {
        RogueliteRun original = new RogueliteRun(483L);
        RogueliteRun.Snapshot snapshot = original.snapshot();
        snapshot.enabledSetIds.clear();
        snapshot.enabledSetIds.add(RogueliteSetId.VELOCITY_PACT.name());

        RogueliteRun restored = new RogueliteRun(1000L);
        assertTrue(restored.restore(snapshot));
        assertEquals(RogueliteSetCatalog.allSetIds(), restored.getEnabledSetIds());
    }
}
