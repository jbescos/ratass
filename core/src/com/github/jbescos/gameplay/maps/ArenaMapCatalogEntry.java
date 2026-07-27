package com.github.jbescos.gameplay.maps;

public final class ArenaMapCatalogEntry {
    enum Source {
        DEFAULT,
        TRAINING
    }

    private final String id;
    private final String name;
    private final Source source;

    ArenaMapCatalogEntry(String id, String name, Source source) {
        this.id = id;
        this.name = name;
        this.source = source;
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    Source getSource() {
        return source;
    }
}
