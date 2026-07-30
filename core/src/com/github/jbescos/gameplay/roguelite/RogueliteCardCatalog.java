package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class RogueliteCardCatalog {
    private static final List<RogueliteCardDefinition> CARDS;
    private static final Map<RogueliteCardId, RogueliteCardDefinition> CARDS_BY_ID;

    static {
        List<RogueliteCardDefinition> cards = new ArrayList<RogueliteCardDefinition>();
        cards.add(card(
                RogueliteCardId.TURBOCHARGER,
                "Turbocharger",
                "Builds speed quickly and rewards sustained full throttle.",
                "+10% acceleration; redline adds speed",
                RogueliteCardId.AERODYNAMIC_KIT,
                1));
        cards.add(card(
                RogueliteCardId.AERODYNAMIC_KIT,
                "Aerodynamic Kit",
                "Reduces drag and preserves temporary speed effects.",
                "9% less drag; boosts fade slower",
                RogueliteCardId.TURBOCHARGER,
                2));
        cards.add(card(
                RogueliteCardId.DRIFT_CAPACITOR,
                "Drift Capacitor",
                "Sustained on-road slip charges acceleration for the corner exit.",
                "Drift exit: up to +10% acceleration",
                RogueliteCardId.COUNTERSTEER_SERVO,
                3));
        cards.add(card(
                RogueliteCardId.COUNTERSTEER_SERVO,
                "Countersteer Servo",
                "Adds steering authority and traction while the car is sliding.",
                "+13% control during slides; extra exit traction",
                RogueliteCardId.DRIFT_CAPACITOR,
                1));
        cards.add(card(
                RogueliteCardId.DRAFT_RECEIVER,
                "Draft Receiver",
                "Expands and strengthens the useful wake behind other cars.",
                "+20% slipstream range and strength",
                RogueliteCardId.OVERTAKE_INJECTOR,
                3));
        cards.add(card(
                RogueliteCardId.OVERTAKE_INJECTOR,
                "Overtake Injector",
                "Improving race position triggers a short acceleration burst.",
                "Pass: +11% acceleration; drafting extends the burst",
                RogueliteCardId.DRAFT_RECEIVER,
                5));
        cards.add(card(
                RogueliteCardId.REINFORCED_BUMPER,
                "Reinforced Bumper",
                "Reduces your collision recoil while pushing rivals harder.",
                "-25% recoil; +18% push",
                RogueliteCardId.KINETIC_RECYCLER,
                2));
        cards.add(card(
                RogueliteCardId.KINETIC_RECYCLER,
                "Kinetic Recycler",
                "Restores acceleration briefly after a collision.",
                "Recover 10% impact energy",
                RogueliteCardId.REINFORCED_BUMPER,
                4));
        cards.add(card(
                RogueliteCardId.STORM_TIRES,
                "Storm Tires",
                "Retains more tire grip in rain and snow.",
                "Retain 50% of weather grip loss",
                RogueliteCardId.STORM_DYNAMO,
                2));
        cards.add(card(
                RogueliteCardId.STORM_DYNAMO,
                "Storm Dynamo",
                "Bad weather gradually charges additional acceleration.",
                "Weather charge: up to +8% acceleration; charge lingers",
                RogueliteCardId.STORM_TIRES,
                5));
        cards.add(card(
                RogueliteCardId.CLEAN_MOMENTUM,
                "Clean Momentum",
                "Clean on-road driving gradually raises maximum speed.",
                "Clean driving: up to +5% speed",
                RogueliteCardId.RECOVERY_DIFFERENTIAL,
                4));
        cards.add(card(
                RogueliteCardId.RECOVERY_DIFFERENTIAL,
                "Recovery Differential",
                "A safe return to the road grants temporary traction and acceleration.",
                "Safe re-entry: +12% traction and +6% power",
                RogueliteCardId.CLEAN_MOMENTUM,
                1));

        Map<RogueliteCardId, RogueliteCardDefinition> cardsById =
                new EnumMap<RogueliteCardId, RogueliteCardDefinition>(RogueliteCardId.class);
        for (int i = 0; i < cards.size(); i++) {
            RogueliteCardDefinition definition = cards.get(i);
            if (cardsById.put(definition.getId(), definition) != null) {
                throw new IllegalStateException(
                        "Duplicate roguelite card ID: " + definition.getId());
            }
        }
        for (int i = 0; i < cards.size(); i++) {
            RogueliteCardDefinition definition = cards.get(i);
            RogueliteCardDefinition synergy =
                    cardsById.get(definition.getSynergyCardId());
            if (synergy == null) {
                throw new IllegalStateException(
                        "Missing synergy card "
                                + definition.getSynergyCardId()
                                + " for "
                                + definition.getId());
            }
            if (synergy.getSynergyCardId() != definition.getId()) {
                throw new IllegalStateException(
                        "Non-reciprocal synergy between "
                                + definition.getId()
                                + " and "
                                + synergy.getId());
            }
        }
        CARDS = Collections.unmodifiableList(cards);
        CARDS_BY_ID = Collections.unmodifiableMap(cardsById);
    }

    private RogueliteCardCatalog() {
    }

    public static List<RogueliteCardDefinition> all() {
        return CARDS;
    }

    public static RogueliteCardDefinition get(RogueliteCardId id) {
        RogueliteCardDefinition definition = CARDS_BY_ID.get(id);
        if (definition == null) {
            throw new IllegalArgumentException("Unknown roguelite card: " + id);
        }
        return definition;
    }

    private static RogueliteCardDefinition card(
            RogueliteCardId id,
            String title,
            String description,
            String effectText,
            RogueliteCardId synergyCardId,
            int tier) {
        return new RogueliteCardDefinition(
                id,
                title,
                description,
                effectText,
                synergyCardId,
                tier);
    }
}
