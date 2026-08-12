package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import com.github.jbescos.gameplay.roguelite.CustomGameRules.WeatherType;
import com.github.jbescos.gameplay.roguelite.RogueliteCardCatalog;
import com.github.jbescos.gameplay.roguelite.RogueliteCardDefinition;
import com.github.jbescos.gameplay.roguelite.RogueliteExperienceAwards.Reason;
import com.github.jbescos.gameplay.roguelite.RogueliteSlotType;
import java.util.Locale;
import org.junit.Test;

public class GameTextTest {
    @Test
    public void usesRequestedSpanishCardCategoryNames() {
        assertEquals("Piloto", GameText.slotType(GameLanguage.SPANISH, RogueliteSlotType.DRIVER));
        assertEquals("Mejoras", GameText.slotType(GameLanguage.SPANISH, RogueliteSlotType.TUNING));
        assertEquals("Técnica", GameText.slotType(GameLanguage.SPANISH, RogueliteSlotType.TECHNIQUE));
        assertEquals("Potenciar", GameText.slotType(GameLanguage.SPANISH, RogueliteSlotType.POWERUP));
        assertEquals("Venganza", GameText.slotType(GameLanguage.SPANISH, RogueliteSlotType.REVENGE));
    }

    @Test
    public void translatesMenusWeatherAndExperienceReasons() {
        assertEquals("Nueva partida", GameText.translate(GameLanguage.SPANISH, "New Game"));
        assertEquals("Pantalla completa", GameText.translate(GameLanguage.SPANISH, "Fullscreen"));
        assertEquals("Lluvia", GameText.weather(GameLanguage.SPANISH, WeatherType.RAIN));
        assertEquals("Adelantamiento", GameText.experienceReason(GameLanguage.SPANISH, Reason.OVERTAKE));
        assertEquals("Nouvelle partie", GameText.translate(GameLanguage.FRENCH, "New Game"));
        assertEquals("Vollbild", GameText.translate(GameLanguage.GERMAN, "Fullscreen"));
        assertEquals("Pioggia", GameText.weather(GameLanguage.ITALIAN, WeatherType.RAIN));
        assertEquals("Überholen", GameText.experienceReason(GameLanguage.GERMAN, Reason.OVERTAKE));
    }

    @Test
    public void leavesInternalAndEnglishTextStable() {
        assertEquals("map018", GameText.translate(GameLanguage.SPANISH, "map018"));
        assertEquals("New Game", GameText.translate(GameLanguage.ENGLISH, "New Game"));
        assertEquals(GameLanguage.SPANISH, GameLanguage.fromStoredValue("es"));
        assertEquals(GameLanguage.FRENCH, GameLanguage.fromLocale(Locale.FRENCH));
        assertEquals(GameLanguage.GERMAN, GameLanguage.fromLocale(Locale.GERMAN));
        assertEquals(GameLanguage.ITALIAN, GameLanguage.fromLocale(Locale.ITALIAN));
        assertEquals(GameLanguage.ENGLISH, GameLanguage.fromLocale(Locale.JAPANESE));
        assertEquals(GameLanguage.ENGLISH, GameLanguage.fromStoredValue("unknown"));
    }

    @Test
    public void automaticLanguageUsesSupportedDeviceLocale() {
        Locale original = Locale.getDefault();
        try {
            Locale.setDefault(Locale.ITALIAN);
            assertEquals(GameLanguage.ITALIAN, GameLanguage.fromConfiguredValue("auto"));
            Locale.setDefault(Locale.JAPANESE);
            assertEquals(GameLanguage.ENGLISH, GameLanguage.fromConfiguredValue("auto"));
        } finally {
            Locale.setDefault(original);
        }
    }

    @Test
    public void translatesUppercaseAndDetailedCardText() {
        assertEquals(
                "PULSO NITRO",
                GameText.translate(GameLanguage.SPANISH, "NITRO PULSE"));
        assertEquals(
                "PULSO NITRO - Impulsa el coche cuando una recta libre permite usar nitro.",
                GameText.translate(
                        GameLanguage.SPANISH,
                        "NITRO PULSE - Kicks the car forward when open road invites a nitro burst."));
        assertEquals(
                "CÁMARA TV: Blitz",
                GameText.translate(GameLanguage.SPANISH, "TV CAMERA: Blitz"));
    }

    @Test
    public void translatesRaceCountdownBeforeAddingCircuitProgress() {
        assertEquals(
                "GET READY  |  CIRCUIT 3 / 19",
                GameText.countdownContext(GameLanguage.ENGLISH, 3, 19));
        assertEquals(
                "PREPÁRATE  |  CIRCUITO 3 / 19",
                GameText.countdownContext(GameLanguage.SPANISH, 3, 19));
        assertEquals(
                "PRÉPAREZ-VOUS  |  CIRCUIT 3 / 19",
                GameText.countdownContext(GameLanguage.FRENCH, 3, 19));
        assertEquals(
                "BEREIT MACHEN  |  STRECKE 3 / 19",
                GameText.countdownContext(GameLanguage.GERMAN, 3, 19));
        assertEquals(
                "PREPARATI  |  CIRCUITO 3 / 19",
                GameText.countdownContext(GameLanguage.ITALIAN, 3, 19));
        assertEquals(
                "PREPÁRATE",
                GameText.countdownContext(GameLanguage.SPANISH, 0, 0));
    }

    @Test
    public void everyCardHasLocalizedTitleDescriptionAndEffectText() {
        GameLanguage[] translatedLanguages = {
            GameLanguage.SPANISH,
            GameLanguage.FRENCH,
            GameLanguage.GERMAN,
            GameLanguage.ITALIAN
        };
        for (GameLanguage language : translatedLanguages) {
            for (RogueliteCardDefinition card : RogueliteCardCatalog.all()) {
                assertNotEquals(
                        language + " title " + card.getId(),
                        card.getTitle(),
                        GameText.translate(language, card.getTitle()));
                assertNotEquals(
                        language + " description " + card.getId(),
                        card.getDescription(),
                        GameText.translate(language, card.getDescription()));
                assertNotEquals(
                        language + " effect " + card.getId(),
                        card.getEffectText(),
                        GameText.translate(language, card.getEffectText()));
            }
        }
    }
}
