package com.github.jbescos.presentation;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.TextResource;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

/** GWT replacement for the JVM resource-bundle loader. */
final class GameTextBundle {
    private static final Resources RESOURCES = GWT.create(Resources.class);
    private static final Map<GameLanguage, Map<String, String>> BUNDLES = loadBundles();

    private GameTextBundle() {
    }

    static String find(GameLanguage language, String key) {
        Map<String, String> bundle = BUNDLES.get(language);
        return bundle == null ? null : bundle.get(key);
    }

    static String required(GameLanguage language, String key) {
        String value = find(language, key);
        if (value == null) {
            throw new MissingResourceException(
                    "Missing game text for " + language + ": " + key,
                    GameTextBundle.class.getName(),
                    key);
        }
        return value;
    }

    static boolean hasExplicitTranslation(GameLanguage language, String key) {
        return language != null
                && language != GameLanguage.ENGLISH
                && find(language, key) != null;
    }

    static Set<String> keys(GameLanguage language) {
        Map<String, String> bundle = BUNDLES.get(language);
        return bundle == null
                ? Collections.<String>emptySet()
                : Collections.unmodifiableSet(new HashSet<String>(bundle.keySet()));
    }

    private static Map<GameLanguage, Map<String, String>> loadBundles() {
        EnumMap<GameLanguage, Map<String, String>> bundles =
                new EnumMap<GameLanguage, Map<String, String>>(GameLanguage.class);
        bundles.put(GameLanguage.ENGLISH, parse(RESOURCES.english().getText()));
        bundles.put(GameLanguage.SPANISH, parse(RESOURCES.spanish().getText()));
        bundles.put(GameLanguage.FRENCH, parse(RESOURCES.french().getText()));
        bundles.put(GameLanguage.GERMAN, parse(RESOURCES.german().getText()));
        bundles.put(GameLanguage.ITALIAN, parse(RESOURCES.italian().getText()));
        return bundles;
    }

    private static Map<String, String> parse(String source) {
        Map<String, String> values = new HashMap<String, String>();
        if (source == null) {
            return values;
        }
        String[] lines = source.replace("\r", "").split("\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.length() == 0 || line.charAt(0) == '#' || line.charAt(0) == '!') {
                continue;
            }
            int separator = findSeparator(line);
            if (separator < 0) {
                values.put(unescape(line), "");
            } else {
                values.put(
                        unescape(line.substring(0, separator)),
                        unescape(line.substring(separator + 1)));
            }
        }
        return values;
    }

    private static int findSeparator(String line) {
        boolean escaped = false;
        for (int i = 0; i < line.length(); i++) {
            char character = line.charAt(i);
            if (!escaped && (character == '=' || character == ':')) {
                return i;
            }
            if (character == '\\' && !escaped) {
                escaped = true;
            } else {
                escaped = false;
            }
        }
        return -1;
    }

    private static String unescape(String text) {
        StringBuilder result = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            char character = text.charAt(i);
            if (character != '\\' || i + 1 >= text.length()) {
                result.append(character);
                continue;
            }
            char escaped = text.charAt(++i);
            if (escaped == 'n') {
                result.append('\n');
            } else if (escaped == 'r') {
                result.append('\r');
            } else if (escaped == 't') {
                result.append('\t');
            } else if (escaped == 'f') {
                result.append('\f');
            } else if (escaped == 'u' && i + 4 < text.length()) {
                result.append((char) Integer.parseInt(text.substring(i + 1, i + 5), 16));
                i += 4;
            } else {
                result.append(escaped);
            }
        }
        return result.toString();
    }

    interface Resources extends ClientBundle {
        @Source("GameText.properties")
        TextResource english();

        @Source("GameText_es.properties")
        TextResource spanish();

        @Source("GameText_fr.properties")
        TextResource french();

        @Source("GameText_de.properties")
        TextResource german();

        @Source("GameText_it.properties")
        TextResource italian();
    }
}
