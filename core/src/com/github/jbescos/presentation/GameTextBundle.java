package com.github.jbescos.presentation;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;

final class GameTextBundle {
    private static final String RESOURCE_ROOT =
            "/com/github/jbescos/presentation/GameText";
    private static final Map<GameLanguage, ResourceBundle> BUNDLES = loadBundles();

    private GameTextBundle() {
    }

    static String find(GameLanguage language, String key) {
        ResourceBundle bundle = BUNDLES.get(language);
        if (bundle == null || !bundle.containsKey(key)) {
            return null;
        }
        return bundle.getString(key);
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
        ResourceBundle bundle = BUNDLES.get(language);
        if (bundle == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(new HashSet<String>(bundle.keySet()));
    }

    private static Map<GameLanguage, ResourceBundle> loadBundles() {
        EnumMap<GameLanguage, ResourceBundle> bundles =
                new EnumMap<GameLanguage, ResourceBundle>(GameLanguage.class);
        for (GameLanguage language : GameLanguage.values()) {
            String suffix = language == GameLanguage.ENGLISH
                    ? ""
                    : "_" + language.getStoredValue();
            bundles.put(language, loadBundle(RESOURCE_ROOT + suffix + ".properties"));
        }
        return bundles;
    }

    private static ResourceBundle loadBundle(String resourcePath) {
        InputStream stream = GameTextBundle.class.getResourceAsStream(resourcePath);
        if (stream == null) {
            throw new IllegalStateException("Missing game text bundle: " + resourcePath);
        }
        try (InputStream input = stream;
                InputStreamReader reader =
                        new InputStreamReader(input, StandardCharsets.UTF_8)) {
            return new PropertyResourceBundle(reader);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot load game text bundle: " + resourcePath, exception);
        }
    }
}
