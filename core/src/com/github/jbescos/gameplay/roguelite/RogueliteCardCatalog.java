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
                RogueliteCardId.CLUB_TUNE,
                "Club Tune",
                "A dependable first race setup with more power, speed and tire grip.",
                "Power +5% | Speed +2% | Grip +3%",
                1,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.SPORT_TUNE,
                "Lightweight Tune",
                "A stripped chassis accelerates and changes direction quickly, but gives up tire stability.",
                "Mass -2% | Power +8% | Speed +3%\nSteering +4% | Grip +3%",
                1,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.AERO_TRIM,
                "Streamline Kit",
                "An aerodynamically efficient body carries speed on open road at the cost of cornering confidence.",
                "Aero +9% | Speed +7% | Power +7% | Grip +3%",
                1,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.SHORT_GEARING,
                "Short-Ratio Gearbox",
                "Close gearing launches hard between corners but reaches its limit earlier on long straights.",
                "Power +8% | Grip +4% | Steering +3% | Speed -2%",
                1,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.CARBON_PANELS,
                "Carbon Panels",
                "Light body panels improve acceleration, response and aero efficiency.",
                "Mass -6% | Power +8% | Speed +3%\nAero +4% | Steering +3% | Grip +3%",
                1,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.RACE_TUNE,
                "Race Tune",
                "Sharper race hardware combines sustained power with high-speed stability.",
                "Power +18% | Speed +8% | Grip +11%\nMass +3% | Aero +4%",
                2,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.HEAVYWEIGHT_TUNE,
                "Ballast Powertrain",
                "A reinforced, heavier car carries extra power and grip so contact no longer ruins its pace.",
                "Mass +10% | Power +20% | Speed +8%\nGrip +10% | Stronger hits",
                2,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.LOW_DRAG_BODY,
                "Le Mans Body",
                "Exceptional aero efficiency rewards committed high-speed driving without sacrificing basic stability.",
                "Aero +14% | Speed +11% | Power +21%\nGrip +10% | Mass +4%",
                2,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.DRIFT_DIFFERENTIAL,
                "Drift Differential",
                "An aggressive differential makes sustained rotation easy and straightens with strong drive.",
                "Power +24% | Speed +11% | Grip +13%\nMass +6% | Freer rear while drifting",
                2,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.CARBON_MONOCOQUE,
                "Carbon Monocoque",
                "A rigid carbon cell cuts inertia while sharpening power delivery, aero and response.",
                "Mass -8% | Power +20% | Speed +7%\nAero +8% | Grip +10% | Steering +6%",
                2,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.CHAMPIONSHIP_TUNE,
                "Aero Prototype",
                "An aerodynamically efficient body and high-speed downforce turn open road into a decisive advantage.",
                "Aero +16% | Power +39% | Speed +17%\nGrip +18% | Mass +7%",
                3,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.GROUND_EFFECT,
                "Ground Effect",
                "A sealed floor creates exceptional cornering force while adding weight and aerodynamic resistance.",
                "Grip +18% | Steering +5% | Power +40%\nSpeed +17% | Mass +9%",
                3,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.VELOCITY_SHELL,
                "Velocity Shell",
                "A radical long-tail body maximizes aero efficiency for huge straight-line pace with modest cornering support.",
                "Aero +39% | Speed +28% | Power +62%\nGrip +17% | Mass +8%",
                3,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.TORQUE_VECTORING,
                "Torque Vectoring",
                "Active torque distribution rotates the car decisively through corners while preserving competitive speed and contact strength.",
                "Power +44% | Speed +18% | Grip +17%\nSteering +9% | Aero +11% | Mass +4%",
                3,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.GRAPHENE_CHASSIS,
                "Graphene Chassis",
                "An ultralight structure delivers extreme acceleration, aero efficiency and precise handling.",
                "Mass -16% | Power +48% | Speed +22%\nAero +25% | Grip +19% | Steering +12%",
                3,
                RogueliteSlotType.TUNING));

        cards.add(card(
                RogueliteCardId.CORNER_EXIT,
                "Corner Exit",
                "Leaving an on-road corner under power creates a short launch onto the next section.",
                "Corner exit: power +12% for 1.4s",
                1,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.DRAFT_HUNTER,
                "Draft Hunter",
                "Finds a rival's wake sooner and turns close following into useful speed.",
                "Draft: reach and boost +25%",
                1,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.CLEAN_MOMENTUM,
                "Clean Momentum",
                "Continuous on-road driving builds a speed advantage that is lost by leaving the circuit.",
                "Stay on-road: Speed +7% | Aero +3%",
                1,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.RECOVERY_LAUNCH,
                "Recovery Launch",
                "A quick legitimate return to the road restores traction and accelerates back into the race.",
                "Safe re-entry: power +14% | Grip +18%",
                2,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.DRIFT_SLINGSHOT,
                "Drift Slingshot",
                "Sustained on-road slip stores energy and releases it when the car straightens.",
                "Drift exit: power up to +18%",
                2,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.SLIPSTREAM_SLINGSHOT,
                "Slipstream Slingshot",
                "Charges in another car's wake and launches when you pull out to pass.",
                "Leave draft: power +20% | Speed +7%",
                2,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.OVERTAKE_SURGE,
                "Overtake Surge",
                "Every gained race position immediately provides power to complete the move.",
                "Overtake: power +24% for 2s",
                2,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.APEX_SLINGSHOT,
                "Apex Slingshot",
                "Loads energy through a fast on-road corner and releases it as the road straightens.",
                "Fast corner exit: power up to +23%",
                3,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.PERFECT_LAP,
                "Perfect Lap",
                "Clean speed, accurate corner exits and uninterrupted momentum compound throughout the lap.",
                "Clean lap: power +15% | Speed +10% | Grip +12%",
                3,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.RACECRAFT_MASTERY,
                "Racecraft Mastery",
                "Drafting, corner exits and overtakes each trigger a powerful race-winning response.",
                "Race events: power, speed and grip +28% bursts",
                3,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.UNDERDOG_INSTINCT,
                "Underdog Instinct",
                "Reads the field and gains performance as the car falls back, reaching full strength in last place.",
                "Lower position: power, speed, grip and aero up to +10%",
                1,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.COMEBACK_DRIVE,
                "Comeback Drive",
                "Reads the field and gains performance as the car falls back, reaching full strength in last place.",
                "Lower position: power, speed, grip and aero up to +15%",
                2,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.LAST_PLACE_FURY,
                "Last Place Fury",
                "Reads the field and gains performance as the car falls back, reaching full strength in last place.",
                "Lower position: power, speed, grip and aero up to +20%",
                3,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.CLOSE_QUARTERS,
                "Close Quarters",
                "Raises the car's pace whenever a rival is nearby, helping attacks and defensive runs.",
                "Nearby rival: power, speed, grip and aero +5%",
                1,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.PACK_RACER,
                "Pack Racer",
                "Raises the car's pace whenever a rival is nearby, helping attacks and defensive runs.",
                "Nearby rival: power, speed, grip and aero +10%",
                2,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.TRAFFIC_DOMINANCE,
                "Traffic Dominance",
                "Raises the car's pace whenever a rival is nearby, helping attacks and defensive runs.",
                "Nearby rival: power, speed, grip and aero +15%",
                3,
                RogueliteSlotType.TECHNIQUE));

        cards.add(powerup(
                RogueliteCardId.NITRO_PULSE,
                "Nitro Pulse",
                "Kicks the car forward when open road invites a nitro burst.",
                "Clear straight: launch | Power +20% | Speed +6%\nCooldown: 9s",
                1,
                RogueliteAbilityVisualStyle.NITRO));
        cards.add(powerup(
                RogueliteCardId.MIRROR_DUO,
                "Quantum Duo",
                "A quantum split creates a second physical car that races beside the original.",
                "Nearby rival on straight: 2 cars for 5s\nCooldown: 10s",
                1,
                RogueliteAbilityVisualStyle.MIRROR));
        cards.add(powerup(
                RogueliteCardId.GRIP_FAN,
                "Grip Fan",
                "A glowing underbody fan pins the car down as a demanding corner arrives.",
                "Corner ahead: grip +18% | Steering +12%\nCooldown: 8.5s",
                1,
                RogueliteAbilityVisualStyle.GRIP));
        cards.add(powerup(
                RogueliteCardId.GHOST_CLOAK,
                "Ghost Cloak",
                "The car phases out when traffic is nearby, becoming invisible and intangible to rivals.",
                "Nearby rival: invisible 3s | Rivals pass through you\nCooldown after effect: 10s",
                1,
                RogueliteAbilityVisualStyle.CLOAK));
        cards.add(powerup(
                RogueliteCardId.LUCKY_SPARK,
                "Lucky Spark",
                "Prepares a random Tier 1 Powerup and copies its real trigger, effect and cooldown.",
                "Each activation: random Tier 1 Powerup",
                1,
                RogueliteAbilityVisualStyle.NITRO));
        cards.add(revenge(
                RogueliteCardId.RAM_REACTOR,
                "Impact Reversal",
                "A qualified rival hit arms a counter that throws away the next car to strike you.",
                "Rival hit -> next hit: reflect impact",
                1,
                RogueliteAbilityVisualStyle.RAM));
        cards.add(revenge(
                RogueliteCardId.DRAFT_MAGNET,
                "Draft Magnet",
                "A qualified rival hit arms a short pulsing field that forces nearby cars toward the outside.",
                "Rival hit -> nearby rival: outward field 2s",
                1,
                RogueliteAbilityVisualStyle.DRAFT));
        cards.add(revenge(
                RogueliteCardId.RECOVERY_BEACON,
                "Position Hijack",
                "A qualified hit marks its offender. After charging, it exchanges positions only while they are ahead.",
                "Rival hit -> after 3s, swap with offender if ahead",
                2,
                RogueliteAbilityVisualStyle.SHIELD));
        cards.add(revenge(
                RogueliteCardId.DRAFT_VENDETTA,
                "Redline Hex",
                "A qualified hit curses its offender's throttle, forcing them to commit through whatever comes next.",
                "Rival hit -> offender: full throttle for 5s",
                1,
                RogueliteAbilityVisualStyle.DRAFT));
        cards.add(powerup(
                RogueliteCardId.PHASE_SHIELD,
                "Phase Shield",
                "An energy shell forms when traffic closes in, absorbing frontal recoil.",
                "Traffic or corner: shield | Power +22% | Grip +20%\nCooldown: 6.8s",
                2,
                RogueliteAbilityVisualStyle.SHIELD));
        cards.add(powerup(
                RogueliteCardId.ROCKET_EXHAUST,
                "Rocket Exhaust",
                "Twin exhaust rockets ignite on a clear straight for a forceful launch.",
                "Clear straight: launch | Power +32% | Speed +11%\nCooldown: 6.8s",
                2,
                RogueliteAbilityVisualStyle.NITRO));
        cards.add(powerup(
                RogueliteCardId.MIRROR_TRIO,
                "Quantum Trio",
                "A quantum split creates three physical cars spread across the track.",
                "Nearby rival on straight: 3 cars for 5s\nCooldown: 10s",
                2,
                RogueliteAbilityVisualStyle.MIRROR));
        cards.add(powerup(
                RogueliteCardId.PHANTOM_CLOAK,
                "Phantom Cloak",
                "An improved phase field hides the car and prevents rivals from making contact for longer.",
                "Nearby rival: invisible 4s | Rivals pass through you\nCooldown after effect: 10s",
                2,
                RogueliteAbilityVisualStyle.CLOAK));
        cards.add(powerup(
                RogueliteCardId.CHAOS_RELAY,
                "Chaos Relay",
                "Prepares a random Tier 2 Powerup and copies its real trigger, effect and cooldown.",
                "Each activation: random Tier 2 Powerup",
                2,
                RogueliteAbilityVisualStyle.MIRROR));
        cards.add(powerup(
                RogueliteCardId.GRAVITY_WELL,
                "Gravity Well",
                "A visible ground field forms in corners or close traffic for extreme stability.",
                "Corner or traffic: power +40% | Speed +16% | Grip +22%\nCooldown: 5.8s",
                3,
                RogueliteAbilityVisualStyle.GRIP));
        cards.add(powerup(
                RogueliteCardId.OVERDRIVE_COIL,
                "Quantum Quartet",
                "A quantum split creates four physical cars spread across the track.",
                "Nearby rival on straight: 4 cars for 5s\nCooldown: 10s",
                3,
                RogueliteAbilityVisualStyle.MIRROR));
        cards.add(powerup(
                RogueliteCardId.HYPERDRIVE,
                "Hyperdrive",
                "Open road triggers an extreme launch and turns the car into a visible streak.",
                "Clear straight: launch | Power +38% | Speed +15% | Grip +16%\nCooldown: 6.5s",
                3,
                RogueliteAbilityVisualStyle.NITRO));
        cards.add(powerup(
                RogueliteCardId.VOID_CLOAK,
                "Void Cloak",
                "A championship phase system removes the car from sight and contact for an extended attack window.",
                "Nearby rival: invisible 5s | Rivals pass through you\nCooldown after effect: 10s",
                3,
                RogueliteAbilityVisualStyle.CLOAK));
        cards.add(powerup(
                RogueliteCardId.WILDCARD_CORE,
                "Wildcard Core",
                "Prepares a random Tier 3 Powerup and copies its real trigger, effect and cooldown.",
                "Each activation: random Tier 3 Powerup",
                3,
                RogueliteAbilityVisualStyle.NITRO));
        cards.add(revenge(
                RogueliteCardId.CROWN_ENGINE,
                "Crown Breaker",
                "A rival hit marks its offender and empowers you until you strike them back.",
                "30s hunt: Power +55% | Speed +22%\nRecoil -75% | Push +70% | Ram on hit",
                3,
                RogueliteAbilityVisualStyle.RAM));
        cards.add(revenge(
                RogueliteCardId.PAYBACK_SHIELD,
                "Vendetta Hook",
                "A qualified hit marks its offender. After charging, the hook pulls you directly back toward them.",
                "Rival hit -> after 3s, pull to offender over 5s",
                1,
                RogueliteAbilityVisualStyle.SHIELD));
        cards.add(revenge(
                RogueliteCardId.REPULSOR_SURGE,
                "Repulsor Surge",
                "A qualified rival hit arms a wide high-energy field that clears space for your comeback.",
                "Rival hit -> nearby rival: wide outward field 2s",
                3,
                RogueliteAbilityVisualStyle.DRAFT));
        cards.add(revenge(
                RogueliteCardId.TAR_TETHER,
                "Tar Tether",
                "Throws a sticky tether at the rival who hit you and strips all tire traction.",
                "Rival hit -> offender: grip 0% for 2s",
                1,
                RogueliteAbilityVisualStyle.DRAFT));
        cards.add(revenge(
                RogueliteCardId.EMP_SNARE,
                "EMP Snare",
                "Launches a disruptive snare that forces the rival responsible for hitting you to brake without reversing.",
                "Rival hit -> offender: full brake for 2s",
                2,
                RogueliteAbilityVisualStyle.SHIELD));
        cards.add(revenge(
                RogueliteCardId.VOID_ANCHOR,
                "Void Anchor",
                "Hurls a heavy energy anchor that forces the rival responsible for hitting you to brake without reversing.",
                "Rival hit -> offender: full brake for 3s",
                3,
                RogueliteAbilityVisualStyle.RAM));
        cards.add(revenge(
                RogueliteCardId.SENSOR_JAMMER,
                "Blind Hex",
                "Curses the rival who hit you until that offender collides with another car.",
                "Offender: blind, +5% mass until collision",
                1,
                RogueliteAbilityVisualStyle.SHIELD));
        cards.add(revenge(
                RogueliteCardId.GRID_BLACKOUT,
                "Burden Hex",
                "Chains the rival who hit you to a heavier, blinded car until its next collision.",
                "Offender: blind, +20% mass until collision",
                2,
                RogueliteAbilityVisualStyle.SHIELD));
        cards.add(revenge(
                RogueliteCardId.TOTAL_BLACKOUT,
                "Doom Hex",
                "Crushes the rival who hit you with blindness, extreme weight, and reduced grip until its next collision.",
                "Offender: blind, +50% mass, -20% grip",
                3,
                RogueliteAbilityVisualStyle.SHIELD));
        cards.add(revenge(
                RogueliteCardId.LOADED_GRUDGE,
                "Loaded Grudge",
                "A rival hit executes a random Tier 1 Revenge card, then prepares a different retaliation.",
                "Hit taken: random Tier 1 Revenge",
                1,
                RogueliteAbilityVisualStyle.DRAFT));
        cards.add(revenge(
                RogueliteCardId.CHAOS_RETORT,
                "Chaos Retort",
                "A rival hit executes a random Tier 2 Revenge card, then prepares a different retaliation.",
                "Hit taken: random Tier 2 Revenge",
                2,
                RogueliteAbilityVisualStyle.SHIELD));
        cards.add(revenge(
                RogueliteCardId.FATES_REVENGE,
                "Fate's Revenge",
                "A rival hit executes a random Tier 3 Revenge card, then prepares a different retaliation.",
                "Hit taken: random Tier 3 Revenge",
                3,
                RogueliteAbilityVisualStyle.RAM));

        Map<RogueliteCardId, RogueliteCardDefinition> cardsById =
                new EnumMap<RogueliteCardId, RogueliteCardDefinition>(RogueliteCardId.class);
        for (int i = 0; i < cards.size(); i++) {
            RogueliteCardDefinition definition = cards.get(i);
            if (cardsById.put(definition.getId(), definition) != null) {
                throw new IllegalStateException(
                        "Duplicate roguelite card ID: " + definition.getId());
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

    /** Converts current or pre-slot-redesign save IDs into an equipable card. */
    public static RogueliteCardId resolveSavedId(String savedId) {
        if (savedId == null) {
            return null;
        }
        String normalized = savedId.trim();
        try {
            return RogueliteCardId.valueOf(normalized);
        } catch (IllegalArgumentException ignored) {
            // Legacy IDs are deliberately mapped by their former slot so one old loadout
            // remains one card in each of the three new categories.
        }
        if ("TURBOCHARGER".equals(normalized)
                || "LIGHTWEIGHT_FLYWHEEL".equals(normalized)) {
            return RogueliteCardId.CLUB_TUNE;
        }
        if ("STRAIGHT_LINE_NITRO".equals(normalized)) {
            return RogueliteCardId.SPORT_TUNE;
        }
        if ("DRIFT_CAPACITOR".equals(normalized)) {
            return RogueliteCardId.RACE_TUNE;
        }
        if ("KINETIC_RECYCLER".equals(normalized)) {
            return RogueliteCardId.HEAVYWEIGHT_TUNE;
        }
        if ("OVERTAKE_INJECTOR".equals(normalized)
                || "STORM_DYNAMO".equals(normalized)) {
            return RogueliteCardId.CHAMPIONSHIP_TUNE;
        }
        if ("COUNTERSTEER_SERVO".equals(normalized)
                || "SPORT_SUSPENSION".equals(normalized)) {
            return RogueliteCardId.CORNER_EXIT;
        }
        if ("QUICK_RACK".equals(normalized)) {
            return RogueliteCardId.DRAFT_HUNTER;
        }
        if ("STORM_TIRES".equals(normalized)
                || "RECOVERY_DIFFERENTIAL".equals(normalized)) {
            return RogueliteCardId.RECOVERY_LAUNCH;
        }
        if ("TRAIL_BRAKE_BATTERY".equals(normalized)) {
            return RogueliteCardId.DRIFT_SLINGSHOT;
        }
        if ("IMPACT_GYROSCOPE".equals(normalized)) {
            return RogueliteCardId.RACECRAFT_MASTERY;
        }
        if ("FRONT_SPLITTER".equals(normalized)) {
            return RogueliteCardId.GRIP_FAN;
        }
        if ("AERODYNAMIC_KIT".equals(normalized)
                || "REINFORCED_BUMPER".equals(normalized)) {
            return RogueliteCardId.RAM_REACTOR;
        }
        if ("DRAFT_RECEIVER".equals(normalized)) {
            return RogueliteCardId.PHASE_SHIELD;
        }
        if ("VACUUM_FAN".equals(normalized)) {
            return RogueliteCardId.GRAVITY_WELL;
        }
        if ("PHASE_ARMOR".equals(normalized)) {
            return RogueliteCardId.CROWN_ENGINE;
        }
        return null;
    }

    private static RogueliteCardDefinition card(
            RogueliteCardId id,
            String title,
            String description,
            String effectText,
            int tier,
            RogueliteSlotType slotType) {
        return new RogueliteCardDefinition(
                id,
                title,
                description,
                effectText,
                tier,
                slotType,
                null,
                artworkIndex(id));
    }

    private static RogueliteCardDefinition powerup(
            RogueliteCardId id,
            String title,
            String description,
            String effectText,
            int tier,
            RogueliteAbilityVisualStyle visualStyle) {
        return new RogueliteCardDefinition(
                id,
                title,
                description,
                effectText,
                tier,
                RogueliteSlotType.POWERUP,
                visualStyle,
                artworkIndex(id));
    }

    private static RogueliteCardDefinition revenge(
            RogueliteCardId id,
            String title,
            String description,
            String effectText,
            int tier,
            RogueliteAbilityVisualStyle visualStyle) {
        return new RogueliteCardDefinition(
                id,
                title,
                description,
                effectText,
                tier,
                RogueliteSlotType.REVENGE,
                visualStyle,
                artworkIndex(id));
    }

    private static int artworkIndex(RogueliteCardId id) {
        switch (id) {
            case CLUB_TUNE:
                return 0;
            case CORNER_EXIT:
                return 1;
            case DRAFT_HUNTER:
                return 2;
            case NITRO_PULSE:
                return 3;
            case MIRROR_DUO:
                return 19;
            case GRIP_FAN:
                return 4;
            case SPORT_TUNE:
                return 5;
            case CLEAN_MOMENTUM:
                return 6;
            case RECOVERY_LAUNCH:
                return 7;
            case RAM_REACTOR:
                return 8;
            case DRAFT_MAGNET:
                return 9;
            case RACE_TUNE:
                return 10;
            case DRIFT_SLINGSHOT:
                return 11;
            case SLIPSTREAM_SLINGSHOT:
                return 12;
            case PHASE_SHIELD:
                return 13;
            case ROCKET_EXHAUST:
                return 14;
            case MIRROR_TRIO:
                return 42;
            case HEAVYWEIGHT_TUNE:
                return 15;
            case OVERTAKE_SURGE:
                return 16;
            case APEX_SLINGSHOT:
                return 17;
            case GRAVITY_WELL:
                return 18;
            case OVERDRIVE_COIL:
                return 43;
            case CHAMPIONSHIP_TUNE:
                return 20;
            case PERFECT_LAP:
                return 21;
            case RACECRAFT_MASTERY:
                return 22;
            case UNDERDOG_INSTINCT:
                return 44;
            case COMEBACK_DRIVE:
                return 45;
            case LAST_PLACE_FURY:
                return 46;
            case CLOSE_QUARTERS:
                return 47;
            case PACK_RACER:
                return 48;
            case TRAFFIC_DOMINANCE:
                return 49;
            case HYPERDRIVE:
                return 23;
            case CROWN_ENGINE:
                return 24;
            case AERO_TRIM:
                return 25;
            case SHORT_GEARING:
                return 26;
            case CARBON_PANELS:
                return 50;
            case LOW_DRAG_BODY:
                return 27;
            case DRIFT_DIFFERENTIAL:
                return 28;
            case CARBON_MONOCOQUE:
                return 51;
            case GROUND_EFFECT:
                return 29;
            case VELOCITY_SHELL:
                return 30;
            case TORQUE_VECTORING:
                return 31;
            case GRAPHENE_CHASSIS:
                return 52;
            case RECOVERY_BEACON:
                return 32;
            case DRAFT_VENDETTA:
                return 33;
            case PAYBACK_SHIELD:
                return 34;
            case REPULSOR_SURGE:
                return 35;
            case TAR_TETHER:
                return 36;
            case EMP_SNARE:
                return 37;
            case VOID_ANCHOR:
                return 38;
            case SENSOR_JAMMER:
                return 53;
            case GRID_BLACKOUT:
                return 54;
            case TOTAL_BLACKOUT:
                return 55;
            case LUCKY_SPARK:
                return 56;
            case CHAOS_RELAY:
                return 57;
            case WILDCARD_CORE:
                return 58;
            case LOADED_GRUDGE:
                return 59;
            case CHAOS_RETORT:
                return 60;
            case FATES_REVENGE:
                return 61;
            case GHOST_CLOAK:
                return 39;
            case PHANTOM_CLOAK:
                return 40;
            case VOID_CLOAK:
                return 41;
            default:
                throw new IllegalArgumentException("No artwork index for " + id);
        }
    }
}
