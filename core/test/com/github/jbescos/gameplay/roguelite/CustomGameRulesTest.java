package com.github.jbescos.gameplay.roguelite;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.github.jbescos.gameplay.roguelite.CustomGameRules.WeatherType;
import java.util.Arrays;
import org.junit.Test;

public class CustomGameRulesTest {
    @Test
    public void defaultsMatchTheStandardRun() {
        CustomGameRules rules = new CustomGameRules();
        rules.resetMaps(Arrays.asList("map000", "map001"));

        for (RogueliteSlotType type : RogueliteSlotType.values()) {
            assertTrue(rules.isCardTypeAllowed(type));
        }
        for (WeatherType weather : WeatherType.values()) {
            assertTrue(rules.isWeatherAllowed(weather));
        }
        for (int tier = 1; tier <= DriverProfileCatalog.MAX_TIER; tier++) {
            assertTrue(rules.isTierAllowed(tier));
        }
        assertEquals(3, rules.getLaps());
        assertEquals(40, rules.getLevelXpIncrement());
        assertEquals(3, rules.getChampionshipCount());
        assertEquals(3, rules.getEliminationsPerChampionship());
    }

    @Test
    public void finalChoiceInRequiredGroupsCannotBeDisabled() {
        CustomGameRules rules = new CustomGameRules();
        rules.resetMaps(Arrays.asList("map000", "map001"));

        for (int i = 1; i < RogueliteSlotType.values().length; i++) {
            assertTrue(rules.toggleCardType(RogueliteSlotType.values()[i]));
        }
        assertFalse(rules.toggleCardType(RogueliteSlotType.DRIVER));
        assertTrue(rules.toggleWeather(WeatherType.RAIN));
        assertTrue(rules.toggleWeather(WeatherType.SNOW));
        assertFalse(rules.toggleWeather(WeatherType.SUNNY));
        assertTrue(rules.toggleTier(2));
        assertTrue(rules.toggleTier(3));
        assertFalse(rules.toggleTier(1));
        assertTrue(rules.toggleMap("map001"));
        assertFalse(rules.toggleMap("map000"));
    }

    @Test
    public void snapshotRestoresAndFiltersUnavailableMaps() {
        CustomGameRules original = new CustomGameRules();
        original.resetMaps(Arrays.asList("map000", "map001", "map002"));
        original.toggleMap("map001");
        original.setLaps(7);
        original.setLevelXpIncrement(90);
        original.setChampionshipCount(5);
        original.setEliminationsPerChampionship(2);

        CustomGameRules restored = new CustomGameRules();

        assertTrue(
                restored.restore(
                        original.snapshot(),
                        Arrays.asList("map000", "map002")));
        assertEquals(Arrays.asList("map000", "map002"), restored.getMapIds());
        assertEquals(7, restored.getLaps());
        assertEquals(90, restored.getLevelXpIncrement());
        assertEquals(5, restored.getChampionshipCount());
        assertEquals(2, restored.getEliminationsPerChampionship());
    }
}
