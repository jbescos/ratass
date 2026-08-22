package com.github.jbescos.presentation;

import com.github.jbescos.gameplay.roguelite.CustomGameRules.WeatherType;
import com.github.jbescos.gameplay.roguelite.RogueliteExperienceAwards.Reason;
import com.github.jbescos.gameplay.roguelite.RogueliteSlotType;

public final class GameText {
    private static final String[][] DYNAMIC_PREFIXES = {
        {"Theme: ", "format.prefix.theme"},
        {"Tier ", "format.prefix.tier"},
        {"Level ", "format.prefix.level"},
        {"Circuit ", "format.prefix.circuit"},
        {"Map ", "format.prefix.map"},
        {"CHAMPIONSHIP ", "format.prefix.championship"},
        {"Lap ", "format.prefix.lap"},
        {"Cars ", "format.prefix.cars"},
        {"Car ", "format.prefix.car"},
        {"Loading ", "format.prefix.loading"},
        {"Preparing ", "format.prefix.preparing"},
        {"Finished #", "format.prefix.finished"},
        {"TV CAMERA: ", "format.prefix.tv_camera"},
        {"CONTINUE ", "format.prefix.continue"},
        {"SKIP ", "format.prefix.skip"},
        {"Starts in ", "format.prefix.starts_in"},
        {"Finish closes in ", "format.prefix.finish_closes_in"}
    };

    private static final String[][] INCIDENT_FRAGMENTS = {
        {"HIT:", "format.incident.hit"},
        {"REVENGE:", "format.incident.revenge"},
        {"PASS:", "format.incident.pass"},
        {"SLAMMED", "format.incident.slammed"},
        {"SHOVED", "format.incident.shoved"},
        {"CLIPPED", "format.incident.clipped"},
        {"HIT", "format.incident.hit_verb"},
        {"USED", "format.incident.used"},
        {"ON", "format.incident.on"},
        {"PASSED", "format.incident.passed"},
        {"POSITIONS GAINED", "format.incident.positions_gained"},
        {"POSITION GAINED", "format.incident.position_gained"}
    };

    private GameText() {
    }

    public static String translate(GameLanguage language, String english) {
        if (english == null || language == null || language == GameLanguage.ENGLISH) {
            return english;
        }
        String exact = GameTextBundle.find(language, english);
        if (exact != null) {
            return exact;
        }
        String tieredCardTitle = translateTieredCardTitle(language, english);
        if (tieredCardTitle != null) {
            return tieredCardTitle;
        }
        String prefixed = translatePrefix(language, english);
        if (prefixed != null) {
            return prefixed;
        }
        if (english.indexOf(" | Tier ") >= 0
                || english.indexOf(" SLOTS") >= 0
                || english.indexOf(" PAGE ") >= 0) {
            return english
                    .replace(" | Tier ", " | " + bundleValue(language, "format.word.tier") + " ")
                    .replace(" SLOTS", " " + bundleValue(language, "format.word.slots"))
                    .replace(" PAGE ", " " + bundleValue(language, "format.word.page") + " ");
        }
        int detailSeparator = english.indexOf(" - ");
        if (detailSeparator > 0) {
            return translate(language, english.substring(0, detailSeparator))
                    + " - "
                    + translate(language, english.substring(detailSeparator + 3));
        }
        if (looksLikeIncident(english)) {
            return translateIncident(language, english);
        }
        int targetSeparator = english.lastIndexOf(" on ");
        if (targetSeparator > 0) {
            return translate(language, english.substring(0, targetSeparator))
                    + " "
                    + bundleValue(language, "format.target.connector")
                    + " "
                    + english.substring(targetSeparator + 4);
        }
        return english;
    }

    static boolean hasExplicitTranslation(GameLanguage language, String english) {
        return GameTextBundle.hasExplicitTranslation(language, english);
    }

    public static String format(GameLanguage language, String key, Object... arguments) {
        GameLanguage resolvedLanguage = language == null ? GameLanguage.ENGLISH : language;
        String text = bundleValue(resolvedLanguage, key);
        if (arguments == null) {
            return text;
        }
        for (int i = 0; i < arguments.length; i++) {
            text = text.replace("{" + i + "}", String.valueOf(arguments[i]));
        }
        return text;
    }

    private static String translateTieredCardTitle(GameLanguage language, String english) {
        if (english.length() < 5 || english.charAt(0) != 'T') {
            return null;
        }
        int separator = english.indexOf("  ");
        if (separator < 2 || separator + 2 >= english.length()) {
            return null;
        }
        for (int i = 1; i < separator; i++) {
            if (!Character.isDigit(english.charAt(i))) {
                return null;
            }
        }
        return english.substring(0, separator + 2)
                + translate(language, english.substring(separator + 2));
    }

    private static String translatePrefix(GameLanguage language, String english) {
        for (String[] prefix : DYNAMIC_PREFIXES) {
            if (!english.startsWith(prefix[0])) {
                continue;
            }
            String separator = prefix[0].endsWith(" ") ? " " : "";
            return bundleValue(language, prefix[1])
                    + separator
                    + english.substring(prefix[0].length());
        }
        return null;
    }

    private static boolean looksLikeIncident(String english) {
        return english.indexOf(" SLAMMED ") >= 0
                || english.indexOf(" SHOVED ") >= 0
                || english.indexOf(" CLIPPED ") >= 0
                || english.indexOf(" USED ") >= 0
                || english.indexOf(" PASSED ") >= 0;
    }

    private static String translateIncident(GameLanguage language, String english) {
        String translated = english;
        for (String[] fragment : INCIDENT_FRAGMENTS) {
            translated = translated.replace(
                    " " + fragment[0] + " ",
                    " " + bundleValue(language, fragment[1]) + " ");
            if (translated.startsWith(fragment[0] + " ")) {
                translated = bundleValue(language, fragment[1])
                        + " "
                        + translated.substring(fragment[0].length() + 1);
            }
            if (translated.endsWith(" " + fragment[0])) {
                translated = translated.substring(
                                0,
                                translated.length() - fragment[0].length())
                        + bundleValue(language, fragment[1]);
            }
        }
        return translated;
    }

    private static String bundleValue(GameLanguage language, String key) {
        return GameTextBundle.required(language, key);
    }

    public static String countdownContext(
            GameLanguage language,
            int currentCircuit,
            int circuitCount) {
        String ready = translate(language, "GET READY");
        if (currentCircuit <= 0 || circuitCount <= 0) {
            return ready;
        }
        return ready
                + "  |  "
                + translate(language, "CIRCUIT")
                + " "
                + currentCircuit
                + " / "
                + circuitCount;
    }

    public static String slotType(GameLanguage language, RogueliteSlotType slotType) {
        return slotType == null ? "" : translate(language, slotType.getDisplayName());
    }

    public static String weather(GameLanguage language, WeatherType weather) {
        return weather == null ? "" : translate(language, weather.getDisplayName());
    }

    public static String experienceReason(GameLanguage language, Reason reason) {
        return reason == null ? "" : translate(language, reason.getDisplayName());
    }
}
