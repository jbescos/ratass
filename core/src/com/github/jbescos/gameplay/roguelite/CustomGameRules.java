package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Rules selected for a custom run. */
public final class CustomGameRules {
    public static final int DEFAULT_LAPS = 5;
    public static final int DEFAULT_LEVEL_XP_INCREMENT = 2;
    public static final int MIN_LAPS = 1;
    public static final int MAX_LAPS = 20;
    public static final int MIN_LEVEL_XP_INCREMENT = 0;
    public static final int MAX_LEVEL_XP_INCREMENT = 200;
    public static final int MIN_RACECRAFT_XP_PER_LAP_CAP = 5;
    public static final int MAX_RACECRAFT_XP_PER_LAP_CAP = 200;
    public static final int MIN_RACECRAFT_XP_AWARD = 0;
    public static final int MAX_RACECRAFT_XP_AWARD = 50;

    private final EnumSet<RogueliteSlotType> cardTypes =
            EnumSet.allOf(RogueliteSlotType.class);
    private final EnumSet<WeatherType> weatherTypes =
            EnumSet.allOf(WeatherType.class);
    private final Set<Integer> tiers = new LinkedHashSet<Integer>();
    private final Set<String> mapIds = new LinkedHashSet<String>();
    private int laps = DEFAULT_LAPS;
    private int levelXpIncrement = DEFAULT_LEVEL_XP_INCREMENT;
    private int racecraftXpPerLapCap =
            RogueliteExperienceAwards.MAX_RACECRAFT_XP_PER_LAP;
    private final int[] racecraftXpAwards =
            new int[RogueliteExperienceAwards.Reason.values().length];

    public CustomGameRules() {
        selectAllTiers();
        resetRacecraftXpAwards();
    }

    public CustomGameRules(CustomGameRules source) {
        this();
        if (source == null) {
            return;
        }
        cardTypes.clear();
        cardTypes.addAll(source.cardTypes);
        weatherTypes.clear();
        weatherTypes.addAll(source.weatherTypes);
        tiers.clear();
        tiers.addAll(source.tiers);
        mapIds.addAll(source.mapIds);
        laps = source.laps;
        levelXpIncrement = source.levelXpIncrement;
        racecraftXpPerLapCap = source.racecraftXpPerLapCap;
        System.arraycopy(
                source.racecraftXpAwards,
                0,
                racecraftXpAwards,
                0,
                racecraftXpAwards.length);
    }

    public CustomGameRules copy() {
        return new CustomGameRules(this);
    }

    public void resetMaps(Iterable<String> availableMapIds) {
        mapIds.clear();
        addMaps(availableMapIds);
    }

    public void reconcileMaps(Iterable<String> availableMapIds) {
        LinkedHashSet<String> available = collectMaps(availableMapIds);
        mapIds.retainAll(available);
        if (mapIds.isEmpty()) {
            mapIds.addAll(available);
        }
    }

    public boolean toggleCardType(RogueliteSlotType type) {
        if (type == null) {
            return false;
        }
        if (cardTypes.contains(type)) {
            if (cardTypes.size() == 1) {
                return false;
            }
            cardTypes.remove(type);
        } else {
            cardTypes.add(type);
        }
        return true;
    }

    public boolean isCardTypeAllowed(RogueliteSlotType type) {
        return type != null && cardTypes.contains(type);
    }

    public boolean toggleWeather(WeatherType weather) {
        if (weather == null) {
            return false;
        }
        if (weatherTypes.contains(weather)) {
            if (weatherTypes.size() == 1) {
                return false;
            }
            weatherTypes.remove(weather);
        } else {
            weatherTypes.add(weather);
        }
        return true;
    }

    public boolean isWeatherAllowed(WeatherType weather) {
        return weather != null && weatherTypes.contains(weather);
    }

    public int getWeatherCount() {
        return weatherTypes.size();
    }

    public boolean toggleTier(int tier) {
        if (tier < 1 || tier > DriverProfileCatalog.MAX_TIER) {
            return false;
        }
        Integer value = Integer.valueOf(tier);
        if (tiers.contains(value)) {
            if (tiers.size() == 1) {
                return false;
            }
            tiers.remove(value);
        } else {
            tiers.add(value);
        }
        return true;
    }

    public boolean isTierAllowed(int tier) {
        return tiers.contains(Integer.valueOf(tier));
    }

    public int resolveTier(int naturalTier) {
        int clamped = Math.max(1, Math.min(DriverProfileCatalog.MAX_TIER, naturalTier));
        for (int tier = clamped; tier >= 1; tier--) {
            if (isTierAllowed(tier)) {
                return tier;
            }
        }
        for (int tier = clamped + 1; tier <= DriverProfileCatalog.MAX_TIER; tier++) {
            if (isTierAllowed(tier)) {
                return tier;
            }
        }
        return 1;
    }

    public boolean toggleMap(String mapId) {
        if (mapId == null || mapId.length() == 0) {
            return false;
        }
        if (mapIds.contains(mapId)) {
            if (mapIds.size() == 1) {
                return false;
            }
            mapIds.remove(mapId);
        } else {
            mapIds.add(mapId);
        }
        return true;
    }

    public boolean isMapAllowed(String mapId) {
        return mapIds.contains(mapId);
    }

    public List<String> getMapIds() {
        return Collections.unmodifiableList(new ArrayList<String>(mapIds));
    }

    public int getLaps() {
        return laps;
    }

    public void setLaps(int value) {
        laps = clamp(value, MIN_LAPS, MAX_LAPS);
    }

    public int getLevelXpIncrement() {
        return levelXpIncrement;
    }

    public void setLevelXpIncrement(int value) {
        levelXpIncrement =
                clamp(value, MIN_LEVEL_XP_INCREMENT, MAX_LEVEL_XP_INCREMENT);
    }

    public int getRacecraftXpPerLapCap() {
        return racecraftXpPerLapCap;
    }

    public void setRacecraftXpPerLapCap(int value) {
        racecraftXpPerLapCap =
                clamp(
                        value,
                        MIN_RACECRAFT_XP_PER_LAP_CAP,
                        MAX_RACECRAFT_XP_PER_LAP_CAP);
    }

    public int getRacecraftXpAward(RogueliteExperienceAwards.Reason reason) {
        if (reason == null || !reason.isCustomizable()) {
            return 0;
        }
        return racecraftXpAwards[reason.ordinal()];
    }

    public void setRacecraftXpAward(
            RogueliteExperienceAwards.Reason reason,
            int value) {
        if (reason == null || !reason.isCustomizable()) {
            return;
        }
        racecraftXpAwards[reason.ordinal()] =
                clamp(value, MIN_RACECRAFT_XP_AWARD, MAX_RACECRAFT_XP_AWARD);
    }

    public Snapshot snapshot() {
        Snapshot snapshot = new Snapshot();
        for (RogueliteSlotType type : RogueliteSlotType.values()) {
            if (cardTypes.contains(type)) {
                snapshot.cardTypes.add(type.name());
            }
        }
        for (WeatherType weather : WeatherType.values()) {
            if (weatherTypes.contains(weather)) {
                snapshot.weatherTypes.add(weather.name());
            }
        }
        snapshot.tiers.addAll(tiers);
        snapshot.mapIds.addAll(mapIds);
        snapshot.laps = laps;
        snapshot.levelXpIncrement = levelXpIncrement;
        snapshot.racecraftXpPerLapCap = racecraftXpPerLapCap;
        for (RogueliteExperienceAwards.Reason reason
                : RogueliteExperienceAwards.Reason.values()) {
            if (reason.isCustomizable()) {
                snapshot.racecraftXpAwards.add(
                        Integer.valueOf(getRacecraftXpAward(reason)));
            }
        }
        return snapshot;
    }

    public boolean restore(Snapshot snapshot, Iterable<String> availableMapIds) {
        if (snapshot == null || !snapshot.isStructurallyValid()) {
            return false;
        }
        EnumSet<RogueliteSlotType> restoredCardTypes =
                EnumSet.noneOf(RogueliteSlotType.class);
        EnumSet<WeatherType> restoredWeatherTypes =
                EnumSet.noneOf(WeatherType.class);
        LinkedHashSet<Integer> restoredTiers = new LinkedHashSet<Integer>();
        try {
            for (String value : snapshot.cardTypes) {
                restoredCardTypes.add(RogueliteSlotType.valueOf(value));
            }
            for (String value : snapshot.weatherTypes) {
                restoredWeatherTypes.add(WeatherType.valueOf(value));
            }
            restoredTiers.addAll(snapshot.tiers);
        } catch (RuntimeException exception) {
            return false;
        }
        LinkedHashSet<String> available = collectMaps(availableMapIds);
        LinkedHashSet<String> restoredMaps = new LinkedHashSet<String>();
        for (String mapId : snapshot.mapIds) {
            if (available.contains(mapId)) {
                restoredMaps.add(mapId);
            }
        }
        if (restoredCardTypes.isEmpty()
                || restoredWeatherTypes.isEmpty()
                || restoredTiers.isEmpty()
                || restoredMaps.isEmpty()) {
            return false;
        }
        int customizableAwardCount = 0;
        for (RogueliteExperienceAwards.Reason reason
                : RogueliteExperienceAwards.Reason.values()) {
            if (reason.isCustomizable()) {
                customizableAwardCount++;
            }
        }
        if (snapshot.racecraftXpAwards == null
                || snapshot.racecraftXpAwards.size() != customizableAwardCount) {
            return false;
        }
        for (Integer award : snapshot.racecraftXpAwards) {
            if (award == null
                    || award.intValue() < MIN_RACECRAFT_XP_AWARD
                    || award.intValue() > MAX_RACECRAFT_XP_AWARD) {
                return false;
            }
        }
        cardTypes.clear();
        cardTypes.addAll(restoredCardTypes);
        weatherTypes.clear();
        weatherTypes.addAll(restoredWeatherTypes);
        tiers.clear();
        tiers.addAll(restoredTiers);
        mapIds.clear();
        mapIds.addAll(restoredMaps);
        laps = snapshot.laps;
        levelXpIncrement = snapshot.levelXpIncrement;
        racecraftXpPerLapCap = snapshot.racecraftXpPerLapCap;
        int awardIndex = 0;
        for (RogueliteExperienceAwards.Reason reason
                : RogueliteExperienceAwards.Reason.values()) {
            if (reason.isCustomizable()) {
                racecraftXpAwards[reason.ordinal()] =
                        snapshot.racecraftXpAwards.get(awardIndex++).intValue();
            }
        }
        return true;
    }

    private void resetRacecraftXpAwards() {
        for (RogueliteExperienceAwards.Reason reason
                : RogueliteExperienceAwards.Reason.values()) {
            racecraftXpAwards[reason.ordinal()] = reason.getDefaultExperience();
        }
    }

    private void selectAllTiers() {
        for (int tier = 1; tier <= DriverProfileCatalog.MAX_TIER; tier++) {
            tiers.add(Integer.valueOf(tier));
        }
    }

    private void addMaps(Iterable<String> values) {
        mapIds.addAll(collectMaps(values));
    }

    private static LinkedHashSet<String> collectMaps(Iterable<String> values) {
        LinkedHashSet<String> result = new LinkedHashSet<String>();
        if (values != null) {
            for (String value : values) {
                if (value != null && value.length() > 0) {
                    result.add(value);
                }
            }
        }
        return result;
    }

    private static int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    public enum WeatherType {
        SUNNY("Sunny"),
        RAIN("Rain"),
        SNOW("Snow");

        private final String displayName;

        WeatherType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public static final class Snapshot {
        public List<String> cardTypes = new ArrayList<String>();
        public List<String> weatherTypes = new ArrayList<String>();
        public List<Integer> tiers = new ArrayList<Integer>();
        public List<String> mapIds = new ArrayList<String>();
        public int laps = DEFAULT_LAPS;
        public int levelXpIncrement = DEFAULT_LEVEL_XP_INCREMENT;
        public int racecraftXpPerLapCap =
                RogueliteExperienceAwards.MAX_RACECRAFT_XP_PER_LAP;
        public List<Integer> racecraftXpAwards = new ArrayList<Integer>();

        public boolean isStructurallyValid() {
            if (cardTypes == null
                    || cardTypes.isEmpty()
                    || weatherTypes == null
                    || weatherTypes.isEmpty()
                    || tiers == null
                    || tiers.isEmpty()
                    || mapIds == null
                    || mapIds.isEmpty()
                    || laps < MIN_LAPS
                    || laps > MAX_LAPS
                    || levelXpIncrement < MIN_LEVEL_XP_INCREMENT
                    || levelXpIncrement > MAX_LEVEL_XP_INCREMENT
                    || racecraftXpPerLapCap < MIN_RACECRAFT_XP_PER_LAP_CAP
                    || racecraftXpPerLapCap > MAX_RACECRAFT_XP_PER_LAP_CAP
                    || racecraftXpAwards == null) {
                return false;
            }
            for (int i = 0; i < tiers.size(); i++) {
                Integer tier = tiers.get(i);
                if (tier == null
                        || tier.intValue() < 1
                        || tier.intValue() > DriverProfileCatalog.MAX_TIER
                        || tiers.indexOf(tier) != i) {
                    return false;
                }
            }
            return hasUniqueValues(cardTypes)
                    && hasUniqueValues(weatherTypes)
                    && hasUniqueValues(mapIds);
        }

        private static boolean hasUniqueValues(List<String> values) {
            for (int i = 0; i < values.size(); i++) {
                String value = values.get(i);
                if (value == null
                        || value.length() == 0
                        || values.indexOf(value) != i) {
                    return false;
                }
            }
            return true;
        }
    }
}
