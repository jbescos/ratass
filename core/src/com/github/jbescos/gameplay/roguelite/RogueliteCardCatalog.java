package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class RogueliteCardCatalog {
    public static final int MAX_CARD_TIER = 4;
    private static final List<RogueliteCardDefinition> CARDS;
    private static final Map<RogueliteCardId, RogueliteCardDefinition> CARDS_BY_ID;

    static {
        List<RogueliteCardDefinition> cards = new ArrayList<RogueliteCardDefinition>();

        cards.add(card(
                RogueliteCardId.CLUB_TUNE,
                "Club Tune",
                "Power and grip trade aerodynamic efficiency.",
                "Power +7%\nGrip +3%\nAero -8%",
                1,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.SPORT_TUNE,
                "Ballast Sprint",
                "Power and grip require extra chassis mass.",
                "Power +10%\nGrip +4%\nMass +8%",
                1,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.AERO_TRIM,
                "Streamline Kit",
                "Power and aero efficiency trade tire grip.",
                "Power +12%\nAero +14%\nGrip -2%",
                1,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.SHORT_GEARING,
                "Reinforced Streamliner",
                "Power and aero efficiency require extra chassis mass.",
                "Power +13%\nAero +13%\nMass +5%",
                1,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.CARBON_PANELS,
                "Carbon Panels",
                "Power and lower mass trade tire grip.",
                "Power +10%\nMass -4%\nGrip -3%",
                1,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.FEATHERWEIGHT_DRIVE,
                "Featherweight Drive",
                "Power and lower mass trade aerodynamic efficiency.",
                "Power +7%\nMass -3%\nAero -8%",
                1,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.TRACK_WING,
                "Track Wing",
                "Grip and aero efficiency trade engine power.",
                "Grip +6%\nAero +15%\nPower -3%",
                1,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.GROUNDED_AERO,
                "Grounded Aero",
                "Grip and aero efficiency require extra chassis mass.",
                "Grip +6%\nAero +18%\nMass +2%",
                1,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.LIGHT_COMPOUND,
                "Light Compound",
                "Grip and lower mass trade engine power.",
                "Grip +4%\nMass -6%\nPower -4%",
                1,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.AGILE_CHASSIS,
                "Agile Chassis",
                "Grip and lower mass trade aerodynamic efficiency.",
                "Grip +5%\nMass -4.5%\nAero -7%",
                1,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.STREAMLINED_CHASSIS,
                "Streamlined Chassis",
                "Aero efficiency and lower mass trade engine power.",
                "Aero +18%\nMass -10%\nPower -3%",
                1,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.LOW_DRAG_FEATHERWEIGHT,
                "Aero Featherweight",
                "Aero efficiency and lower mass trade tire grip.",
                "Aero +18%\nMass -10%\nGrip -2%",
                1,
                RogueliteSlotType.TUNING));

        cards.add(card(
                RogueliteCardId.RACE_TUNE,
                "Race Tune",
                "Power and grip trade aerodynamic efficiency.",
                "Power +12%\nGrip +5%\nAero -8%",
                2,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.HEAVYWEIGHT_TUNE,
                "Ballast Powertrain",
                "Power and grip require extra chassis mass.",
                "Power +16%\nGrip +6%\nMass +7%",
                2,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.LOW_DRAG_BODY,
                "Le Mans Body",
                "Power and aero efficiency trade tire grip.",
                "Power +22%\nAero +20%\nGrip -3%",
                2,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.DRIFT_DIFFERENTIAL,
                "Reinforced Longtail",
                "Power and aero efficiency require extra chassis mass.",
                "Power +24%\nAero +26%\nMass +5%",
                2,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.CARBON_MONOCOQUE,
                "Carbon Monocoque",
                "Power and lower mass trade tire grip.",
                "Power +19%\nMass -7%\nGrip -3%",
                2,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.TITANIUM_DRIVE,
                "Titanium Drive",
                "Power and lower mass trade aerodynamic efficiency.",
                "Power +17%\nMass -6%\nAero -4%",
                2,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.DOWNFORCE_PACKAGE,
                "Downforce Package",
                "Grip and aero efficiency trade engine power.",
                "Grip +12%\nAero +30%\nPower -3%",
                2,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.GROUNDED_DOWNFORCE,
                "Grounded Downforce",
                "Grip and aero efficiency require extra chassis mass.",
                "Grip +12%\nAero +30%\nMass +3%",
                2,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.MAGNESIUM_SUSPENSION,
                "Magnesium Suspension",
                "Grip and lower mass trade engine power.",
                "Grip +10%\nMass -8%\nPower -5%",
                2,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.AERO_AGILE_CHASSIS,
                "Aero-Agile Chassis",
                "Grip and lower mass trade aerodynamic efficiency.",
                "Grip +8%\nMass -6%\nAero -5%",
                2,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.CARBON_LONGTAIL,
                "Carbon Longtail",
                "Aero efficiency and lower mass trade engine power.",
                "Aero +32%\nMass -18%\nPower -4%",
                2,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.VENTURI_MONOCOQUE,
                "Venturi Monocoque",
                "Aero efficiency and lower mass trade tire grip.",
                "Aero +32%\nMass -18%\nGrip -2%",
                2,
                RogueliteSlotType.TUNING));

        cards.add(card(
                RogueliteCardId.CHAMPIONSHIP_TUNE,
                "Aero Prototype",
                "Power and aero efficiency improve together.",
                "Power +26%\nAero +48%",
                3,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.GROUND_EFFECT,
                "Ground Effect",
                "Grip and aero efficiency improve together.",
                "Grip +16%\nAero +8%",
                3,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.VELOCITY_SHELL,
                "Velocity Shell",
                "Power and grip improve together.",
                "Power +26%\nGrip +2%",
                3,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.TORQUE_VECTORING,
                "Power Monocoque",
                "Power and grip improve together.",
                "Power +2%\nGrip +15%",
                3,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.GRAPHENE_CHASSIS,
                "Graphene Chassis",
                "Grip and lower mass improve together.",
                "Grip +12%\nMass -5%",
                3,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.TITANIUM_SKELETON,
                "Titanium Skeleton",
                "Power and lower mass improve together.",
                "Power +25%\nMass -7%",
                3,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.HYPERCAR_CORE,
                "Hypercar Core",
                "Power and aero efficiency improve together.",
                "Power +28%\nAero +16%",
                3,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.ACTIVE_AERO_SHELL,
                "Active Aero Shell",
                "Aero efficiency and lower mass improve together.",
                "Aero +55%\nMass -22%",
                3,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.CARBON_PROTOTYPE,
                "Carbon Prototype",
                "Power and lower mass improve together.",
                "Power +9%\nMass -25%",
                3,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.TRACK_VACUUM,
                "Track Vacuum",
                "Grip and aero efficiency improve together.",
                "Grip +12%\nAero +60%",
                3,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.WING_CAR,
                "Wing Car",
                "Aero efficiency and lower mass improve together.",
                "Aero +18%\nMass -28%",
                3,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.FEATHERWEIGHT_GROUND_EFFECT,
                "Feather Ground",
                "Grip and lower mass improve together.",
                "Grip +7%\nMass -16%",
                3,
                RogueliteSlotType.TUNING));
        cards.add(card(
                RogueliteCardId.TECHNIQUE_SINGULARITY,
                "Technique Singularity",
                "An experimental control core doubles the equipped Technique effect.",
                "Technique effects x2",
                4,
                RogueliteSlotType.TUNING));

        cards.add(card(
                RogueliteCardId.CORNER_FOCUS,
                "Corner Focus",
                "Cornering amplifies active grip bonuses and every active aero bonus or penalty. Grip penalties and weather stay unchanged.",
                "Activation: Corner | 2s\nGrip x1.5\nAero x1.5",
                1,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.DRAFT_FOCUS,
                "Draft Focus",
                "Slipstreaming amplifies every active power and aero bonus or penalty.",
                "Activation: Slipstream | 10s\nPower x2\nAero x2",
                1,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.STRAIGHT_FOCUS,
                "Straight Focus",
                "A long straight amplifies every active power and aero bonus or penalty.",
                "Activation: Long straight | 3s\nPower x1.5\nAero x1.5",
                1,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.DRIFT_FOCUS,
                "Drift Focus",
                "Drifting amplifies every active power and mass bonus or penalty.",
                "Activation: Drifting | 3s\nPower x1.5\nMass x1.5",
                1,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.RALLY_FOCUS,
                "Rally Focus",
                "Leaving the road amplifies every active power, aero, and mass bonus or penalty, plus active grip bonuses. Grip penalties and weather stay unchanged.",
                "Activation: Off-road | 10s\nPower x1.5\nGrip x1.5\nAero x1.5\nMass x1.5",
                1,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.APEX_FOCUS,
                "Apex Focus",
                "Cornering amplifies every active aero and mass bonus or penalty.",
                "Activation: Corner | 2s\nAero x1.5\nMass x1.5",
                1,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.SPRINT_FOCUS,
                "Sprint Focus",
                "A long straight amplifies every active power and mass bonus or penalty.",
                "Activation: Long straight | 3s\nPower x1.5\nMass x1.5",
                1,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.SLIDE_FOCUS,
                "Slide Focus",
                "Drifting amplifies every active aero and mass bonus or penalty.",
                "Activation: Drifting | 3s\nAero x1.5\nMass x1.5",
                1,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.TRACTION_FOCUS,
                "Traction Focus",
                "Cornering amplifies every active power bonus or penalty and active grip bonuses. Grip penalties and weather stay unchanged.",
                "Activation: Corner | 2s\nPower x1.5\nGrip x1.5",
                1,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.AGILITY_FOCUS,
                "Agility Focus",
                "Cornering amplifies active grip bonuses and every active mass bonus or penalty. Grip penalties and weather stay unchanged.",
                "Activation: Corner | 2s\nGrip x1.5\nMass x1.5",
                1,
                RogueliteSlotType.TECHNIQUE));

        cards.add(card(
                RogueliteCardId.CORNER_EXPERT,
                "Corner Expert",
                "Cornering amplifies active grip bonuses and every active aero bonus or penalty. Grip penalties and weather stay unchanged.",
                "Activation: Corner | 3s\nGrip x2\nAero x2",
                2,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.DRAFT_EXPERT,
                "Draft Expert",
                "Slipstreaming amplifies every active power and aero bonus or penalty.",
                "Activation: Slipstream | 10s\nPower x3\nAero x3",
                2,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.STRAIGHT_EXPERT,
                "Straight Expert",
                "A long straight amplifies every active power and aero bonus or penalty.",
                "Activation: Long straight | 4s\nPower x2\nAero x2",
                2,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.DRIFT_EXPERT,
                "Drift Expert",
                "Drifting amplifies every active power and mass bonus or penalty.",
                "Activation: Drifting | 4s\nPower x2\nMass x2",
                2,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.RALLY_EXPERT,
                "Rally Expert",
                "Leaving the road amplifies every active power, aero, and mass bonus or penalty, plus active grip bonuses. Grip penalties and weather stay unchanged.",
                "Activation: Off-road | 10s\nPower x2\nGrip x2\nAero x2\nMass x2",
                2,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.APEX_EXPERT,
                "Apex Expert",
                "Cornering amplifies every active aero and mass bonus or penalty.",
                "Activation: Corner | 3s\nAero x2\nMass x2",
                2,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.SPRINT_EXPERT,
                "Sprint Expert",
                "A long straight amplifies every active power and mass bonus or penalty.",
                "Activation: Long straight | 4s\nPower x2\nMass x2",
                2,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.SLIDE_EXPERT,
                "Slide Expert",
                "Drifting amplifies every active aero and mass bonus or penalty.",
                "Activation: Drifting | 4s\nAero x2\nMass x2",
                2,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.TRACTION_EXPERT,
                "Traction Expert",
                "Cornering amplifies every active power bonus or penalty and active grip bonuses. Grip penalties and weather stay unchanged.",
                "Activation: Corner | 3s\nPower x2\nGrip x2",
                2,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.AGILITY_EXPERT,
                "Agility Expert",
                "Cornering amplifies active grip bonuses and every active mass bonus or penalty. Grip penalties and weather stay unchanged.",
                "Activation: Corner | 3s\nGrip x2\nMass x2",
                2,
                RogueliteSlotType.TECHNIQUE));

        cards.add(card(
                RogueliteCardId.CORNER_MASTER,
                "Corner Master",
                "Cornering amplifies active grip bonuses and every active aero bonus or penalty. Grip penalties and weather stay unchanged.",
                "Activation: Corner | 4s\nGrip x3\nAero x3",
                3,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.DRAFT_MASTER,
                "Draft Master",
                "Slipstreaming amplifies every active power and aero bonus or penalty.",
                "Activation: Slipstream | 10s\nPower x4\nAero x4",
                3,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.STRAIGHT_MASTER,
                "Straight Master",
                "A long straight amplifies every active power and aero bonus or penalty.",
                "Activation: Long straight | 5s\nPower x3\nAero x3",
                3,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.DRIFT_MASTER,
                "Drift Master",
                "Drifting amplifies every active power and mass bonus or penalty.",
                "Activation: Drifting | 5s\nPower x3\nMass x3",
                3,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.RALLY_MASTER,
                "Rally Master",
                "Leaving the road amplifies every active power, aero, and mass bonus or penalty, plus active grip bonuses. Grip penalties and weather stay unchanged.",
                "Activation: Off-road | 10s\nPower x3\nGrip x3\nAero x3\nMass x3",
                3,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.APEX_MASTER,
                "Apex Master",
                "Cornering amplifies every active aero and mass bonus or penalty.",
                "Activation: Corner | 4s\nAero x3\nMass x3",
                3,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.SPRINT_MASTER,
                "Sprint Master",
                "A long straight amplifies every active power and mass bonus or penalty.",
                "Activation: Long straight | 5s\nPower x3\nMass x3",
                3,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.SLIDE_MASTER,
                "Slide Master",
                "Drifting amplifies every active aero and mass bonus or penalty.",
                "Activation: Drifting | 5s\nAero x3\nMass x3",
                3,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.TRACTION_MASTER,
                "Traction Master",
                "Cornering amplifies every active power bonus or penalty and active grip bonuses. Grip penalties and weather stay unchanged.",
                "Activation: Corner | 4s\nPower x3\nGrip x3",
                3,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.AGILITY_MASTER,
                "Agility Master",
                "Cornering amplifies active grip bonuses and every active mass bonus or penalty. Grip penalties and weather stay unchanged.",
                "Activation: Corner | 4s\nGrip x3\nMass x3",
                3,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.UNDERDOG_INSTINCT,
                "Underdog Instinct",
                "Reads the field and gains performance as the car falls back, reaching full strength in last place.",
                "Activation: Lower position\nPower up to +10%\nGrip up to +10%\nAero up to +10%\nMass up to -10%",
                1,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.COMEBACK_DRIVE,
                "Comeback Drive",
                "Reads the field and gains performance as the car falls back, reaching full strength in last place.",
                "Activation: Lower position\nPower up to +20%\nGrip up to +20%\nAero up to +20%\nMass up to -20%",
                2,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.LAST_PLACE_FURY,
                "Last Place Fury",
                "Reads the field and gains performance as the car falls back, reaching full strength in last place.",
                "Activation: Lower position\nPower up to +40%\nGrip up to +40%\nAero up to +40%\nMass up to -40%",
                3,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.CLOSE_QUARTERS,
                "Close Quarters",
                "Raises the car's pace whenever a rival is nearby, helping attacks and defensive runs.",
                "Activation: Nearby rival\nPower +5%\nGrip +5%\nAero +5%\nMass -5%",
                1,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.PACK_RACER,
                "Pack Racer",
                "Raises the car's pace whenever a rival is nearby, helping attacks and defensive runs.",
                "Activation: Nearby rival\nPower +10%\nGrip +10%\nAero +10%\nMass -10%",
                2,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.TRAFFIC_DOMINANCE,
                "Traffic Dominance",
                "Raises the car's pace whenever a rival is nearby, helping attacks and defensive runs.",
                "Activation: Nearby rival\nPower +20%\nGrip +20%\nAero +20%\nMass -20%",
                3,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.LAP_DIVIDEND,
                "Lap Dividend",
                "While equipped, doubles both lap XP capacity and the amount transferred at the line. Replacing it clamps pending lap XP to the new capacity. Finish XP is unchanged.",
                "Activation: Passive\nLap XP capacity x2\nBanked lap XP x2",
                1,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.LAP_BOOSTER,
                "Lap Booster",
                "While equipped, triples both lap XP capacity and the amount transferred at the line. Replacing it clamps pending lap XP to the new capacity. Finish XP is unchanged.",
                "Activation: Passive\nLap XP capacity x3\nBanked lap XP x3",
                2,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.LAP_DOUBLER,
                "Lap Doubler",
                "While equipped, quadruples both lap XP capacity and the amount transferred at the line. Replacing it clamps pending lap XP to the new capacity. Finish XP is unchanged.",
                "Activation: Passive\nLap XP capacity x4\nBanked lap XP x4",
                3,
                RogueliteSlotType.TECHNIQUE));
        cards.add(card(
                RogueliteCardId.POWERUP_NEXUS,
                "Powerup Nexus",
                "A passive timing nexus doubles Powerup effects and cooldown recovery.",
                "Activation: Passive\nPowerup effects x2\nCooldown recovery x2",
                4,
                RogueliteSlotType.TECHNIQUE));

        cards.add(powerup(
                RogueliteCardId.NITRO_PULSE,
                "Nitro Pulse",
                "Kicks the car forward when open road invites a nitro burst.",
                "Clear straight: launch | Power +20%\nCooldown: 9s",
                1,
                RogueliteAbilityVisualStyle.NITRO_T1));
        cards.add(powerup(
                RogueliteCardId.ACE_HOTLINE,
                "Ace Hotline",
                "Calls the best benchmarked driver, who gives you driving advice for 10 seconds.",
                "Automatic call: best-driver advice | 10s\nCooldown: 20s",
                1,
                RogueliteAbilityVisualStyle.HOTLINE_T1));
        cards.add(powerup(
                RogueliteCardId.TIME_RIPPLE,
                "Time Ripple",
                "Doubles local time for your car and quantum copies; movement and decisions run x2.",
                "Automatic: local time x2 | 2s\nCooldown: 60s",
                1,
                RogueliteAbilityVisualStyle.TIME_T1));
        cards.add(powerup(
                RogueliteCardId.MIRROR_DUO,
                "Quantum Duo",
                "Creates two physical cars. Each drives independently, shares the same cards and executes Revenge with the group; a hit to any copy arms it.",
                "Nearby rival on straight: 2 cars for 5s\nShared cards and Revenge | Cooldown: 10s",
                1,
                RogueliteAbilityVisualStyle.MIRROR));
        cards.add(powerup(
                RogueliteCardId.BULK_FIELD,
                "Bulk Field",
                "Projects a larger car-only collision body without changing the road footprint. Blind and immune to control-disrupting Revenge effects while active.",
                "Nearby rival: area x2 | 10s | Blind\nControl-Revenge immune | Mass +20% | Grip +5% | CD 20s",
                1,
                RogueliteAbilityVisualStyle.ICON_ONLY));
        cards.add(powerup(
                RogueliteCardId.GRIP_FAN,
                "Grip Fan",
                "A glowing underbody fan pins the car down as a demanding corner arrives.",
                "Corner ahead: grip +18% | Steering +12%\nCooldown: 8.5s",
                1,
                RogueliteAbilityVisualStyle.GRIP_T1));
        cards.add(powerup(
                RogueliteCardId.GHOST_CLOAK,
                "Ghost Cloak",
                "The car phases out when traffic is nearby, becoming invisible and intangible to rivals.",
                "Nearby rival: invisible 3s | Intangible\nCancels targeting Revenge | Debuffs remain | Cooldown 10s",
                1,
                RogueliteAbilityVisualStyle.CLOAK));
        cards.add(powerup(
                RogueliteCardId.LUCKY_SPARK,
                "Lucky Spark",
                "Prepares a random Tier 1 Powerup and copies its real trigger, effect and cooldown.",
                "Each activation: random Tier 1 Powerup",
                1,
                RogueliteAbilityVisualStyle.NITRO_T1));
        cards.add(revenge(
                RogueliteCardId.DRAFT_MAGNET,
                "Draft Magnet",
                "A qualified rival hit arms a short pulsing field that forces nearby cars toward the outside.",
                "Activation: Rival hit\nNearby rival: outward field 2s",
                1,
                RogueliteAbilityVisualStyle.DRAFT));
        cards.add(revenge(
                RogueliteCardId.RECOVERY_BEACON,
                "Position Hijack",
                "A qualified hit marks its offender. After charging, it exchanges positions only while they are ahead.",
                "Activation: Rival hit\nAfter 2s: swap with offender if ahead",
                2,
                RogueliteAbilityVisualStyle.SHIELD));
        cards.add(revenge(
                RogueliteCardId.DRAFT_VENDETTA,
                "Redline Hex",
                "A qualified hit curses its offender's throttle, forcing them to commit through whatever comes next.",
                "Activation: Rival hit\nOffender: full throttle for 5s",
                1,
                RogueliteAbilityVisualStyle.DRAFT));
        cards.add(powerup(
                RogueliteCardId.PHASE_SHIELD,
                "Phase Shield",
                "An energy shell forms when traffic closes in, absorbing frontal recoil.",
                "Traffic or corner: shield | Power +22% | Grip +20%\nCooldown: 6.8s",
                2,
                RogueliteAbilityVisualStyle.GRIP_T2));
        cards.add(powerup(
                RogueliteCardId.ROCKET_EXHAUST,
                "Rocket Exhaust",
                "Twin exhaust rockets ignite on a clear straight for a forceful launch.",
                "Clear straight: launch | Power +32%\nCooldown: 6.8s",
                2,
                RogueliteAbilityVisualStyle.NITRO_T2));
        cards.add(powerup(
                RogueliteCardId.PRIORITY_HOTLINE,
                "Priority Hotline",
                "Renews advice from the best benchmarked driver in repeating ten-second sessions.",
                "Automatic: best avg-lap driver | 10s\nCooldown: 10s",
                2,
                RogueliteAbilityVisualStyle.HOTLINE_T2));
        cards.add(powerup(
                RogueliteCardId.CHRONO_SHIFT,
                "Chrono Shift",
                "Doubles local time for your car and quantum copies, recharging faster.",
                "Automatic: local time x2 | 2s\nCooldown: 40s",
                2,
                RogueliteAbilityVisualStyle.TIME_T2));
        cards.add(powerup(
                RogueliteCardId.MIRROR_TRIO,
                "Quantum Trio",
                "Creates three physical cars. Each drives independently, shares the same cards and executes Revenge with the group; a hit to any copy arms it.",
                "Nearby rival on straight: 3 cars for 5s\nShared cards and Revenge | Cooldown: 10s",
                2,
                RogueliteAbilityVisualStyle.MIRROR));
        cards.add(powerup(
                RogueliteCardId.TITAN_FIELD,
                "Titan Field",
                "Projects a much larger car-only collision body without changing the road footprint. Blind and immune to control-disrupting Revenge effects while active.",
                "Nearby rival: area x3 | 10s | Blind\nControl-Revenge immune | Mass +20% | Grip +5% | CD 15s",
                2,
                RogueliteAbilityVisualStyle.ICON_ONLY));
        cards.add(powerup(
                RogueliteCardId.PHANTOM_CLOAK,
                "Phantom Cloak",
                "An improved phase field hides the car and prevents rivals from making contact for longer.",
                "Nearby rival: invisible 4s | Intangible\nCancels targeting Revenge | Debuffs remain | Cooldown 10s",
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
                "Corner or traffic: power +40% | Grip +22%\nCooldown: 5.8s",
                3,
                RogueliteAbilityVisualStyle.GRIP_T3));
        cards.add(powerup(
                RogueliteCardId.OVERDRIVE_COIL,
                "Quantum Quartet",
                "Creates four physical cars. Each drives independently, shares the same cards and executes Revenge with the group; a hit to any copy arms it.",
                "Nearby rival on straight: 4 cars for 5s\nShared cards and Revenge | Cooldown: 10s",
                3,
                RogueliteAbilityVisualStyle.MIRROR));
        cards.add(powerup(
                RogueliteCardId.COLOSSUS_FIELD,
                "Colossus Field",
                "Projects an enormous car-only collision body without changing the road footprint. Blind and immune to control-disrupting Revenge effects while active.",
                "Nearby rival: area x4 | 10s | Blind\nControl-Revenge immune | Mass +20% | Grip +5% | CD 10s",
                3,
                RogueliteAbilityVisualStyle.ICON_ONLY));
        cards.add(powerup(
                RogueliteCardId.HYPERDRIVE,
                "Hyperdrive",
                "Open road triggers an extreme launch and turns the car into a visible streak.",
                "Clear straight: launch | Power +38% | Grip +16%\nCooldown: 6.5s",
                3,
                RogueliteAbilityVisualStyle.NITRO_T3));
        cards.add(powerup(
                RogueliteCardId.TEMPORAL_DOMINION,
                "Temporal Dominion",
                "Doubles local time for your entire quantum family with the fastest recharge.",
                "Automatic: local time x2 | 2s\nCooldown: 30s",
                3,
                RogueliteAbilityVisualStyle.TIME_T3));
        cards.add(powerup(
                RogueliteCardId.VOID_CLOAK,
                "Void Cloak",
                "A championship phase system removes the car from sight and contact for an extended attack window.",
                "Nearby rival: invisible 5s | Intangible\nCancels targeting Revenge | Debuffs remain | Cooldown 10s",
                3,
                RogueliteAbilityVisualStyle.CLOAK));
        cards.add(powerup(
                RogueliteCardId.WILDCARD_CORE,
                "Wildcard Core",
                "Prepares a random Tier 3 Powerup and copies its real trigger, effect and cooldown.",
                "Each activation: random Tier 3 Powerup",
                3,
                RogueliteAbilityVisualStyle.NITRO_T3));
        cards.add(powerup(
                RogueliteCardId.TUNE_LINK,
                "Tune Link",
                "An always-on antenna keeps your Tuning and imports the strongest missing Tuning attribute from linked cars.",
                "Always active\nImport 1 Tuning attribute",
                2,
                RogueliteAbilityVisualStyle.ANTENNA_T2));
        cards.add(powerup(
                RogueliteCardId.TECHNIQUE_LINK,
                "Dual Link",
                "An always-on antenna keeps your Tuning and imports the two strongest missing Tuning attributes from linked cars.",
                "Always active\nImport 2 Tuning attributes",
                3,
                RogueliteAbilityVisualStyle.ANTENNA_T3));
        cards.add(powerup(
                RogueliteCardId.TIER_FOUR_SIGNAL,
                "Apex Key",
                "Using this key permanently unlocks Tier 4 offers for this driver, even after the card is replaced.",
                "Automatic: unlock Tier 4 permanently | 10s\nCooldown: 10s",
                3,
                RogueliteAbilityVisualStyle.TIER_FOUR_SIGNAL));
        cards.add(powerup(
                RogueliteCardId.NEMESIS_ENGINE,
                "Nemesis Engine",
                "An extreme green engine doubles the consequences whenever Revenge activates.",
                "Revenge activation: effect x2",
                4,
                RogueliteAbilityVisualStyle.REVENGE_BOOST_T3));
        cards.add(revenge(
                RogueliteCardId.CROWN_ENGINE,
                "Crown Breaker",
                "A rival hit marks its offender and empowers you until an automatic close-range ram.",
                "Activation: Rival hit\n3s charge | 30s hunt | Power +55% | Recoil -75% | Push +70%",
                3,
                RogueliteAbilityVisualStyle.RAM));
        cards.add(revenge(
                RogueliteCardId.PAYBACK_SHIELD,
                "Vendetta Hook",
                "A qualified hit marks its offender. After charging, the hook pulls them back only while they are ahead.",
                "Activation: Rival hit\nAfter 2s: if ahead, pull offender to you over 1s",
                1,
                RogueliteAbilityVisualStyle.SHIELD));
        cards.add(revenge(
                RogueliteCardId.REPULSOR_WAVE,
                "Repulsor Wave",
                "A qualified rival hit arms a medium-range energy wave that pushes nearby cars away.",
                "Activation: Rival hit\nNearby rival: medium outward field 2s",
                2,
                RogueliteAbilityVisualStyle.DRAFT));
        cards.add(revenge(
                RogueliteCardId.HUNTER_BARRAGE,
                "Hunter Barrage",
                "Marks the rival who hit you, then hunts them anywhere on the circuit with three impact shots.",
                "Activation: Rival hit\nOffender: 3 impact shots, 1s apart",
                2,
                RogueliteAbilityVisualStyle.RAM));
        cards.add(revenge(
                RogueliteCardId.HUNTER_STORM,
                "Hunter Storm",
                "Marks the rival who hit you, then saturates their position with a rapid impact storm anywhere on the circuit.",
                "Activation: Rival hit\nOffender: 2 shots/s for 3s",
                3,
                RogueliteAbilityVisualStyle.RAM));
        cards.add(revenge(
                RogueliteCardId.REPULSOR_SURGE,
                "Repulsor Surge",
                "A qualified rival hit arms a wide high-energy field that clears space for your comeback.",
                "Activation: Rival hit\nNearby rival: wide outward field 2s",
                3,
                RogueliteAbilityVisualStyle.DRAFT));
        cards.add(revenge(
                RogueliteCardId.TAR_TETHER,
                "Tar Tether",
                "Throws a sticky tether at the rival who hit you and strips all tire traction.",
                "Activation: Rival hit\nOffender: grip 0% for 2s",
                1,
                RogueliteAbilityVisualStyle.DRAFT));
        cards.add(revenge(
                RogueliteCardId.EMP_SNARE,
                "EMP Snare",
                "Launches a disruptive snare that forces the rival responsible for hitting you to brake without reversing.",
                "Activation: Rival hit\nOffender: full brake for 2s",
                2,
                RogueliteAbilityVisualStyle.SHIELD));
        cards.add(revenge(
                RogueliteCardId.VOID_ANCHOR,
                "Void Anchor",
                "Hurls a heavy energy anchor that forces the rival responsible for hitting you to brake without reversing.",
                "Activation: Rival hit\nOffender: full brake for 3s",
                3,
                RogueliteAbilityVisualStyle.RAM));
        cards.add(revenge(
                RogueliteCardId.SENSOR_JAMMER,
                "Blind Hex",
                "Blinds and weakens the rival who hit you for 20 seconds.",
                "Activation: Rival hit\nOffender for 20s: blind, +5% mass, -5% power/grip/aero",
                1,
                RogueliteAbilityVisualStyle.SHIELD));
        cards.add(revenge(
                RogueliteCardId.GRID_BLACKOUT,
                "Burden Hex",
                "Chains the rival who hit you to a heavier, weakened and blinded car for 30 seconds.",
                "Activation: Rival hit\nOffender for 30s: blind, +10% mass, -10% power/grip/aero",
                2,
                RogueliteAbilityVisualStyle.SHIELD));
        cards.add(revenge(
                RogueliteCardId.TOTAL_BLACKOUT,
                "Doom Hex",
                "Crushes the rival who hit you with blindness, extreme weight, and severe performance loss for 40 seconds.",
                "Activation: Rival hit\nOffender for 40s: blind, +20% mass, -20% power/grip/aero",
                3,
                RogueliteAbilityVisualStyle.SHIELD));
        cards.add(revenge(
                RogueliteCardId.TRIAD_COUP,
                "Triad Coup",
                "Binds the offender and the car directly behind you, then reverses their places while moving you to the front.",
                "Activation: Rival hit\nAfter 2s: you lead | leading rival falls last",
                3,
                RogueliteAbilityVisualStyle.SHIELD));
        cards.add(revenge(
                RogueliteCardId.LOADED_GRUDGE,
                "Loaded Grudge",
                "A rival hit executes a random Tier 1 Revenge card, then prepares a different retaliation.",
                "Activation: Rival hit\nRandom Tier 1 Revenge",
                1,
                RogueliteAbilityVisualStyle.DRAFT));
        cards.add(revenge(
                RogueliteCardId.CHAOS_RETORT,
                "Chaos Retort",
                "A rival hit executes a random Tier 2 Revenge card, then prepares a different retaliation.",
                "Activation: Rival hit\nRandom Tier 2 Revenge",
                2,
                RogueliteAbilityVisualStyle.SHIELD));
        cards.add(revenge(
                RogueliteCardId.FATES_REVENGE,
                "Fate's Revenge",
                "A rival hit executes a random Tier 3 Revenge card, then prepares a different retaliation.",
                "Activation: Rival hit\nRandom Tier 3 Revenge",
                3,
                RogueliteAbilityVisualStyle.RAM));
        cards.add(revenge(
                RogueliteCardId.TELEMETRY_THEFT,
                "Telemetry Theft",
                "For 5 seconds after a rival hit, the offender's Tuning and Technique cards are disabled and any new lap XP they earn flows to you.",
                "Activation: Rival hit\nDisable Tuning + Technique | Steal lap XP | 5s",
                1,
                RogueliteAbilityVisualStyle.SHIELD));
        cards.add(revenge(
                RogueliteCardId.BUILD_HEIST,
                "Build Heist",
                "For 10 seconds after a rival hit, the offender's Tuning and Technique cards are disabled and any new lap XP they earn flows to you.",
                "Activation: Rival hit\nDisable Tuning + Technique | Steal lap XP | 10s",
                2,
                RogueliteAbilityVisualStyle.SHIELD));
        cards.add(revenge(
                RogueliteCardId.APEX_PLUNDER,
                "Apex Plunder",
                "For 15 seconds after a rival hit, the offender's Tuning and Technique cards are disabled and any new lap XP they earn flows to you.",
                "Activation: Rival hit\nDisable Tuning + Technique | Steal lap XP | 15s",
                3,
                RogueliteAbilityVisualStyle.SHIELD));
        cards.add(revenge(
                RogueliteCardId.FINAL_RECKONING,
                "Final Reckoning",
                "A rival hit immediately marks the offender for a 15-second hunt. You gain power and impact control while every nearby car rams the target and transfers stolen lap XP to you.",
                "Activation: Rival hit\n15s hunt | Power +50% | Recoil -150% | Push +150% | XP",
                4,
                RogueliteAbilityVisualStyle.RAM));

        List<RogueliteCardDefinition> retiredCards =
                new ArrayList<RogueliteCardDefinition>();
        retiredCards.add(powerup(
                RogueliteCardId.GRID_LINK,
                "Grid Link",
                "An always-on antenna imports the two strongest missing Tuning attributes and the strongest missing Technique from linked cars.",
                "Always active\nImport 2 Tuning + 1 Technique",
                3,
                RogueliteAbilityVisualStyle.ANTENNA_T3));
        retiredCards.add(card(
                RogueliteCardId.TECHNIQUE_COUPLER,
                "Technique Coupler",
                "A calibrated control unit strengthens the equipped Technique effect.",
                "Technique effects x1.25",
                1,
                RogueliteSlotType.TUNING));
        retiredCards.add(card(
                RogueliteCardId.TECHNIQUE_MATRIX,
                "Technique Matrix",
                "A racing control matrix greatly strengthens the equipped Technique effect.",
                "Technique effects x1.5",
                2,
                RogueliteSlotType.TUNING));
        retiredCards.add(card(
                RogueliteCardId.POWERUP_LINK,
                "Powerup Link",
                "A passive timing link strengthens Powerups and recharges them faster.",
                "Activation: Passive\nPowerup effects x1.25\nCooldown recovery x1.25",
                1,
                RogueliteSlotType.TECHNIQUE));
        retiredCards.add(card(
                RogueliteCardId.POWERUP_MATRIX,
                "Powerup Matrix",
                "A passive timing matrix strengthens Powerups and recharges them much faster.",
                "Activation: Passive\nPowerup effects x1.5\nCooldown recovery x1.5",
                2,
                RogueliteSlotType.TECHNIQUE));
        retiredCards.add(powerup(
                RogueliteCardId.GRUDGE_SPARK,
                "Grudge Spark",
                "A green catalyst ignites whenever Revenge activates and strengthens its real effect.",
                "Revenge activation: effect x1.25",
                1,
                RogueliteAbilityVisualStyle.REVENGE_BOOST_T1));
        retiredCards.add(powerup(
                RogueliteCardId.VENGEANCE_CORE,
                "Vengeance Core",
                "A stronger green core surges whenever Revenge activates and magnifies its outcome.",
                "Revenge activation: effect x1.5",
                2,
                RogueliteAbilityVisualStyle.REVENGE_BOOST_T2));

        Map<RogueliteCardId, RogueliteCardDefinition> cardsById =
                new EnumMap<RogueliteCardId, RogueliteCardDefinition>(RogueliteCardId.class);
        for (int i = 0; i < cards.size(); i++) {
            RogueliteCardDefinition definition = cards.get(i);
            if (cardsById.put(definition.getId(), definition) != null) {
                throw new IllegalStateException(
                        "Duplicate roguelite card ID: " + definition.getId());
            }
        }
        for (int i = 0; i < retiredCards.size(); i++) {
            RogueliteCardDefinition definition = retiredCards.get(i);
            if (cardsById.put(definition.getId(), definition) != null) {
                throw new IllegalStateException(
                        "Duplicate retired roguelite card ID: " + definition.getId());
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
        if ("IMPACT_FOCUS".equals(normalized)) {
            return RogueliteCardId.RALLY_FOCUS;
        }
        if ("IMPACT_EXPERT".equals(normalized)) {
            return RogueliteCardId.RALLY_EXPERT;
        }
        if ("IMPACT_MASTER".equals(normalized)) {
            return RogueliteCardId.RALLY_MASTER;
        }
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
            return RogueliteCardId.CORNER_FOCUS;
        }
        if ("QUICK_RACK".equals(normalized)) {
            return RogueliteCardId.DRAFT_FOCUS;
        }
        if ("STORM_TIRES".equals(normalized)
                || "RECOVERY_DIFFERENTIAL".equals(normalized)) {
            return RogueliteCardId.RALLY_EXPERT;
        }
        if ("TRAIL_BRAKE_BATTERY".equals(normalized)) {
            return RogueliteCardId.DRIFT_EXPERT;
        }
        if ("IMPACT_GYROSCOPE".equals(normalized)) {
            return RogueliteCardId.RALLY_MASTER;
        }
        if ("CORNER_EXIT".equals(normalized)) {
            return RogueliteCardId.CORNER_FOCUS;
        }
        if ("DRAFT_HUNTER".equals(normalized)) {
            return RogueliteCardId.DRAFT_FOCUS;
        }
        if ("CLEAN_MOMENTUM".equals(normalized)) {
            return RogueliteCardId.STRAIGHT_FOCUS;
        }
        if ("RECOVERY_LAUNCH".equals(normalized)) {
            return RogueliteCardId.RALLY_EXPERT;
        }
        if ("DRIFT_SLINGSHOT".equals(normalized)) {
            return RogueliteCardId.DRIFT_EXPERT;
        }
        if ("SLIPSTREAM_SLINGSHOT".equals(normalized)) {
            return RogueliteCardId.DRAFT_EXPERT;
        }
        if ("OVERTAKE_SURGE".equals(normalized)) {
            return RogueliteCardId.STRAIGHT_EXPERT;
        }
        if ("APEX_SLINGSHOT".equals(normalized)) {
            return RogueliteCardId.CORNER_MASTER;
        }
        if ("PERFECT_LAP".equals(normalized)) {
            return RogueliteCardId.STRAIGHT_MASTER;
        }
        if ("RACECRAFT_MASTERY".equals(normalized)) {
            return RogueliteCardId.RALLY_MASTER;
        }
        if ("FRONT_SPLITTER".equals(normalized)) {
            return RogueliteCardId.GRIP_FAN;
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
        if (slotType == RogueliteSlotType.TECHNIQUE
                && !effectText.startsWith("Activation: ")) {
            throw new IllegalArgumentException(
                    "Technique card must start with its activation: " + id);
        }
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
        if (!effectText.startsWith("Activation: Rival hit\n")) {
            throw new IllegalArgumentException(
                    "Revenge card must start with rival-hit activation: " + id);
        }
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
            case CORNER_FOCUS:
                return 1;
            case DRAFT_FOCUS:
                return 2;
            case NITRO_PULSE:
                return 3;
            case ACE_HOTLINE:
                return 104;
            case TIME_RIPPLE:
                return 8;
            case MIRROR_DUO:
                return 19;
            case BULK_FIELD:
                return 120;
            case GRIP_FAN:
                return 4;
            case SPORT_TUNE:
                return 5;
            case STRAIGHT_FOCUS:
                return 6;
            case RALLY_FOCUS:
                return 7;
            case DRAFT_MAGNET:
                return 9;
            case RACE_TUNE:
                return 10;
            case DRIFT_FOCUS:
                return 11;
            case DRAFT_EXPERT:
                return 12;
            case PHASE_SHIELD:
                return 13;
            case ROCKET_EXHAUST:
                return 14;
            case PRIORITY_HOTLINE:
                return 105;
            case CHRONO_SHIFT:
                return 106;
            case MIRROR_TRIO:
                return 42;
            case TITAN_FIELD:
                return 121;
            case HEAVYWEIGHT_TUNE:
                return 15;
            case APEX_FOCUS:
                return 16;
            case CORNER_EXPERT:
                return 17;
            case GRAVITY_WELL:
                return 18;
            case OVERDRIVE_COIL:
                return 43;
            case COLOSSUS_FIELD:
                return 122;
            case TUNE_LINK:
                return 123;
            case TECHNIQUE_LINK:
                return 124;
            case GRID_LINK:
                return 125;
            case TIER_FOUR_SIGNAL:
                return 126;
            case CHAMPIONSHIP_TUNE:
                return 20;
            case STRAIGHT_EXPERT:
                return 21;
            case DRIFT_EXPERT:
                return 22;
            case RALLY_EXPERT:
                return 84;
            case SPRINT_FOCUS:
                return 85;
            case CORNER_MASTER:
                return 86;
            case DRAFT_MASTER:
                return 87;
            case STRAIGHT_MASTER:
                return 88;
            case DRIFT_MASTER:
                return 89;
            case RALLY_MASTER:
                return 90;
            case SLIDE_FOCUS:
                return 91;
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
            case TEMPORAL_DOMINION:
                return 107;
            case CROWN_ENGINE:
                return 24;
            case AERO_TRIM:
                return 25;
            case SHORT_GEARING:
                return 26;
            case CARBON_PANELS:
                return 50;
            case FEATHERWEIGHT_DRIVE:
                return 62;
            case TRACK_WING:
                return 63;
            case GROUNDED_AERO:
                return 64;
            case LIGHT_COMPOUND:
                return 65;
            case AGILE_CHASSIS:
                return 66;
            case STREAMLINED_CHASSIS:
                return 67;
            case LOW_DRAG_FEATHERWEIGHT:
                return 68;
            case LOW_DRAG_BODY:
                return 27;
            case DRIFT_DIFFERENTIAL:
                return 28;
            case CARBON_MONOCOQUE:
                return 51;
            case TITANIUM_DRIVE:
                return 69;
            case DOWNFORCE_PACKAGE:
                return 70;
            case GROUNDED_DOWNFORCE:
                return 71;
            case MAGNESIUM_SUSPENSION:
                return 72;
            case AERO_AGILE_CHASSIS:
                return 73;
            case CARBON_LONGTAIL:
                return 74;
            case VENTURI_MONOCOQUE:
                return 75;
            case GROUND_EFFECT:
                return 29;
            case VELOCITY_SHELL:
                return 30;
            case TORQUE_VECTORING:
                return 31;
            case GRAPHENE_CHASSIS:
                return 52;
            case TITANIUM_SKELETON:
                return 76;
            case HYPERCAR_CORE:
                return 77;
            case ACTIVE_AERO_SHELL:
                return 78;
            case CARBON_PROTOTYPE:
                return 79;
            case TRACK_VACUUM:
                return 80;
            case WING_CAR:
                return 81;
            case FEATHERWEIGHT_GROUND_EFFECT:
                return 82;
            case RECOVERY_BEACON:
                return 32;
            case DRAFT_VENDETTA:
                return 33;
            case PAYBACK_SHIELD:
                return 34;
            case REPULSOR_WAVE:
                return 92;
            case HUNTER_BARRAGE:
                return 93;
            case HUNTER_STORM:
                return 103;
            case GRUDGE_SPARK:
                return 94;
            case VENGEANCE_CORE:
                return 95;
            case NEMESIS_ENGINE:
                return 96;
            case APEX_EXPERT:
                return 97;
            case SPRINT_EXPERT:
                return 98;
            case SLIDE_EXPERT:
                return 99;
            case APEX_MASTER:
                return 100;
            case SPRINT_MASTER:
                return 101;
            case SLIDE_MASTER:
                return 102;
            case TRACTION_FOCUS:
                return 108;
            case TRACTION_EXPERT:
                return 109;
            case TRACTION_MASTER:
                return 110;
            case AGILITY_FOCUS:
                return 111;
            case AGILITY_EXPERT:
                return 112;
            case AGILITY_MASTER:
                return 113;
            case LAP_DIVIDEND:
                return 127;
            case LAP_BOOSTER:
                return 128;
            case LAP_DOUBLER:
                return 129;
            case TELEMETRY_THEFT:
                return 130;
            case BUILD_HEIST:
                return 131;
            case APEX_PLUNDER:
                return 132;
            case FINAL_RECKONING:
                return 133;
            case TECHNIQUE_COUPLER:
                return 114;
            case TECHNIQUE_MATRIX:
                return 115;
            case TECHNIQUE_SINGULARITY:
                return 116;
            case POWERUP_LINK:
                return 117;
            case POWERUP_MATRIX:
                return 118;
            case POWERUP_NEXUS:
                return 119;
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
            case TRIAD_COUP:
                return 83;
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
