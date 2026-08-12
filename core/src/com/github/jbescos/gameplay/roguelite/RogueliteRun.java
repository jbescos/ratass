package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RogueliteRun {
    // Level 2 grants the first card; later tiers unlock at these level boundaries.
    public static final int TIER_TWO_LEVEL = 10;
    public static final int TIER_THREE_LEVEL = 20;
    private static final float MIN_SYNERGY_GAIN = 0.0001f;
    private static final int RIVAL_EXPLORATION_CHANCE = 10;

    private DriverProfileCatalog driverCatalog;
    private RogueliteCompetitorProgress player;
    private final Map<Integer, RogueliteCompetitorProgress> rivals =
            new LinkedHashMap<Integer, RogueliteCompetitorProgress>();
    private final RogueliteRandom random;
    private CustomGameRules gameRules = new CustomGameRules();
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
        this.driverCatalog = requireCatalog(driverCatalog);
        player = newPlayerProgress();
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
        championshipNumber = 1;
        startingTier = selectedStartingTier;
    }

    public RogueliteCompetitorProgress getPlayerProgress() {
        return player;
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
        return progress;
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
    }

    public void removeRival(int vehicleId) {
        rivals.remove(Integer.valueOf(vehicleId));
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

    public void resetPlayerLapExperience() {
        player.resetLapExperience();
    }

    public void resetRivalLapExperience(int vehicleId) {
        getRivalProgress(vehicleId).resetLapExperience();
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
            RogueliteCardOffer offer = chooseRivalOffer(rival, offers);
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
        snapshot.player = snapshotProgress(player);
        for (Map.Entry<Integer, RogueliteCompetitorProgress> entry : rivals.entrySet()) {
            RivalSnapshot rival = new RivalSnapshot();
            rival.vehicleId = entry.getKey().intValue();
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
            }
        }

        player = restoredPlayer;
        rivals.clear();
        rivals.putAll(restoredRivals);
        championshipNumber = snapshot.championshipNumber;
        startingTier = snapshot.startingTier;
        random.setState(snapshot.randomState);
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
        List<RogueliteCardOffer> driverOffers =
                new ArrayList<RogueliteCardOffer>();
        if (gameRules.isCardTypeAllowed(unlockedTier, RogueliteSlotType.DRIVER)) {
            List<DriverProfileMetadata> eligibleDrivers =
                    driverCatalog.eligibleThroughTier(unlockedTier);
            for (int i = 0; i < eligibleDrivers.size(); i++) {
                DriverProfileMetadata driver = eligibleDrivers.get(i);
                if (driverCatalog.getTier(driver.getProfileId()) == unlockedTier
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
            if (card.getTier() == unlockedTier
                    && gameRules.isCardTypeAllowed(unlockedTier, card.getSlotType())
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
            }
        }
        return applied && progress.consumePendingReward();
    }

    private boolean isEligible(
            RogueliteCompetitorProgress progress,
            RogueliteCardOffer offer) {
        if (offer == null || offer.getTier() != getUnlockedTier(progress)) {
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
                    snapshot.pendingRewards);
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
        return gameRules.resolveTierForLevel(level, startingTier);
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
        RivalOfferBucket bucket = rivalOfferBucket(progress, offers);
        RogueliteCardOffer synergyChoice = bestSynergyOffer(progress, bucket.offers);
        if (synergyChoice != null) {
            if (random.nextInt(100) < RIVAL_EXPLORATION_CHANCE) {
                return bucket.offers.get(random.nextInt(bucket.offers.size()));
            }
            return synergyChoice;
        }
        if (!bucket.mandatory) {
            List<RogueliteCardOffer> fallbacks = rivalFallbackOffers(progress, bucket.offers);
            if (!fallbacks.isEmpty()) {
                return fallbacks.get(random.nextInt(fallbacks.size()));
            }
        }
        return highestScoredRivalOffer(progress, bucket.offers);
    }

    private RivalOfferBucket rivalOfferBucket(
            RogueliteCompetitorProgress progress,
            List<RogueliteCardOffer> offers) {
        RogueliteLoadout loadout = progress.getLoadout();
        List<RogueliteCardOffer> emptySlotOffers = new ArrayList<RogueliteCardOffer>();
        for (int i = 0; i < offers.size(); i++) {
            RogueliteCardOffer offer = offers.get(i);
            if (!offer.isDriver() && !loadout.hasCardIn(offer.getSlotType())) {
                emptySlotOffers.add(offer);
            }
        }
        if (!emptySlotOffers.isEmpty()) {
            return new RivalOfferBucket(emptySlotOffers, true);
        }

        int highestTierGain = Integer.MIN_VALUE;
        for (int i = 0; i < offers.size(); i++) {
            highestTierGain = Math.max(
                    highestTierGain,
                    rivalTierGain(progress, offers.get(i)));
        }
        List<RogueliteCardOffer> tierOffers = new ArrayList<RogueliteCardOffer>();
        for (int i = 0; i < offers.size(); i++) {
            RogueliteCardOffer offer = offers.get(i);
            if (rivalTierGain(progress, offer) == highestTierGain) {
                tierOffers.add(offer);
            }
        }
        return new RivalOfferBucket(tierOffers, highestTierGain > 0);
    }

    private RogueliteCardOffer bestSynergyOffer(
            RogueliteCompetitorProgress progress,
            List<RogueliteCardOffer> offers) {
        RogueliteLoadout loadout = progress.getLoadout();
        RogueliteCardId currentTuning = loadout.get(RogueliteSlotType.TUNING);
        RogueliteCardId currentTechnique = loadout.get(RogueliteSlotType.TECHNIQUE);
        float currentScore = RaceTechniqueEffect.tuningTechniqueScore(
                currentTuning, currentTechnique);
        if (Float.isNaN(currentScore)) {
            currentScore = RaceTechniqueEffect.tuningBaselineScore(currentTuning);
        }

        RogueliteCardOffer best = null;
        float bestGain = MIN_SYNERGY_GAIN;
        for (int i = 0; i < offers.size(); i++) {
            RogueliteCardOffer offer = offers.get(i);
            if (offer.isDriver()
                    || (offer.getSlotType() != RogueliteSlotType.TUNING
                            && offer.getSlotType() != RogueliteSlotType.TECHNIQUE)) {
                continue;
            }
            RogueliteCardId candidateTuning =
                    offer.getSlotType() == RogueliteSlotType.TUNING
                            ? offer.getCard().getId()
                            : currentTuning;
            RogueliteCardId candidateTechnique =
                    offer.getSlotType() == RogueliteSlotType.TECHNIQUE
                            ? offer.getCard().getId()
                            : currentTechnique;
            float candidateScore = RaceTechniqueEffect.tuningTechniqueScore(
                    candidateTuning, candidateTechnique);
            if (Float.isNaN(candidateScore)) {
                continue;
            }
            float comparisonScore = currentScore;
            if (Float.isNaN(comparisonScore)) {
                comparisonScore = RaceTechniqueEffect.tuningBaselineScore(candidateTuning);
            }
            float gain = candidateScore - comparisonScore;
            if (gain > bestGain) {
                best = offer;
                bestGain = gain;
            }
        }
        return best;
    }

    private List<RogueliteCardOffer> rivalFallbackOffers(
            RogueliteCompetitorProgress progress,
            List<RogueliteCardOffer> offers) {
        List<RogueliteCardOffer> fallbacks = new ArrayList<RogueliteCardOffer>();
        DriverProfileMetadata currentDriver = driverCatalog.get(
                progress.getLoadout().getDriverProfileId());
        for (int i = 0; i < offers.size(); i++) {
            RogueliteCardOffer offer = offers.get(i);
            if ((!offer.isDriver() && offer.getSlotType() == RogueliteSlotType.REVENGE)
                    || (offer.isDriver()
                            && driverQualityGain(currentDriver, offer.getDriver()) > 0f)) {
                fallbacks.add(offer);
            }
        }
        return fallbacks;
    }

    private RogueliteCardOffer highestScoredRivalOffer(
            RogueliteCompetitorProgress progress,
            List<RogueliteCardOffer> offers) {
        RogueliteCardOffer best = offers.get(0);
        float bestScore = rivalOfferScore(progress, best);
        for (int i = 1; i < offers.size(); i++) {
            RogueliteCardOffer candidate = offers.get(i);
            float score = rivalOfferScore(progress, candidate);
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private int rivalTierGain(
            RogueliteCompetitorProgress progress,
            RogueliteCardOffer offer) {
        if (offer.isDriver()) {
            DriverProfileMetadata current = driverCatalog.get(
                    progress.getLoadout().getDriverProfileId());
            int currentTier = current == null
                    ? 0
                    : driverCatalog.getTier(current.getProfileId());
            return offer.getTier() - currentTier;
        }
        RogueliteCardId equipped = progress.getLoadout().get(offer.getSlotType());
        int currentTier = equipped == null
                ? 0
                : RogueliteCardCatalog.get(equipped).getTier();
        return offer.getTier() - currentTier;
    }

    private float rivalOfferScore(
            RogueliteCompetitorProgress progress,
            RogueliteCardOffer offer) {
        RogueliteLoadout loadout = progress.getLoadout();
        int currentTier;
        float qualityGain = 0f;
        if (offer.isDriver()) {
            DriverProfileMetadata current =
                    driverCatalog.get(loadout.getDriverProfileId());
            currentTier =
                    current == null
                            ? 0
                            : driverCatalog.getTier(current.getProfileId());
            qualityGain = driverQualityGain(current, offer.getDriver());
            if (offer.getTier() <= currentTier && qualityGain <= 0f) {
                return -100000f + qualityGain;
            }
        } else {
            RogueliteCardId equippedId = loadout.get(offer.getSlotType());
            currentTier =
                    equippedId == null
                            ? 0
                            : RogueliteCardCatalog.get(equippedId).getTier();
        }

        int tierGain = offer.getTier() - currentTier;
        if (tierGain < 0) {
            return -100000f + tierGain * 1000f;
        }

        // Tier gain is the primary objective. When gains tie, improve the
        // weakest slot first so a rival cannot spend every level swapping
        // drivers while the rest of its loadout remains under-tiered.
        float weakestSlotPriority =
                (DriverProfileCatalog.MAX_TIER - currentTier) * 100f;
        return tierGain * 10000f
                + weakestSlotPriority
                + Math.max(0f, qualityGain);
    }

    private static float driverQualityGain(
            DriverProfileMetadata current,
            DriverProfileMetadata offered) {
        float offeredLap = offered == null ? 0f : offered.getAverageLapSeconds();
        boolean offeredValid = isValidAverageLap(offeredLap);
        if (current == null) {
            return offeredValid ? 1f / offeredLap : 0f;
        }

        float currentLap = current.getAverageLapSeconds();
        boolean currentValid = isValidAverageLap(currentLap);
        if (currentValid && offeredValid) {
            return currentLap - offeredLap;
        }
        if (offeredValid) {
            return 1f;
        }
        return currentValid ? -1f : 0f;
    }

    private static boolean isValidAverageLap(float averageLap) {
        return averageLap > 0f
                && !Float.isNaN(averageLap)
                && !Float.isInfinite(averageLap);
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

    private static final class RivalOfferBucket {
        private final List<RogueliteCardOffer> offers;
        private final boolean mandatory;

        private RivalOfferBucket(
                List<RogueliteCardOffer> offers,
                boolean mandatory) {
            this.offers = offers;
            this.mandatory = mandatory;
        }
    }

    private void shuffle(List<RogueliteCardOffer> offers) {
        for (int i = offers.size() - 1; i > 0; i--) {
            int swapIndex = random.nextInt(i + 1);
            RogueliteCardOffer value = offers.get(i);
            offers.set(i, offers.get(swapIndex));
            offers.set(swapIndex, value);
        }
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
        public ProgressSnapshot player = new ProgressSnapshot();
        public List<RivalSnapshot> rivals = new ArrayList<RivalSnapshot>();
    }

    public static final class ProgressSnapshot {
        public String driverProfileId = "";
        public int level = 1;
        public int experience;
        public int lapExperience;
        public int pendingRewards;
        // Retained only to normalize saves written by the former postpone system.
        public int rewardDeferredUntilLevel;
        public List<String> modificationCardIds = new ArrayList<String>();
        public List<String> acquiredDriverProfileIds = new ArrayList<String>();
        public List<String> acquiredModificationCardIds = new ArrayList<String>();
    }

    public static final class RivalSnapshot {
        public int vehicleId;
        public ProgressSnapshot progress = new ProgressSnapshot();
    }
}
