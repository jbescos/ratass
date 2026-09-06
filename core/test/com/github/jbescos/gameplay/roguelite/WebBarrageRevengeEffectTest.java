package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class WebBarrageRevengeEffectTest {
    private static final float EPSILON = 0.001f;

    @Test
    public void emitsThreeHooksAtThreeSecondIntervals() {
        WebBarrageRevengeEffect effect = new WebBarrageRevengeEffect(7L);
        assertTrue(effect.onHitBy(42, 10f));

        advance(effect, 2.99f);
        assertNull(effect.tryActivateOffenderStrike(42, 100f, false));

        for (int strikeIndex = 1; strikeIndex <= 3; strikeIndex++) {
            advance(effect, strikeIndex == 1 ? 0.01f : 3f);
            RogueliteRevengeStrike strike =
                    effect.tryActivateOffenderStrike(42, 100f, false);
            assertNotNull(strike);
            assertEquals(RogueliteRevengeStrike.Action.HOOK, strike.getAction());
            assertEquals(strikeIndex, strike.getStrikeIndex());
            assertNull(strike.getSecondaryDebuffCardId());
            effect.completeOffenderStrike(RogueliteCardId.CIPHER_SIPHON);
        }

        assertFalse(effect.isActive());
        assertEquals(-1, effect.revengeTargetVehicleId());
    }

    @Test
    public void venomWebAddsDistinctStackableDebuffsToItsHooks() {
        WebBarrageRevengeEffect effect = new WebBarrageRevengeEffect(11L);
        effect.setVenomWebEnabled(true);
        effect.onHitBy(42, 10f);

        RogueliteCardId[] selected = new RogueliteCardId[3];
        for (int index = 0; index < selected.length; index++) {
            advance(effect, 3f);
            RogueliteRevengeStrike strike =
                    effect.tryActivateOffenderStrike(42, 100f, false);
            selected[index] = strike.getSecondaryDebuffCardId();
            assertNotNull(selected[index]);
            assertNotNull(strike.createSecondaryDebuffStrike());
            effect.completeOffenderStrike(RogueliteCardId.CIPHER_SIPHON);
        }

        assertNotEquals(selected[0], selected[1]);
        assertNotEquals(selected[0], selected[2]);
        assertNotEquals(selected[1], selected[2]);
    }

    @Test
    public void revengeAmplifierAddsHooksWithoutChangingTheirSchedule() {
        WebBarrageRevengeEffect effect = new WebBarrageRevengeEffect(13L);
        effect.onHitBy(42, 10f);
        advance(effect, 3f);

        RogueliteRevengeStrike first =
                effect.tryActivateOffenderStrike(42, 100f, true);
        effect.amplifyActiveRevenge(2f);
        effect.amplifyActiveRevenge(2f);
        effect.completeOffenderStrike(first.getCardId());

        assertEquals(15f, effect.activeTimeRemainingSeconds(), EPSILON);
        for (int index = 2; index <= 6; index++) {
            advance(effect, 3f);
            RogueliteRevengeStrike strike =
                    effect.tryActivateOffenderStrike(42, 100f, true);
            assertNotNull(strike);
            assertEquals(index, strike.getStrikeIndex());
            effect.completeOffenderStrike(strike.getCardId());
        }
        assertFalse(effect.isActive());
    }

    @Test
    public void completedVenomWebSetEnablesHookDebuffs() {
        RogueliteSetDefinition set =
                RogueliteSetCatalog.get(RogueliteSetId.CIPHER_SYNDICATE);
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        loadout.equip(set.getTuningCardId());
        loadout.equip(set.getTechniqueCardId());
        loadout.equip(set.getPowerupCardId());
        loadout.equip(set.getRevengeCardId());
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        upgrades.configure(loadout, 0f, set);

        assertTrue(upgrades.onHitBy(42, 10f));
        upgrades.update(3f, 0f, true, false, false, 0f, 0f, 0f, 1f, 0f);
        RogueliteRevengeStrike strike =
                upgrades.tryActivateOffenderStrike(42, 100f, false);

        assertNotNull(strike);
        assertNotNull(strike.getSecondaryDebuffCardId());
    }

    private static void advance(WebBarrageRevengeEffect effect, float seconds) {
        effect.advance(seconds, seconds, new RogueliteDrivingFrame());
    }
}
