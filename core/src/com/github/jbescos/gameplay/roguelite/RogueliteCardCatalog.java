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
                "More engine force, with a redline bonus at maximum level.",
                new String[] {
                        "+6% acceleration",
                        "+10% acceleration",
                        "+14% acceleration; redline adds speed"
                },
                RogueliteCardId.AERODYNAMIC_KIT));
        cards.add(card(
                RogueliteCardId.AERODYNAMIC_KIT,
                "Aerodynamic Kit",
                "Reduces drag and preserves temporary speed effects.",
                new String[] {
                        "5% less drag",
                        "9% less drag",
                        "13% less drag; boosts fade slower"
                },
                RogueliteCardId.TURBOCHARGER));
        cards.add(card(
                RogueliteCardId.DRIFT_CAPACITOR,
                "Drift Capacitor",
                "Sustained on-road slip charges acceleration for the corner exit.",
                new String[] {
                        "Drift exit: up to +6% acceleration",
                        "Drift exit: up to +10% acceleration",
                        "Drift exit: up to +14% acceleration"
                },
                RogueliteCardId.COUNTERSTEER_SERVO));
        cards.add(card(
                RogueliteCardId.COUNTERSTEER_SERVO,
                "Countersteer Servo",
                "Adds steering authority and traction while the car is sliding.",
                new String[] {
                        "+8% control during slides",
                        "+13% control during slides",
                        "+18% control; extra exit traction"
                },
                RogueliteCardId.DRIFT_CAPACITOR));
        cards.add(card(
                RogueliteCardId.DRAFT_RECEIVER,
                "Draft Receiver",
                "Expands and strengthens the useful wake behind other cars.",
                new String[] {
                        "+10% slipstream range and strength",
                        "+20% slipstream range and strength",
                        "+30% strength; wake lingers"
                },
                RogueliteCardId.OVERTAKE_INJECTOR));
        cards.add(card(
                RogueliteCardId.OVERTAKE_INJECTOR,
                "Overtake Injector",
                "Improving race position triggers a short acceleration burst.",
                new String[] {
                        "Pass: +7% acceleration for 1.0s",
                        "Pass: +11% acceleration for 1.3s",
                        "Pass: +15%; drafting doubles duration"
                },
                RogueliteCardId.DRAFT_RECEIVER));
        cards.add(card(
                RogueliteCardId.REINFORCED_BUMPER,
                "Reinforced Bumper",
                "Reduces your collision recoil while pushing rivals harder.",
                new String[] {
                        "-15% recoil; +10% push",
                        "-25% recoil; +18% push",
                        "-35% recoil; +26% push"
                },
                RogueliteCardId.KINETIC_RECYCLER));
        cards.add(card(
                RogueliteCardId.KINETIC_RECYCLER,
                "Kinetic Recycler",
                "Restores acceleration briefly after a collision.",
                new String[] {
                        "Recover 6% impact energy",
                        "Recover 10% impact energy",
                        "Recover 14%; bumper synergy adds more"
                },
                RogueliteCardId.REINFORCED_BUMPER));
        cards.add(card(
                RogueliteCardId.STORM_TIRES,
                "Storm Tires",
                "Retains more tire grip in rain and snow.",
                new String[] {
                        "Retain 30% of weather grip loss",
                        "Retain 50% of weather grip loss",
                        "Retain 70% of weather grip loss"
                },
                RogueliteCardId.STORM_DYNAMO));
        cards.add(card(
                RogueliteCardId.STORM_DYNAMO,
                "Storm Dynamo",
                "Bad weather gradually charges additional acceleration.",
                new String[] {
                        "Weather charge: up to +5% acceleration",
                        "Weather charge: up to +8% acceleration",
                        "Weather charge: up to +12%; lingers"
                },
                RogueliteCardId.STORM_TIRES));
        cards.add(card(
                RogueliteCardId.CLEAN_MOMENTUM,
                "Clean Momentum",
                "Clean on-road driving gradually raises maximum speed.",
                new String[] {
                        "Clean driving: up to +3% speed",
                        "Clean driving: up to +5% speed",
                        "Clean driving: up to +7%; resists impacts"
                },
                RogueliteCardId.RECOVERY_DIFFERENTIAL));
        cards.add(card(
                RogueliteCardId.RECOVERY_DIFFERENTIAL,
                "Recovery Differential",
                "A safe return to the road grants temporary traction and acceleration.",
                new String[] {
                        "Safe re-entry: +8% traction",
                        "Safe re-entry: +12% traction and power",
                        "Safe re-entry: +16%; stronger when slow"
                },
                RogueliteCardId.CLEAN_MOMENTUM));

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
            String[] effectTexts,
            RogueliteCardId synergyCardId) {
        return new RogueliteCardDefinition(
                id,
                title,
                description,
                effectTexts,
                synergyCardId);
    }
}
