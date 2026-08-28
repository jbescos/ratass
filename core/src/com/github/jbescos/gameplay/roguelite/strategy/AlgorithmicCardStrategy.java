package com.github.jbescos.gameplay.roguelite.strategy;

import com.github.jbescos.gameplay.roguelite.DriverProfileCatalog;
import com.github.jbescos.gameplay.roguelite.DriverProfileMetadata;
import com.github.jbescos.gameplay.roguelite.RogueliteCardCatalog;
import com.github.jbescos.gameplay.roguelite.RogueliteCardId;
import com.github.jbescos.gameplay.roguelite.RogueliteCardOffer;
import com.github.jbescos.gameplay.roguelite.RogueliteCompetitorProgress;
import com.github.jbescos.gameplay.roguelite.RogueliteLoadout;
import com.github.jbescos.gameplay.roguelite.RogueliteSlotType;
import com.github.jbescos.gameplay.roguelite.RogueliteStrategyMetrics;
import com.github.jbescos.gameplay.roguelite.RogueliteSetCatalog;
import com.github.jbescos.gameplay.roguelite.RogueliteSetDefinition;
import com.github.jbescos.gameplay.roguelite.RogueliteSetId;
import java.util.ArrayList;
import java.util.List;

/** Existing deterministic tier/synergy heuristic, retained as a runtime profile and baseline. */
public final class AlgorithmicCardStrategy implements CardStrategy {
    public static final String PROFILE_ID = "algorithmic";
    private static final float MIN_SYNERGY_GAIN = 0.0001f;
    private static final int EXPLORATION_CHANCE = 10;

    @Override
    public String getProfileId() {
        return PROFILE_ID;
    }

    @Override
    public RogueliteCardOffer choose(
        CardStrategyDecision decision,
            CardStrategyRandom random) {
        RogueliteCompetitorProgress progress = decision.getProgress();
        RogueliteCardOffer finalReckoning = findCard(
                decision.getOffers(), RogueliteCardId.FINAL_RECKONING);
        if (finalReckoning != null) {
            return finalReckoning;
        }
        List<RogueliteCardOffer> eligibleOffers = decision.getOffers();
        if (progress.getLoadout().has(RogueliteCardId.FINAL_RECKONING)) {
            eligibleOffers = withoutSlot(eligibleOffers, RogueliteSlotType.REVENGE);
            if (eligibleOffers.isEmpty()) {
                return null;
            }
        }
        RogueliteCardOffer setOffer = bestSetBuildingOffer(
                progress.getLoadout(),
                eligibleOffers,
                decision.getContext().getEnabledSetIds());
        if (setOffer != null) {
            return setOffer;
        }
        eligibleOffers = preserveStrongestPartialSet(
                progress.getLoadout(),
                eligibleOffers,
                decision.getContext().getEnabledSetIds());
        if (eligibleOffers.isEmpty()) {
            return null;
        }
        OfferBucket bucket = offerBucket(
                decision.getDriverCatalog(), progress, eligibleOffers);
        RogueliteCardOffer synergy = bestSynergyOffer(
                progress,
                bucket.offers,
                decision.getContext());
        if (synergy != null) {
            if (random.nextInt(100) < EXPLORATION_CHANCE) {
                return randomOffer(bucket.offers, random);
            }
            return synergy;
        }
        if (!bucket.mandatory) {
            List<RogueliteCardOffer> fallbacks = fallbackOffers(
                    decision.getDriverCatalog(), progress, bucket.offers);
            if (!fallbacks.isEmpty()) {
                return randomOffer(fallbacks, random);
            }
        }
        return highestScoredOffer(
                decision.getDriverCatalog(), progress, bucket.offers);
    }

    private static RogueliteCardOffer bestSetBuildingOffer(
            RogueliteLoadout loadout,
            List<RogueliteCardOffer> offers,
            List<RogueliteSetId> enabledSetIds) {
        int currentBest = RogueliteSetCatalog.bestMatchingCardCount(loadout, enabledSetIds);
        RogueliteCardOffer bestOffer = null;
        int bestScore = Integer.MIN_VALUE;
        for (RogueliteCardOffer offer : offers) {
            if (offer.isDriver()) {
                continue;
            }
            RogueliteCardId candidate = offer.getCard().getId();
            for (RogueliteSetId setId : enabledSetIds) {
                RogueliteSetDefinition set = RogueliteSetCatalog.get(setId);
                if (set == null || set.getRequiredCard(offer.getSlotType()) != candidate) {
                    continue;
                }
                int before = set.matchingCardCount(loadout);
                int after = RogueliteSetCatalog.matchingCardCountAfter(
                        loadout, candidate, set);
                if (after <= before || after < currentBest) {
                    continue;
                }
                int score = after * 1000 + before * 100 + set.getTier();
                if (after == RogueliteLoadout.MODIFICATION_SLOT_COUNT) {
                    score += 100000;
                }
                if (score > bestScore) {
                    bestScore = score;
                    bestOffer = offer;
                }
            }
        }
        return bestOffer;
    }

    private static List<RogueliteCardOffer> preserveStrongestPartialSet(
            RogueliteLoadout loadout,
            List<RogueliteCardOffer> offers,
            List<RogueliteSetId> enabledSetIds) {
        int currentBest = RogueliteSetCatalog.bestMatchingCardCount(loadout, enabledSetIds);
        if (currentBest < 1) {
            return offers;
        }
        List<RogueliteCardOffer> preserved = new ArrayList<RogueliteCardOffer>();
        for (RogueliteCardOffer offer : offers) {
            if (offer.isDriver()
                    || !breaksStrongestSet(
                            loadout, offer.getCard().getId(), enabledSetIds, currentBest)) {
                preserved.add(offer);
            }
        }
        return preserved;
    }

    private static boolean breaksStrongestSet(
            RogueliteLoadout loadout,
            RogueliteCardId candidate,
            List<RogueliteSetId> enabledSetIds,
            int currentBest) {
        for (RogueliteSetId setId : enabledSetIds) {
            RogueliteSetDefinition set = RogueliteSetCatalog.get(setId);
            if (set != null
                    && set.matchingCardCount(loadout) == currentBest
                    && RogueliteSetCatalog.matchingCardCountAfter(loadout, candidate, set)
                            < currentBest) {
                return true;
            }
        }
        return false;
    }

    private static RogueliteCardOffer findCard(
            List<RogueliteCardOffer> offers,
            RogueliteCardId cardId) {
        for (int i = 0; i < offers.size(); i++) {
            RogueliteCardOffer offer = offers.get(i);
            if (!offer.isDriver() && offer.getCard().getId() == cardId) {
                return offer;
            }
        }
        return null;
    }

    private static List<RogueliteCardOffer> withoutSlot(
            List<RogueliteCardOffer> offers,
            RogueliteSlotType excludedSlot) {
        List<RogueliteCardOffer> filtered = new ArrayList<RogueliteCardOffer>();
        for (int i = 0; i < offers.size(); i++) {
            RogueliteCardOffer offer = offers.get(i);
            if (offer.getSlotType() != excludedSlot) {
                filtered.add(offer);
            }
        }
        return filtered;
    }

    private static OfferBucket offerBucket(
            DriverProfileCatalog catalog,
            RogueliteCompetitorProgress progress,
            List<RogueliteCardOffer> offers) {
        RogueliteLoadout loadout = progress.getLoadout();
        List<RogueliteCardOffer> emptySlots = new ArrayList<RogueliteCardOffer>();
        for (RogueliteCardOffer offer : offers) {
            if (!offer.isDriver() && !loadout.hasCardIn(offer.getSlotType())) {
                emptySlots.add(offer);
            }
        }
        if (!emptySlots.isEmpty()) {
            return new OfferBucket(emptySlots, true);
        }

        int highestTierGain = Integer.MIN_VALUE;
        for (RogueliteCardOffer offer : offers) {
            highestTierGain = Math.max(
                    highestTierGain, tierGain(catalog, progress, offer));
        }
        List<RogueliteCardOffer> highestTier = new ArrayList<RogueliteCardOffer>();
        for (RogueliteCardOffer offer : offers) {
            if (tierGain(catalog, progress, offer) == highestTierGain) {
                highestTier.add(offer);
            }
        }
        return new OfferBucket(highestTier, highestTierGain > 0);
    }

    private static RogueliteCardOffer bestSynergyOffer(
            RogueliteCompetitorProgress progress,
            List<RogueliteCardOffer> offers,
            CardStrategyContext context) {
        RogueliteCardOffer best = null;
        float bestGain = MIN_SYNERGY_GAIN;
        for (RogueliteCardOffer offer : offers) {
            if (offer.isDriver()) {
                continue;
            }
            float gain = RogueliteSetCatalog.selectionGain(
                    progress.getLoadout(),
                    offer.getCard().getId(),
                    context.getEnabledSetIds());
            if (offer.getSlotType() == RogueliteSlotType.TUNING
                    || offer.getSlotType() == RogueliteSlotType.TECHNIQUE) {
                gain += TuningTechniqueSynergy.selectionGain(
                        progress.getLoadout(), offer.getCard().getId());
            }
            if (gain > bestGain) {
                best = offer;
                bestGain = gain;
            }
        }
        return best;
    }

    private static List<RogueliteCardOffer> fallbackOffers(
            DriverProfileCatalog catalog,
            RogueliteCompetitorProgress progress,
            List<RogueliteCardOffer> offers) {
        List<RogueliteCardOffer> result = new ArrayList<RogueliteCardOffer>();
        DriverProfileMetadata current = catalog.get(
                progress.getLoadout().getDriverProfileId());
        for (RogueliteCardOffer offer : offers) {
            if ((!offer.isDriver() && offer.getSlotType() == RogueliteSlotType.REVENGE)
                    || (offer.isDriver()
                            && RogueliteStrategyMetrics.driverQualityGain(
                                    current, offer.getDriver()) > 0f)) {
                result.add(offer);
            }
        }
        return result;
    }

    private static RogueliteCardOffer highestScoredOffer(
            DriverProfileCatalog catalog,
            RogueliteCompetitorProgress progress,
            List<RogueliteCardOffer> offers) {
        RogueliteCardOffer best = offers.get(0);
        float bestScore = offerScore(catalog, progress, best);
        for (int i = 1; i < offers.size(); i++) {
            RogueliteCardOffer candidate = offers.get(i);
            float score = offerScore(catalog, progress, candidate);
            if (score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private static int tierGain(
            DriverProfileCatalog catalog,
            RogueliteCompetitorProgress progress,
            RogueliteCardOffer offer) {
        int gain;
        if (offer.isDriver()) {
            DriverProfileMetadata current = catalog.get(
                    progress.getLoadout().getDriverProfileId());
            int currentTier = current == null ? 0 : catalog.getTier(current.getProfileId());
            gain = offer.getTier() - currentTier;
        } else {
            RogueliteCardId equipped = progress.getLoadout().get(offer.getSlotType());
            int currentTier = equipped == null ? 0 : RogueliteCardCatalog.get(equipped).getTier();
            gain = offer.getTier() - currentTier;
        }
        // Tier 4 contains exceptional amplifiers, not automatic upgrades over Tier 3.
        return offer.getTier() == RogueliteCardCatalog.MAX_CARD_TIER && gain > 0
                ? 0 : gain;
    }

    private static float offerScore(
            DriverProfileCatalog catalog,
            RogueliteCompetitorProgress progress,
            RogueliteCardOffer offer) {
        RogueliteLoadout loadout = progress.getLoadout();
        int currentTier;
        float qualityGain = 0f;
        if (offer.isDriver()) {
            DriverProfileMetadata current = catalog.get(loadout.getDriverProfileId());
            currentTier = current == null ? 0 : catalog.getTier(current.getProfileId());
            qualityGain = RogueliteStrategyMetrics.driverQualityGain(current, offer.getDriver());
            if (offer.getTier() <= currentTier && qualityGain <= 0f) {
                return -100000f + qualityGain;
            }
        } else {
            RogueliteCardId equipped = loadout.get(offer.getSlotType());
            currentTier = equipped == null ? 0 : RogueliteCardCatalog.get(equipped).getTier();
        }
        int tierGain = tierGain(catalog, progress, offer);
        if (tierGain < 0) {
            return -100000f + tierGain * 1000f;
        }
        float weakestSlotPriority =
                (RogueliteCardCatalog.MAX_CARD_TIER - currentTier) * 100f;
        return tierGain * 10000f + weakestSlotPriority + Math.max(0f, qualityGain);
    }

    private static RogueliteCardOffer randomOffer(
            List<RogueliteCardOffer> offers,
            CardStrategyRandom random) {
        return offers.get(random.nextInt(offers.size()));
    }

    private static final class OfferBucket {
        private final List<RogueliteCardOffer> offers;
        private final boolean mandatory;

        private OfferBucket(List<RogueliteCardOffer> offers, boolean mandatory) {
            this.offers = offers;
            this.mandatory = mandatory;
        }
    }
}
