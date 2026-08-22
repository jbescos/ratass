package com.github.jbescos.presentation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

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
    public void everyLanguageBundleDefinesEveryEnglishKey() {
        for (GameLanguage language : GameLanguage.values()) {
            assertEquals(
                    language.toString(),
                    GameTextBundle.keys(GameLanguage.ENGLISH),
                    GameTextBundle.keys(language));
        }
    }

    @Test
    public void formatsRuntimeMessagesFromLanguageBundles() {
        assertEquals(
                "XP VUELTA 7 / 20",
                GameText.format(GameLanguage.SPANISH, "message.lap_xp", 7, 20));
        assertEquals(
                "Bereit für den Start. Fahre 3 Runden.",
                GameText.format(GameLanguage.GERMAN, "message.prepare_laps", 3));
        assertEquals(
                "Il leader ha finito. Restano 9s agli altri.",
                GameText.format(GameLanguage.ITALIAN, "message.finish_leader", 9));
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
    public void carNamesStayTheSameInEveryLanguage() {
        String[] carNames = {
            "Veltryn VX",
            "Aurevox GT",
            "Caldris R",
            "Novaryn RS",
            "Torvane X",
            "Elystral S",
            "Vantory GT",
            "Orphira R",
            "Kavren XR",
            "Solvyr RS",
            "Noctyra VX",
            "Morvane GT",
            "Vesperon R",
            "Grimvolt RS",
            "Hexora X",
            "Umbryss S",
            "Corvane GT",
            "Dreadwyn R",
            "Ebonyx XR",
            "Phantyr RS"
        };
        for (GameLanguage language : GameLanguage.values()) {
            for (String carName : carNames) {
                assertEquals(carName, GameText.translate(language, carName));
            }
        }
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
        assertEquals("PASAR", GameText.translate(GameLanguage.SPANISH, "SKIP"));
        assertEquals("PASAR 30s", GameText.translate(GameLanguage.SPANISH, "SKIP 30s"));
        assertEquals(
                "Rival cercano en recta: 2 coches durante 5s\n"
                        + "Cartas y Venganza compartidas | Recarga: 10s",
                GameText.translate(
                        GameLanguage.SPANISH,
                        "Nearby rival on straight: 2 cars for 5s\n"
                                + "Shared cards and Revenge | Cooldown: 10s"));
        assertEquals(
                "Activación: Golpe rival\nAgresor: frenado máximo durante 2s",
                GameText.translate(
                        GameLanguage.SPANISH,
                        "Activation: Rival hit\nOffender: full brake for 2s"));
    }

    @Test
    public void translatesTierPrefixedTitlesUsedByTheCardsPanel() {
        assertEquals(
                "T1  Ajuste club",
                GameText.translate(GameLanguage.SPANISH, "T1  Club Tune"));
        assertEquals(
                "T2  Impulsion Nitro",
                GameText.translate(GameLanguage.FRENCH, "T2  Nitro Pulse"));
        assertEquals(
                "T3  Nitro-Impuls",
                GameText.translate(GameLanguage.GERMAN, "T3  Nitro Pulse"));
        assertEquals(
                "T1  Impulso Nitro",
                GameText.translate(GameLanguage.ITALIAN, "T1  Nitro Pulse"));
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
                assertTrue(
                        language + " title bundle entry " + card.getId(),
                        GameText.hasExplicitTranslation(language, card.getTitle()));
                assertTrue(
                        language + " description bundle entry " + card.getId(),
                        GameText.hasExplicitTranslation(language, card.getDescription()));
                assertTrue(
                        language + " effect bundle entry " + card.getId(),
                        GameText.hasExplicitTranslation(language, card.getEffectText()));
            }
        }
    }

    @Test
    public void spanishCardEffectsDoNotRetainEnglishControlPhrases() {
        String[] untranslatedFragments = {
            "Activation", "Nearby rival", "Rival hit", "Offender", "Cooldown",
            "Shared cards", "After ", " for ", "Random Tier", "best-driver",
            " shots", " charge", " hunt", "Powerup"
        };
        for (RogueliteCardDefinition card : RogueliteCardCatalog.all()) {
            String translated = GameText.translate(GameLanguage.SPANISH, card.getEffectText());
            for (String fragment : untranslatedFragments) {
                assertFalse(
                        card.getId() + " retains '" + fragment + "': " + translated,
                        translated.contains(fragment));
            }
        }
    }

    @Test
    public void europeanCardEffectsDoNotRetainEnglishControlPhrases() {
        GameLanguage[] languages = {
            GameLanguage.FRENCH,
            GameLanguage.GERMAN,
            GameLanguage.ITALIAN
        };
        String[] untranslatedFragments = {
            "best-driver", "Nearby rival", "Rival hit", "Offender", "Cooldown",
            "Shared cards", "After ", " for ", "Random Tier", " shots/s", "Powerup"
        };
        for (GameLanguage language : languages) {
            for (RogueliteCardDefinition card : RogueliteCardCatalog.all()) {
                String translated = GameText.translate(language, card.getEffectText());
                for (String fragment : untranslatedFragments) {
                    assertFalse(
                            language + " " + card.getId() + " retains '" + fragment
                                    + "': " + translated,
                            translated.contains(fragment));
                }
            }
        }
    }
}
