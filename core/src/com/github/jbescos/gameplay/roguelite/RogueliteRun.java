package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class RogueliteRun {
    private DriverProfileCatalog driverCatalog;
    private RogueliteCompetitorProgress player;
    private final Map<Integer, RogueliteCompetitorProgress> rivals =
            new LinkedHashMap<Integer, RogueliteCompetitorProgress>();
    private final RogueliteRandom random;
    private int championshipNumber = 1;

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
        player = newProgress();
    }

    public void configureDriverCatalog(DriverProfileCatalog catalog) {
        String previousDefault = driverCatalog.getWorst().getProfileId();
        driverCatalog = requireCatalog(catalog);
        repairDriver(player, previousDefault);
        for (RogueliteCompetitorProgress rival : rivals.values()) {
            repairDriver(rival, previousDefault);
        }
    }

    public void reset() {
        player = newProgress();
        rivals.clear();
        championshipNumber = 1;
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
            progress = newProgress();
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

    public int getUnlockedTier() {
        return Math.min(
                DriverProfileCatalog.MAX_TIER,
                championshipNumber);
    }

    public int advanceChampionship() {
        championshipNumber++;
        return getUnlockedTier();
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

    public List<RogueliteCardOffer> createOffers(int count) {
        if (!player.hasPendingReward()) {
            return Collections.emptyList();
        }
        return createOffersFor(player, count);
    }

    public boolean needsReplacement(RogueliteCardOffer offer) {
        return offer != null
                && !offer.isDriver()
                && player.getLoadout().isFull();
    }

    public boolean select(RogueliteCardOffer offer) {
        return select(offer, -1);
    }

    public boolean select(RogueliteCardOffer offer, int replacementSlot) {
        return applyOffer(player, offer, replacementSlot);
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
            RogueliteCompetitorProgress rival =
                    getRivalProgress(vehicleId.intValue());
            while (rival.hasPendingReward()) {
                List<RogueliteCardOffer> offers = createOffersFor(rival, 3);
                if (offers.isEmpty()) {
                    rival.consumePendingReward();
                    continue;
                }
                RogueliteCardOffer offer = offers.get(random.nextInt(offers.size()));
                int replacementSlot =
                        !offer.isDriver() && rival.getLoadout().isFull()
                                ? random.nextInt(RogueliteLoadout.MODIFICATION_SLOT_COUNT)
                                : -1;
                if (!applyOffer(rival, offer, replacementSlot)) {
                    rival.consumePendingReward();
                }
            }
        }
    }

    public Snapshot snapshot() {
        Snapshot snapshot = new Snapshot();
        snapshot.randomState = random.getState();
        snapshot.championshipNumber = championshipNumber;
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
                || snapshot.championshipNumber < 1) {
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
        random.setState(snapshot.randomState);
        return true;
    }

    public List<RogueliteCardOffer> restoreOffers(Iterable<String> offerIds) {
        if (offerIds == null || !player.hasPendingReward()) {
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
        int unlockedTier = getUnlockedTier();
        List<RogueliteCardOffer> driverOffers =
                new ArrayList<RogueliteCardOffer>();
        List<DriverProfileMetadata> eligibleDrivers =
                driverCatalog.eligibleThroughTier(unlockedTier);
        for (int i = 0; i < eligibleDrivers.size(); i++) {
            DriverProfileMetadata driver = eligibleDrivers.get(i);
            if (!driver.getProfileId().equals(
                    progress.getLoadout().getDriverProfileId())) {
                driverOffers.add(
                        RogueliteCardOffer.driver(
                                driver,
                                driverCatalog.getTier(driver.getProfileId())));
            }
        }

        List<RogueliteCardOffer> modificationOffers =
                new ArrayList<RogueliteCardOffer>();
        List<RogueliteCardDefinition> cards = RogueliteCardCatalog.all();
        for (int i = 0; i < cards.size(); i++) {
            RogueliteCardDefinition card = cards.get(i);
            if (card.getTier() <= unlockedTier
                    && !progress.getLoadout().has(card.getId())) {
                modificationOffers.add(RogueliteCardOffer.modification(card));
            }
        }

        List<RogueliteCardOffer> selected =
                new ArrayList<RogueliteCardOffer>(count);
        if (!driverOffers.isEmpty() && selected.size() < count) {
            selected.add(takeRandom(driverOffers));
        }
        while (selected.size() < count && !modificationOffers.isEmpty()) {
            selected.add(takeRandom(modificationOffers));
        }
        while (selected.size() < count && !driverOffers.isEmpty()) {
            selected.add(takeRandom(driverOffers));
        }
        shuffle(selected);
        return Collections.unmodifiableList(selected);
    }

    private boolean applyOffer(
            RogueliteCompetitorProgress progress,
            RogueliteCardOffer offer,
            int replacementSlot) {
        if (!progress.hasPendingReward() || !isEligible(progress, offer)) {
            return false;
        }
        boolean applied;
        if (offer.isDriver()) {
            progress.getLoadout().setDriverProfileId(
                    offer.getDriver().getProfileId());
            applied = true;
        } else {
            applied =
                    progress.getLoadout().equip(
                            offer.getCard().getId(),
                            replacementSlot);
        }
        return applied && progress.consumePendingReward();
    }

    private boolean isEligible(
            RogueliteCompetitorProgress progress,
            RogueliteCardOffer offer) {
        if (offer == null || offer.getTier() > getUnlockedTier()) {
            return false;
        }
        if (offer.isDriver()) {
            return driverCatalog.get(offer.getDriver().getProfileId()) != null
                    && !offer.getDriver().getProfileId().equals(
                            progress.getLoadout().getDriverProfileId());
        }
        return offer.getCard() != null
                && !progress.getLoadout().has(offer.getCard().getId());
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
            return RogueliteCardOffer.modification(
                    RogueliteCardCatalog.get(
                            RogueliteCardId.valueOf(
                                    offerId.substring("card:".length()))));
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
        snapshot.pendingRewards = progress.getPendingRewards();
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
                || snapshot.modificationCardIds == null) {
            return null;
        }
        try {
            RogueliteCompetitorProgress progress =
                    new RogueliteCompetitorProgress(snapshot.driverProfileId);
            for (int i = 0; i < snapshot.modificationCardIds.size(); i++) {
                progress.getLoadout().restoreModification(
                        RogueliteCardId.valueOf(
                                snapshot.modificationCardIds.get(i)));
            }
            progress.restore(
                    snapshot.level,
                    snapshot.experience,
                    snapshot.pendingRewards);
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

    private RogueliteCompetitorProgress newProgress() {
        return new RogueliteCompetitorProgress(
                driverCatalog.getWorst().getProfileId());
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
        }
    }

    private RogueliteCardOffer takeRandom(List<RogueliteCardOffer> offers) {
        return offers.remove(random.nextInt(offers.size()));
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
        public ProgressSnapshot player = new ProgressSnapshot();
        public List<RivalSnapshot> rivals = new ArrayList<RivalSnapshot>();
    }

    public static final class ProgressSnapshot {
        public String driverProfileId = "";
        public int level = 1;
        public int experience;
        public int pendingRewards;
        public List<String> modificationCardIds = new ArrayList<String>();
    }

    public static final class RivalSnapshot {
        public int vehicleId;
        public ProgressSnapshot progress = new ProgressSnapshot();
    }
}
