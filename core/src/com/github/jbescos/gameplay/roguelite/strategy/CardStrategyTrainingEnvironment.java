package com.github.jbescos.gameplay.roguelite.strategy;

import com.github.jbescos.ai.rl.RlPolicy;
import com.github.jbescos.gameplay.roguelite.DriverProfileCatalog;
import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import com.github.jbescos.gameplay.roguelite.RogueliteCardOffer;
import com.github.jbescos.gameplay.roguelite.RogueliteCompetitorProgress;
import com.github.jbescos.gameplay.roguelite.RogueliteExperienceAwards;
import com.github.jbescos.gameplay.roguelite.RogueliteRun;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

/**
 * Event-driven strategic environment. It uses real card/progression rules and a fast race estimate;
 * no rendering, physics, driving observations, or presentation state are created.
 */
public final class CardStrategyTrainingEnvironment {
    private static final int[] POSITION_POINTS = {25, 18, 15, 12, 10, 8, 6, 4, 2, 1};
    private static final int OFFER_COUNT = 3;
    private static final float STRATEGY_TEACHER_GRIP_WEIGHT = 1.45f;
    private static final float DEFAULT_PERSONALITY_TEACHER_WEIGHT = 0.0035f;

    private final DriverProfileCatalog driverCatalog;
    private final CardStrategyRewardCalculator rewards;
    private final CardStrategyObservationEncoder encoder =
            new CardStrategyObservationEncoder();
    private final int fieldSize;
    private final int circuitCount;
    private final int lapCount;
    private final float personalityTeacherWeight;
    private final int[] cardSelections = new int[RogueliteCardId.values().length];

    private Random random;
    private Random strategyRandom;
    private RogueliteRun run;
    private CardStrategyRaceEstimator raceEstimator;
    private int[] points;
    private int[] racePositions;
    private int[] championshipPositions;
    private int circuitIndex;
    private int nextLap;
    private float weatherGripWeight;
    private List<RogueliteCardOffer> offers = Collections.emptyList();
    private CardStrategyContext context = CardStrategyContext.empty();
    private boolean done;
    private float transitionReward;
    private int totalExperience;
    private int finalPosition;
    private CardStrategy selfPlayStrategy;
    private boolean selfPlayOpponents;

    public CardStrategyTrainingEnvironment(
            DriverProfileCatalog driverCatalog,
            CardStrategyRewardConfig rewardConfig,
            int fieldSize,
            int circuitCount,
            int lapCount) {
        this(
                driverCatalog,
                rewardConfig,
                fieldSize,
                circuitCount,
                lapCount,
                DEFAULT_PERSONALITY_TEACHER_WEIGHT);
    }

    public CardStrategyTrainingEnvironment(
            DriverProfileCatalog driverCatalog,
            CardStrategyRewardConfig rewardConfig,
            int fieldSize,
            int circuitCount,
            int lapCount,
            float personalityTeacherWeight) {
        if (driverCatalog == null
                || fieldSize < 2
                || fieldSize > POSITION_POINTS.length
                || circuitCount < 1
                || lapCount < 1) {
            throw new IllegalArgumentException("Invalid card strategy training configuration.");
        }
        this.driverCatalog = driverCatalog;
        rewards = new CardStrategyRewardCalculator(rewardConfig);
        this.fieldSize = fieldSize;
        this.circuitCount = circuitCount;
        this.lapCount = lapCount;
        this.personalityTeacherWeight = finiteNonNegative(personalityTeacherWeight);
    }

    public void reset(long seed) {
        random = new Random(seed);
        strategyRandom = new Random(seed ^ 0x5deece66dL);
        run = new RogueliteRun(seed, driverCatalog);
        if (selfPlayOpponents && selfPlayStrategy != null) {
            run.configureCardStrategies(CardStrategyCatalog.fixed(selfPlayStrategy));
        }
        raceEstimator = new CardStrategyRaceEstimator(driverCatalog);
        points = new int[fieldSize];
        racePositions = new int[fieldSize];
        championshipPositions = new int[fieldSize];
        Arrays.fill(racePositions, 1);
        Arrays.fill(championshipPositions, 1);
        Arrays.fill(cardSelections, 0);
        for (int vehicleId = 1; vehicleId < fieldSize; vehicleId++) {
            run.getRivalProgress(vehicleId);
        }
        circuitIndex = 1;
        nextLap = 1;
        weatherGripWeight = randomWeatherGripWeight();
        done = false;
        transitionReward = 0f;
        totalExperience = 0;
        finalPosition = 0;
        advanceUntilDecision();
        transitionReward = 0f;
    }

    public void setSelfPlayPolicyJson(String policyJson) {
        if (policyJson == null || policyJson.trim().length() == 0) {
            throw new IllegalArgumentException("A self-play policy is required.");
        }
        selfPlayStrategy = new NeuralCardStrategy(
                "selfplay",
                RlPolicy.fromJson(policyJson));
    }

    public void setSelfPlayOpponents(boolean enabled) {
        selfPlayOpponents = enabled;
    }

    public float step(int actionIndex) {
        ensureDecision();
        if (actionIndex < 0 || actionIndex > offers.size()) {
            throw new IllegalArgumentException("Card strategy action is out of range.");
        }
        RogueliteCardOffer selected = actionIndex == offers.size()
                ? null : offers.get(actionIndex);
        int priorSelections = selected == null || selected.isDriver()
                ? 0 : cardSelections[selected.getCard().getId().ordinal()];
        float selectionReward = rewards.selection(
                selected, run.getPlayerProgress().getLoadout(), priorSelections);
        boolean applied = selected == null
                ? run.skipPlayerReward()
                : run.select(selected);
        if (!applied) {
            throw new IllegalStateException("The selected strategic action was not legal.");
        }
        if (selected != null && !selected.isDriver()) {
            cardSelections[selected.getCard().getId().ordinal()]++;
        }
        transitionReward = selectionReward;
        offers = Collections.emptyList();
        advanceUntilDecision();
        return transitionReward;
    }

    public float[][] getCandidateObservations() {
        ensureDecision();
        CardStrategyDecision decision = new CardStrategyDecision(
                run.getPlayerProgress(), driverCatalog, offers, context);
        float[][] observations = new float[offers.size() + 1][];
        for (int i = 0; i < offers.size(); i++) {
            observations[i] = encoder.encode(decision, offers.get(i));
        }
        observations[offers.size()] = encoder.encode(decision, null);
        return observations;
    }

    public int getObservationSize() {
        return encoder.getObservationSize();
    }

    public int getActionCount() {
        return done ? 0 : offers.size() + 1;
    }

    public boolean isDone() {
        return done;
    }

    public int getFinalPosition() {
        return finalPosition;
    }

    public int getLevel() {
        return run == null ? 1 : run.getPlayerProgress().getLevel();
    }

    public int getTotalExperience() {
        return totalExperience;
    }

    public String[] getOfferIds() {
        ensureDecision();
        String[] ids = new String[offers.size() + 1];
        for (int i = 0; i < offers.size(); i++) {
            ids[i] = offers.get(i).getOfferId();
        }
        ids[offers.size()] = "skip";
        return ids;
    }

    public int[] getOfferTiers() {
        ensureDecision();
        int[] tiers = new int[offers.size() + 1];
        for (int i = 0; i < offers.size(); i++) {
            tiers[i] = offers.get(i).getTier();
        }
        return tiers;
    }

    public String[] getOfferTypes() {
        ensureDecision();
        String[] types = new String[offers.size() + 1];
        for (int i = 0; i < offers.size(); i++) {
            types[i] = offers.get(i).getSlotType().name().toLowerCase();
        }
        types[offers.size()] = "skip";
        return types;
    }

    public int getAlgorithmicAction() {
        ensureDecision();
        CardStrategyDecision decision = new CardStrategyDecision(
                run.getPlayerProgress(), driverCatalog, offers, context);
        RogueliteCardOffer selected = new AlgorithmicCardStrategy().choose(
                decision,
                new CardStrategyRandom() {
                    @Override
                    public int nextInt(int bound) {
                        return strategyRandom.nextInt(bound);
                    }
                });
        if (selected == null) {
            return offers.size();
        }
        for (int i = 0; i < offers.size(); i++) {
            if (offers.get(i).getOfferId().equals(selected.getOfferId())) {
                return i;
            }
        }
        throw new IllegalStateException("Algorithmic strategy selected an unavailable offer.");
    }

    public int getRaceStrengthAction() {
        ensureDecision();
        float[] strengths = getRaceStrengthScores();
        int selected = strengths.length - 1;
        float bestStrength = strengths[selected];
        for (int i = 0; i < strengths.length - 1; i++) {
            if (strengths[i] > bestStrength) {
                selected = i;
                bestStrength = strengths[i];
            }
        }
        return selected;
    }

    public float[] getRaceStrengthScores() {
        ensureDecision();
        RogueliteCompetitorProgress progress = run.getPlayerProgress();
        float[] strengths = new float[offers.size() + 1];
        for (int i = 0; i < offers.size(); i++) {
            strengths[i] = raceEstimator.estimate(
                    progress,
                    offers.get(i),
                    STRATEGY_TEACHER_GRIP_WEIGHT);
        }
        strengths[offers.size()] = raceEstimator.estimate(
                progress,
                STRATEGY_TEACHER_GRIP_WEIGHT);
        return strengths;
    }

    public float[] getTrainingTargetScores() {
        float[] strengths = getRaceStrengthScores();
        for (int i = 0; i < offers.size(); i++) {
            RogueliteCardOffer offer = offers.get(i);
            int priorSelections = offer.isDriver()
                    ? 0 : cardSelections[offer.getCard().getId().ordinal()];
            strengths[i] += rewards.selection(
                            offer, run.getPlayerProgress().getLoadout(), priorSelections)
                    * personalityTeacherWeight;
        }
        strengths[offers.size()] += rewards.selection(
                        null, run.getPlayerProgress().getLoadout(), 0)
                * personalityTeacherWeight;
        return strengths;
    }

    private void advanceUntilDecision() {
        while (!done && !run.getPlayerProgress().hasPendingReward()) {
            if (nextLap <= lapCount) {
                simulateLapExperience();
                nextLap++;
            } else {
                simulateRaceFinish();
                circuitIndex++;
                nextLap = 1;
                if (circuitIndex > circuitCount) {
                    finishChampionship();
                } else {
                    weatherGripWeight = randomWeatherGripWeight();
                }
            }
        }
        if (done) {
            offers = Collections.emptyList();
            return;
        }
        offers = run.createOffers(OFFER_COUNT);
        if (offers.isEmpty()) {
            run.skipPlayerReward();
            advanceUntilDecision();
            return;
        }
        context = buildContext();
    }

    private void simulateLapExperience() {
        run.resetAllLapExperience();
        rankCompetitors();
        updateRunRaceState();
        for (int vehicleId = 0; vehicleId < fieldSize; vehicleId++) {
            RogueliteCompetitorProgress progress = progress(vehicleId);
            float strength = raceEstimator.estimate(progress, weatherGripWeight);
            int amount = Math.max(
                    4,
                    Math.min(
                            RogueliteExperienceAwards.MAX_RACECRAFT_XP_PER_LAP,
                            Math.round(8f + strength * 4f + random.nextInt(7))));
            awardExperience(vehicleId, amount);
        }
    }

    private void simulateRaceFinish() {
        rankCompetitors();
        updateRunRaceState();
        for (int vehicleId = 0; vehicleId < fieldSize; vehicleId++) {
            int position = racePositions[vehicleId];
            points[vehicleId] += POSITION_POINTS[position - 1];
            int beforeLevel = progress(vehicleId).getLevel();
            int beforeRequirement = progress(vehicleId).getExperienceForNextLevel();
            int gained = vehicleId == 0
                    ? run.awardPlayerRacePosition(position, fieldSize)
                    : run.awardRivalRacePosition(vehicleId, position, fieldSize);
            if (vehicleId == 0) {
                totalExperience += gained;
                transitionReward += rewards.experience(
                        gained,
                        beforeRequirement,
                        progress(0).getLevel() - beforeLevel);
                transitionReward += rewards.racePosition(position, fieldSize);
            } else {
                run.resolveRivalReward(vehicleId);
            }
        }
        updateChampionshipPositions();
    }

    private void awardExperience(int vehicleId, int amount) {
        RogueliteCompetitorProgress progress = progress(vehicleId);
        int beforeLevel = progress.getLevel();
        int requirement = progress.getExperienceForNextLevel();
        int gained = vehicleId == 0
                ? run.awardPlayerRacecraftExperience(amount)
                : run.awardRivalRacecraftExperience(vehicleId, amount);
        if (vehicleId == 0) {
            totalExperience += gained;
            transitionReward += rewards.experience(
                    gained, requirement, progress.getLevel() - beforeLevel);
        } else {
            run.resolveRivalReward(vehicleId);
        }
    }

    private void rankCompetitors() {
        List<RankedCompetitor> ranked = new ArrayList<RankedCompetitor>(fieldSize);
        for (int vehicleId = 0; vehicleId < fieldSize; vehicleId++) {
            float noise = (random.nextFloat() + random.nextFloat() + random.nextFloat() - 1.5f)
                    * 0.18f;
            ranked.add(new RankedCompetitor(
                    vehicleId,
                    raceEstimator.estimate(progress(vehicleId), weatherGripWeight) + noise));
        }
        Collections.sort(ranked, new Comparator<RankedCompetitor>() {
            @Override
            public int compare(RankedCompetitor left, RankedCompetitor right) {
                int score = Float.compare(right.score, left.score);
                return score != 0 ? score : Integer.compare(left.vehicleId, right.vehicleId);
            }
        });
        for (int i = 0; i < ranked.size(); i++) {
            racePositions[ranked.get(i).vehicleId] = i + 1;
        }
    }

    private void updateChampionshipPositions() {
        List<Integer> ids = new ArrayList<Integer>(fieldSize);
        for (int vehicleId = 0; vehicleId < fieldSize; vehicleId++) {
            ids.add(Integer.valueOf(vehicleId));
        }
        Collections.sort(ids, new Comparator<Integer>() {
            @Override
            public int compare(Integer left, Integer right) {
                int score = Integer.compare(points[right.intValue()], points[left.intValue()]);
                if (score != 0) {
                    return score;
                }
                return Integer.compare(
                        racePositions[left.intValue()], racePositions[right.intValue()]);
            }
        });
        for (int i = 0; i < ids.size(); i++) {
            championshipPositions[ids.get(i).intValue()] = i + 1;
        }
    }

    private void finishChampionship() {
        updateChampionshipPositions();
        finalPosition = championshipPositions[0];
        transitionReward += rewards.championship(finalPosition, fieldSize);
        done = true;
    }

    private CardStrategyContext buildContext() {
        List<CardStrategyContext.Opponent> opponents =
                new ArrayList<CardStrategyContext.Opponent>(fieldSize - 1);
        List<Integer> ids = new ArrayList<Integer>(fieldSize - 1);
        for (int vehicleId = 1; vehicleId < fieldSize; vehicleId++) {
            ids.add(Integer.valueOf(vehicleId));
        }
        Collections.sort(ids, new Comparator<Integer>() {
            @Override
            public int compare(Integer left, Integer right) {
                return Integer.compare(
                        racePositions[left.intValue()], racePositions[right.intValue()]);
            }
        });
        for (Integer id : ids) {
            int vehicleId = id.intValue();
            RogueliteCompetitorProgress progress = progress(vehicleId);
            opponents.add(new CardStrategyContext.Opponent(
                    progress.getLevel(),
                    racePositions[vehicleId],
                    championshipPositions[vehicleId],
                    driverCatalog.get(progress.getLoadout().getDriverProfileId())));
        }
        return new CardStrategyContext(
                circuitIndex,
                circuitCount,
                Math.min(nextLap, lapCount),
                lapCount,
                racePositions[0],
                championshipPositions[0],
                Math.max(0, circuitCount - circuitIndex),
                opponents);
    }

    private void updateRunRaceState() {
        CardStrategyRaceState.Builder builder = new CardStrategyRaceState.Builder()
                .race(circuitIndex, circuitCount, lapCount);
        for (int vehicleId = 0; vehicleId < fieldSize; vehicleId++) {
            builder.competitor(
                    vehicleId,
                    vehicleId == 0,
                    Math.min(nextLap, lapCount),
                    racePositions[vehicleId],
                    championshipPositions[vehicleId]);
        }
        run.updateCardStrategyRaceState(builder.build());
    }

    private RogueliteCompetitorProgress progress(int vehicleId) {
        return vehicleId == 0
                ? run.getPlayerProgress()
                : run.getRivalProgress(vehicleId);
    }

    private float randomWeatherGripWeight() {
        int value = random.nextInt(3);
        return value == 0 ? 1.25f : value == 1 ? 1.45f : 1.65f;
    }

    private void ensureDecision() {
        if (run == null || done || offers.isEmpty()) {
            throw new IllegalStateException("No card strategy decision is available.");
        }
    }

    private static final class RankedCompetitor {
        private final int vehicleId;
        private final float score;

        private RankedCompetitor(int vehicleId, float score) {
            this.vehicleId = vehicleId;
            this.score = score;
        }
    }

    private static float finiteNonNegative(float value) {
        return Float.isNaN(value) || Float.isInfinite(value)
                ? 0f : Math.max(0f, value);
    }
}
