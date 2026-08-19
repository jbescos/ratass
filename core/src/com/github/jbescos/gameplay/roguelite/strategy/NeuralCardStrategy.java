package com.github.jbescos.gameplay.roguelite.strategy;

import com.github.jbescos.ai.rl.RlPolicy;
import com.github.jbescos.gameplay.roguelite.RogueliteCardOffer;
import java.util.List;

/** Scores every legal offer and the skip action with one shared value network. */
public final class NeuralCardStrategy implements CardStrategy {
    private final String profileId;
    private final String displayName;
    private final RlPolicy policy;
    private final CardStrategyObservationEncoder encoder;
    private final float[] observation;
    private final float[] scratchA;
    private final float[] scratchB;
    private final float[] output;

    public NeuralCardStrategy(String profileId, RlPolicy policy) {
        this(profileId, profileId, policy);
    }

    public NeuralCardStrategy(String profileId, String displayName, RlPolicy policy) {
        if (profileId == null || profileId.trim().length() == 0 || policy == null) {
            throw new IllegalArgumentException("A neural card strategy requires an ID and policy.");
        }
        this.profileId = profileId.trim();
        this.displayName = displayName == null || displayName.trim().length() == 0
                ? this.profileId : displayName.trim();
        this.policy = policy;
        encoder = new CardStrategyObservationEncoder();
        if (policy.getObservationSize() != encoder.getObservationSize()
                || policy.getActionSize() != 1) {
            throw new IllegalArgumentException("Card strategy policy shape does not match the encoder.");
        }
        observation = new float[encoder.getObservationSize()];
        scratchA = new float[policy.getScratchSize()];
        scratchB = new float[policy.getScratchSize()];
        output = new float[1];
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
        List<RogueliteCardOffer> offers = decision.getOffers();
        RogueliteCardOffer selected = null;
        float bestScore = score(decision, null);
        for (int i = 0; i < offers.size(); i++) {
            RogueliteCardOffer candidate = offers.get(i);
            float score = score(decision, candidate);
            if (score > bestScore) {
                selected = candidate;
                bestScore = score;
            }
        }
        return selected;
    }

    private float score(
            CardStrategyDecision decision,
            RogueliteCardOffer candidate) {
        encoder.encode(decision, candidate, observation);
        policy.computeOutputs(observation, scratchA, scratchB, output);
        return Float.isNaN(output[0]) ? -Float.MAX_VALUE : output[0];
    }
}
