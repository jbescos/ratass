package com.github.jbescos.gameplay.roguelite.strategy;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import com.github.jbescos.gameplay.roguelite.RogueliteCardCatalog;
import com.github.jbescos.gameplay.roguelite.RogueliteLoadout;
import com.github.jbescos.gameplay.roguelite.RogueliteSlotType;
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

    @Test
    public void lapExperienceTechniqueIsNeutralToDrivingStatSynergy() {
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        loadout.equip(RogueliteCardId.CLUB_TUNE);

        assertEquals(0f, TuningTechniqueSynergy.selectionGain(
                loadout, RogueliteCardId.LAP_DIVIDEND), 0.001f);
    }

    @Test
    public void engineerCanLeaveAnOldSynergyForAHigherTierCard() {
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        loadout.equip(RogueliteCardId.SHORT_GEARING);
        loadout.equip(RogueliteCardId.STRAIGHT_FOCUS);

        boolean checkedNonImprovingUpgrade = false;
        for (RogueliteCardId candidate : RogueliteCardId.values()) {
            if (RogueliteCardCatalog.get(candidate).getSlotType() != RogueliteSlotType.TUNING
                    || RogueliteCardCatalog.get(candidate).getTier() != 2) {
                continue;
            }
            float rawGain = TuningTechniqueSynergy.statSelectionGain(loadout, candidate);
            if (rawGain <= 0f) {
                assertTrue(TuningTechniqueSynergy.engineerSelectionGain(loadout, candidate) > 0f);
                checkedNonImprovingUpgrade = true;
                break;
            }
        }

        assertTrue("test setup needs a temporarily weaker Tier 2 tuning", checkedNonImprovingUpgrade);
    }
}
