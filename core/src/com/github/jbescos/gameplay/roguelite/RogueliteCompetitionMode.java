package com.github.jbescos.gameplay.roguelite;

public enum RogueliteCompetitionMode {
    CHAMPIONSHIP("championship"),
    INFINITE("infinite"),
    CUSTOM("custom");

    private final String id;

    RogueliteCompetitionMode(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public boolean isInfinite() {
        return this == INFINITE;
    }

    public boolean isCustom() {
        return this == CUSTOM;
    }

    public static RogueliteCompetitionMode fromId(String id) {
        for (RogueliteCompetitionMode mode : values()) {
            if (mode.id.equals(id)) {
                return mode;
            }
        }
        return CHAMPIONSHIP;
    }

    public static boolean isKnownId(String id) {
        for (RogueliteCompetitionMode mode : values()) {
            if (mode.id.equals(id)) {
                return true;
            }
        }
        return false;
    }
}
