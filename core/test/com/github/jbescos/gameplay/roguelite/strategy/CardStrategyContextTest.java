package com.github.jbescos.gameplay.roguelite.strategy;

import static org.junit.Assert.assertEquals;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import com.github.jbescos.gameplay.roguelite.RogueliteLoadout;
import com.github.jbescos.gameplay.roguelite.RogueliteSlotType;
import java.util.Arrays;
import org.junit.Test;

public final class CardStrategyContextTest {
    @Test
    public void exposesOnlyPowerupAndRevengeCardOverlap() {
        RogueliteLoadout first = new RogueliteLoadout("profile00");
        first.equip(RogueliteCardId.NITRO_PULSE);
        first.equip(RogueliteCardId.TELEMETRY_THEFT);
        RogueliteLoadout second = new RogueliteLoadout("profile01");
        second.equip(RogueliteCardId.NITRO_PULSE);

        CardStrategyContext context = context(first, second);

        assertEquals(1f, context.equippedCardOverlap(
                RogueliteSlotType.POWERUP, RogueliteCardId.NITRO_PULSE), 0.001f);
        assertEquals(0.5f, context.equippedCardOverlap(
                RogueliteSlotType.REVENGE, RogueliteCardId.TELEMETRY_THEFT), 0.001f);
        assertEquals(0f, context.equippedCardOverlap(
                RogueliteSlotType.TUNING, RogueliteCardId.SPORT_TUNE), 0.001f);
    }

    @Test
    public void snapshotsOpponentCardsAtDecisionTime() {
        RogueliteLoadout first = new RogueliteLoadout("profile00");
        first.equip(RogueliteCardId.NITRO_PULSE);
        CardStrategyContext context = context(first);

        first.equip(RogueliteCardId.PHASE_SHIELD);

        assertEquals(1f, context.equippedCardOverlap(
                RogueliteSlotType.POWERUP, RogueliteCardId.NITRO_PULSE), 0.001f);
        assertEquals(0f, context.equippedCardOverlap(
                RogueliteSlotType.POWERUP, RogueliteCardId.PHASE_SHIELD), 0.001f);
    }

    private static CardStrategyContext context(RogueliteLoadout... loadouts) {
        CardStrategyContext.Opponent[] opponents =
                new CardStrategyContext.Opponent[loadouts.length];
        for (int i = 0; i < loadouts.length; i++) {
            opponents[i] = new CardStrategyContext.Opponent(
                    1, i + 1, i + 1, null, loadouts[i]);
        }
        return new CardStrategyContext(
                1, 19, 1, 5, 1, 1, 18, Arrays.asList(opponents));
    }
}
