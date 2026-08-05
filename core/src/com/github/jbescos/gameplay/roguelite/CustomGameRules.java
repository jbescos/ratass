package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Rules selected for a custom run. */
public final class CustomGameRules {
    public static final int DEFAULT_LAPS = 3;
    public static final int DEFAULT_LEVEL_XP_INCREMENT = 40;
    public static final int DEFAULT_CHAMPIONSHIP_COUNT = 3;
    public static final int DEFAULT_ELIMINATIONS_PER_CHAMPIONSHIP = 3;
    public static final int MIN_LAPS = 1;
    public static final int MAX_LAPS = 20;
    public static final int MIN_LEVEL_XP_INCREMENT = 0;
    public static final int MAX_LEVEL_XP_INCREMENT = 200;
    public static final int MIN_CHAMPIONSHIP_COUNT = 1;
    public static final int MAX_CHAMPIONSHIP_COUNT = 10;
    public static final int MIN_ELIMINATIONS_PER_CHAMPIONSHIP = 0;
    public static final int MAX_ELIMINATIONS_PER_CHAMPIONSHIP = 9;

    private final EnumSet<RogueliteSlotType> cardTypes =
            EnumSet.allOf(RogueliteSlotType.class);
    private final EnumSet<WeatherType> weatherTypes =
            EnumSet.allOf(WeatherType.class);
    private final Set<Integer> tiers = new LinkedHashSet<Integer>();
    private final Set<String> mapIds = new LinkedHashSet<String>();
    private int laps = DEFAULT_LAPS;
    private int levelXpIncrement = DEFAULT_LEVEL_XP_INCREMENT;
    private int championshipCount = DEFAULT_CHAMPIONSHIP_COUNT;
    private int eliminationsPerChampionship =
            DEFAULT_ELIMINATIONS_PER_CHAMPIONSHIP;

    public CustomGameRules() {
        selectAllTiers();
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
        championshipCount = source.championshipCount;
        eliminationsPerChampionship = source.eliminationsPerChampionship;
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

    public int getChampionshipCount() {
        return championshipCount;
    }

    public void setChampionshipCount(int value) {
        championshipCount =
                clamp(value, MIN_CHAMPIONSHIP_COUNT, MAX_CHAMPIONSHIP_COUNT);
    }

    public int getEliminationsPerChampionship() {
        return eliminationsPerChampionship;
    }

    public void setEliminationsPerChampionship(int value) {
        eliminationsPerChampionship =
                clamp(
                        value,
                        MIN_ELIMINATIONS_PER_CHAMPIONSHIP,
                        MAX_ELIMINATIONS_PER_CHAMPIONSHIP);
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
        snapshot.championshipCount = championshipCount;
        snapshot.eliminationsPerChampionship = eliminationsPerChampionship;
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
        championshipCount = snapshot.championshipCount;
        eliminationsPerChampionship = snapshot.eliminationsPerChampionship;
        return true;
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
        public int championshipCount = DEFAULT_CHAMPIONSHIP_COUNT;
        public int eliminationsPerChampionship =
                DEFAULT_ELIMINATIONS_PER_CHAMPIONSHIP;

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
                    || championshipCount < MIN_CHAMPIONSHIP_COUNT
                    || championshipCount > MAX_CHAMPIONSHIP_COUNT
                    || eliminationsPerChampionship
                            < MIN_ELIMINATIONS_PER_CHAMPIONSHIP
                    || eliminationsPerChampionship
                            > MAX_ELIMINATIONS_PER_CHAMPIONSHIP) {
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
