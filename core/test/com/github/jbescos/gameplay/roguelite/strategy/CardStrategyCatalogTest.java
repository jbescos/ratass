package com.github.jbescos.gameplay.roguelite.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.github.jbescos.gameplay.roguelite.RogueliteCardOffer;
import com.github.jbescos.gameplay.roguelite.RogueliteRun;
import java.util.Arrays;
import org.junit.Test;

public final class CardStrategyCatalogTest {
    @Test
    public void runAssignsAvailableProfilesAndPersistsThem() {
        CardStrategyCatalog catalog = new CardStrategyCatalog(
                Arrays.<CardStrategy>asList(new FirstOfferStrategy("strategy00")));
        RogueliteRun original = new RogueliteRun(713L);
        original.configureCardStrategies(catalog);
        original.assignRivalStrategiesEvenly(
                Arrays.asList(
                        Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3),
                        Integer.valueOf(4), Integer.valueOf(5), Integer.valueOf(6),
                        Integer.valueOf(7), Integer.valueOf(8), Integer.valueOf(9)));

        boolean neuralAssigned = false;
        int algorithmicCount = 0;
        int neuralCount = 0;
        for (int vehicleId = 1; vehicleId <= 9; vehicleId++) {
            String profileId = original.getRivalStrategyProfileId(vehicleId);
            assertTrue(catalog.contains(profileId));
            neuralAssigned |= "strategy00".equals(profileId);
            if (AlgorithmicCardStrategy.PROFILE_ID.equals(profileId)) {
                algorithmicCount++;
            } else if ("strategy00".equals(profileId)) {
                neuralCount++;
            }
        }
        assertTrue(neuralAssigned);
        assertEquals(9, algorithmicCount + neuralCount);
        assertEquals(1, Math.abs(algorithmicCount - neuralCount));

        RogueliteRun.Snapshot snapshot = original.snapshot();
        RogueliteRun restored = new RogueliteRun(999L);
        restored.configureCardStrategies(catalog);
        assertTrue(restored.restore(snapshot));
        for (int vehicleId = 1; vehicleId <= 9; vehicleId++) {
            assertEquals(
                    original.getRivalStrategyProfileId(vehicleId),
                    restored.getRivalStrategyProfileId(vehicleId));
        }
    }

    @Test
    public void unknownProfileUsesAlgorithmicFallback() {
        CardStrategyCatalog catalog = CardStrategyCatalog.algorithmicOnly();

        assertEquals(
                AlgorithmicCardStrategy.PROFILE_ID,
                catalog.get("missing").getProfileId());
    }

    @Test
    public void fixedCatalogSelectsOnlyTheRequestedStrategy() {
        CardStrategyCatalog catalog = CardStrategyCatalog.fixed(
                new FirstOfferStrategy("strategy00"));

        assertEquals(
                "strategy00",
                catalog.chooseProfileId(new CardStrategyRandom() {
                    @Override
                    public int nextInt(int bound) {
                        return 0;
                    }
                }));
        assertEquals(
                AlgorithmicCardStrategy.PROFILE_ID,
                catalog.get("missing").getProfileId());
    }

    @Test
    public void strategyExposesItsDisplayName() {
        CardStrategyCatalog catalog = CardStrategyCatalog.fixed(
                new FirstOfferStrategy("strategy02", "Engineer"));

        assertEquals("Engineer", catalog.get("strategy02").getDisplayName());
    }

    private static final class FirstOfferStrategy implements CardStrategy {
        private final String profileId;
        private final String displayName;

        private FirstOfferStrategy(String profileId) {
            this(profileId, profileId);
        }

        private FirstOfferStrategy(String profileId, String displayName) {
            this.profileId = profileId;
            this.displayName = displayName;
        }

        @Override
        public String getProfileId() {
            return profileId;
        }

        @Override
        public String getDisplayName() {
            return displayName;
        }

        @Override
        public RogueliteCardOffer choose(
                CardStrategyDecision decision,
                CardStrategyRandom random) {
            return decision.getOffers().get(0);
        }
    }
}
