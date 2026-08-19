package com.github.jbescos.gameplay.roguelite.strategy;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import com.github.jbescos.gameplay.roguelite.RogueliteLoadout;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class CardStrategyChainRewardTest {
    private final CardStrategyRewardConfig config =
            CardStrategyRewardProfiles.forProfile("strategy08");

    @Test
    public void rewardsExactAmplifiersButNotUnrelatedTechniqueCards() {
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");

        assertTrue(CardStrategyChainReward.selection(
                loadout, RogueliteCardId.TECHNIQUE_COUPLER, config) > 0f);
        assertEquals(0f, CardStrategyChainReward.selection(
                loadout, RogueliteCardId.CORNER_FOCUS, config), 0.001f);
    }

    @Test
    public void rewardsEachCompatibleChainLinkAsItIsBuilt() {
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        loadout.equip(RogueliteCardId.TECHNIQUE_COUPLER);
        float isolated = CardStrategyChainReward.selection(
                new RogueliteLoadout("profile00"), RogueliteCardId.POWERUP_LINK, config);
        float linked = CardStrategyChainReward.selection(
                loadout, RogueliteCardId.POWERUP_LINK, config);

        assertEquals(config.getAmplifierLink(), linked - isolated, 0.001f);

        loadout.equip(RogueliteCardId.POWERUP_LINK);
        isolated = CardStrategyChainReward.selection(
                new RogueliteLoadout("profile00"), RogueliteCardId.GRUDGE_SPARK, config);
        linked = CardStrategyChainReward.selection(
                loadout, RogueliteCardId.GRUDGE_SPARK, config);
        assertEquals(config.getAmplifierLink(), linked - isolated, 0.001f);
    }

    @Test
    public void rewardsRandomRelaysAndTheirCompatibleAmplifierLink() {
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        loadout.equip(RogueliteCardId.TECHNIQUE_COUPLER);
        loadout.equip(RogueliteCardId.POWERUP_LINK);
        float relayOnly = CardStrategyChainReward.selection(
                new RogueliteLoadout("profile00"), RogueliteCardId.LUCKY_SPARK, config);
        float linkedRelay = CardStrategyChainReward.selection(
                loadout, RogueliteCardId.LUCKY_SPARK, config);
        assertEquals(config.getAmplifierLink(), linkedRelay - relayOnly, 0.001f);

        loadout.equip(RogueliteCardId.GRUDGE_SPARK);
        relayOnly = CardStrategyChainReward.selection(
                new RogueliteLoadout("profile00"), RogueliteCardId.LOADED_GRUDGE, config);
        linkedRelay = CardStrategyChainReward.selection(
                loadout, RogueliteCardId.LOADED_GRUDGE, config);
        assertEquals(config.getAmplifierLink(), linkedRelay - relayOnly, 0.001f);
    }
}
