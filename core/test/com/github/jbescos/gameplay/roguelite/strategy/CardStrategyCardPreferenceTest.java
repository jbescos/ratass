package com.github.jbescos.gameplay.roguelite.strategy;

import static org.junit.Assert.assertEquals;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import com.github.jbescos.gameplay.roguelite.RogueliteLoadout;
import org.junit.Test;

public final class CardStrategyCardPreferenceTest {
    @Test
    public void rewardsPreferredAndPenalizesDiscouragedCards() {
        CardStrategyCardPreference preference = new CardStrategyCardPreference(
                "CORNER_FOCUS, CORNER_EXPERT",
                3f,
                "POWERUP_LINK,POWERUP_MATRIX",
                1.5f);
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");

        assertEquals(3f, preference.reward(loadout, RogueliteCardId.CORNER_FOCUS), 0f);
        assertEquals(-1.5f, preference.reward(loadout, RogueliteCardId.POWERUP_LINK), 0f);
        assertEquals(0f, preference.reward(loadout, RogueliteCardId.DRIFT_FOCUS), 0f);
    }

    @Test
    public void rewardsEnteringAndUpgradingButNotChurningPreferredFamily() {
        CardStrategyCardPreference preference = new CardStrategyCardPreference(
                "CORNER_FOCUS,CORNER_EXPERT,CORNER_MASTER,APEX_FOCUS",
                6f,
                "",
                1.5f);
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");

        assertEquals(6f, preference.reward(loadout, RogueliteCardId.CORNER_FOCUS), 0f);
        loadout.equip(RogueliteCardId.CORNER_FOCUS);
        assertEquals(-1.5f, preference.reward(loadout, RogueliteCardId.APEX_FOCUS), 0f);
        assertEquals(2f, preference.reward(loadout, RogueliteCardId.CORNER_EXPERT), 0f);
        assertEquals(4f, preference.reward(loadout, RogueliteCardId.CORNER_MASTER), 0f);
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsUnknownCardIds() {
        new CardStrategyCardPreference("NOT_A_CARD", 1f, "", 0f);
    }
}
