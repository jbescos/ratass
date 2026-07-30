package com.github.jbescos.gameplay.roguelite.save;

import com.badlogic.gdx.Preferences;

public final class GdxPreferencesRogueliteSaveStore implements RogueliteSaveStore {
    private final Preferences preferences;

    public GdxPreferencesRogueliteSaveStore(Preferences preferences) {
        if (preferences == null) {
            throw new IllegalArgumentException("Preferences are required.");
        }
        this.preferences = preferences;
    }

    @Override
    public String get(String key) {
        return preferences.getString(key, "");
    }

    @Override
    public void put(String key, String value) {
        preferences.putString(key, value);
    }

    @Override
    public void remove(String key) {
        preferences.remove(key);
    }

    @Override
    public void flush() {
        preferences.flush();
    }
}
