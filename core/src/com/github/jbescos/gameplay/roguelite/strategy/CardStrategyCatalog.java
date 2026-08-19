package com.github.jbescos.gameplay.roguelite.strategy;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Available rival card strategies, including the algorithmic fallback. */
public final class CardStrategyCatalog {
    private final Map<String, CardStrategy> strategies =
            new LinkedHashMap<String, CardStrategy>();
    private final List<String> profileIds;

    public CardStrategyCatalog(List<? extends CardStrategy> neuralStrategies) {
        this(neuralStrategies, true);
    }

    private CardStrategyCatalog(
            List<? extends CardStrategy> neuralStrategies,
            boolean algorithmicSelectable) {
        add(new AlgorithmicCardStrategy());
        if (neuralStrategies != null) {
            for (CardStrategy strategy : neuralStrategies) {
                add(strategy);
            }
        }
        List<String> selectable = new ArrayList<String>(strategies.keySet());
        if (!algorithmicSelectable && selectable.size() > 1) {
            selectable.remove(AlgorithmicCardStrategy.PROFILE_ID);
        }
        profileIds = Collections.unmodifiableList(selectable);
    }

    public static CardStrategyCatalog algorithmicOnly() {
        return new CardStrategyCatalog(Collections.<CardStrategy>emptyList());
    }

    public static CardStrategyCatalog fixed(CardStrategy strategy) {
        if (strategy == null) {
            return algorithmicOnly();
        }
        return new CardStrategyCatalog(
                Collections.singletonList(strategy),
                false);
    }

    public CardStrategy get(String profileId) {
        CardStrategy strategy = strategies.get(profileId);
        return strategy == null
                ? strategies.get(AlgorithmicCardStrategy.PROFILE_ID)
                : strategy;
    }

    public boolean contains(String profileId) {
        return profileId != null && strategies.containsKey(profileId);
    }

    public List<String> getProfileIds() {
        return profileIds;
    }

    public String chooseProfileId(CardStrategyRandom random) {
        if (profileIds.size() == 1) {
            return profileIds.get(0);
        }
        return profileIds.get(random.nextInt(profileIds.size()));
    }

    private void add(CardStrategy strategy) {
        if (strategy == null
                || strategy.getProfileId() == null
                || strategy.getProfileId().trim().length() == 0
                || strategies.containsKey(strategy.getProfileId())) {
            return;
        }
        strategies.put(strategy.getProfileId(), strategy);
    }
}
