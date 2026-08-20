package com.github.jbescos.gameplay.roguelite.strategy;

import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import com.github.jbescos.gameplay.roguelite.RogueliteLoadout;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public final class CardStrategyChainRewardTest {
    private final CardStrategyRewardConfig config =
            new CardStrategyRewardConfig(
                    100f, 30f, 3f, 1f, 0.10f, 0f,
                    0.5f, 0f, 0f, 0f, 0f, 0f,
                    10f, 10f, 10f, 20f, 0f, 0f,
                    "", 0f, "", 0f,
                    0f, 0f, 0f, 0f);

    @Test
    public void rewardsExactAmplifiersButNotUnrelatedTechniqueCards() {
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");

        assertTrue(CardStrategyChainReward.selection(
                loadout, RogueliteCardId.TECHNIQUE_SINGULARITY, config) > 0f);
        assertEquals(0f, CardStrategyChainReward.selection(
                loadout, RogueliteCardId.CORNER_FOCUS, config), 0.001f);
    }

    @Test
    public void rewardsEachCompatibleChainLinkAsItIsBuilt() {
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        loadout.equip(RogueliteCardId.TECHNIQUE_SINGULARITY);
        float isolated = CardStrategyChainReward.selection(
                new RogueliteLoadout("profile00"), RogueliteCardId.POWERUP_NEXUS, config);
        float linked = CardStrategyChainReward.selection(
                loadout, RogueliteCardId.POWERUP_NEXUS, config);

        assertEquals(config.getAmplifierLink(), linked - isolated, 0.001f);

        loadout.equip(RogueliteCardId.POWERUP_NEXUS);
        isolated = CardStrategyChainReward.selection(
                new RogueliteLoadout("profile00"), RogueliteCardId.NEMESIS_ENGINE, config);
        linked = CardStrategyChainReward.selection(
                loadout, RogueliteCardId.NEMESIS_ENGINE, config);
        assertEquals(config.getAmplifierLink(), linked - isolated, 0.001f);
    }

    @Test
    public void rewardsRandomRelaysAndTheirCompatibleAmplifierLink() {
        RogueliteLoadout loadout = new RogueliteLoadout("profile00");
        loadout.equip(RogueliteCardId.TECHNIQUE_SINGULARITY);
        loadout.equip(RogueliteCardId.POWERUP_NEXUS);
        float relayOnly = CardStrategyChainReward.selection(
                new RogueliteLoadout("profile00"), RogueliteCardId.LUCKY_SPARK, config);
        float linkedRelay = CardStrategyChainReward.selection(
                loadout, RogueliteCardId.LUCKY_SPARK, config);
        assertEquals(config.getAmplifierLink(), linkedRelay - relayOnly, 0.001f);

        loadout.equip(RogueliteCardId.NEMESIS_ENGINE);
        relayOnly = CardStrategyChainReward.selection(
                new RogueliteLoadout("profile00"), RogueliteCardId.LOADED_GRUDGE, config);
        linkedRelay = CardStrategyChainReward.selection(
                loadout, RogueliteCardId.LOADED_GRUDGE, config);
        assertEquals(config.getAmplifierLink(), linkedRelay - relayOnly, 0.001f);
    }
}
