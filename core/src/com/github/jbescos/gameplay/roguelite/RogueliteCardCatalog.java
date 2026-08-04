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
                "+5% power, +2% top speed, +4% grip",
                1,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.SPORT_TUNE,
                "Lightweight Tune",
                "A stripped chassis changes direction quickly while keeping enough power and grip for racing.",
                "-8% mass, +7% power, +3% top speed, +5% grip",
                1,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.RACE_TUNE,
                "Race Tune",
                "Sharper race hardware combines sustained power with high-speed stability.",
                "+14% power, +6% top speed, +10% grip",
                2,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.HEAVYWEIGHT_TUNE,
                "Ballast Powertrain",
                "A reinforced, heavier car carries extra power and grip so contact no longer ruins its pace.",
                "+16% mass, +24% power, +8% speed, +14% grip; stronger contact",
                2,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.CHAMPIONSHIP_TUNE,
                "Aero Prototype",
                "A low-drag body and high-speed downforce turn open road into a decisive advantage.",
                "-10% drag, +27% power, +12% speed, +18% grip",
                3,
                RogueliteSlotType.TUNING));

        cards.add(card(
                RogueliteCardId.CORNER_EXIT,
                "Corner Exit",
                "Leaving an on-road corner under power creates a short launch onto the next section.",
                "Corner exit: +12% power for 1.4s",
                1,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.DRAFT_HUNTER,
                "Draft Hunter",
                "Finds a rival's wake sooner and turns close following into useful speed.",
                "+25% slipstream reach and strength",
                1,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.CLEAN_MOMENTUM,
                "Clean Momentum",
                "Continuous on-road driving builds a speed advantage that is lost by leaving the circuit.",
                "Clean driving: build up to +7% speed",
                1,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.RECOVERY_LAUNCH,
                "Recovery Launch",
                "A quick legitimate return to the road restores traction and accelerates back into the race.",
                "Safe re-entry: +14% power and +18% grip",
                2,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.DRIFT_SLINGSHOT,
                "Drift Slingshot",
                "Sustained on-road slip stores energy and releases it when the car straightens.",
                "Drift exit: up to +18% power",
                2,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.SLIPSTREAM_SLINGSHOT,
                "Slipstream Slingshot",
                "Charges in another car's wake and launches when you pull out to pass.",
                "Leave a draft: +20% power and +7% speed",
                2,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.OVERTAKE_SURGE,
                "Overtake Surge",
                "Every gained race position immediately provides power to complete the move.",
                "Pass a rival: +24% power for 2s",
                2,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.APEX_SLINGSHOT,
                "Apex Slingshot",
                "Loads energy through a fast on-road corner and releases it as the road straightens.",
                "Fast corner exit: up to +23% power",
                3,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.PERFECT_LAP,
                "Perfect Lap",
                "Clean speed, accurate corner exits and uninterrupted momentum compound throughout the lap.",
                "Clean racing: up to +15% power, +10% speed and +12% grip",
                3,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.RACECRAFT_MASTERY,
                "Racecraft Mastery",
                "Drafting, corner exits and overtakes each trigger a powerful race-winning response.",
                "Race events: +28% power, speed and grip bursts",
                3,
                RogueliteSlotType.TECHNIQUE));

        cards.add(gadget(
                RogueliteCardId.NITRO_PULSE,
                "Nitro Pulse",
                "Kicks the car forward when open road invites a nitro burst.",
                "Long straight; 9s cooldown: launch, +20% power, +6% speed",
                1,
                RogueliteGadgetVisualStyle.NITRO));
        cards.add(gadget(
                RogueliteCardId.GRIP_FAN,
                "Grip Fan",
                "A glowing underbody fan pins the car down as a demanding corner arrives.",
                "Corner approach; 8.5s cooldown: +18% grip and +12% steering",
                1,
                RogueliteGadgetVisualStyle.GRIP));
        cards.add(gadget(
                RogueliteCardId.RAM_REACTOR,
                "Ram Reactor",
                "Arms an unmistakable impact charge only when a rival is close ahead.",
                "Close rival; 8s cooldown: powered ram and +16% power",
                2,
                RogueliteGadgetVisualStyle.RAM));
        cards.add(gadget(
                RogueliteCardId.DRAFT_MAGNET,
                "Draft Magnet",
                "A pulsing field blasts nearby rivals toward the outside of the circuit.",
                "Close rival; 8s cooldown: outward pulse and stronger drafting",
                1,
                RogueliteGadgetVisualStyle.DRAFT));
        cards.add(gadget(
                RogueliteCardId.PHASE_SHIELD,
                "Phase Shield",
                "An energy shell forms when traffic closes in, absorbing frontal recoil.",
                "Close traffic; 7.2s cooldown: shield, +18% power and +12% grip",
                2,
                RogueliteGadgetVisualStyle.SHIELD));
        cards.add(gadget(
                RogueliteCardId.ROCKET_EXHAUST,
                "Rocket Exhaust",
                "Twin exhaust rockets ignite on a clear straight for a forceful launch.",
                "Long straight; 7s cooldown: strong launch, +28% power, +9% speed",
                2,
                RogueliteGadgetVisualStyle.NITRO));
        cards.add(gadget(
                RogueliteCardId.GRAVITY_WELL,
                "Gravity Well",
                "A visible ground field forms in corners or close traffic for extreme stability.",
                "Corner or traffic; 6.5s cooldown: +28% grip and stronger contact",
                3,
                RogueliteGadgetVisualStyle.GRIP));
        cards.add(gadget(
                RogueliteCardId.OVERDRIVE_COIL,
                "Overdrive Coil",
                "The powertrain releases its stored energy when the car reaches open road.",
                "Long straight; 6s cooldown: +30% power and +11% speed",
                3,
                RogueliteGadgetVisualStyle.OVERDRIVE));
        cards.add(gadget(
                RogueliteCardId.HYPERDRIVE,
                "Hyperdrive",
                "Open road triggers an extreme launch and turns the car into a visible streak.",
                "Long straight; 5s cooldown: extreme launch, +38% power, +15% speed, +16% grip",
                3,
                RogueliteGadgetVisualStyle.OVERDRIVE));
        cards.add(gadget(
                RogueliteCardId.CROWN_ENGINE,
                "Crown Engine",
                "A championship core reacts to straights, corners and nearby rivals with the right force.",
                "Race opportunity; 5.5s cooldown: power, speed, grip and contact",
                3,
                RogueliteGadgetVisualStyle.RAM));

        Map<RogueliteCardId, RogueliteCardDefinition> cardsById =
                new EnumMap<RogueliteCardId, RogueliteCardDefinition>(RogueliteCardId.class);
        boolean[] assignedArtwork = new boolean[25];
        for (int i = 0; i < cards.size(); i++) {
            RogueliteCardDefinition definition = cards.get(i);
            if (cardsById.put(definition.getId(), definition) != null) {
                throw new IllegalStateException(
                        "Duplicate roguelite card ID: " + definition.getId());
            }
            if (assignedArtwork[definition.getArtworkIndex()]) {
                throw new IllegalStateException(
                        "Duplicate roguelite card artwork index: "
                                + definition.getArtworkIndex());
            }
            assignedArtwork[definition.getArtworkIndex()] = true;
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

    private static RogueliteCardDefinition gadget(
            RogueliteCardId id,
            String title,
            String description,
            String effectText,
            int tier,
            RogueliteGadgetVisualStyle visualStyle) {
        return new RogueliteCardDefinition(
                id,
                title,
                description,
                effectText,
                tier,
                RogueliteSlotType.GADGET,
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
            case HEAVYWEIGHT_TUNE:
                return 15;
            case OVERTAKE_SURGE:
                return 16;
            case APEX_SLINGSHOT:
                return 17;
            case GRAVITY_WELL:
                return 18;
            case OVERDRIVE_COIL:
                return 19;
            case CHAMPIONSHIP_TUNE:
                return 20;
            case PERFECT_LAP:
                return 21;
            case RACECRAFT_MASTERY:
                return 22;
            case HYPERDRIVE:
                return 23;
            case CROWN_ENGINE:
                return 24;
            default:
                throw new IllegalArgumentException("No artwork index for " + id);
        }
    }
}
