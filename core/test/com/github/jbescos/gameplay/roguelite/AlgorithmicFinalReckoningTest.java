package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.github.jbescos.gameplay.roguelite.strategy.AlgorithmicCardStrategy;
import com.github.jbescos.gameplay.roguelite.strategy.CardStrategyContext;
import com.github.jbescos.gameplay.roguelite.strategy.CardStrategyDecision;
import com.github.jbescos.gameplay.roguelite.strategy.CardStrategyRandom;
import java.util.Arrays;
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

    private static RogueliteCompetitorProgress progress() {
        return new RogueliteCompetitorProgress("profile00");
    }

    private static RogueliteCardOffer offer(RogueliteCardId cardId) {
        return RogueliteCardOffer.modification(RogueliteCardCatalog.get(cardId));
    }
}
