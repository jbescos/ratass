package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Static four-card recipes shared by every run. */
public final class RogueliteSetCatalog {
    private static final Map<RogueliteSetId, RogueliteSetDefinition> BY_ID =
            new EnumMap<RogueliteSetId, RogueliteSetDefinition>(RogueliteSetId.class);
    private static final List<RogueliteSetDefinition> ALL;
    private static final List<RogueliteSetId> ALL_IDS;
    private static final List<RogueliteSetDefinition> TIER_THREE;
    private static final RogueliteSetDefinition TIER_FOUR;

    static {
        add(set(
                RogueliteSetId.VELOCITY_PACT, "Velocity Pact", 3,
                RogueliteCardId.CHAMPIONSHIP_TUNE,
                RogueliteCardId.STRAIGHT_MASTER,
                RogueliteCardId.HYPERDRIVE,
                RogueliteCardId.CROWN_ENGINE,
                RogueliteCardId.TITANIUM_SKELETON,
                false, 0));
        add(set(
                RogueliteSetId.APEX_BASTION, "Apex Bastion", 3,
                RogueliteCardId.GROUND_EFFECT,
                RogueliteCardId.CORNER_MASTER,
                RogueliteCardId.GRAVITY_WELL,
                RogueliteCardId.REPULSOR_SURGE,
                RogueliteCardId.TRACK_VACUUM,
                false, 1));
        add(set(
                RogueliteSetId.QUANTUM_PACK, "Quantum Pack", 3,
                RogueliteCardId.VELOCITY_SHELL,
                RogueliteCardId.TRAFFIC_DOMINANCE,
                RogueliteCardId.OVERDRIVE_COIL,
                RogueliteCardId.HUNTER_STORM,
                RogueliteCardId.TEMPORAL_DOMINION,
                false, 2));
        add(set(
                RogueliteSetId.IRON_GIANT, "Iron Giant", 3,
                RogueliteCardId.GRAPHENE_CHASSIS,
                RogueliteCardId.AGILITY_MASTER,
                RogueliteCardId.COLOSSUS_FIELD,
                RogueliteCardId.VOID_ANCHOR,
                RogueliteCardId.TORQUE_VECTORING,
                true, 3,
                "Immune to all debuffs"));
        add(set(
                RogueliteSetId.PHANTOM_ORDER, "Phantom Order", 3,
                RogueliteCardId.ACTIVE_AERO_SHELL,
                RogueliteCardId.APEX_MASTER,
                RogueliteCardId.VOID_CLOAK,
                RogueliteCardId.TRIAD_COUP,
                RogueliteCardId.LAST_PLACE_FURY,
                false, 4));
        add(set(
                RogueliteSetId.CHAOS_CIRCUIT, "Chaos Circuit", 3,
                RogueliteCardId.HYPERCAR_CORE,
                RogueliteCardId.DRAFT_MASTER,
                RogueliteCardId.WILDCARD_CORE,
                RogueliteCardId.FATES_REVENGE,
                RogueliteCardId.DRIFT_MASTER,
                true, 5,
                "Technique always active"));
        add(set(
                RogueliteSetId.PLUNDER_SYNDICATE, "Plunder Syndicate", 3,
                RogueliteCardId.WING_CAR,
                RogueliteCardId.LAP_DOUBLER,
                RogueliteCardId.TECHNIQUE_LINK,
                RogueliteCardId.APEX_PLUNDER,
                RogueliteCardId.TRAFFIC_DOMINANCE,
                false, 6));
        add(set(
                RogueliteSetId.DOOM_RALLY, "Doom Rally", 3,
                RogueliteCardId.FEATHERWEIGHT_GROUND_EFFECT,
                RogueliteCardId.RALLY_MASTER,
                RogueliteCardId.TEMPORAL_DOMINION,
                RogueliteCardId.TOTAL_BLACKOUT,
                RogueliteCardId.FATES_REVENGE,
                false, 7));
        add(set(
                RogueliteSetId.CHRONO_APOCALYPSE, "Apex Ascension", 4,
                RogueliteCardId.TECHNIQUE_SINGULARITY,
                RogueliteCardId.POWERUP_NEXUS,
                RogueliteCardId.NEMESIS_ENGINE,
                RogueliteCardId.FINAL_RECKONING,
                RogueliteCardId.TEMPORAL_DOMINION,
                true, 8,
                "Power +80%\nGrip +20%\nAero +100%\nMass -30%"));

        List<RogueliteSetDefinition> tierThree = new ArrayList<RogueliteSetDefinition>();
        List<RogueliteSetDefinition> all = new ArrayList<RogueliteSetDefinition>(BY_ID.size());
        List<RogueliteSetId> allIds = new ArrayList<RogueliteSetId>(BY_ID.size());
        RogueliteSetDefinition tierFour = null;
        for (RogueliteSetDefinition definition : BY_ID.values()) {
            all.add(definition);
            allIds.add(definition.getId());
            if (definition.getTier() == 3) {
                tierThree.add(definition);
            } else if (definition.getTier() == 4) {
                tierFour = definition;
            }
        }
        ALL = Collections.unmodifiableList(all);
        ALL_IDS = Collections.unmodifiableList(allIds);
        TIER_THREE = Collections.unmodifiableList(tierThree);
        TIER_FOUR = tierFour;
    }

    private RogueliteSetCatalog() {
    }

    public static RogueliteSetDefinition get(RogueliteSetId id) {
        return id == null ? null : BY_ID.get(id);
    }

    public static List<RogueliteSetId> allSetIds() {
        return ALL_IDS;
    }

    public static List<RogueliteSetDefinition> allSets() {
        return ALL;
    }

    public static List<RogueliteSetDefinition> tierThreeSets() {
        return TIER_THREE;
    }

    public static RogueliteSetDefinition tierFourSet() {
        return TIER_FOUR;
    }

    public static RogueliteSetDefinition completedSet(
            RogueliteLoadout loadout,
            Iterable<RogueliteSetId> enabledSetIds) {
        if (loadout == null || enabledSetIds == null) {
            return null;
        }
        for (RogueliteSetId id : enabledSetIds) {
            RogueliteSetDefinition definition = get(id);
            if (definition != null && definition.isCompletedBy(loadout)) {
                return definition;
            }
        }
        return null;
    }

    public static RogueliteSetDefinition componentSet(
            RogueliteCardId cardId,
            Iterable<RogueliteSetId> enabledSetIds) {
        if (cardId == null || enabledSetIds == null) {
            return null;
        }
        for (RogueliteSetId id : enabledSetIds) {
            RogueliteSetDefinition definition = get(id);
            if (definition != null && definition.contains(cardId)) {
                return definition;
            }
        }
        return null;
    }

    public static float selectionGain(
            RogueliteLoadout loadout,
            RogueliteCardId candidate,
            Iterable<RogueliteSetId> enabledSetIds) {
        if (loadout == null || candidate == null || enabledSetIds == null) {
            return 0f;
        }
        RogueliteCardDefinition card = RogueliteCardCatalog.get(candidate);
        float bestGain = 0f;
        for (RogueliteSetId id : enabledSetIds) {
            RogueliteSetDefinition set = get(id);
            if (set == null || set.getRequiredCard(card.getSlotType()) != candidate) {
                continue;
            }
            int before = set.matchingCardCount(loadout);
            RogueliteCardId replaced = loadout.get(card.getSlotType());
            int after = before + (replaced == candidate ? 0 : 1);
            if (replaced != null && set.contains(replaced)) {
                after--;
            }
            float gain = Math.max(0, after - before) * 0.20f;
            if (after == RogueliteLoadout.MODIFICATION_SLOT_COUNT) {
                gain += set.getTier() == 4 ? 2.0f : 1.0f;
            } else if (after == RogueliteLoadout.MODIFICATION_SLOT_COUNT - 1) {
                gain += 0.35f;
            }
            bestGain = Math.max(bestGain, gain);
        }
        return bestGain;
    }

    public static int matchingCardCountAfter(
            RogueliteLoadout loadout,
            RogueliteCardId candidate,
            RogueliteSetDefinition set) {
        if (loadout == null || set == null) {
            return 0;
        }
        int count = set.matchingCardCount(loadout);
        if (candidate == null) {
            return count;
        }
        RogueliteSlotType slot = RogueliteCardCatalog.get(candidate).getSlotType();
        RogueliteCardId required = set.getRequiredCard(slot);
        RogueliteCardId replaced = loadout.get(slot);
        if (replaced == required) {
            count--;
        }
        if (candidate == required) {
            count++;
        }
        return count;
    }

    public static int bestMatchingCardCount(
            RogueliteLoadout loadout,
            Iterable<RogueliteSetId> enabledSetIds) {
        int best = 0;
        if (loadout == null || enabledSetIds == null) {
            return best;
        }
        for (RogueliteSetId id : enabledSetIds) {
            RogueliteSetDefinition set = get(id);
            if (set != null) {
                best = Math.max(best, set.matchingCardCount(loadout));
            }
        }
        return best;
    }

    public static int bestMatchingCardCountAfter(
            RogueliteLoadout loadout,
            RogueliteCardId candidate,
            Iterable<RogueliteSetId> enabledSetIds) {
        int best = 0;
        if (loadout == null || enabledSetIds == null) {
            return best;
        }
        for (RogueliteSetId id : enabledSetIds) {
            RogueliteSetDefinition set = get(id);
            if (set != null) {
                best = Math.max(best, matchingCardCountAfter(loadout, candidate, set));
            }
        }
        return best;
    }

    public static int selectionProgress(
            RogueliteLoadout loadout,
            RogueliteCardId candidate,
            Iterable<RogueliteSetId> enabledSetIds) {
        if (loadout == null || candidate == null || enabledSetIds == null) {
            return 0;
        }
        RogueliteSlotType slot = RogueliteCardCatalog.get(candidate).getSlotType();
        int best = 0;
        for (RogueliteSetId id : enabledSetIds) {
            RogueliteSetDefinition set = get(id);
            if (set == null || set.getRequiredCard(slot) != candidate) {
                continue;
            }
            best = Math.max(
                    best,
                    matchingCardCountAfter(loadout, candidate, set)
                            - set.matchingCardCount(loadout));
        }
        return best;
    }

    public static int selectionDepth(
            RogueliteLoadout loadout,
            RogueliteCardId candidate,
            Iterable<RogueliteSetId> enabledSetIds) {
        if (loadout == null || candidate == null || enabledSetIds == null) {
            return 0;
        }
        RogueliteSlotType slot = RogueliteCardCatalog.get(candidate).getSlotType();
        int best = 0;
        for (RogueliteSetId id : enabledSetIds) {
            RogueliteSetDefinition set = get(id);
            if (set == null || set.getRequiredCard(slot) != candidate) {
                continue;
            }
            int before = set.matchingCardCount(loadout);
            int after = matchingCardCountAfter(loadout, candidate, set);
            if (after > before) {
                best = Math.max(best, after);
            }
        }
        return best;
    }

    public static boolean completesSetAfter(
            RogueliteLoadout loadout,
            RogueliteCardId candidate,
            Iterable<RogueliteSetId> enabledSetIds) {
        if (loadout == null || candidate == null || enabledSetIds == null) {
            return false;
        }
        for (RogueliteSetId id : enabledSetIds) {
            RogueliteSetDefinition set = get(id);
            if (set != null
                    && matchingCardCountAfter(loadout, candidate, set)
                            == RogueliteLoadout.MODIFICATION_SLOT_COUNT) {
                return true;
            }
        }
        return false;
    }

    public static boolean breaksCompletedSet(
            RogueliteLoadout loadout,
            RogueliteCardId candidate,
            Iterable<RogueliteSetId> enabledSetIds) {
        RogueliteSetDefinition completed = completedSet(loadout, enabledSetIds);
        return completed != null
                && matchingCardCountAfter(loadout, candidate, completed)
                        < RogueliteLoadout.MODIFICATION_SLOT_COUNT;
    }

    public static int selectionRegression(
            RogueliteLoadout loadout,
            RogueliteCardId candidate,
            Iterable<RogueliteSetId> enabledSetIds) {
        int before = bestMatchingCardCount(loadout, enabledSetIds);
        int after = bestMatchingCardCountAfter(loadout, candidate, enabledSetIds);
        return Math.max(0, before - after);
    }

    private static RogueliteSetDefinition set(
            RogueliteSetId id,
            String displayName,
            int tier,
            RogueliteCardId tuning,
            RogueliteCardId technique,
            RogueliteCardId powerup,
            RogueliteCardId revenge,
            RogueliteCardId bonus,
            boolean setScopedBonusEffect,
            int iconIndex) {
        return set(
                id, displayName, tier, tuning, technique, powerup, revenge,
                bonus, setScopedBonusEffect, iconIndex, null);
    }

    private static RogueliteSetDefinition set(
            RogueliteSetId id,
            String displayName,
            int tier,
            RogueliteCardId tuning,
            RogueliteCardId technique,
            RogueliteCardId powerup,
            RogueliteCardId revenge,
            RogueliteCardId bonus,
            boolean setScopedBonusEffect,
            int iconIndex,
            String bonusEffectText) {
        return new RogueliteSetDefinition(
                id, displayName, tier, tuning, technique, powerup, revenge,
                bonus, bonusEffectText, setScopedBonusEffect, iconIndex);
    }

    private static void add(RogueliteSetDefinition definition) {
        if (BY_ID.put(definition.getId(), definition) != null) {
            throw new IllegalStateException("Duplicate roguelite set: " + definition.getId());
        }
    }
}
