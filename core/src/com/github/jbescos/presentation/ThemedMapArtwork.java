package com.github.jbescos.presentation;

public final class ThemedMapArtwork {
    private static final String MAP_DIRECTORY = "maps";

    private ThemedMapArtwork() {
    }

    public static String relativePath(String mapId) {
        if (mapId == null) {
            return "";
        }
        String normalized = mapId.trim();
        if (normalized.length() == 0
                || normalized.indexOf('/') >= 0
                || normalized.indexOf('\\') >= 0
                || normalized.indexOf("..") >= 0) {
            return "";
        }
        return MAP_DIRECTORY + "/" + normalized + ".png";
    }
}
