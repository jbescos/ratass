package com.github.jbescos.gameplay.roguelite.save;

public interface RogueliteSaveStore {
    String get(String key);

    void put(String key, String value);

    void remove(String key);

    void flush();
}
