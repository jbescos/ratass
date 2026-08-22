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
            for (int tier = 1; tier <= CustomGameRules.MAX_CONFIGURABLE_CARD_TIER; tier++) {
                assertTrue(rules.isCardTypeAllowed(tier, type));
            }
            assertTrue(rules.isCardTypeAllowed(4, type));
        }
        assertEquals(1, rules.getTierUnlockLevel(1));
        assertEquals(10, rules.getTierUnlockLevel(2));
        assertEquals(20, rules.getTierUnlockLevel(3));
        assertEquals(1, rules.getTierUnlockLevel(4));
        for (WeatherType weather : WeatherType.values()) {
            assertTrue(rules.isWeatherAllowed(weather));
        }
        for (int tier = 1; tier <= CustomGameRules.MAX_CONFIGURABLE_CARD_TIER; tier++) {
            assertTrue(rules.isTierAllowed(tier));
        }
        assertEquals(5, rules.getLaps());
        assertEquals(2, rules.getLevelXpIncrement());
        assertEquals(30, rules.getRacecraftXpPerLapCap());
        for (RogueliteExperienceAwards.Reason reason
                : RogueliteExperienceAwards.Reason.values()) {
            if (reason.isCustomizable()) {
                assertEquals(
                        reason.getDefaultExperience(),
                        rules.getRacecraftXpAward(reason));
            }
        }
    }

    @Test
    public void cardTypesCanBeConfiguredIndependentlyForEveryTier() {
        CustomGameRules rules = new CustomGameRules();

        assertTrue(rules.toggleTierCardType(2, RogueliteSlotType.POWERUP));

        assertTrue(rules.isCardTypeAllowed(1, RogueliteSlotType.POWERUP));
        assertFalse(rules.isCardTypeAllowed(2, RogueliteSlotType.POWERUP));
        assertTrue(rules.isCardTypeAllowed(3, RogueliteSlotType.POWERUP));
        assertTrue(rules.isCardTypeAllowed(RogueliteSlotType.POWERUP));
    }

    @Test
    public void customUnlockLevelsControlTheResolvedTier() {
        CustomGameRules rules = new CustomGameRules();
        rules.setTierUnlockLevel(1, 4);
        rules.setTierUnlockLevel(2, 8);
        rules.setTierUnlockLevel(3, 12);

        assertEquals(0, rules.resolveTierForLevel(3, 1));
        assertEquals(1, rules.resolveTierForLevel(4, 1));
        assertEquals(2, rules.resolveTierForLevel(8, 1));
        assertEquals(3, rules.resolveTierForLevel(12, 1));
        assertEquals(3, rules.resolveTierForLevel(30, 1));
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
        assertFalse(rules.toggleTier(4));
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
        original.setRacecraftXpPerLapCap(45);
        original.setRacecraftXpAward(RogueliteExperienceAwards.Reason.OVERTAKE, 7);
        original.setRacecraftXpAward(RogueliteExperienceAwards.Reason.DRIFT, 3);
        original.toggleTierCardType(2, RogueliteSlotType.REVENGE);
        original.setTierUnlockLevel(2, 14);

        CustomGameRules restored = new CustomGameRules();

        assertTrue(
                restored.restore(
                        original.snapshot(),
                        Arrays.asList("map000", "map002")));
        assertEquals(Arrays.asList("map000", "map002"), restored.getMapIds());
        assertEquals(7, restored.getLaps());
        assertEquals(90, restored.getLevelXpIncrement());
        assertEquals(45, restored.getRacecraftXpPerLapCap());
        assertFalse(restored.isCardTypeAllowed(2, RogueliteSlotType.REVENGE));
        assertTrue(restored.isCardTypeAllowed(1, RogueliteSlotType.REVENGE));
        assertEquals(14, restored.getTierUnlockLevel(2));
        assertEquals(
                7,
                restored.getRacecraftXpAward(
                        RogueliteExperienceAwards.Reason.OVERTAKE));
        assertEquals(
                3,
                restored.getRacecraftXpAward(
                        RogueliteExperienceAwards.Reason.DRIFT));
    }

    @Test
    public void numericSettingsAreClampedToSupportedRanges() {
        CustomGameRules rules = new CustomGameRules();

        rules.setRacecraftXpPerLapCap(-1);
        assertEquals(
                CustomGameRules.MIN_RACECRAFT_XP_PER_LAP_CAP,
                rules.getRacecraftXpPerLapCap());
        rules.setRacecraftXpPerLapCap(1000);
        assertEquals(
                CustomGameRules.MAX_RACECRAFT_XP_PER_LAP_CAP,
                rules.getRacecraftXpPerLapCap());

        rules.setRacecraftXpAward(RogueliteExperienceAwards.Reason.REVENGE, -1);
        assertEquals(
                CustomGameRules.MIN_RACECRAFT_XP_AWARD,
                rules.getRacecraftXpAward(RogueliteExperienceAwards.Reason.REVENGE));
        rules.setRacecraftXpAward(RogueliteExperienceAwards.Reason.REVENGE, 1000);
        assertEquals(
                CustomGameRules.MAX_RACECRAFT_XP_AWARD,
                rules.getRacecraftXpAward(RogueliteExperienceAwards.Reason.REVENGE));
    }
}
