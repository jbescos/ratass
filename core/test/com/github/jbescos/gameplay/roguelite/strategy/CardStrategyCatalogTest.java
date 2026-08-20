package com.github.jbescos.gameplay.roguelite.strategy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.github.jbescos.gameplay.roguelite.RogueliteCardOffer;
import com.github.jbescos.gameplay.roguelite.RogueliteRun;
import java.util.Arrays;
import java.util.List;
import org.junit.Test;

public final class CardStrategyCatalogTest {
    @Test
    public void raceAssignmentGuaranteesEveryProfileAndPersistsIt() {
        CardStrategyCatalog catalog = new CardStrategyCatalog(
                Arrays.<CardStrategy>asList(
                        new FirstOfferStrategy("strategy00"),
                        new FirstOfferStrategy("strategy01"),
                        new FirstOfferStrategy("strategy02")));
        RogueliteRun original = new RogueliteRun(713L);
        original.configureCardStrategies(catalog);
        original.assignRivalStrategiesForRace(
                Arrays.asList(
                        Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3),
                        Integer.valueOf(4), Integer.valueOf(5), Integer.valueOf(6),
                        Integer.valueOf(7), Integer.valueOf(8), Integer.valueOf(9)));

        int algorithmicCount = 0;
        int winnerCount = 0;
        int explorerCount = 0;
        int engineerCount = 0;
        for (int vehicleId = 1; vehicleId <= 9; vehicleId++) {
            String profileId = original.getRivalStrategyProfileId(vehicleId);
            assertTrue(catalog.contains(profileId));
            if (AlgorithmicCardStrategy.PROFILE_ID.equals(profileId)) {
                algorithmicCount++;
            } else if ("strategy00".equals(profileId)) {
                winnerCount++;
            } else if ("strategy01".equals(profileId)) {
                explorerCount++;
            } else if ("strategy02".equals(profileId)) {
                engineerCount++;
            }
        }
        assertTrue(algorithmicCount >= 1);
        assertTrue(winnerCount >= 1);
        assertTrue(explorerCount >= 1);
        assertTrue(engineerCount >= 1);
        assertEquals(
                9,
                algorithmicCount + winnerCount + explorerCount
                        + engineerCount);

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
    public void fieldsSmallerThanTheCatalogAvoidDuplicateStrategies() {
        CardStrategyCatalog catalog = new CardStrategyCatalog(
                Arrays.<CardStrategy>asList(
                        new FirstOfferStrategy("strategy00"),
                        new FirstOfferStrategy("strategy01"),
                        new FirstOfferStrategy("strategy02")));
        RogueliteRun run = new RogueliteRun(91L);
        run.configureCardStrategies(catalog);
        run.assignRivalStrategiesForRace(Arrays.asList(
                Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3)));

        String first = run.getRivalStrategyProfileId(1);
        String second = run.getRivalStrategyProfileId(2);
        String third = run.getRivalStrategyProfileId(3);
        assertTrue(!first.equals(second));
        assertTrue(!first.equals(third));
        assertTrue(!second.equals(third));
    }

    @Test
    public void eachRaceDrawsFreshAssignments() {
        CardStrategyCatalog catalog = new CardStrategyCatalog(
                Arrays.<CardStrategy>asList(
                        new FirstOfferStrategy("strategy00"),
                        new FirstOfferStrategy("strategy01"),
                        new FirstOfferStrategy("strategy02")));
        RogueliteRun run = new RogueliteRun(713L);
        run.configureCardStrategies(catalog);
        List<Integer> vehicleIds = Arrays.asList(
                Integer.valueOf(1), Integer.valueOf(2), Integer.valueOf(3),
                Integer.valueOf(4), Integer.valueOf(5), Integer.valueOf(6),
                Integer.valueOf(7), Integer.valueOf(8), Integer.valueOf(9));
        run.assignRivalStrategiesForRace(vehicleIds);
        String firstRace = assignmentSignature(run, vehicleIds);

        run.assignRivalStrategiesForRace(vehicleIds);

        assertTrue(!firstRace.equals(assignmentSignature(run, vehicleIds)));
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

    private static String assignmentSignature(
            RogueliteRun run,
            List<Integer> vehicleIds) {
        StringBuilder signature = new StringBuilder();
        for (Integer vehicleId : vehicleIds) {
            signature.append(run.getRivalStrategyProfileId(vehicleId.intValue())).append('|');
        }
        return signature.toString();
    }
}
