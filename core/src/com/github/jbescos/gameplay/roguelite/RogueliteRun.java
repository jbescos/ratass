package com.github.jbescos.gameplay.roguelite;

import com.github.jbescos.gameplay.roguelite.strategy.AlgorithmicCardStrategy;
import com.github.jbescos.gameplay.roguelite.strategy.CardStrategy;
import com.github.jbescos.gameplay.roguelite.strategy.CardStrategyCatalog;
import com.github.jbescos.gameplay.roguelite.strategy.CardStrategyContext;
import com.github.jbescos.gameplay.roguelite.strategy.CardStrategyDecision;
import com.github.jbescos.gameplay.roguelite.strategy.CardStrategyRandom;
import com.github.jbescos.gameplay.roguelite.strategy.CardStrategyRaceState;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RogueliteRun {
    // Level 2 grants the first card; Tier 4 requires a persistent card unlock.
    public static final int TIER_TWO_LEVEL = 10;
    public static final int TIER_THREE_LEVEL = 20;
    private DriverProfileCatalog driverCatalog;
    private RogueliteCompetitorProgress player;
    private final Map<Integer, RogueliteCompetitorProgress> rivals =
            new LinkedHashMap<Integer, RogueliteCompetitorProgress>();
    private final Map<Integer, String> rivalStrategyProfileIds =
            new LinkedHashMap<Integer, String>();
    private final RogueliteRandom random;
    private final AlgorithmicCardStrategy algorithmicCardStrategy =
            new AlgorithmicCardStrategy();
    private final CardStrategyRandom strategyRandom;
    private CardStrategyCatalog cardStrategyCatalog = CardStrategyCatalog.algorithmicOnly();
    private CardStrategyRaceState cardStrategyRaceState = CardStrategyRaceState.empty();
    private CustomGameRules gameRules = new CustomGameRules();
    private final List<RogueliteSetId> enabledSetIds =
            new ArrayList<RogueliteSetId>(RogueliteSetId.values().length);
    private final List<RogueliteSetId> readOnlyEnabledSetIds =
            Collections.unmodifiableList(enabledSetIds);
    private int championshipNumber = 1;
    private int startingTier = 1;

    public RogueliteRun() {
        this(new RogueliteRandom(), DriverProfileCatalog.fallback());
    }

    public RogueliteRun(long seed) {
        this(new RogueliteRandom(seed), DriverProfileCatalog.fallback());
    }

    public RogueliteRun(long seed, DriverProfileCatalog driverCatalog) {
        this(new RogueliteRandom(seed), driverCatalog);
    }

    private RogueliteRun(
            RogueliteRandom random,
            DriverProfileCatalog driverCatalog) {
        this.random = random;
        strategyRandom = new CardStrategyRandom() {
            @Override
            public int nextInt(int bound) {
                return RogueliteRun.this.random.nextInt(bound);
            }
        };
        this.driverCatalog = requireCatalog(driverCatalog);
        player = newPlayerProgress();
        enableStaticSets();
    }

    public void configureDriverCatalog(DriverProfileCatalog catalog) {
        String previousDefault = driverCatalog.getWorst().getProfileId();
        driverCatalog = requireCatalog(catalog);
        repairDriver(player, previousDefault);
        if (player.isPristine()
                && driverCatalog.getTier(
                                player.getLoadout().getDriverProfileId())
                        > 1) {
            String profileId = randomTierOneDriver().getProfileId();
            player.getLoadout().setDriverProfileId(profileId);
            player.recordAcquiredDriver(profileId);
        }
        for (RogueliteCompetitorProgress rival : rivals.values()) {
            repairDriver(rival, previousDefault);
        }
    }

    public void configureGameRules(CustomGameRules rules) {
        gameRules = rules == null ? new CustomGameRules() : rules.copy();
    }

    public void configureCardStrategies(CardStrategyCatalog catalog) {
        cardStrategyCatalog = catalog == null
                ? CardStrategyCatalog.algorithmicOnly() : catalog;
        repairRivalStrategyAssignments();
    }

    /**
     * Draws strategies for a new race. Every available strategy is represented once when the
     * field is large enough; remaining rivals are independent random draws.
     */
    public void assignRivalStrategiesForRace(List<Integer> vehicleIds) {
        List<Integer> normalizedIds = normalizeVehicleIds(vehicleIds);
        if (normalizedIds.isEmpty()) {
            return;
        }

        List<Integer> shuffledIds = new ArrayList<Integer>(normalizedIds);
        List<String> guaranteedProfiles =
                new ArrayList<String>(cardStrategyCatalog.getProfileIds());
        shuffle(shuffledIds);
        shuffle(guaranteedProfiles);
        for (int i = 0; i < shuffledIds.size(); i++) {
            String profileId = i < guaranteedProfiles.size()
                    ? guaranteedProfiles.get(i)
                    : cardStrategyCatalog.chooseProfileId(strategyRandom);
            rivalStrategyProfileIds.put(
                    shuffledIds.get(i),
                    profileId);
        }
    }

    public void updateCardStrategyRaceState(CardStrategyRaceState raceState) {
        cardStrategyRaceState = raceState == null
                ? CardStrategyRaceState.empty() : raceState;
    }

    public void reset() {
        reset(1);
    }

    public void reset(int selectedStartingTier) {
        if (selectedStartingTier < 1
                || selectedStartingTier > DriverProfileCatalog.MAX_TIER) {
            throw new IllegalArgumentException("Starting tier is out of range.");
        }
        player = newPlayerProgress();
        rivals.clear();
        rivalStrategyProfileIds.clear();
        cardStrategyRaceState = CardStrategyRaceState.empty();
        championshipNumber = 1;
        startingTier = selectedStartingTier;
        enableStaticSets();
    }

    public RogueliteCompetitorProgress getPlayerProgress() {
        return player;
    }

    /** Applies a persistent Tier 4 signal emitted by a direct or random Powerup. */
    public boolean unlockTierFour(boolean playerControlled, int vehicleId) {
        RogueliteCompetitorProgress progress = playerControlled
                ? player : getRivalProgress(vehicleId);
        return progress.unlockTierFour();
    }

    public RogueliteLoadout getPlayerLoadout() {
        return player.getLoadout();
    }

    public List<RogueliteCardDefinition> getPlayerCards() {
        return cardsFor(player.getLoadout());
    }

    public DriverProfileMetadata getPlayerDriver() {
        return driverCatalog.get(player.getLoadout().getDriverProfileId());
    }

    public DriverProfileMetadata getDriver(String profileId) {
        return driverCatalog.get(profileId);
    }

    public int getDriverTier(String profileId) {
        return driverCatalog.getTier(profileId);
    }

    public RogueliteCompetitorProgress getRivalProgress(int vehicleId) {
        Integer key = Integer.valueOf(vehicleId);
        RogueliteCompetitorProgress progress = rivals.get(key);
        if (progress == null) {
            progress = newRivalProgress();
            rivals.put(key, progress);
        }
        if (!rivalStrategyProfileIds.containsKey(key)) {
            rivalStrategyProfileIds.put(
                    key,
                    cardStrategyCatalog.chooseProfileId(strategyRandom));
        }
        return progress;
    }

    public String getRivalStrategyProfileId(int vehicleId) {
        getRivalProgress(vehicleId);
        return rivalStrategyProfileIds.get(Integer.valueOf(vehicleId));
    }

    public String getRivalStrategyDisplayName(int vehicleId) {
        return cardStrategyCatalog.get(getRivalStrategyProfileId(vehicleId)).getDisplayName();
    }

    public RogueliteLoadout getRivalLoadout(int vehicleId) {
        return getRivalProgress(vehicleId).getLoadout();
    }

    public int getChampionshipNumber() {
        return championshipNumber;
    }

    public int getStartingTier() {
        return startingTier;
    }

    public List<RogueliteSetId> getEnabledSetIds() {
        return readOnlyEnabledSetIds;
    }

    public RogueliteSetDefinition getCompletedSet(RogueliteLoadout loadout) {
        return RogueliteSetCatalog.completedSet(loadout, enabledSetIds);
    }

    public RogueliteSetDefinition getSetForComponent(RogueliteCardId cardId) {
        return RogueliteSetCatalog.componentSet(cardId, enabledSetIds);
    }

    public int getRacecraftXpPerLapCap() {
        return gameRules.getRacecraftXpPerLapCap();
    }

    public int getRacecraftXpAward(RogueliteExperienceAwards.Reason reason) {
        return gameRules.getRacecraftXpAward(reason);
    }

    public int getUnlockedTier() {
        return getUnlockedTier(player);
    }

    public int getRivalUnlockedTier(int vehicleId) {
        return getUnlockedTier(getRivalProgress(vehicleId));
    }

    public int advanceChampionship() {
        return advanceProgression();
    }

    public int advanceProgression() {
        championshipNumber++;
        return getUnlockedTier();
    }

    public void restartChampionship() {
        championshipNumber = 1;
        enableStaticSets();
    }

    public void removeRival(int vehicleId) {
        Integer key = Integer.valueOf(vehicleId);
        rivals.remove(key);
        rivalStrategyProfileIds.remove(key);
    }

    public int awardPlayerRacePosition(int position, int fieldSize) {
        return player.awardRacePosition(position, fieldSize);
    }

    public int awardRivalRacePosition(int vehicleId, int position, int fieldSize) {
        return getRivalProgress(vehicleId).awardRacePosition(position, fieldSize);
    }

    public int awardPlayerExperience(int amount) {
        return player.awardExperience(amount);
    }

    public int awardRivalExperience(int vehicleId, int amount) {
        return getRivalProgress(vehicleId).awardExperience(amount);
    }

    public int awardPlayerRacecraftExperience(int amount) {
        return player.awardRacecraftExperience(
                amount,
                getRacecraftXpPerLapCap());
    }

    public int awardPlayerRacecraftExperience(
            RogueliteExperienceAwards.Reason reason,
            int amount) {
        return player.awardRacecraftExperience(
                reason,
                amount,
                getRacecraftXpPerLapCap());
    }

    public int awardRivalRacecraftExperience(int vehicleId, int amount) {
        return getRivalProgress(vehicleId).awardRacecraftExperience(
                amount,
                getRacecraftXpPerLapCap());
    }

    public int awardRivalRacecraftExperience(
            int vehicleId,
            RogueliteExperienceAwards.Reason reason,
            int amount) {
        return getRivalProgress(vehicleId).awardRacecraftExperience(
                reason,
                amount,
                getRacecraftXpPerLapCap());
    }

    public int stealLapExperience(
            boolean recipientPlayerControlled,
            int recipientVehicleId,
            boolean offenderPlayerControlled,
            int offenderVehicleId) {
        RogueliteCompetitorProgress recipient = recipientPlayerControlled
                ? player : getRivalProgress(recipientVehicleId);
        RogueliteCompetitorProgress offender = offenderPlayerControlled
                ? player : getRivalProgress(offenderVehicleId);
        return recipient.stealLapExperienceFrom(
                offender,
                getRacecraftXpPerLapCap());
    }

    public int bankPlayerLapExperience() {
        return player.bankLapExperience();
    }

    public int bankPlayerLapExperience(float multiplier) {
        return player.bankLapExperience(multiplier);
    }

    public int bankRivalLapExperience(int vehicleId) {
        return getRivalProgress(vehicleId).bankLapExperience();
    }

    public int bankRivalLapExperience(int vehicleId, float multiplier) {
        return getRivalProgress(vehicleId).bankLapExperience(multiplier);
    }

    public void resetPlayerLapExperience() {
        player.resetLapExperience();
    }

    public void resetRivalLapExperience(int vehicleId) {
        getRivalProgress(vehicleId).resetLapExperience();
    }

    public void discardLapExperience(boolean playerControlled, int vehicleId) {
        if (playerControlled) {
            resetPlayerLapExperience();
        } else {
            resetRivalLapExperience(vehicleId);
        }
    }

    public void resetAllLapExperience() {
        player.resetLapExperience();
        for (RogueliteCompetitorProgress rival : rivals.values()) {
            rival.resetLapExperience();
        }
    }

    public void resetAllRaceExperience() {
        player.resetRaceExperience();
        for (RogueliteCompetitorProgress rival : rivals.values()) {
            rival.resetRaceExperience();
        }
    }

    public List<RogueliteCardOffer> createOffers(int count) {
        if (!player.hasOfferableReward()) {
            return Collections.emptyList();
        }
        return createOffersFor(player, count);
    }

    public boolean select(RogueliteCardOffer offer) {
        return applyOffer(player, offer);
    }

    public boolean skipPlayerReward() {
        return player.consumePendingReward();
    }

    public void resolveRivalRewards(Iterable<Integer> vehicleIds) {
        if (vehicleIds == null) {
            return;
        }
        for (Integer vehicleId : vehicleIds) {
            if (vehicleId == null) {
                continue;
            }
            resolveRivalReward(vehicleId.intValue());
        }
    }

    public void resolveRivalReward(int vehicleId) {
        RogueliteCompetitorProgress rival = getRivalProgress(vehicleId);
        while (rival.hasPendingReward()) {
            List<RogueliteCardOffer> offers = createOffersFor(rival, 3);
            if (offers.isEmpty()) {
                rival.consumePendingReward();
                continue;
            }
            RogueliteCardOffer offer = chooseRivalOffer(vehicleId, rival, offers);
            if (!applyOffer(rival, offer)) {
                rival.consumePendingReward();
            }
        }
    }

    public Snapshot snapshot() {
        Snapshot snapshot = new Snapshot();
        snapshot.randomState = random.getState();
        snapshot.championshipNumber = championshipNumber;
        snapshot.startingTier = startingTier;
        for (int i = 0; i < enabledSetIds.size(); i++) {
            snapshot.enabledSetIds.add(enabledSetIds.get(i).name());
        }
        snapshot.player = snapshotProgress(player);
        for (Map.Entry<Integer, RogueliteCompetitorProgress> entry : rivals.entrySet()) {
            RivalSnapshot rival = new RivalSnapshot();
            rival.vehicleId = entry.getKey().intValue();
            rival.strategyProfileId = getRivalStrategyProfileId(rival.vehicleId);
            rival.progress = snapshotProgress(entry.getValue());
            snapshot.rivals.add(rival);
        }
        return snapshot;
    }

    public boolean restore(Snapshot snapshot) {
        if (snapshot == null
                || snapshot.player == null
                || snapshot.championshipNumber < 1
                || snapshot.startingTier < 1
                || snapshot.startingTier > DriverProfileCatalog.MAX_TIER) {
            return false;
        }

        RogueliteCompetitorProgress restoredPlayer =
                restoreProgress(snapshot.player);
        if (restoredPlayer == null) {
            return false;
        }
        Map<Integer, RogueliteCompetitorProgress> restoredRivals =
                new LinkedHashMap<Integer, RogueliteCompetitorProgress>();
        Map<Integer, String> restoredStrategies = new LinkedHashMap<Integer, String>();
        if (snapshot.rivals != null) {
            for (int i = 0; i < snapshot.rivals.size(); i++) {
                RivalSnapshot rival = snapshot.rivals.get(i);
                if (rival == null || rival.vehicleId < 0 || rival.progress == null) {
                    return false;
                }
                RogueliteCompetitorProgress progress =
                        restoreProgress(rival.progress);
                if (progress == null
                        || restoredRivals.put(
                                        Integer.valueOf(rival.vehicleId),
                                        progress)
                                != null) {
                    return false;
                }
                String strategyProfileId = cardStrategyCatalog.contains(rival.strategyProfileId)
                        ? rival.strategyProfileId
                        : cardStrategyCatalog.chooseProfileId(strategyRandom);
                restoredStrategies.put(Integer.valueOf(rival.vehicleId), strategyProfileId);
            }
        }

        player = restoredPlayer;
        rivals.clear();
        rivals.putAll(restoredRivals);
        rivalStrategyProfileIds.clear();
        rivalStrategyProfileIds.putAll(restoredStrategies);
        championshipNumber = snapshot.championshipNumber;
        startingTier = snapshot.startingTier;
        random.setState(snapshot.randomState);
        enableStaticSets();
        return true;
    }

    public List<RogueliteCardOffer> restoreOffers(Iterable<String> offerIds) {
        if (offerIds == null || !player.hasOfferableReward()) {
            return Collections.emptyList();
        }
        List<RogueliteCardOffer> offers = new ArrayList<RogueliteCardOffer>();
        for (String offerId : offerIds) {
            RogueliteCardOffer offer = restoreOffer(offerId);
            if (offer == null || !isEligible(player, offer)) {
                return Collections.emptyList();
            }
            offers.add(offer);
        }
        return Collections.unmodifiableList(offers);
    }

    private List<RogueliteCardOffer> createOffersFor(
            RogueliteCompetitorProgress progress,
            int count) {
        if (count <= 0) {
            return Collections.emptyList();
        }
        int unlockedTier = getUnlockedTier(progress);
        int driverOfferTier = Math.min(unlockedTier, DriverProfileCatalog.MAX_TIER);
        List<RogueliteCardOffer> driverOffers =
                new ArrayList<RogueliteCardOffer>();
        if (gameRules.isCardTypeAllowed(driverOfferTier, RogueliteSlotType.DRIVER)) {
            List<DriverProfileMetadata> eligibleDrivers =
                    driverCatalog.eligibleThroughTier(driverOfferTier);
            for (int i = 0; i < eligibleDrivers.size(); i++) {
                DriverProfileMetadata driver = eligibleDrivers.get(i);
                if (driverCatalog.getTier(driver.getProfileId()) == driverOfferTier
                        && isNewDriver(progress, driver.getProfileId())) {
                    driverOffers.add(
                            RogueliteCardOffer.driver(
                                    driver,
                                    driverCatalog.getTier(driver.getProfileId())));
                }
            }
        }

        List<RogueliteCardOffer> modificationOffers =
                new ArrayList<RogueliteCardOffer>();
        List<RogueliteCardDefinition> cards = RogueliteCardCatalog.all();
        for (int i = 0; i < cards.size(); i++) {
            RogueliteCardDefinition card = cards.get(i);
            if (isOfferTierEligible(card.getTier(), unlockedTier)
                    && gameRules.isCardTypeAllowed(card.getTier(), card.getSlotType())
                    && isNewModification(progress, card.getId())) {
                modificationOffers.add(RogueliteCardOffer.modification(card));
            }
        }

        List<RogueliteCardOffer> candidates =
                new ArrayList<RogueliteCardOffer>(
                        driverOffers.size() + modificationOffers.size());
        candidates.addAll(driverOffers);
        candidates.addAll(modificationOffers);
        List<RogueliteCardOffer> selected =
                new ArrayList<RogueliteCardOffer>(count);
        while (selected.size() < count && !candidates.isEmpty()) {
            selected.add(takeSlotBalancedOffer(candidates, selected));
        }
        shuffle(selected);
        return Collections.unmodifiableList(selected);
    }

    private boolean applyOffer(
            RogueliteCompetitorProgress progress,
            RogueliteCardOffer offer) {
        if (!progress.hasOfferableReward() || !isEligible(progress, offer)) {
            return false;
        }
        boolean applied;
        if (offer.isDriver()) {
            progress.getLoadout().setDriverProfileId(
                    offer.getDriver().getProfileId());
            progress.recordAcquiredDriver(offer.getDriver().getProfileId());
            applied = true;
        } else {
            applied =
                    progress.getLoadout().equip(
                            offer.getCard().getId());
            if (applied) {
                progress.recordAcquiredModification(offer.getCard().getId());
                if (offer.getCard().getId() == RogueliteCardId.TIER_FOUR_SIGNAL) {
                    progress.unlockTierFour();
                }
            }
        }
        return applied && progress.consumePendingReward();
    }

    private boolean isEligible(
            RogueliteCompetitorProgress progress,
            RogueliteCardOffer offer) {
        if (offer == null
                || !isOfferTierEligible(offer.getTier(), getUnlockedTier(progress))) {
            return false;
        }
        if (!gameRules.isCardTypeAllowed(offer.getTier(), offer.getSlotType())) {
            return false;
        }
        if (offer.isDriver()) {
            return driverCatalog.get(offer.getDriver().getProfileId()) != null
                    && isNewDriver(progress, offer.getDriver().getProfileId());
        }
        return offer.getCard() != null
                && isNewModification(progress, offer.getCard().getId());
    }

    private static boolean isNewDriver(
            RogueliteCompetitorProgress progress,
            String profileId) {
        return profileId != null
                && !profileId.equals(progress.getLoadout().getDriverProfileId())
                && !progress.hasAcquiredDriver(profileId);
    }

    private static boolean isNewModification(
            RogueliteCompetitorProgress progress,
            RogueliteCardId cardId) {
        return cardId != null
                && !progress.getLoadout().has(cardId)
                && !progress.hasAcquiredModification(cardId);
    }

    private RogueliteCardOffer restoreOffer(String offerId) {
        if (offerId == null) {
            return null;
        }
        if (offerId.startsWith("driver:")) {
            String profileId = offerId.substring("driver:".length());
            DriverProfileMetadata driver = driverCatalog.get(profileId);
            return driver == null
                    ? null
                    : RogueliteCardOffer.driver(
                            driver,
                            driverCatalog.getTier(profileId));
        }
        if (!offerId.startsWith("card:")) {
            return null;
        }
        try {
            RogueliteCardId cardId =
                    RogueliteCardCatalog.resolveSavedId(
                            offerId.substring("card:".length()));
            if (cardId == null) {
                return null;
            }
            return RogueliteCardOffer.modification(
                    RogueliteCardCatalog.get(cardId));
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private ProgressSnapshot snapshotProgress(
            RogueliteCompetitorProgress progress) {
        ProgressSnapshot snapshot = new ProgressSnapshot();
        snapshot.driverProfileId = progress.getLoadout().getDriverProfileId();
        snapshot.level = progress.getLevel();
        snapshot.experience = progress.getExperience();
        snapshot.lapExperience = progress.getLapExperience();
        snapshot.pendingRewards = progress.getPendingRewards();
        snapshot.tierFourUnlocked = progress.isTierFourUnlocked();
        snapshot.acquiredDriverProfileIds.addAll(
                progress.getAcquiredDriverProfileIds());
        for (RogueliteCardId cardId : progress.getAcquiredModificationCardIds()) {
            snapshot.acquiredModificationCardIds.add(cardId.name());
        }
        List<RogueliteCardId> modifications =
                progress.getLoadout().getModifications();
        for (int i = 0; i < modifications.size(); i++) {
            snapshot.modificationCardIds.add(modifications.get(i).name());
        }
        return snapshot;
    }

    private RogueliteCompetitorProgress restoreProgress(
            ProgressSnapshot snapshot) {
        if (snapshot == null
                || driverCatalog.get(snapshot.driverProfileId) == null
                || snapshot.modificationCardIds == null
                || snapshot.acquiredDriverProfileIds == null
                || snapshot.acquiredModificationCardIds == null) {
            return null;
        }
        try {
            RogueliteCompetitorProgress progress =
                    new RogueliteCompetitorProgress(
                            snapshot.driverProfileId,
                            gameRules.getLevelXpIncrement());
            for (int i = 0; i < snapshot.acquiredDriverProfileIds.size(); i++) {
                String profileId = snapshot.acquiredDriverProfileIds.get(i);
                if (driverCatalog.get(profileId) == null) {
                    return null;
                }
                progress.recordAcquiredDriver(profileId);
            }
            for (int i = 0; i < snapshot.acquiredModificationCardIds.size(); i++) {
                RogueliteCardId cardId =
                        RogueliteCardCatalog.resolveSavedId(
                                snapshot.acquiredModificationCardIds.get(i));
                if (cardId == null) {
                    return null;
                }
                progress.recordAcquiredModification(cardId);
            }
            for (int i = 0; i < snapshot.modificationCardIds.size(); i++) {
                RogueliteCardId cardId =
                        RogueliteCardCatalog.resolveSavedId(
                                snapshot.modificationCardIds.get(i));
                if (cardId == null) {
                    return null;
                }
                progress.getLoadout().restoreModification(cardId);
                progress.recordAcquiredModification(cardId);
            }
            progress.restore(
                    snapshot.level,
                    snapshot.experience,
                    snapshot.pendingRewards,
                    snapshot.tierFourUnlocked
                            || progress.hasAcquiredModification(
                                    RogueliteCardId.TIER_FOUR_SIGNAL));
            progress.restoreLapExperience(
                    snapshot.lapExperience,
                    getRacecraftXpPerLapCap());
            return progress;
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private List<RogueliteCardDefinition> cardsFor(RogueliteLoadout loadout) {
        List<RogueliteCardDefinition> cards =
                new ArrayList<RogueliteCardDefinition>();
        List<RogueliteCardId> modifications = loadout.getModifications();
        for (int i = 0; i < modifications.size(); i++) {
            cards.add(RogueliteCardCatalog.get(modifications.get(i)));
        }
        return Collections.unmodifiableList(cards);
    }

    private RogueliteCompetitorProgress newPlayerProgress() {
        return new RogueliteCompetitorProgress(
                randomTierOneDriver().getProfileId(),
                gameRules.getLevelXpIncrement());
    }

    private DriverProfileMetadata randomTierOneDriver() {
        List<DriverProfileMetadata> tierOneDrivers =
                driverCatalog.eligibleThroughTier(1);
        return tierOneDrivers.get(random.nextInt(tierOneDrivers.size()));
    }

    private RogueliteCompetitorProgress newRivalProgress() {
        return new RogueliteCompetitorProgress(
                randomTierOneDriver().getProfileId(),
                gameRules.getLevelXpIncrement());
    }

    private int getUnlockedTier(RogueliteCompetitorProgress progress) {
        int level = progress == null ? 1 : progress.getLevel();
        int naturalTier = gameRules.resolveTierForLevel(level, startingTier);
        return progress != null && progress.isTierFourUnlocked()
                ? RogueliteCardCatalog.MAX_CARD_TIER
                : naturalTier;
    }

    private static boolean isOfferTierEligible(int offerTier, int unlockedTier) {
        return offerTier == unlockedTier
                || (unlockedTier == RogueliteCardCatalog.MAX_CARD_TIER
                        && offerTier == DriverProfileCatalog.MAX_TIER);
    }

    private void repairDriver(
            RogueliteCompetitorProgress progress,
            String previousDefault) {
        String currentProfileId = progress.getLoadout().getDriverProfileId();
        if (driverCatalog.get(currentProfileId) == null
                || (progress.isPristine()
                        && currentProfileId.equals(previousDefault))) {
            progress.getLoadout().setDriverProfileId(
                    driverCatalog.getWorst().getProfileId());
            progress.recordAcquiredDriver(
                    driverCatalog.getWorst().getProfileId());
        }
    }

    private RogueliteCardOffer takeRandom(List<RogueliteCardOffer> offers) {
        return offers.remove(random.nextInt(offers.size()));
    }

    private RogueliteCardOffer takeSlotBalancedOffer(
            List<RogueliteCardOffer> offers,
            List<RogueliteCardOffer> selected) {
        List<RogueliteSlotType> availableSlots =
                new ArrayList<RogueliteSlotType>();
        for (int i = 0; i < offers.size(); i++) {
            RogueliteCardOffer offer = offers.get(i);
            RogueliteSlotType slotType = offer.getSlotType();
            if (!containsSlot(selected, slotType)
                    && !availableSlots.contains(slotType)) {
                availableSlots.add(slotType);
            }
        }
        if (availableSlots.isEmpty()) {
            for (int i = 0; i < offers.size(); i++) {
                RogueliteSlotType slotType = offers.get(i).getSlotType();
                if (!availableSlots.contains(slotType)) {
                    availableSlots.add(slotType);
                }
            }
        }
        RogueliteSlotType chosenSlot =
                availableSlots.get(random.nextInt(availableSlots.size()));
        List<RogueliteCardOffer> slotOffers =
                new ArrayList<RogueliteCardOffer>();
        for (int i = 0; i < offers.size(); i++) {
            RogueliteCardOffer offer = offers.get(i);
            if (offer.getSlotType() == chosenSlot) {
                slotOffers.add(offer);
            }
        }
        RogueliteCardOffer chosen =
                slotOffers.get(random.nextInt(slotOffers.size()));
        offers.remove(chosen);
        return chosen;
    }

    RogueliteCardOffer chooseRivalOffer(
            RogueliteCompetitorProgress progress,
            List<RogueliteCardOffer> offers) {
        return algorithmicCardStrategy.choose(
                new CardStrategyDecision(
                        progress,
                        driverCatalog,
                        offers,
                        CardStrategyContext.empty()),
                strategyRandom);
    }

    private RogueliteCardOffer chooseRivalOffer(
            int vehicleId,
            RogueliteCompetitorProgress progress,
            List<RogueliteCardOffer> offers) {
        CardStrategy strategy = cardStrategyCatalog.get(
                getRivalStrategyProfileId(vehicleId));
        return strategy.choose(
                new CardStrategyDecision(
                        progress,
                        driverCatalog,
                        offers,
                        buildStrategyContext(vehicleId)),
                strategyRandom);
    }

    private CardStrategyContext buildStrategyContext(int vehicleId) {
        List<CardStrategyContext.Opponent> opponents =
                new ArrayList<CardStrategyContext.Opponent>(rivals.size());
        opponents.add(strategyOpponent(player, cardStrategyRaceState.getPlayer()));
        for (Map.Entry<Integer, RogueliteCompetitorProgress> entry : rivals.entrySet()) {
            if (entry.getKey().intValue() != vehicleId) {
                opponents.add(strategyOpponent(
                        entry.getValue(),
                        cardStrategyRaceState.get(entry.getKey().intValue())));
            }
        }
        CardStrategyRaceState.Competitor self = cardStrategyRaceState.get(vehicleId);
        return new CardStrategyContext(
                cardStrategyRaceState.getCircuitIndex(),
                cardStrategyRaceState.getCircuitCount(),
                self == null ? 0 : self.getLap(),
                cardStrategyRaceState.getLapCount(),
                self == null ? 0 : self.getRacePosition(),
                self == null ? 0 : self.getChampionshipPosition(),
                Math.max(
                        0,
                        cardStrategyRaceState.getCircuitCount()
                                - cardStrategyRaceState.getCircuitIndex()),
                opponents,
                enabledSetIds);
    }

    private CardStrategyContext.Opponent strategyOpponent(
            RogueliteCompetitorProgress progress,
            CardStrategyRaceState.Competitor raceState) {
        return new CardStrategyContext.Opponent(
                progress.getLevel(),
                raceState == null ? 0 : raceState.getRacePosition(),
                raceState == null ? 0 : raceState.getChampionshipPosition(),
                driverCatalog.get(progress.getLoadout().getDriverProfileId()),
                progress.getLoadout());
    }

    private List<Integer> normalizeVehicleIds(List<Integer> vehicleIds) {
        if (vehicleIds == null || vehicleIds.isEmpty()) {
            return Collections.emptyList();
        }
        List<Integer> normalized = new ArrayList<Integer>(vehicleIds.size());
        Set<Integer> seen = new HashSet<Integer>();
        for (Integer vehicleId : vehicleIds) {
            if (vehicleId != null
                    && vehicleId.intValue() >= 0
                    && seen.add(vehicleId)) {
                normalized.add(vehicleId);
            }
        }
        Collections.sort(normalized);
        return normalized;
    }

    private void repairRivalStrategyAssignments() {
        for (Integer vehicleId : rivals.keySet()) {
            String profileId = rivalStrategyProfileIds.get(vehicleId);
            if (!cardStrategyCatalog.contains(profileId)) {
                rivalStrategyProfileIds.put(
                        vehicleId,
                        cardStrategyCatalog.chooseProfileId(strategyRandom));
            }
        }
    }

    private static boolean containsSlot(
            List<RogueliteCardOffer> offers,
            RogueliteSlotType slotType) {
        for (int i = 0; i < offers.size(); i++) {
            if (offers.get(i).getSlotType() == slotType) {
                return true;
            }
        }
        return false;
    }

    private <T> void shuffle(List<T> values) {
        for (int i = values.size() - 1; i > 0; i--) {
            int swapIndex = random.nextInt(i + 1);
            T value = values.get(i);
            values.set(i, values.get(swapIndex));
            values.set(swapIndex, value);
        }
    }

    private void enableStaticSets() {
        enabledSetIds.clear();
        enabledSetIds.addAll(RogueliteSetCatalog.allSetIds());
    }

    private static DriverProfileCatalog requireCatalog(
            DriverProfileCatalog catalog) {
        if (catalog == null) {
            throw new IllegalArgumentException("Driver catalog is required.");
        }
        return catalog;
    }

    public static final class Snapshot {
        public int randomState;
        public int championshipNumber = 1;
        public int startingTier = 1;
        public List<String> enabledSetIds = new ArrayList<String>();
        public ProgressSnapshot player = new ProgressSnapshot();
        public List<RivalSnapshot> rivals = new ArrayList<RivalSnapshot>();
    }

    public static final class ProgressSnapshot {
        public String driverProfileId = "";
        public int level = 1;
        public int experience;
        public int lapExperience;
        public int pendingRewards;
        public boolean tierFourUnlocked;
        // Retained only to normalize saves written by the former postpone system.
        public int rewardDeferredUntilLevel;
        public List<String> modificationCardIds = new ArrayList<String>();
        public List<String> acquiredDriverProfileIds = new ArrayList<String>();
        public List<String> acquiredModificationCardIds = new ArrayList<String>();
    }

    public static final class RivalSnapshot {
        public int vehicleId;
        public String strategyProfileId = AlgorithmicCardStrategy.PROFILE_ID;
        public ProgressSnapshot progress = new ProgressSnapshot();
    }
}
