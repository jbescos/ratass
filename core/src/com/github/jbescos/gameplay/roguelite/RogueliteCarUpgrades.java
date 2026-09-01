package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RogueliteCarUpgrades {
    private static final float MIN_EFFECTIVE_STAT_MULTIPLIER = 0.10f;
    private static final float MIN_TOP_SPEED_MULTIPLIER = 0.65f;
    private static final RogueliteCardId[] RIVAL_BUILD_LEECH_CARD_IDS = {
        RogueliteCardId.TELEMETRY_THEFT,
        RogueliteCardId.BUILD_HEIST,
        RogueliteCardId.APEX_PLUNDER
    };

    private final List<RogueliteUpgradeEffect> effects =
            new ArrayList<RogueliteUpgradeEffect>();
    private final List<RogueliteCardId> activeCardIds =
            new ArrayList<RogueliteCardId>();
    private final List<RogueliteCardId> readOnlyActiveCardIds =
            Collections.unmodifiableList(activeCardIds);
    private final RogueliteDrivingFrame frame = new RogueliteDrivingFrame();
    private final RogueliteDrivingFrame techniqueObservationFrame =
            new RogueliteDrivingFrame();
    private float timedEffectDecay = 1f;
    private float techniqueEffectMultiplier = 1f;
    private float powerupEffectMultiplier = 1f;
    private float powerupCooldownRateMultiplier = 1f;
    private float benchmarkTuningEffectMultiplier = 1f;
    private AntennaNetworkBonuses antennaNetwork = AntennaNetworkBonuses.NONE;
    private boolean buildCardsSuppressed;
    private boolean overtakeInjectorEnabled;
    private RogueliteCardId activeRevengeCardId;
    private long revengeActivationSequence;
    private RogueliteUpgradeEffect amplifiedActiveRevengeEffect;
    private RogueliteSetDefinition configuredSetBonus;

    public void configure(RogueliteLoadout loadout) {
        configure(loadout, 0f);
    }

    public void configure(RogueliteLoadout loadout, float powerupCycleOffset) {
        configure(loadout, powerupCycleOffset, null);
    }

    public void configure(
            RogueliteLoadout loadout,
            float powerupCycleOffset,
            RogueliteSetDefinition setBonus) {
        effects.clear();
        activeCardIds.clear();
        activeRevengeCardId = null;
        amplifiedActiveRevengeEffect = null;
        buildCardsSuppressed = false;
        reconfigurePreservingCardState(loadout, powerupCycleOffset, setBonus);
    }

    public void reconfigurePreservingCardState(
            RogueliteLoadout loadout,
            float powerupCycleOffset) {
        reconfigurePreservingCardState(loadout, powerupCycleOffset, null);
    }

    public void reconfigurePreservingCardState(
            RogueliteLoadout loadout,
            float powerupCycleOffset,
            RogueliteSetDefinition setBonus) {
        List<RogueliteUpgradeEffect> previousEffects =
                new ArrayList<RogueliteUpgradeEffect>(effects);
        effects.clear();
        frame.clear();
        techniqueObservationFrame.clear();
        timedEffectDecay = 1f;
        techniqueEffectMultiplier = 1f;
        powerupEffectMultiplier = 1f;
        powerupCooldownRateMultiplier = 1f;
        antennaNetwork = AntennaNetworkBonuses.NONE;
        overtakeInjectorEnabled = false;
        configuredSetBonus = setBonus;

        if (loadout != null) {
            List<RogueliteCardId> cardIds = loadout.getModifications();
            for (int i = 0; i < cardIds.size(); i++) {
                RogueliteCardId cardId = cardIds.get(i);
                RogueliteUpgradeEffect effect =
                        takePreviousEffect(previousEffects, cardId);
                if (effect == null) {
                    effect = RogueliteEffectFactory.create(cardId, powerupCycleOffset);
                }
                effects.add(effect);
                timedEffectDecay = Math.min(timedEffectDecay, effect.timedEffectDecay());
                overtakeInjectorEnabled |= effect.tracksRacePosition();
            }
        }

        if (setBonus != null) {
            RogueliteCardId bonusCardId = setBonus.getBonusCardId();
            RogueliteUpgradeEffect bonusEffect =
                    takePreviousEffect(previousEffects, bonusCardId);
            if (!matchesSetBonusEffect(setBonus, bonusEffect)) {
                bonusEffect = createSetBonusEffect(setBonus, powerupCycleOffset);
            }
            effects.add(bonusEffect);
            timedEffectDecay = Math.min(timedEffectDecay, bonusEffect.timedEffectDecay());
            overtakeInjectorEnabled |= bonusEffect.tracksRacePosition();
        }

        boolean techniqueAlwaysActive = setBonus != null
                && setBonus.getId() == RogueliteSetId.CHAOS_CIRCUIT;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (effect instanceof RaceTechniqueEffect) {
                ((RaceTechniqueEffect) effect).setAlwaysActive(techniqueAlwaysActive);
            }
        }

        for (int i = 0; i < effects.size(); i++) {
            techniqueEffectMultiplier *= effects.get(i).techniqueEffectMultiplier();
        }
        float rawPowerupEffectMultiplier = 1f;
        float rawPowerupCooldownRateMultiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            rawPowerupEffectMultiplier *= effects.get(i).powerupEffectMultiplier();
            rawPowerupCooldownRateMultiplier *=
                    effects.get(i).powerupCooldownRateMultiplier();
        }
        powerupEffectMultiplier = CardAmplifierChain.combine(
                rawPowerupEffectMultiplier,
                techniqueEffectMultiplier);
        powerupCooldownRateMultiplier = CardAmplifierChain.combine(
                rawPowerupCooldownRateMultiplier,
                techniqueEffectMultiplier);

        if (amplifiedActiveRevengeEffect != null
                && !effects.contains(amplifiedActiveRevengeEffect)) {
            amplifiedActiveRevengeEffect = null;
            notifyRevengeAmplifierFinished();
        }
        refreshActiveCards();
    }

    public RogueliteSetDefinition getConfiguredSetBonus() {
        return configuredSetBonus;
    }

    public void setAntennaNetwork(AntennaNetworkBonuses antennaNetwork) {
        AntennaNetworkBonuses availableNetwork = antennaNetwork == null
                ? AntennaNetworkBonuses.NONE
                : antennaNetwork;
        this.antennaNetwork = availableNetwork.forRecipient(
                configuredCardId(RogueliteSlotType.TUNING),
                configuredCardId(RogueliteSlotType.TECHNIQUE),
                getActiveAntennaCardId());
    }

    /** Scales Tuning deviations for isolated headless balance benchmarks only. */
    public void setBenchmarkTuningEffectMultiplier(float multiplier) {
        benchmarkTuningEffectMultiplier = Math.max(0f, multiplier);
    }

    public void setBuildCardsSuppressed(boolean suppressed) {
        if (buildCardsSuppressed == suppressed) {
            return;
        }
        buildCardsSuppressed = suppressed;
        refreshActiveCards();
    }

    public boolean areBuildCardsSuppressed() {
        return buildCardsSuppressed;
    }

    private static RogueliteUpgradeEffect takePreviousEffect(
            List<RogueliteUpgradeEffect> previousEffects,
            RogueliteCardId cardId) {
        for (int i = 0; i < previousEffects.size(); i++) {
            RogueliteUpgradeEffect effect = previousEffects.get(i);
            if (effect.getCardId() == cardId) {
                previousEffects.remove(i);
                return effect;
            }
        }
        return null;
    }

    public boolean isEnabled() {
        return !effects.isEmpty();
    }

    public boolean hasOvertakeInjector() {
        return overtakeInjectorEnabled;
    }

    public List<RogueliteCardId> getActiveCardIds() {
        return readOnlyActiveCardIds;
    }

    public RogueliteCardId getActiveCardId(RogueliteSlotType slotType) {
        for (int i = 0; i < activeCardIds.size(); i++) {
            RogueliteCardId cardId = activeCardIds.get(i);
            if (RogueliteCardCatalog.get(cardId).getSlotType() == slotType) {
                return cardId;
            }
        }
        return null;
    }

    public RogueliteCardId getActivePowerupCardId() {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteCardId nestedPowerupCardId = effects.get(i).activePowerupCardId();
            if (nestedPowerupCardId != null) {
                return nestedPowerupCardId;
            }
        }
        return getActiveCardId(RogueliteSlotType.POWERUP);
    }

    public RogueliteCardId getActiveAntennaCardId() {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            RogueliteCardId cardId = effect.behaviorCardId();
            if (effect.isActive() && AntennaPowerupSpec.isAntennaCard(cardId)) {
                return cardId;
            }
        }
        return null;
    }

    public RogueliteCardId getActiveTechniqueCardId() {
        return getActiveCardId(RogueliteSlotType.TECHNIQUE);
    }

    public RogueliteCardId getLoadedCardId(RogueliteSlotType slotType) {
        if (slotType == null) {
            return null;
        }
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (RogueliteCardCatalog.get(effect.getCardId()).getSlotType()
                    != slotType) {
                continue;
            }
            RogueliteCardId loadedCardId = effect.loadedDisplayCardId();
            if (loadedCardId != null) {
                return loadedCardId;
            }
        }
        return null;
    }

    public long getRevengeActivationSequence() {
        return revengeActivationSequence;
    }

    public RogueliteCardId getActiveAbilityCardId() {
        if (isCardEffectActive(RogueliteCardId.REPULSOR_SURGE)) {
            return RogueliteCardId.REPULSOR_SURGE;
        }
        for (int i = 0; i < activeCardIds.size(); i++) {
            RogueliteCardId cardId = activeCardIds.get(i);
            RogueliteSlotType slotType =
                    RogueliteCardCatalog.get(cardId).getSlotType();
            if (slotType == RogueliteSlotType.POWERUP
                    || slotType == RogueliteSlotType.REVENGE) {
                return cardId;
            }
        }
        return null;
    }

    public boolean isCardEffectActive(RogueliteCardId cardId) {
        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i).isCardEffectActive(cardId)) {
                return true;
            }
        }
        return false;
    }

    public boolean isCardEffectArmed(RogueliteCardId cardId) {
        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i).isCardEffectArmed(cardId)) {
                return true;
            }
        }
        return false;
    }

    public float getCardEffectReadiness(RogueliteCardId cardId) {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (effect.containsCardEffect(cardId)) {
                return effect.cardEffectReadiness(cardId);
            }
        }
        return 0f;
    }

    public float getCardEffectActiveTimeRemainingSeconds(RogueliteCardId cardId) {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (effect.containsCardEffect(cardId)) {
                return effect.cardEffectActiveTimeRemainingSeconds(cardId);
            }
        }
        return 0f;
    }

    public boolean isPowerupReady() {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (RogueliteCardCatalog.get(effect.getCardId()).getSlotType()
                            == RogueliteSlotType.POWERUP
                    && effect.isReady()) {
                return true;
            }
        }
        return false;
    }

    public void setAutomaticPowerupActivationAllowed(boolean allowed) {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            RogueliteCardDefinition definition =
                    RogueliteCardCatalog.get(effect.getCardId());
            if (definition != null
                    && definition.getSlotType() == RogueliteSlotType.POWERUP) {
                effect.setAutomaticPowerupActivationAllowed(allowed);
            }
        }
    }

    public boolean canManuallyActivatePowerup() {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            RogueliteCardDefinition definition =
                    RogueliteCardCatalog.get(effect.getCardId());
            if (definition != null
                    && definition.getSlotType() == RogueliteSlotType.POWERUP
                    && effect.supportsManualPowerupActivation()
                    && effect.isReady()) {
                return true;
            }
        }
        return false;
    }

    public boolean requestManualPowerupActivation() {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            RogueliteCardDefinition definition =
                    RogueliteCardCatalog.get(effect.getCardId());
            if (definition != null
                    && definition.getSlotType() == RogueliteSlotType.POWERUP
                    && effect.requestManualPowerupActivation()) {
                return true;
            }
        }
        return false;
    }

    public boolean isBestDriverActive() {
        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i).usesBestDriver()) {
                return true;
            }
        }
        return false;
    }

    public boolean isTimeDilationActive() {
        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i).acceleratesOwnDecisions()) {
                return true;
            }
        }
        return false;
    }

    public float getPowerupReadiness() {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (RogueliteCardCatalog.get(effect.getCardId()).getSlotType()
                    == RogueliteSlotType.POWERUP) {
                return effect.readiness();
            }
        }
        return 0f;
    }

    public boolean isRevengeArmed() {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (RogueliteCardCatalog.get(effect.getCardId()).getSlotType()
                            == RogueliteSlotType.REVENGE
                    && effect.isArmed()) {
                return true;
            }
        }
        return false;
    }

    public boolean isRevengeReady() {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (RogueliteCardCatalog.get(effect.getCardId()).getSlotType()
                            == RogueliteSlotType.REVENGE
                    && effect.isReady()) {
                return true;
            }
        }
        return false;
    }

    public float getRevengeReadiness() {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (RogueliteCardCatalog.get(effect.getCardId()).getSlotType()
                    == RogueliteSlotType.REVENGE) {
                return effect.readiness();
            }
        }
        return 0f;
    }

    public float getRevengeActiveTimeRemainingSeconds() {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (RogueliteCardCatalog.get(effect.getCardId()).getSlotType()
                    == RogueliteSlotType.REVENGE) {
                return effect.activeTimeRemainingSeconds();
            }
        }
        return 0f;
    }

    public RogueliteCardId getRevengeCardId() {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteCardId cardId = effects.get(i).behaviorCardId();
            if (RogueliteCardCatalog.get(cardId).getSlotType()
                    == RogueliteSlotType.REVENGE) {
                return cardId;
            }
        }
        return null;
    }

    public float getActiveTimeRemainingSeconds(RogueliteCardId cardId) {
        RogueliteUpgradeEffect effect = findEffect(cardId);
        if (effect == null) {
            return 0f;
        }
        float remaining = effect.activeTimeRemainingSeconds();
        return isAmplifiablePowerup(effect)
                ? remaining * effectivePowerupEffectMultiplier()
                : remaining;
    }

    public float getCooldownTimeRemainingSeconds(RogueliteCardId cardId) {
        RogueliteUpgradeEffect effect = findEffect(cardId);
        if (effect == null) {
            return 0f;
        }
        float remaining = effect.cooldownTimeRemainingSeconds();
        return isAmplifiablePowerup(effect)
                ? remaining / Math.max(0.001f, effectivePowerupCooldownRateMultiplier())
                : remaining;
    }

    public int getMirrorTotalVehicleCount(RogueliteCardId cardId) {
        float effectMultiplier = effectivePowerupEffectMultiplier();
        for (int i = 0; i < effects.size(); i++) {
            effectMultiplier = Math.max(
                    effectMultiplier,
                    effects.get(i).nestedPowerupEffectMultiplier(cardId));
        }
        return MirrorPowerupSpec.amplifiedTotalVehicleCount(
                cardId,
                effectMultiplier);
    }

    private RogueliteUpgradeEffect findEffect(RogueliteCardId cardId) {
        if (cardId == null) {
            return null;
        }
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (effect.getCardId() == cardId) {
                return effect;
            }
        }
        return null;
    }

    public void update(
            float delta,
            float throttle,
            boolean onRoad,
            boolean adverseWeather,
            boolean recentlyImpacted,
            float slip,
            float speedRatio,
            float routeProgress,
            float routeLength,
            float safeRecoveryRouteGain) {
        update(
                delta,
                throttle,
                onRoad,
                adverseWeather,
                recentlyImpacted,
                slip,
                speedRatio,
                0f,
                routeProgress,
                routeLength,
                safeRecoveryRouteGain);
    }

    public void update(
            float delta,
            float throttle,
            boolean onRoad,
            boolean adverseWeather,
            boolean recentlyImpacted,
            float slip,
            float speedRatio,
            float slipstreamBoost,
            float routeProgress,
            float routeLength,
            float safeRecoveryRouteGain) {
        update(
                delta,
                throttle,
                onRoad,
                adverseWeather,
                recentlyImpacted,
                slip,
                speedRatio,
                slipstreamBoost,
                routeProgress,
                routeLength,
                safeRecoveryRouteGain,
                0f,
                1f,
                0f,
                0f,
                0f);
    }

    public void update(
            float delta,
            float throttle,
            boolean onRoad,
            boolean adverseWeather,
            boolean recentlyImpacted,
            float slip,
            float speedRatio,
            float slipstreamBoost,
            float routeProgress,
            float routeLength,
            float safeRecoveryRouteGain,
            float cornerSeverity,
            float nextCornerDistance,
            float nextCornerSeverity) {
        update(
                delta,
                throttle,
                onRoad,
                adverseWeather,
                recentlyImpacted,
                slip,
                speedRatio,
                slipstreamBoost,
                routeProgress,
                routeLength,
                safeRecoveryRouteGain,
                cornerSeverity,
                nextCornerDistance,
                nextCornerSeverity,
                0f,
                0f);
    }

    public void update(
            float delta,
            float throttle,
            boolean onRoad,
            boolean adverseWeather,
            boolean recentlyImpacted,
            float slip,
            float speedRatio,
            float slipstreamBoost,
            float routeProgress,
            float routeLength,
            float safeRecoveryRouteGain,
            float cornerSeverity,
            float nextCornerDistance,
            float nextCornerSeverity,
            float opponentAheadProximity,
            float nearbyOpponentProximity) {
        update(
                delta,
                throttle,
                onRoad,
                adverseWeather,
                recentlyImpacted,
                slip,
                speedRatio,
                slipstreamBoost,
                routeProgress,
                routeLength,
                safeRecoveryRouteGain,
                cornerSeverity,
                nextCornerDistance,
                nextCornerSeverity,
                opponentAheadProximity,
                nearbyOpponentProximity,
                false);
    }

    public void update(
            float delta,
            float throttle,
            boolean onRoad,
            boolean adverseWeather,
            boolean recentlyImpacted,
            float slip,
            float speedRatio,
            float slipstreamBoost,
            float routeProgress,
            float routeLength,
            float safeRecoveryRouteGain,
            float cornerSeverity,
            float nextCornerDistance,
            float nextCornerSeverity,
            float opponentAheadProximity,
            float nearbyOpponentProximity,
            boolean forwardLaneBlocked) {
        update(
                delta,
                throttle,
                onRoad,
                adverseWeather,
                recentlyImpacted,
                slip,
                speedRatio,
                slipstreamBoost,
                routeProgress,
                routeLength,
                safeRecoveryRouteGain,
                cornerSeverity,
                nextCornerDistance,
                nextCornerSeverity,
                opponentAheadProximity,
                nearbyOpponentProximity,
                forwardLaneBlocked,
                0f);
    }

    public void update(
            float delta,
            float throttle,
            boolean onRoad,
            boolean adverseWeather,
            boolean recentlyImpacted,
            float slip,
            float speedRatio,
            float slipstreamBoost,
            float routeProgress,
            float routeLength,
            float safeRecoveryRouteGain,
            float cornerSeverity,
            float nextCornerDistance,
            float nextCornerSeverity,
            float opponentAheadProximity,
            float nearbyOpponentProximity,
            boolean forwardLaneBlocked,
            float racePositionFactor) {
        update(
                delta,
                throttle,
                onRoad,
                adverseWeather,
                recentlyImpacted,
                slip,
                speedRatio,
                slipstreamBoost,
                routeProgress,
                routeLength,
                safeRecoveryRouteGain,
                cornerSeverity,
                nextCornerDistance,
                nextCornerSeverity,
                opponentAheadProximity,
                nearbyOpponentProximity,
                forwardLaneBlocked,
                racePositionFactor,
                nearbyOpponentProximity);
    }

    public void update(
            float delta,
            float throttle,
            boolean onRoad,
            boolean adverseWeather,
            boolean recentlyImpacted,
            float slip,
            float speedRatio,
            float slipstreamBoost,
            float routeProgress,
            float routeLength,
            float safeRecoveryRouteGain,
            float cornerSeverity,
            float nextCornerDistance,
            float nextCornerSeverity,
            float opponentAheadProximity,
            float nearbyOpponentProximity,
            boolean forwardLaneBlocked,
            float racePositionFactor,
            float revengeNearbyOpponentProximity) {
        boolean inferredLongStraight =
                onRoad
                        && throttle > 0.05f
                        && speedRatio >= 0.24f
                        && slip <= 0.10f
                        && cornerSeverity <= 0.055f
                        && (nextCornerSeverity <= 0.13f || nextCornerDistance >= 0.85f)
                        && !forwardLaneBlocked;
        update(
                delta,
                throttle,
                onRoad,
                adverseWeather,
                recentlyImpacted,
                slip,
                speedRatio,
                slipstreamBoost,
                routeProgress,
                routeLength,
                safeRecoveryRouteGain,
                cornerSeverity,
                nextCornerDistance,
                nextCornerSeverity,
                opponentAheadProximity,
                nearbyOpponentProximity,
                forwardLaneBlocked,
                racePositionFactor,
                revengeNearbyOpponentProximity,
                inferredLongStraight);
    }

    public void update(
            float delta,
            float throttle,
            boolean onRoad,
            boolean adverseWeather,
            boolean recentlyImpacted,
            float slip,
            float speedRatio,
            float slipstreamBoost,
            float routeProgress,
            float routeLength,
            float safeRecoveryRouteGain,
            float cornerSeverity,
            float nextCornerDistance,
            float nextCornerSeverity,
            float opponentAheadProximity,
            float nearbyOpponentProximity,
            boolean forwardLaneBlocked,
            float racePositionFactor,
            float revengeNearbyOpponentProximity,
            boolean longStraight) {
        update(
                delta,
                throttle,
                onRoad,
                adverseWeather,
                recentlyImpacted,
                slip,
                speedRatio,
                slipstreamBoost,
                routeProgress,
                routeLength,
                safeRecoveryRouteGain,
                cornerSeverity,
                nextCornerDistance,
                nextCornerSeverity,
                opponentAheadProximity,
                nearbyOpponentProximity,
                forwardLaneBlocked,
                racePositionFactor,
                revengeNearbyOpponentProximity,
                longStraight,
                nearbyOpponentProximity);
    }

    public void update(
            float delta,
            float throttle,
            boolean onRoad,
            boolean adverseWeather,
            boolean recentlyImpacted,
            float slip,
            float speedRatio,
            float slipstreamBoost,
            float routeProgress,
            float routeLength,
            float safeRecoveryRouteGain,
            float cornerSeverity,
            float nextCornerDistance,
            float nextCornerSeverity,
            float opponentAheadProximity,
            float nearbyOpponentProximity,
            boolean forwardLaneBlocked,
            float racePositionFactor,
            float revengeNearbyOpponentProximity,
            boolean longStraight,
            float techniqueNearbyOpponentProximity) {
        if (effects.isEmpty()) {
            return;
        }
        frame.set(
                throttle,
                onRoad,
                adverseWeather,
                recentlyImpacted,
                slip,
                speedRatio,
                slipstreamBoost,
                routeProgress,
                routeLength,
                safeRecoveryRouteGain,
                RogueliteEffectMath.clamp(cornerSeverity, 0f, 1f),
                RogueliteEffectMath.clamp(nextCornerDistance, 0f, 1f),
                RogueliteEffectMath.clamp(nextCornerSeverity, 0f, 1f),
                RogueliteEffectMath.clamp(opponentAheadProximity, 0f, 1f),
                RogueliteEffectMath.clamp(nearbyOpponentProximity, 0f, 1f),
                forwardLaneBlocked,
                RogueliteEffectMath.clamp(racePositionFactor, 0f, 1f),
                RogueliteEffectMath.clamp(revengeNearbyOpponentProximity, 0f, 1f),
                longStraight,
                RogueliteEffectMath.clamp(techniqueNearbyOpponentProximity, 0f, 1f));
        float timerDelta = delta * timedEffectDecay;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            float effectTimerDelta = timerDelta;
            if (isAmplifiablePowerup(effect)) {
                effectTimerDelta *= effect.isActive()
                        ? 1f / Math.max(0.001f, effectivePowerupEffectMultiplier())
                        : effectivePowerupCooldownRateMultiplier();
            }
            effect.advance(delta, effectTimerDelta, frame);
        }
        refreshActiveCards();
    }

    /**
     * Lets another physical member of the same quantum family activate the shared
     * Technique card without advancing shared timers or other card workflows.
     */
    public void observeTechniqueConditions(
            boolean onRoad,
            float slip,
            float speedRatio,
            float slipstreamBoost,
            float cornerSeverity,
            boolean longStraight) {
        if (effects.isEmpty()) {
            return;
        }
        techniqueObservationFrame.set(
                0f,
                onRoad,
                false,
                false,
                slip,
                speedRatio,
                slipstreamBoost,
                0f,
                0f,
                0f,
                RogueliteEffectMath.clamp(cornerSeverity, 0f, 1f),
                1f,
                0f,
                0f,
                0f,
                false,
                0f,
                0f,
                longStraight);
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).observeTechniqueCondition(techniqueObservationFrame);
        }
        refreshActiveCards();
    }

    public float adjustSurfaceGrip(float baseGripMultiplier) {
        float adjusted = baseGripMultiplier;
        for (int i = 0; i < effects.size(); i++) {
            adjusted = effects.get(i).adjustSurfaceGrip(adjusted);
        }
        return RogueliteEffectMath.clamp(adjusted, 0f, 1f);
    }

    public float getAccelerationMultiplier() {
        return getAccelerationMultiplier(1f);
    }

    public float getAccelerationMultiplier(float externalMultiplier) {
        float bonus = sharesTuningThroughAntenna()
                ? antennaNetwork.getPowerBonus()
                : 0f;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (isReplacedTuningEffect(effect)) {
                continue;
            }
            bonus += effect.accelerationBonus() * effectStrengthMultiplier(effect);
        }
        float combined = (1f + bonus) * externalMultiplier;
        return Math.max(
                MIN_EFFECTIVE_STAT_MULTIPLIER,
                amplifyDeviation(combined, powerDeviationScale()));
    }

    public float getDriveForceLimitMultiplier() {
        float multiplier = sharesTuningThroughAntenna()
                ? (1f + antennaNetwork.getPowerBonus())
                        / antennaNetwork.getMassMultiplier()
                : 1f;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (isReplacedTuningEffect(effect)) {
                continue;
            }
            multiplier *= amplifyDeviation(
                    effect.driveForceLimitMultiplier(),
                    effectStrengthMultiplier(effect));
        }
        return Math.max(0.70f, amplifyDeviation(multiplier, powerDeviationScale()));
    }

    public float getMaxSpeedMultiplier() {
        return getMaxSpeedMultiplier(1f, 1f);
    }

    public float getMaxSpeedMultiplier(
            float externalPowerMultiplier,
            float externalAerodynamicEfficiencyMultiplier) {
        return deriveMaxSpeedMultiplier(
                getAccelerationMultiplier(externalPowerMultiplier),
                getAerodynamicEfficiencyMultiplier(
                        externalAerodynamicEfficiencyMultiplier));
    }

    static float deriveMaxSpeedMultiplier(
            float powerMultiplier,
            float aerodynamicEfficiencyMultiplier) {
        float performanceProduct = Math.max(
                0f,
                powerMultiplier * aerodynamicEfficiencyMultiplier);
        return Math.max(
                MIN_TOP_SPEED_MULTIPLIER,
                (float) Math.cbrt(performanceProduct));
    }

    public float getDragMultiplier() {
        return getDragMultiplier(1f);
    }

    public float getDragMultiplier(float externalAerodynamicEfficiencyMultiplier) {
        return 1f
                / Math.max(
                        0.01f,
                        getAerodynamicEfficiencyMultiplier(
                                externalAerodynamicEfficiencyMultiplier));
    }

    public float getAerodynamicEfficiencyMultiplier(float externalMultiplier) {
        float multiplier = sharesTuningThroughAntenna()
                ? 1f / antennaNetwork.getAerodynamicEfficiency()
                : 1f;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (isReplacedTuningEffect(effect)) {
                continue;
            }
            multiplier *= amplifyDeviation(
                    effect.dragMultiplier(),
                    effectStrengthMultiplier(effect));
        }
        float combinedEfficiency = externalMultiplier / Math.max(0.01f, multiplier);
        return Math.max(
                MIN_EFFECTIVE_STAT_MULTIPLIER,
                amplifyDeviation(combinedEfficiency, aeroDeviationScale()));
    }

    public float getMassMultiplier() {
        return getMassMultiplier(1f);
    }

    public float getMassMultiplier(float externalMultiplier) {
        float multiplier = sharesTuningThroughAntenna()
                ? antennaNetwork.getMassMultiplier()
                : 1f;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (isReplacedTuningEffect(effect)) {
                continue;
            }
            multiplier *= amplifyDeviation(
                    effect.massMultiplier(),
                    effectStrengthMultiplier(effect));
        }
        return Math.max(
                MIN_EFFECTIVE_STAT_MULTIPLIER,
                amplifyDeviation(multiplier * externalMultiplier, massDeviationScale()));
    }

    public float getGripMultiplier(float slip) {
        return getGripMultiplier(slip, 1f, 1f);
    }

    public float getGripMultiplier(float slip, float carEffectMultiplier) {
        return getGripMultiplier(slip, 1f, carEffectMultiplier);
    }

    public float getGripMultiplier(
            float slip,
            float surfaceMultiplier,
            float carEffectMultiplier) {
        float bonus = sharesTuningThroughAntenna()
                ? antennaNetwork.getGripBonus()
                : 0f;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (isReplacedTuningEffect(effect)) {
                continue;
            }
            bonus += effect.gripBonus(slip) * effectStrengthMultiplier(effect);
        }
        float carGrip = (1f + bonus) * carEffectMultiplier;
        float techniqueGrip = RogueliteEffectMath.amplifyPositiveDeviation(
                carGrip,
                gripDeviationScale());
        // Weather and other surface loss remain independent from Technique effects.
        return Math.max(
                MIN_EFFECTIVE_STAT_MULTIPLIER,
                techniqueGrip * surfaceMultiplier);
    }

    public float getSteeringMultiplier(float slip) {
        float bonus = 0f;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            bonus += effect.steeringBonus(slip) * effectStrengthMultiplier(effect);
        }
        return Math.max(0.70f, 1f + bonus);
    }

    public float getSteeringTorqueMultiplier() {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            multiplier *= amplifyDeviation(
                    effect.steeringTorqueMultiplier(),
                    effectStrengthMultiplier(effect));
        }
        return Math.max(0.1f, multiplier);
    }

    public float getLapExperienceBankMultiplier() {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            multiplier = Math.max(
                    multiplier,
                    CardAmplifierChain.combine(
                            effect.lapExperienceBankMultiplier(),
                            effectStrengthMultiplier(effect)));
        }
        if (AntennaPowerupSpec.sharesTechnique(configuredAntennaCardId())) {
            multiplier = Math.max(
                    multiplier,
                    CardAmplifierChain.combine(
                            antennaNetwork.getLapExperienceBankMultiplier(),
                            effectiveTechniqueEffectMultiplier()));
        }
        return multiplier;
    }

    public float getSlipstreamRangeMultiplier() {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            multiplier *= amplifyDeviation(
                    effect.slipstreamRangeMultiplier(),
                    effectStrengthMultiplier(effect));
        }
        return RogueliteEffectMath.clamp(multiplier, 1f, 2f);
    }

    private float powerDeviationScale() {
        float scale = benchmarkTuningEffectMultiplier;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (isReplacedTechniqueEffect(effect)) {
                continue;
            }
            scale *= amplifyDeviation(
                    effect.powerDeviationScale(),
                    effectStrengthMultiplier(effect));
        }
        if (sharesActiveTechniqueThroughAntenna()) {
            scale *= amplifyDeviation(
                    antennaNetwork.getPowerTechniqueScale(),
                    effectiveTechniqueEffectMultiplier());
        }
        return scale;
    }

    private float gripDeviationScale() {
        float scale = benchmarkTuningEffectMultiplier;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (isReplacedTechniqueEffect(effect)) {
                continue;
            }
            scale *= amplifyDeviation(
                    effect.gripDeviationScale(),
                    effectStrengthMultiplier(effect));
        }
        if (sharesActiveTechniqueThroughAntenna()) {
            scale *= amplifyDeviation(
                    antennaNetwork.getGripTechniqueScale(),
                    effectiveTechniqueEffectMultiplier());
        }
        return scale;
    }

    private float aeroDeviationScale() {
        float scale = benchmarkTuningEffectMultiplier;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (isReplacedTechniqueEffect(effect)) {
                continue;
            }
            scale *= amplifyDeviation(
                    effect.aeroDeviationScale(),
                    effectStrengthMultiplier(effect));
        }
        if (sharesActiveTechniqueThroughAntenna()) {
            scale *= amplifyDeviation(
                    antennaNetwork.getAeroTechniqueScale(),
                    effectiveTechniqueEffectMultiplier());
        }
        return scale;
    }

    private float massDeviationScale() {
        float scale = benchmarkTuningEffectMultiplier;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (isReplacedTechniqueEffect(effect)) {
                continue;
            }
            scale *= amplifyDeviation(
                    effect.massDeviationScale(),
                    effectStrengthMultiplier(effect));
        }
        if (sharesActiveTechniqueThroughAntenna()) {
            scale *= amplifyDeviation(
                    antennaNetwork.getMassTechniqueScale(),
                    effectiveTechniqueEffectMultiplier());
        }
        return scale;
    }

    private boolean sharesTuningThroughAntenna() {
        return !buildCardsSuppressed
                && AntennaPowerupSpec.sharesTuning(configuredAntennaCardId());
    }

    private boolean sharesActiveTechniqueThroughAntenna() {
        if (buildCardsSuppressed
                || !AntennaPowerupSpec.sharesTechnique(configuredAntennaCardId())) {
            return false;
        }
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (effect instanceof RaceTechniqueEffect
                    && ((RaceTechniqueEffect) effect)
                            .isMultiplicativeTechniqueActive()) {
                return true;
            }
        }
        return false;
    }

    private RogueliteCardId configuredAntennaCardId() {
        return getActiveAntennaCardId();
    }

    private RogueliteCardId configuredCardId(RogueliteSlotType slotType) {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteCardId cardId = effects.get(i).getCardId();
            if (RogueliteCardCatalog.get(cardId).getSlotType() == slotType) {
                return cardId;
            }
        }
        return null;
    }

    private boolean isReplacedTuningEffect(RogueliteUpgradeEffect effect) {
        return sharesTuningThroughAntenna()
                && effect instanceof TieredTuningEffect;
    }

    private boolean isReplacedTechniqueEffect(RogueliteUpgradeEffect effect) {
        return sharesActiveTechniqueThroughAntenna()
                && effect instanceof RaceTechniqueEffect;
    }

    private static float amplifyDeviation(float multiplier, float scale) {
        return RogueliteEffectMath.amplifyDeviation(multiplier, scale);
    }

    public float getSlipstreamStrengthMultiplier() {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            multiplier *= amplifyDeviation(
                    effect.slipstreamStrengthMultiplier(),
                    effectStrengthMultiplier(effect));
        }
        return RogueliteEffectMath.clamp(multiplier, 1f, 2f);
    }

    public float getSlipstreamReleaseLerp(float baseReleaseLerp) {
        float releaseLerp = baseReleaseLerp;
        for (int i = 0; i < effects.size(); i++) {
            releaseLerp = effects.get(i).slipstreamReleaseLerp(releaseLerp);
        }
        return releaseLerp;
    }

    public float getFrontCollisionRecoilMultiplier() {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            multiplier *= amplifyDeviation(
                    effect.frontCollisionRecoilMultiplier(),
                    effectStrengthMultiplier(effect));
        }
        return RogueliteEffectMath.clamp(multiplier, 0f, 1f);
    }

    public float getFrontCollisionPushMultiplier() {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            multiplier *= amplifyDeviation(
                    effect.frontCollisionPushMultiplier(),
                    effectStrengthMultiplier(effect));
        }
        return multiplier;
    }

    public float getCarCollisionScale() {
        float areaMultiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            float effectAreaMultiplier = effect.carCollisionAreaMultiplier();
            if (effectAreaMultiplier > 1f) {
                areaMultiplier = Math.max(
                        areaMultiplier,
                        effectAreaMultiplier * effectStrengthMultiplier(effect));
            }
        }
        return (float) Math.sqrt(areaMultiplier);
    }

    public float getCarCollisionMassMultiplier() {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            float effectMultiplier = effect.carCollisionMassMultiplier();
            if (effectMultiplier > 1f) {
                multiplier = Math.max(
                        multiplier,
                        effectMultiplier * effectStrengthMultiplier(effect));
            }
        }
        return multiplier;
    }

    public float consumeForwardLaunchSpeedRatio() {
        float speedRatio = 0f;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            speedRatio += effect.consumeForwardLaunchSpeedRatio()
                    * effectStrengthMultiplier(effect);
        }
        return RogueliteEffectMath.clamp(speedRatio, 0f, 0.50f);
    }

    public boolean isDraftMagnetActive() {
        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i).isDraftMagnetActive()) {
                return true;
            }
        }
        return false;
    }

    public float getDraftMagnetRangeMultiplier() {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            multiplier = Math.max(
                    multiplier,
                    amplifyDeviation(
                            effect.draftMagnetRangeMultiplier(),
                            effectStrengthMultiplier(effect)));
        }
        return multiplier;
    }

    public float getDraftMagnetForceMultiplier() {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            multiplier = Math.max(
                    multiplier,
                    amplifyDeviation(
                            effect.draftMagnetForceMultiplier(),
                            effectStrengthMultiplier(effect)));
        }
        return multiplier;
    }

    public boolean isRamChargeActive() {
        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i).isRamChargeActive()) {
                return true;
            }
        }
        return false;
    }

    public void consumeRamCharge() {
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).consumeRamCharge();
        }
        refreshActiveCards();
    }

    public void onRacePositionImproved(int positionsGained, float slipstreamBoost) {
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).onRacePositionImproved(positionsGained, slipstreamBoost);
        }
        refreshActiveCards();
    }

    public void onCollision(float impactStrength) {
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).onCollision(impactStrength);
        }
        refreshActiveCards();
    }

    public boolean onHitBy(int vehicleId, float impactStrength) {
        return onHitBy(vehicleId, impactStrength, true);
    }

    public boolean onHitBy(int vehicleId, float impactStrength, boolean canArmRevenge) {
        if (!canArmRevenge) {
            return false;
        }
        boolean accepted = false;
        boolean resetAmplifier = false;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            boolean effectAccepted = effect.onHitBy(vehicleId, impactStrength);
            accepted |= effectAccepted;
            resetAmplifier |= effectAccepted && effect == amplifiedActiveRevengeEffect;
        }
        if (resetAmplifier) {
            amplifiedActiveRevengeEffect = null;
            notifyRevengeAmplifierFinished();
        }
        refreshActiveCards();
        return accepted;
    }

    public void onContactEnded(int vehicleId) {
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).onContactEnded(vehicleId);
        }
        refreshActiveCards();
    }

    public boolean isInvisible() {
        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i).isInvisible()) {
                return true;
            }
        }
        return false;
    }

    public boolean blocksHostileEffects() {
        return isInvisible();
    }

    public boolean blocksDebuffs() {
        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i).blocksDebuffs()) {
                return true;
            }
        }
        return false;
    }

    public boolean isCollisionFieldActive() {
        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i).carCollisionAreaMultiplier() > 1f) {
                return true;
            }
        }
        return false;
    }

    public boolean blocksRevengeCard(RogueliteCardId cardId) {
        return isCollisionFieldActive()
                && CollisionFieldPowerupSpec.blocksRevengeCard(cardId);
    }

    public boolean isRevengeStrikeBlockedBy(RogueliteCarUpgrades targetUpgrades) {
        return targetUpgrades != null
                && targetUpgrades.blocksRevengeCard(getRevengeCardId());
    }

    public boolean blocksOpponentAwareness() {
        return isCollisionFieldActive();
    }

    public void deferInvisibilityExpiration() {
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).deferInvisibilityExpiration();
        }
    }

    public int getRevengeTargetVehicleId() {
        for (int i = 0; i < effects.size(); i++) {
            int targetVehicleId = effects.get(i).revengeTargetVehicleId();
            if (targetVehicleId >= 0) {
                return targetVehicleId;
            }
        }
        return -1;
    }

    public boolean isRivalBuildLeechActive() {
        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i).suppressesOffenderBuildAndTransfersLapExperience()) {
                return true;
            }
        }
        return false;
    }

    public RogueliteCardId getActiveRivalBuildLeechCardId() {
        for (int i = 0; i < RIVAL_BUILD_LEECH_CARD_IDS.length; i++) {
            if (isCardEffectActive(RIVAL_BUILD_LEECH_CARD_IDS[i])) {
                return RIVAL_BUILD_LEECH_CARD_IDS[i];
            }
        }
        return null;
    }

    public float getRivalBuildLeechTargetAgeSeconds() {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (effect.suppressesOffenderBuildAndTransfersLapExperience()) {
                return effect.revengeTargetAgeSeconds();
            }
        }
        return Float.POSITIVE_INFINITY;
    }

    public void setRevengeSecondaryTargetVehicleId(int vehicleId) {
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).setRevengeSecondaryTargetVehicleId(vehicleId);
        }
    }

    public int getRevengeSecondaryTargetVehicleId() {
        for (int i = 0; i < effects.size(); i++) {
            int targetVehicleId = effects.get(i).revengeSecondaryTargetVehicleId();
            if (targetVehicleId >= 0) {
                return targetVehicleId;
            }
        }
        return -1;
    }

    public boolean cancelRevengeTarget(int vehicleId) {
        boolean cancelled = false;
        for (int i = 0; i < effects.size(); i++) {
            cancelled |= effects.get(i).cancelRevengeTarget(vehicleId);
        }
        if (cancelled) {
            refreshActiveCards();
        }
        return cancelled;
    }

    public boolean allowsOffRoadOffenderStrike() {
        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i).allowsOffRoadOffenderStrike()) {
                return true;
            }
        }
        return false;
    }

    public boolean isOffenderCurseArmed() {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (effect.isOffenderCurse() && effect.revengeTargetVehicleId() >= 0) {
                return true;
            }
        }
        return false;
    }

    public boolean expireOffenderStrikeIfConditionFailed(
            int targetVehicleId,
            boolean offenderAhead) {
        for (int i = 0; i < effects.size(); i++) {
            if (effects.get(i).expireOffenderStrikeIfConditionFailed(
                    targetVehicleId,
                    offenderAhead)) {
                refreshActiveCards();
                return true;
            }
        }
        return false;
    }

    public RogueliteRevengeStrike tryActivateOffenderHit(int targetVehicleId) {
        long activationSequence = revengeActivationSequence;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteRevengeStrike strike =
                    effects.get(i).tryActivateOffenderHit(targetVehicleId);
            if (strike != null) {
                strike = amplifyRevengeStrike(effects.get(i), strike);
                refreshActiveCards();
                recordRevengeActivationIfMissing(true, activationSequence);
                return strike;
            }
        }
        return null;
    }

    public RogueliteRevengeStrike tryActivateOffenderStrike(
            int targetVehicleId,
            float distance) {
        return tryActivateOffenderStrike(targetVehicleId, distance, true);
    }

    public RogueliteRevengeStrike tryActivateOffenderStrike(
            int targetVehicleId,
            float distance,
            boolean offenderAhead) {
        long activationSequence = revengeActivationSequence;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteRevengeStrike strike =
                    effects.get(i).tryActivateOffenderStrike(
                            targetVehicleId,
                            distance,
                            offenderAhead);
            if (strike != null) {
                strike = amplifyRevengeStrike(effects.get(i), strike);
                refreshActiveCards();
                recordRevengeActivationIfMissing(true, activationSequence);
                return strike;
            }
        }
        return null;
    }

    public boolean isFinalReckoningHuntActive() {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (effect instanceof FinalReckoningEffect && effect.isReady()) {
                return true;
            }
        }
        return false;
    }

    public RogueliteRevengeStrike tryActivateFinalReckoningRam(
            int rammerVehicleId,
            int targetVehicleId,
            float distance) {
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (effect instanceof FinalReckoningEffect) {
                return ((FinalReckoningEffect) effect).tryActivateHuntRam(
                        rammerVehicleId,
                        targetVehicleId,
                        distance);
            }
        }
        return null;
    }

    public void completeOffenderStrike(RogueliteCardId cardId) {
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).completeOffenderStrike(cardId);
        }
        refreshActiveCards();
    }

    private void refreshActiveCards() {
        synchronizeRevengeAmplifier();
        activeCardIds.clear();
        int highestPriority = 0;
        for (int i = 0; i < effects.size(); i++) {
            highestPriority = Math.max(highestPriority, effects.get(i).activeDisplayPriority());
        }
        for (int priority = highestPriority; priority >= 0; priority--) {
            for (int i = 0; i < effects.size(); i++) {
                RogueliteUpgradeEffect effect = effects.get(i);
                if (!isBuildEffectSuppressed(effect)
                        && effect.activeDisplayPriority() == priority
                        && effect.isActive()) {
                    RogueliteCardId displayCardId = effect.activeDisplayCardId();
                    activeCardIds.add(displayCardId);
                    if (displayCardId != effect.getCardId()) {
                        activeCardIds.add(effect.getCardId());
                    }
                }
            }
        }
        RogueliteCardId currentRevengeCardId = getActiveCardId(RogueliteSlotType.REVENGE);
        if (currentRevengeCardId != null
                && currentRevengeCardId != activeRevengeCardId) {
            revengeActivationSequence++;
        }
        activeRevengeCardId = currentRevengeCardId;
    }

    public float getRevengeEffectMultiplier() {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            multiplier = Math.max(
                    multiplier,
                    CardAmplifierChain.combine(
                            effect.revengeEffectMultiplier(),
                            effectStrengthMultiplier(effect)));
        }
        return multiplier;
    }

    private static RogueliteSlotType slotType(RogueliteUpgradeEffect effect) {
        return RogueliteCardCatalog.get(effect.getCardId()).getSlotType();
    }

    private static boolean isAmplifiablePowerup(RogueliteUpgradeEffect effect) {
        return slotType(effect) == RogueliteSlotType.POWERUP
                && !isSetScopedBonusEffect(effect);
    }

    private float effectStrengthMultiplier(RogueliteUpgradeEffect effect) {
        if (isBuildEffectSuppressed(effect)) {
            return 0f;
        }
        if (isSetScopedBonusEffect(effect)) {
            return 1f;
        }
        RogueliteSlotType slotType = slotType(effect);
        if (slotType == RogueliteSlotType.TECHNIQUE) {
            return effectiveTechniqueEffectMultiplier();
        }
        if (slotType == RogueliteSlotType.POWERUP) {
            return effectivePowerupEffectMultiplier();
        }
        return 1f;
    }

    private boolean isBuildEffectSuppressed(RogueliteUpgradeEffect effect) {
        if (!buildCardsSuppressed) {
            return false;
        }
        RogueliteSlotType slotType = slotType(effect);
        return slotType == RogueliteSlotType.TUNING
                || slotType == RogueliteSlotType.TECHNIQUE;
    }

    private static boolean matchesSetBonusEffect(
            RogueliteSetDefinition setBonus,
            RogueliteUpgradeEffect effect) {
        if (effect == null) {
            return false;
        }
        if (!setBonus.usesSetScopedBonusEffect()) {
            return !isSetScopedBonusEffect(effect);
        }
        if (setBonus.getId() == RogueliteSetId.IRON_GIANT) {
            return effect instanceof IronGiantSetEffect;
        }
        if (setBonus.getId() == RogueliteSetId.CHAOS_CIRCUIT) {
            return effect instanceof ChaosCircuitSetEffect;
        }
        return effect instanceof ApexAscensionSetEffect;
    }

    private static RogueliteUpgradeEffect createSetBonusEffect(
            RogueliteSetDefinition setBonus,
            float powerupCycleOffset) {
        if (!setBonus.usesSetScopedBonusEffect()) {
            return RogueliteEffectFactory.create(
                    setBonus.getBonusCardId(),
                    powerupCycleOffset);
        }
        if (setBonus.getId() == RogueliteSetId.IRON_GIANT) {
            return new IronGiantSetEffect();
        }
        if (setBonus.getId() == RogueliteSetId.CHAOS_CIRCUIT) {
            return new ChaosCircuitSetEffect();
        }
        return new ApexAscensionSetEffect();
    }

    private static boolean isSetScopedBonusEffect(RogueliteUpgradeEffect effect) {
        return effect instanceof ApexAscensionSetEffect
                || effect instanceof IronGiantSetEffect
                || effect instanceof ChaosCircuitSetEffect;
    }

    private float effectiveTechniqueEffectMultiplier() {
        return buildCardsSuppressed ? 1f : techniqueEffectMultiplier;
    }

    private float effectivePowerupEffectMultiplier() {
        return buildCardsSuppressed ? 1f : powerupEffectMultiplier;
    }

    private float effectivePowerupCooldownRateMultiplier() {
        return buildCardsSuppressed ? 1f : powerupCooldownRateMultiplier;
    }

    private RogueliteRevengeStrike amplifyRevengeStrike(
            RogueliteUpgradeEffect revengeEffect,
            RogueliteRevengeStrike strike) {
        float multiplier = getRevengeEffectMultiplier();
        if (multiplier <= 1f) {
            return strike;
        }
        revengeEffect.amplifyActiveRevenge(multiplier);
        RogueliteRevengeStrike amplifiedStrike = strike.amplified(multiplier);
        notifyRevengeAmplifierActivated(
                Math.max(
                        revengeEffect.activeTimeRemainingSeconds(),
                        amplifiedStrike.getDurationSeconds()));
        if (revengeEffect.isActive()) {
            amplifiedActiveRevengeEffect = revengeEffect;
        } else {
            notifyRevengeAmplifierFinished();
        }
        return amplifiedStrike;
    }

    private void synchronizeRevengeAmplifier() {
        if (amplifiedActiveRevengeEffect != null
                && !amplifiedActiveRevengeEffect.isActive()) {
            amplifiedActiveRevengeEffect = null;
            notifyRevengeAmplifierFinished();
        }

        RogueliteUpgradeEffect activeRevengeEffect = null;
        for (int i = 0; i < effects.size(); i++) {
            RogueliteUpgradeEffect effect = effects.get(i);
            if (RogueliteCardCatalog.get(effect.getCardId()).getSlotType()
                            == RogueliteSlotType.REVENGE
                    && effect.isActive()) {
                activeRevengeEffect = effect;
                break;
            }
        }
        if (activeRevengeEffect == null
                || activeRevengeEffect == amplifiedActiveRevengeEffect) {
            return;
        }

        float multiplier = getRevengeEffectMultiplier();
        if (multiplier <= 1f) {
            return;
        }
        activeRevengeEffect.amplifyActiveRevenge(multiplier);
        amplifiedActiveRevengeEffect = activeRevengeEffect;
        notifyRevengeAmplifierActivated(
                activeRevengeEffect.activeTimeRemainingSeconds());
    }

    private void notifyRevengeAmplifierActivated(float durationSeconds) {
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).onRevengeActivated(durationSeconds);
        }
    }

    private void notifyRevengeAmplifierFinished() {
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).onRevengeFinished();
        }
    }

    private void recordRevengeActivationIfMissing(
            boolean activated,
            long previousSequence) {
        if (activated && revengeActivationSequence == previousSequence) {
            revengeActivationSequence++;
        }
    }
}
