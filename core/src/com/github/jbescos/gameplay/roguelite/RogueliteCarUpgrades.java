package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class RogueliteCarUpgrades {
    private final List<RogueliteUpgradeEffect> effects =
            new ArrayList<RogueliteUpgradeEffect>();
    private final List<RogueliteCardId> activeCardIds =
            new ArrayList<RogueliteCardId>();
    private final List<RogueliteCardId> readOnlyActiveCardIds =
            Collections.unmodifiableList(activeCardIds);
    private final RogueliteDrivingFrame frame = new RogueliteDrivingFrame();
    private float timedEffectDecay = 1f;
    private boolean overtakeInjectorEnabled;

    public void configure(RogueliteCardInventory inventory) {
        effects.clear();
        activeCardIds.clear();
        frame.clear();
        timedEffectDecay = 1f;
        overtakeInjectorEnabled = false;
        if (inventory == null || inventory.getSelectionCount() == 0) {
            return;
        }

        List<RogueliteCardDefinition> cards = inventory.getOwnedCards();
        for (int i = 0; i < cards.size(); i++) {
            RogueliteCardDefinition card = cards.get(i);
            RogueliteUpgradeEffect effect =
                    RogueliteEffectFactory.create(
                            card.getId(),
                            inventory.getLevel(card.getId()),
                            inventory);
            effects.add(effect);
            timedEffectDecay = Math.min(timedEffectDecay, effect.timedEffectDecay());
            overtakeInjectorEnabled |= effect.tracksRacePosition();
        }
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
                safeRecoveryRouteGain);
        float timerDelta = delta * timedEffectDecay;
        for (int i = 0; i < effects.size(); i++) {
            effects.get(i).advance(delta, timerDelta, frame);
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
        float bonus = 0f;
        for (int i = 0; i < effects.size(); i++) {
            bonus += effects.get(i).accelerationBonus();
        }
        return RogueliteEffectMath.clamp(1f + bonus, 1f, 1.65f);
    }

    public float getMaxSpeedMultiplier() {
        float bonus = 0f;
        for (int i = 0; i < effects.size(); i++) {
            bonus += effects.get(i).maxSpeedBonus();
        }
        return RogueliteEffectMath.clamp(1f + bonus, 1f, 1.20f);
    }

    public float getDragMultiplier() {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            multiplier *= effects.get(i).dragMultiplier();
        }
        return multiplier;
    }

    public float getGripMultiplier(float slip) {
        float bonus = 0f;
        for (int i = 0; i < effects.size(); i++) {
            bonus += effects.get(i).gripBonus(slip);
        }
        return 1f + bonus;
    }

    public float getSteeringMultiplier(float slip) {
        float bonus = 0f;
        for (int i = 0; i < effects.size(); i++) {
            bonus += effects.get(i).steeringBonus(slip);
        }
        return 1f + bonus;
    }

    public float getSlipstreamRangeMultiplier() {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            multiplier *= effects.get(i).slipstreamRangeMultiplier();
        }
        return multiplier;
    }

    public float getSlipstreamStrengthMultiplier() {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            multiplier *= effects.get(i).slipstreamStrengthMultiplier();
        }
        return multiplier;
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
            multiplier *= effects.get(i).frontCollisionRecoilMultiplier();
        }
        return multiplier;
    }

    public float getFrontCollisionPushMultiplier() {
        float multiplier = 1f;
        for (int i = 0; i < effects.size(); i++) {
            multiplier *= effects.get(i).frontCollisionPushMultiplier();
        }
        return multiplier;
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

    private void refreshActiveCards() {
        activeCardIds.clear();
        int highestPriority = 0;
        for (int i = 0; i < effects.size(); i++) {
            highestPriority = Math.max(highestPriority, effects.get(i).activeDisplayPriority());
        }
        for (int priority = highestPriority; priority >= 0; priority--) {
            for (int i = 0; i < effects.size(); i++) {
                RogueliteUpgradeEffect effect = effects.get(i);
                if (effect.activeDisplayPriority() == priority && effect.isActive()) {
                    activeCardIds.add(effect.getCardId());
                }
            }
        }
    }
}
