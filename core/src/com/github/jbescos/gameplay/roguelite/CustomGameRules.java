package com.github.jbescos.gameplay.roguelite;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/** Rules selected for a custom run. */
public final class CustomGameRules {
    public static final int MAX_CONFIGURABLE_CARD_TIER = 3;
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
    public static final int MIN_TIER_UNLOCK_LEVEL = 1;
    public static final int MAX_TIER_UNLOCK_LEVEL = 99;

    private final boolean[][] tierCardTypes =
            new boolean[RogueliteCardCatalog.MAX_CARD_TIER][RogueliteSlotType.values().length];
    private final int[] tierUnlockLevels = {1, 10, 20};
    private final EnumSet<WeatherType> weatherTypes =
            EnumSet.allOf(WeatherType.class);
    private final Set<String> mapIds = new LinkedHashSet<String>();
    private int laps = DEFAULT_LAPS;
    private int levelXpIncrement = DEFAULT_LEVEL_XP_INCREMENT;
    private int racecraftXpPerLapCap =
            RogueliteExperienceAwards.MAX_RACECRAFT_XP_PER_LAP;
    private final int[] racecraftXpAwards =
            new int[RogueliteExperienceAwards.Reason.values().length];

    public CustomGameRules() {
        selectAllTierCardTypes();
        resetRacecraftXpAwards();
    }

    public CustomGameRules(CustomGameRules source) {
        this();
        if (source == null) {
            return;
        }
        for (int tier = 0; tier < tierCardTypes.length; tier++) {
            System.arraycopy(
                    source.tierCardTypes[tier],
                    0,
                    tierCardTypes[tier],
                    0,
                    tierCardTypes[tier].length);
        }
        System.arraycopy(
                source.tierUnlockLevels,
                0,
                tierUnlockLevels,
                0,
                tierUnlockLevels.length);
        weatherTypes.clear();
        weatherTypes.addAll(source.weatherTypes);
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
        boolean enable = !isCardTypeAllowedInEveryTier(type);
        if (!enable) {
            int enabledCount = getEnabledTierCardTypeCount();
            int enabledForType = getEnabledTierCount(type);
            if (enabledCount == enabledForType) {
                return false;
            }
        }
        for (int tier = 1; tier <= MAX_CONFIGURABLE_CARD_TIER; tier++) {
            tierCardTypes[tier - 1][type.ordinal()] = enable;
        }
        return true;
    }

    public boolean isCardTypeAllowed(RogueliteSlotType type) {
        if (type == null) {
            return false;
        }
        for (int tier = 1; tier <= MAX_CONFIGURABLE_CARD_TIER; tier++) {
            if (isCardTypeAllowed(tier, type)) {
                return true;
            }
        }
        return false;
    }

    public boolean toggleTierCardType(int tier, RogueliteSlotType type) {
        if (!isConfigurableTier(tier) || type == null) {
            return false;
        }
        int tierIndex = tier - 1;
        int typeIndex = type.ordinal();
        if (tierCardTypes[tierIndex][typeIndex]
                && getEnabledTierCardTypeCount() == 1) {
            return false;
        }
        tierCardTypes[tierIndex][typeIndex] = !tierCardTypes[tierIndex][typeIndex];
        return true;
    }

    public boolean isCardTypeAllowed(int tier, RogueliteSlotType type) {
        if (tier == RogueliteCardCatalog.MAX_CARD_TIER) {
            return type != null;
        }
        return isValidTier(tier)
                && type != null
                && tierCardTypes[tier - 1][type.ordinal()];
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
        if (!isConfigurableTier(tier)) {
            return false;
        }
        boolean enable = !isTierAllowed(tier);
        if (!enable) {
            int enabledCount = getEnabledTierCardTypeCount();
            int enabledInTier = getEnabledCardTypeCount(tier);
            if (enabledCount == enabledInTier) {
                return false;
            }
        }
        for (int type = 0; type < tierCardTypes[tier - 1].length; type++) {
            tierCardTypes[tier - 1][type] = enable;
        }
        return true;
    }

    public boolean isTierAllowed(int tier) {
        return getEnabledCardTypeCount(tier) > 0;
    }

    public int resolveTier(int naturalTier) {
        int clamped = Math.max(1, Math.min(MAX_CONFIGURABLE_CARD_TIER, naturalTier));
        for (int tier = clamped; tier >= 1; tier--) {
            if (isTierAllowed(tier)) {
                return tier;
            }
        }
        for (int tier = clamped + 1; tier <= MAX_CONFIGURABLE_CARD_TIER; tier++) {
            if (isTierAllowed(tier)) {
                return tier;
            }
        }
        return 1;
    }

    public int resolveTierForLevel(int level, int minimumTier) {
        int safeLevel = Math.max(MIN_TIER_UNLOCK_LEVEL, level);
        int safeMinimumTier = Math.max(1, Math.min(DriverProfileCatalog.MAX_TIER, minimumTier));
        int resolvedTier = 0;
        for (int tier = safeMinimumTier; tier <= MAX_CONFIGURABLE_CARD_TIER; tier++) {
            if (isTierAllowed(tier)
                    && ((safeMinimumTier > 1 && tier == safeMinimumTier)
                            || safeLevel >= getTierUnlockLevel(tier))) {
                resolvedTier = tier;
            }
        }
        return resolvedTier;
    }

    public int getTierUnlockLevel(int tier) {
        if (!isConfigurableTier(tier)) {
            return MIN_TIER_UNLOCK_LEVEL;
        }
        return tierUnlockLevels[tier - 1];
    }

    public void setTierUnlockLevel(int tier, int level) {
        if (!isConfigurableTier(tier)) {
            return;
        }
        tierUnlockLevels[tier - 1] =
                clamp(level, MIN_TIER_UNLOCK_LEVEL, MAX_TIER_UNLOCK_LEVEL);
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
            if (isCardTypeAllowed(type)) {
                snapshot.cardTypes.add(type.name());
            }
        }
        for (WeatherType weather : WeatherType.values()) {
            if (weatherTypes.contains(weather)) {
                snapshot.weatherTypes.add(weather.name());
            }
        }
        for (int tier = 1; tier <= MAX_CONFIGURABLE_CARD_TIER; tier++) {
            if (isTierAllowed(tier)) {
                snapshot.tiers.add(Integer.valueOf(tier));
            }
            snapshot.tierUnlockLevels.add(Integer.valueOf(getTierUnlockLevel(tier)));
            for (RogueliteSlotType type : RogueliteSlotType.values()) {
                if (isCardTypeAllowed(tier, type)) {
                    snapshot.tierCardTypes.add(tierCardTypeKey(tier, type));
                }
            }
        }
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
        boolean[][] restoredTierCardTypes =
                new boolean[RogueliteCardCatalog.MAX_CARD_TIER][RogueliteSlotType.values().length];
        for (RogueliteSlotType type : RogueliteSlotType.values()) {
            restoredTierCardTypes[RogueliteCardCatalog.MAX_CARD_TIER - 1][type.ordinal()] = true;
        }
        if (snapshot.tierCardTypes == null || snapshot.tierCardTypes.isEmpty()) {
            for (Integer tier : restoredTiers) {
                for (RogueliteSlotType type : restoredCardTypes) {
                    restoredTierCardTypes[tier.intValue() - 1][type.ordinal()] = true;
                }
            }
        } else {
            try {
                for (String key : snapshot.tierCardTypes) {
                    int separator = key.indexOf(':');
                    int tier = Integer.parseInt(key.substring(0, separator));
                    RogueliteSlotType type =
                            RogueliteSlotType.valueOf(key.substring(separator + 1));
                    if (!isConfigurableTier(tier)) {
                        return false;
                    }
                    restoredTierCardTypes[tier - 1][type.ordinal()] = true;
                }
            } catch (RuntimeException exception) {
                return false;
            }
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
        for (int tier = 0; tier < tierCardTypes.length; tier++) {
            System.arraycopy(
                    restoredTierCardTypes[tier],
                    0,
                    tierCardTypes[tier],
                    0,
                    tierCardTypes[tier].length);
        }
        if (snapshot.tierUnlockLevels != null
                && snapshot.tierUnlockLevels.size() == MAX_CONFIGURABLE_CARD_TIER) {
            for (int tier = 1; tier <= MAX_CONFIGURABLE_CARD_TIER; tier++) {
                setTierUnlockLevel(
                        tier,
                        snapshot.tierUnlockLevels.get(tier - 1).intValue());
            }
        }
        weatherTypes.clear();
        weatherTypes.addAll(restoredWeatherTypes);
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

    private void selectAllTierCardTypes() {
        for (int tier = 1; tier <= RogueliteCardCatalog.MAX_CARD_TIER; tier++) {
            for (RogueliteSlotType type : RogueliteSlotType.values()) {
                tierCardTypes[tier - 1][type.ordinal()] = true;
            }
        }
    }

    private boolean isCardTypeAllowedInEveryTier(RogueliteSlotType type) {
        return getEnabledTierCount(type) == MAX_CONFIGURABLE_CARD_TIER;
    }

    private int getEnabledTierCount(RogueliteSlotType type) {
        int count = 0;
        for (int tier = 1; tier <= MAX_CONFIGURABLE_CARD_TIER; tier++) {
            if (isCardTypeAllowed(tier, type)) {
                count++;
            }
        }
        return count;
    }

    private int getEnabledCardTypeCount(int tier) {
        if (!isValidTier(tier)) {
            return 0;
        }
        int count = 0;
        for (boolean enabled : tierCardTypes[tier - 1]) {
            if (enabled) {
                count++;
            }
        }
        return count;
    }

    private int getEnabledTierCardTypeCount() {
        int count = 0;
        for (int tier = 1; tier <= MAX_CONFIGURABLE_CARD_TIER; tier++) {
            count += getEnabledCardTypeCount(tier);
        }
        return count;
    }

    private static boolean isValidTier(int tier) {
        return tier >= 1 && tier <= RogueliteCardCatalog.MAX_CARD_TIER;
    }

    private static boolean isConfigurableTier(int tier) {
        return tier >= 1 && tier <= MAX_CONFIGURABLE_CARD_TIER;
    }

    private static String tierCardTypeKey(int tier, RogueliteSlotType type) {
        return tier + ":" + type.name();
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
        public List<String> tierCardTypes = new ArrayList<String>();
        public List<Integer> tierUnlockLevels = new ArrayList<Integer>();
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
            if (tierCardTypes != null && !hasUniqueValues(tierCardTypes)) {
                return false;
            }
            if (tierUnlockLevels != null && !tierUnlockLevels.isEmpty()) {
                if (tierUnlockLevels.size() != MAX_CONFIGURABLE_CARD_TIER) {
                    return false;
                }
                for (Integer level : tierUnlockLevels) {
                    if (level == null
                            || level.intValue() < MIN_TIER_UNLOCK_LEVEL
                            || level.intValue() > MAX_TIER_UNLOCK_LEVEL) {
                        return false;
                    }
                }
            }
            for (int i = 0; i < tiers.size(); i++) {
                Integer tier = tiers.get(i);
                if (tier == null
                        || tier.intValue() < 1
                        || tier.intValue() > MAX_CONFIGURABLE_CARD_TIER
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
