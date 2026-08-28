package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.github.jbescos.gameplay.roguelite.strategy.AlgorithmicCardStrategy;
import com.github.jbescos.gameplay.roguelite.strategy.CardStrategyContext;
import com.github.jbescos.gameplay.roguelite.strategy.CardStrategyDecision;
import com.github.jbescos.gameplay.roguelite.strategy.CardStrategyRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;

public final class AlgorithmicFinalReckoningTest {
    private static final CardStrategyRandom FIRST = new CardStrategyRandom() {
        @Override
        public int nextInt(int bound) {
            return 0;
        }
    };

    @Test
    public void alwaysTakesFinalReckoningWhenOffered() {
        RogueliteCompetitorProgress progress = progress();
        List<RogueliteCardOffer> offers = Arrays.asList(
                offer(RogueliteCardId.CLUB_TUNE),
                offer(RogueliteCardId.FINAL_RECKONING),
                offer(RogueliteCardId.NITRO_PULSE));

        RogueliteCardOffer selected = choose(progress, offers);

        assertEquals(RogueliteCardId.FINAL_RECKONING, selected.getCard().getId());
    }

    @Test
    public void neverReplacesFinalReckoningWhenAnotherSlotIsOffered() {
        RogueliteCompetitorProgress progress = progress();
        progress.getLoadout().equip(RogueliteCardId.FINAL_RECKONING);

        RogueliteCardOffer selected = choose(progress, Arrays.asList(
                offer(RogueliteCardId.FATES_REVENGE),
                offer(RogueliteCardId.CLUB_TUNE)));

        assertEquals(RogueliteCardId.CLUB_TUNE, selected.getCard().getId());
    }

    @Test
    public void skipsWhenOnlyReplacementRevengesAreOffered() {
        RogueliteCompetitorProgress progress = progress();
        progress.getLoadout().equip(RogueliteCardId.FINAL_RECKONING);

        assertNull(choose(progress, Arrays.asList(
                offer(RogueliteCardId.FATES_REVENGE),
                offer(RogueliteCardId.APEX_PLUNDER))));
    }

    @Test
    public void completesTheStrongestPartialSet() {
        RogueliteSetDefinition set = RogueliteSetCatalog.tierThreeSets().get(0);
        RogueliteCompetitorProgress progress = progress();
        progress.getLoadout().equip(set.getTuningCardId());
        progress.getLoadout().equip(set.getTechniqueCardId());
        progress.getLoadout().equip(set.getPowerupCardId());

        RogueliteCardOffer selected = chooseWithSets(progress, Arrays.asList(
                offer(RogueliteCardId.HUNTER_STORM),
                offer(set.getRevengeCardId())));

        assertEquals(set.getRevengeCardId(), selected.getCard().getId());
    }

    @Test
    public void preservesAThreeCardRecipeWhenOffersWouldBreakIt() {
        RogueliteSetDefinition set = RogueliteSetCatalog.tierThreeSets().get(0);
        RogueliteCompetitorProgress progress = progress();
        progress.getLoadout().equip(set.getTuningCardId());
        progress.getLoadout().equip(set.getTechniqueCardId());
        progress.getLoadout().equip(set.getPowerupCardId());

        assertNull(chooseWithSets(progress, Arrays.asList(
                offer(RogueliteCardId.HYPERCAR_CORE),
                offer(RogueliteCardId.DRAFT_MASTER),
                offer(RogueliteCardId.WILDCARD_CORE))));
    }

    private static RogueliteCardOffer choose(
            RogueliteCompetitorProgress progress,
            List<RogueliteCardOffer> offers) {
        return new AlgorithmicCardStrategy().choose(
                new CardStrategyDecision(
                        progress,
                        DriverProfileCatalog.fallback(),
                        offers,
                        CardStrategyContext.empty()),
                FIRST);
    }

    private static RogueliteCardOffer chooseWithSets(
            RogueliteCompetitorProgress progress,
            List<RogueliteCardOffer> offers) {
        CardStrategyContext context = new CardStrategyContext(
                1,
                19,
                1,
                5,
                1,
                1,
                18,
                Collections.<CardStrategyContext.Opponent>emptyList(),
                RogueliteSetCatalog.allSetIds());
        return new AlgorithmicCardStrategy().choose(
                new CardStrategyDecision(
                        progress,
                        DriverProfileCatalog.fallback(),
                        offers,
                        context),
                FIRST);
    }

    private static RogueliteCompetitorProgress progress() {
        return new RogueliteCompetitorProgress("profile00");
    }

    private static RogueliteCardOffer offer(RogueliteCardId cardId) {
        return RogueliteCardOffer.modification(RogueliteCardCatalog.get(cardId));
    }
}
