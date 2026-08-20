package com.github.jbescos.gameplay.roguelite.strategy;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import com.github.jbescos.gameplay.roguelite.RogueliteLoadout;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class TuningTechniqueSynergyTest {
    @Test
    public void statGainRewardsCompatibleTuningAndTechniqueCards() {
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        loadout.equip(RogueliteCardId.SHORT_GEARING);

        float matching = TuningTechniqueSynergy.statSelectionGain(
                loadout, RogueliteCardId.STRAIGHT_FOCUS);
        float mismatched = TuningTechniqueSynergy.statSelectionGain(
                loadout, RogueliteCardId.AGILITY_FOCUS);

        assertTrue(matching > 0f);
        assertTrue(matching > mismatched);
    }

    @Test
    public void engineerExcludesAmplifierChainFromStatSynergy() {
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        loadout.equip(RogueliteCardId.TECHNIQUE_COUPLER);

        assertTrue(TuningTechniqueSynergy.selectionGain(
                loadout, RogueliteCardId.POWERUP_LINK) > 0f);
        assertEquals(0f, TuningTechniqueSynergy.statSelectionGain(
                loadout, RogueliteCardId.POWERUP_LINK), 0.001f);
    }
}
