package com.github.jbescos.gameplay.maps;

import com.badlogic.gdx.utils.Array;
import com.github.jbescos.gameplay.ArenaMap;

public final class ArenaMaps {
    private static final float DEFAULT_MAP_SCALE = 2f;

    private ArenaMaps() {
    }

    public static Array<ArenaMap> createDefaultSet() {
        return createDefaultSet(DEFAULT_MAP_SCALE);
    }

    public static Array<ArenaMap> createDefaultSet(float mapScale) {
        return ImageArenaMapLoader.loadDefaultMaps(mapScale);
    }
}
