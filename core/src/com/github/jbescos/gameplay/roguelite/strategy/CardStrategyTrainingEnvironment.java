package com.github.jbescos.gameplay.roguelite.strategy;

import com.github.jbescos.ai.rl.RlPolicy;
import com.github.jbescos.gameplay.roguelite.AntennaNetworkBonuses;
import com.github.jbescos.gameplay.roguelite.RogueliteCarUpgrades;
import com.github.jbescos.gameplay.roguelite.DriverProfileCatalog;
import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import com.github.jbescos.gameplay.roguelite.RogueliteCardOffer;
import com.github.jbescos.gameplay.roguelite.RogueliteCompetitorProgress;
import com.github.jbescos.gameplay.roguelite.RogueliteExperienceAwards;
import com.github.jbescos.gameplay.roguelite.RogueliteRun;
import com.github.jbescos.gameplay.roguelite.RogueliteSlotType;
import com.github.jbescos.gameplay.roguelite.RogueliteSetCatalog;
import com.github.jbescos.gameplay.roguelite.RogueliteSetDefinition;
import com.github.jbescos.gameplay.roguelite.RogueliteSetId;
import com.github.jbescos.gameplay.roguelite.RivalBuildLeechSpec;
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
    private final int[] cardTypeSelections = new int[RogueliteSlotType.values().length];
    private final boolean[] completedSets = new boolean[RogueliteSetId.values().length];
    private final int[] championshipSetCounts = new int[RogueliteSetId.values().length];

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
    private int setCompletionCount;
    private int minimumChampionships = 1;
    private int maximumChampionships = 1;
    private int targetChampionships = 1;
    private int completedChampionshipCount;
    private int championshipWinCount;
    private int championshipPositionSum;
    private int firstChampionshipPosition;
    private int championshipsWithSet;
    private CardStrategy selfPlayStrategy;
    private boolean selfPlayOpponents;
    private CardStrategyCatalog mixedOpponentCatalog;
    private boolean mixedOpponents;

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
        } else if (mixedOpponents && mixedOpponentCatalog != null) {
            run.configureCardStrategies(mixedOpponentCatalog);
        }
        raceEstimator = new CardStrategyRaceEstimator(driverCatalog);
        points = new int[fieldSize];
        racePositions = new int[fieldSize];
        championshipPositions = new int[fieldSize];
        Arrays.fill(racePositions, 1);
        Arrays.fill(championshipPositions, 1);
        Arrays.fill(cardSelections, 0);
        Arrays.fill(cardTypeSelections, 0);
        Arrays.fill(completedSets, false);
        Arrays.fill(championshipSetCounts, 0);
        List<Integer> rivalIds = new ArrayList<Integer>();
        for (int vehicleId = 1; vehicleId < fieldSize; vehicleId++) {
            run.getRivalProgress(vehicleId);
            rivalIds.add(Integer.valueOf(vehicleId));
        }
        if (mixedOpponents && mixedOpponentCatalog != null && !selfPlayOpponents) {
            run.assignRivalStrategiesForRace(rivalIds);
        }
        circuitIndex = 1;
        nextLap = 1;
        weatherGripWeight = randomWeatherGripWeight();
        done = false;
        transitionReward = 0f;
        totalExperience = 0;
        finalPosition = 0;
        setCompletionCount = 0;
        targetChampionships = championshipTarget(seed);
        completedChampionshipCount = 0;
        championshipWinCount = 0;
        championshipPositionSum = 0;
        firstChampionshipPosition = 0;
        championshipsWithSet = 0;
        advanceUntilDecision();
        transitionReward = 0f;
    }

    public void setChampionshipRange(int minimum, int maximum) {
        if (minimum < 1 || maximum < minimum) {
            throw new IllegalArgumentException("Invalid championship training range.");
        }
        minimumChampionships = minimum;
        maximumChampionships = maximum;
    }

    public int getMinimumChampionships() {
        return minimumChampionships;
    }

    public int getMaximumChampionships() {
        return maximumChampionships;
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

    public void setMixedOpponentPolicies(String[] profileIds, String[] policyJsons) {
        if (profileIds == null
                || policyJsons == null
                || profileIds.length == 0
                || profileIds.length != policyJsons.length) {
            throw new IllegalArgumentException("Mixed opponent policies are invalid.");
        }
        List<CardStrategy> strategies = new ArrayList<CardStrategy>();
        for (int i = 0; i < profileIds.length; i++) {
            strategies.add(new NeuralCardStrategy(
                    profileIds[i], RlPolicy.fromJson(policyJsons[i])));
        }
        mixedOpponentCatalog = new CardStrategyCatalog(strategies);
    }

    public void setMixedOpponents(boolean enabled) {
        mixedOpponents = enabled;
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
        int priorTypeSelections = selected == null
                ? 0 : cardTypeSelections[selected.getSlotType().ordinal()];
        float selectionReward = rewards.selection(
                selected,
                run.getPlayerProgress().getLoadout(),
                priorSelections,
                priorTypeSelections,
                averageTypeSelections(),
                context.equippedCandidateOverlap(selected),
                context.getEnabledSetIds());
        boolean setBuildingSelection = selected != null
                && !selected.isDriver()
                && rewards.rewardsSetBuilding()
                && RogueliteSetCatalog.selectionProgress(
                        run.getPlayerProgress().getLoadout(),
                        selected.getCard().getId(),
                        context.getEnabledSetIds()) > 0;
        if (selectionReward > 0f
                && selected != null
                && !setBuildingSelection
                && !isCompetitiveOffer(actionIndex)) {
            selectionReward = 0f;
        }
        RogueliteSetDefinition completedBefore = RogueliteSetCatalog.completedSet(
                run.getPlayerProgress().getLoadout(), context.getEnabledSetIds());
        boolean applied = selected == null
                ? run.skipPlayerReward()
                : run.select(selected);
        if (!applied) {
            throw new IllegalStateException("The selected strategic action was not legal.");
        }
        if (selected != null && !selected.isDriver()) {
            cardSelections[selected.getCard().getId().ordinal()]++;
        }
        if (selected != null) {
            cardTypeSelections[selected.getSlotType().ordinal()]++;
        }
        RogueliteSetDefinition completedAfter = RogueliteSetCatalog.completedSet(
                run.getPlayerProgress().getLoadout(), context.getEnabledSetIds());
        if (completedAfter != null && completedAfter != completedBefore) {
            setCompletionCount++;
            completedSets[completedAfter.getId().ordinal()] = true;
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

    public int getCompletedChampionshipCount() {
        return completedChampionshipCount;
    }

    public int getChampionshipWinCount() {
        return championshipWinCount;
    }

    public int getChampionshipPositionSum() {
        return championshipPositionSum;
    }

    public int getFirstChampionshipPosition() {
        return firstChampionshipPosition;
    }

    public int getChampionshipsWithSet() {
        return championshipsWithSet;
    }

    public String[] getChampionshipSetIds() {
        List<String> ids = new ArrayList<String>();
        for (RogueliteSetId setId : RogueliteSetId.values()) {
            if (championshipSetCounts[setId.ordinal()] > 0) {
                ids.add(setId.name());
            }
        }
        return ids.toArray(new String[ids.size()]);
    }

    public int[] getChampionshipSetCounts() {
        return Arrays.copyOf(championshipSetCounts, championshipSetCounts.length);
    }

    public String[] getChampionshipSetOccurrences() {
        List<String> ids = new ArrayList<String>();
        for (RogueliteSetId setId : RogueliteSetId.values()) {
            for (int count = 0; count < championshipSetCounts[setId.ordinal()]; count++) {
                ids.add(setId.name());
            }
        }
        return ids.toArray(new String[ids.size()]);
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

    public float[] getOfferStatSynergyGains() {
        ensureDecision();
        float[] gains = new float[offers.size() + 1];
        RogueliteCompetitorProgress progress = run.getPlayerProgress();
        for (int i = 0; i < offers.size(); i++) {
            RogueliteCardOffer offer = offers.get(i);
            if (!offer.isDriver()) {
                gains[i] = Math.max(0f, TuningTechniqueSynergy.statSelectionGain(
                        progress.getLoadout(), offer.getCard().getId()));
            }
        }
        return gains;
    }

    public int[] getOfferSetProgressGains() {
        ensureDecision();
        int[] gains = new int[offers.size() + 1];
        for (int i = 0; i < offers.size(); i++) {
            RogueliteCardOffer offer = offers.get(i);
            if (!offer.isDriver()) {
                gains[i] = RogueliteSetCatalog.selectionProgress(
                        run.getPlayerProgress().getLoadout(),
                        offer.getCard().getId(),
                        context.getEnabledSetIds());
            }
        }
        return gains;
    }

    public int[] getOfferSetDepths() {
        ensureDecision();
        int[] depths = new int[offers.size() + 1];
        for (int i = 0; i < offers.size(); i++) {
            RogueliteCardOffer offer = offers.get(i);
            if (!offer.isDriver()) {
                depths[i] = RogueliteSetCatalog.selectionDepth(
                        run.getPlayerProgress().getLoadout(),
                        offer.getCard().getId(),
                        context.getEnabledSetIds());
            }
        }
        return depths;
    }

    public int getSetCompletionCount() {
        return setCompletionCount;
    }

    public String[] getCompletedSetIds() {
        List<String> ids = new ArrayList<String>();
        for (RogueliteSetId setId : RogueliteSetId.values()) {
            if (completedSets[setId.ordinal()]) {
                ids.add(setId.name());
            }
        }
        return ids.toArray(new String[ids.size()]);
    }

    public String getCurrentCompletedSetId() {
        if (run == null) {
            return "";
        }
        RogueliteSetDefinition completed = RogueliteSetCatalog.completedSet(
                run.getPlayerProgress().getLoadout(), run.getEnabledSetIds());
        return completed == null ? "" : completed.getId().name();
    }

    public int getBestSetProgress() {
        return run == null
                ? 0
                : RogueliteSetCatalog.bestMatchingCardCount(
                        run.getPlayerProgress().getLoadout(), run.getEnabledSetIds());
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
            AntennaNetworkBonuses network = buildAntennaNetwork(offers.get(i));
            strengths[i] = raceEstimator.estimate(
                    progress,
                    offers.get(i),
                    STRATEGY_TEACHER_GRIP_WEIGHT,
                    network);
        }
        strengths[offers.size()] = raceEstimator.estimate(
                progress,
                STRATEGY_TEACHER_GRIP_WEIGHT,
                buildAntennaNetwork(null));
        return strengths;
    }

    public float[] getTrainingTargetScores() {
        float[] raceStrengths = getRaceStrengthScores();
        float[] strengths = Arrays.copyOf(raceStrengths, raceStrengths.length);
        for (int i = 0; i < offers.size(); i++) {
            RogueliteCardOffer offer = offers.get(i);
            int priorSelections = offer.isDriver()
                    ? 0 : cardSelections[offer.getCard().getId().ordinal()];
            int priorTypeSelections = cardTypeSelections[offer.getSlotType().ordinal()];
            float personalityReward = rewards.selection(
                            offer,
                            run.getPlayerProgress().getLoadout(),
                            priorSelections,
                            priorTypeSelections,
                            averageTypeSelections(),
                            context.equippedCandidateOverlap(offer),
                            context.getEnabledSetIds());
            boolean setBuildingSelection = !offer.isDriver()
                    && rewards.rewardsSetBuilding()
                    && RogueliteSetCatalog.selectionProgress(
                            run.getPlayerProgress().getLoadout(),
                            offer.getCard().getId(),
                            context.getEnabledSetIds()) > 0;
            if (personalityReward > 0f
                    && !setBuildingSelection
                    && !isCompetitiveOffer(raceStrengths, i)) {
                personalityReward = 0f;
            }
            strengths[i] += personalityReward * personalityTeacherWeight;
        }
        strengths[offers.size()] += rewards.selection(
                        null,
                        run.getPlayerProgress().getLoadout(),
                        0,
                        0,
                        0f,
                        0f,
                        context.getEnabledSetIds())
                * personalityTeacherWeight;
        return strengths;
    }

    public int getTrainingTargetAction() {
        float[] strengths = getTrainingTargetScores();
        int selected = strengths.length - 1;
        float best = strengths[selected];
        for (int i = 0; i < strengths.length - 1; i++) {
            if (strengths[i] > best) {
                selected = i;
                best = strengths[i];
            }
        }
        return selected;
    }

    private boolean isCompetitiveOffer(int actionIndex) {
        return isCompetitiveOffer(getRaceStrengthScores(), actionIndex);
    }

    private boolean isCompetitiveOffer(float[] strengths, int actionIndex) {
        if (actionIndex < 0 || actionIndex >= offers.size()) {
            return false;
        }
        int strongerOffers = 0;
        for (int i = 0; i < offers.size(); i++) {
            if (strengths[i] > strengths[actionIndex]) {
                strongerOffers++;
            }
        }
        return strongerOffers < 2;
    }

    private float averageTypeSelections() {
        int total = 0;
        for (int count : cardTypeSelections) {
            total += count;
        }
        return total / (float) cardTypeSelections.length;
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
        transitionReward += rewards.lapWin(racePositions[0]);
        updateRunRaceState();
        AntennaNetworkBonuses network = buildAntennaNetwork(null);
        int[] lapExperience = new int[fieldSize];
        float[] lapExperienceMultipliers = new float[fieldSize];
        int[] lapExperienceCaps = new int[fieldSize];
        for (int vehicleId = 0; vehicleId < fieldSize; vehicleId++) {
            RogueliteCompetitorProgress progress = progress(vehicleId);
            lapExperienceMultipliers[vehicleId] =
                    lapExperienceMultiplier(vehicleId, network);
            lapExperienceCaps[vehicleId] = run.getRacecraftXpPerLapCap(
                    lapExperienceMultipliers[vehicleId]);
            float strength = raceEstimator.estimate(
                    progress, weatherGripWeight, network);
            int amount = Math.max(
                    4,
                    Math.min(
                            lapExperienceCaps[vehicleId],
                            run.getRacecraftXpAward(
                                            RogueliteExperienceAwards.Reason.LAP_COMPLETE)
                                    + Math.round(8f + strength * 4f + random.nextInt(7))));
            lapExperience[vehicleId] = amount;
        }
        applyExpectedBuildLeechTransfers(lapExperience, lapExperienceCaps);
        for (int vehicleId = 0; vehicleId < fieldSize; vehicleId++) {
            accumulateLapExperience(
                    vehicleId,
                    lapExperience[vehicleId],
                    lapExperienceMultipliers[vehicleId]);
        }
        for (int vehicleId = 0; vehicleId < fieldSize; vehicleId++) {
            bankLapExperience(vehicleId);
        }
    }

    private void applyExpectedBuildLeechTransfers(
            int[] lapExperience,
            int[] lapExperienceCaps) {
        List<Integer> recipients = new ArrayList<Integer>(fieldSize);
        for (int vehicleId = 0; vehicleId < fieldSize; vehicleId++) {
            recipients.add(Integer.valueOf(vehicleId));
        }
        Collections.shuffle(recipients, random);
        for (int i = 0; i < recipients.size(); i++) {
            int recipientId = recipients.get(i).intValue();
            RogueliteCardId revenge = progress(recipientId)
                    .getLoadout()
                    .get(RogueliteSlotType.REVENGE);
            if (!RivalBuildLeechSpec.isCard(revenge)) {
                continue;
            }
            int offenderId = random.nextInt(fieldSize - 1);
            if (offenderId >= recipientId) {
                offenderId++;
            }
            int requested = Math.round(
                    lapExperience[offenderId]
                            * RivalBuildLeechSpec.expectedLapTransferFraction(revenge));
            int transferred = Math.min(
                    requested,
                    Math.min(
                            lapExperience[offenderId],
                            Math.max(
                                    0,
                                    lapExperienceCaps[recipientId]
                                            - lapExperience[recipientId])));
            lapExperience[offenderId] -= transferred;
            lapExperience[recipientId] += transferred;
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

    private void accumulateLapExperience(
            int vehicleId,
            int amount,
            float lapExperienceMultiplier) {
        if (vehicleId == 0) {
            run.awardPlayerRacecraftExperience(amount, lapExperienceMultiplier);
        } else {
            run.awardRivalRacecraftExperience(
                    vehicleId,
                    amount,
                    lapExperienceMultiplier);
        }
    }

    private float lapExperienceMultiplier(
            int vehicleId,
            AntennaNetworkBonuses network) {
        RogueliteCarUpgrades upgrades = new RogueliteCarUpgrades();
        upgrades.configure(progress(vehicleId).getLoadout());
        upgrades.setAntennaNetwork(network);
        return upgrades.getLapExperienceBankMultiplier();
    }

    private void bankLapExperience(int vehicleId) {
        RogueliteCompetitorProgress progress = progress(vehicleId);
        int beforeLevel = progress.getLevel();
        int requirement = progress.getExperienceForNextLevel();
        float multiplier = lapExperienceMultiplier(
                vehicleId,
                buildAntennaNetwork(null));
        int gained = vehicleId == 0
                ? run.bankPlayerLapExperience(multiplier)
                : run.bankRivalLapExperience(vehicleId, multiplier);
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
        AntennaNetworkBonuses network = buildAntennaNetwork(null);
        for (int vehicleId = 0; vehicleId < fieldSize; vehicleId++) {
            float noise = (random.nextFloat() + random.nextFloat() + random.nextFloat() - 1.5f)
                    * 0.18f;
            ranked.add(new RankedCompetitor(
                    vehicleId,
                    raceEstimator.estimate(
                            progress(vehicleId), weatherGripWeight, network) + noise));
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
        completedChampionshipCount++;
        championshipPositionSum += finalPosition;
        championshipWinCount += finalPosition == 1 ? 1 : 0;
        if (completedChampionshipCount == 1) {
            firstChampionshipPosition = finalPosition;
        }
        RogueliteSetDefinition completedSet = RogueliteSetCatalog.completedSet(
                run.getPlayerProgress().getLoadout(), run.getEnabledSetIds());
        if (completedSet != null) {
            championshipsWithSet++;
            championshipSetCounts[completedSet.getId().ordinal()]++;
        }
        if (completedChampionshipCount >= targetChampionships) {
            done = true;
            return;
        }
        continueChampionship();
    }

    private void continueChampionship() {
        run.restartChampionship();
        Arrays.fill(points, 0);
        Arrays.fill(racePositions, 1);
        Arrays.fill(championshipPositions, 1);
        circuitIndex = 1;
        nextLap = 1;
        weatherGripWeight = randomWeatherGripWeight();
    }

    private int championshipTarget(long seed) {
        if (minimumChampionships == maximumChampionships) {
            return minimumChampionships;
        }
        Random championshipRandom = new Random(seed ^ 0x43a5c9e27d4b1f60L);
        return minimumChampionships
                + championshipRandom.nextInt(maximumChampionships - minimumChampionships + 1);
    }

    private AntennaNetworkBonuses buildAntennaNetwork(
            RogueliteCardOffer playerPreview) {
        AntennaNetworkBonuses.Builder builder = AntennaNetworkBonuses.builder();
        RogueliteCardId previewCard = playerPreview == null || playerPreview.isDriver()
                ? null
                : playerPreview.getCard().getId();
        for (int vehicleId = 0; vehicleId < fieldSize; vehicleId++) {
            if (vehicleId == 0) {
                builder.include(progress(vehicleId).getLoadout(), previewCard);
            } else {
                builder.include(progress(vehicleId).getLoadout());
            }
        }
        return builder.build();
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
                    driverCatalog.get(progress.getLoadout().getDriverProfileId()),
                    progress.getLoadout()));
        }
        return new CardStrategyContext(
                circuitIndex,
                circuitCount,
                Math.min(nextLap, lapCount),
                lapCount,
                racePositions[0],
                championshipPositions[0],
                Math.max(0, circuitCount - circuitIndex),
                opponents,
                run.getEnabledSetIds());
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
