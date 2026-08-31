package com.github.jbescos.presentation;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Theme-specific public aliases for internal RL driver profile IDs. */
public final class ThemeDriverNames {
    private static final ThemeDriverNames EMPTY =
            new ThemeDriverNames(Collections.<String, String>emptyMap());

    private final Map<String, String> namesByProfileId;

    private ThemeDriverNames(Map<String, String> namesByProfileId) {
        this.namesByProfileId = namesByProfileId;
    }

    public static ThemeDriverNames empty() {
        return EMPTY;
    }

    public static ThemeDriverNames parse(String text) {
        if (text == null || text.trim().length() == 0) {
            return empty();
        }
        Map<String, String> names = new LinkedHashMap<String, String>();
        String[] lines = text.split("\\r?\\n");
        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();
            if (line.length() == 0 || line.startsWith("#")) {
                continue;
            }
            int separator = line.indexOf('=');
            if (separator <= 0 || separator >= line.length() - 1) {
                continue;
            }
            String profileId = line.substring(0, separator).trim();
            String displayName = line.substring(separator + 1).trim();
            if (profileId.length() == 0
                    || displayName.length() == 0
                    || names.containsKey(profileId)) {
                continue;
            }
            names.put(profileId, displayName);
        }
        if (names.isEmpty()) {
            return empty();
        }
        return new ThemeDriverNames(
                Collections.unmodifiableMap(names));
    }

    public String get(String profileId) {
        return profileId == null ? null : namesByProfileId.get(profileId);
    }
}
