package com.github.jbescos.gameplay.roguelite.strategy;

import com.github.jbescos.gameplay.roguelite.DriverProfileCatalog;
import com.github.jbescos.gameplay.roguelite.RogueliteCardOffer;
import com.github.jbescos.gameplay.roguelite.RogueliteCompetitorProgress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Immutable input for one card-selection decision. */
public final class CardStrategyDecision {
    private final RogueliteCompetitorProgress progress;
    private final DriverProfileCatalog driverCatalog;
    private final List<RogueliteCardOffer> offers;
    private final CardStrategyContext context;

    public CardStrategyDecision(
            RogueliteCompetitorProgress progress,
            DriverProfileCatalog driverCatalog,
            List<RogueliteCardOffer> offers,
            CardStrategyContext context) {
        if (progress == null || driverCatalog == null || offers == null || offers.isEmpty()) {
            throw new IllegalArgumentException("A card decision requires progress, drivers, and offers.");
        }
        this.progress = progress;
        this.driverCatalog = driverCatalog;
        this.offers = Collections.unmodifiableList(
                new ArrayList<RogueliteCardOffer>(offers));
        this.context = context == null ? CardStrategyContext.empty() : context;
    }

    public RogueliteCompetitorProgress getProgress() {
        return progress;
    }

    public DriverProfileCatalog getDriverCatalog() {
        return driverCatalog;
    }

    public List<RogueliteCardOffer> getOffers() {
        return offers;
    }

    public CardStrategyContext getContext() {
        return context;
    }
}
