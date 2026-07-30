package com.github.jbescos.gameplay;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class MapProgression {
    public interface MapLoader {
        ArenaMap load(String mapId);
    }

    private final ArrayList<MapSlot> maps = new ArrayList<MapSlot>();
    private final MapLoader mapLoader;
    private final Random random;
    private int currentIndex;

    public MapProgression(Array<ArenaMap> maps) {
        this(maps, null);
    }

    public MapProgression(Array<ArenaMap> maps, Random random) {
        if (maps == null || maps.size == 0) {
            throw new IllegalArgumentException("Map progression requires at least one map.");
        }
        for (int i = 0; i < maps.size; i++) {
            ArenaMap map = maps.get(i);
            this.maps.add(new MapSlot(map.getId(), map));
        }
        mapLoader = null;
        this.random = random;
        shuffleCycle(null);
    }

    private MapProgression(Array<String> mapIds, MapLoader mapLoader, Random random) {
        if (mapIds == null || mapIds.size == 0) {
            throw new IllegalArgumentException("Map progression requires at least one map.");
        }
        if (mapLoader == null) {
            throw new IllegalArgumentException("Lazy map progression requires a map loader.");
        }
        for (int i = 0; i < mapIds.size; i++) {
            String mapId = mapIds.get(i);
            if (mapId == null || mapId.trim().length() == 0) {
                throw new IllegalArgumentException("Lazy map progression requires non-empty map ids.");
            }
            maps.add(new MapSlot(mapId, null));
        }
        this.mapLoader = mapLoader;
        this.random = random;
        shuffleCycle(null);
    }

    public static MapProgression lazy(Array<String> mapIds, MapLoader mapLoader) {
        return lazy(mapIds, mapLoader, null);
    }

    public static MapProgression lazy(
            Array<String> mapIds,
            MapLoader mapLoader,
            Random random) {
        return new MapProgression(mapIds, mapLoader, random);
    }

    public ArenaMap getCurrentMap() {
        return resolveMap(maps.get(currentIndex));
    }

    public ArenaMap getNextMap() {
        if (maps.size() == 1) {
            return resolveMap(maps.get(0));
        }
        if (currentIndex + 1 < maps.size()) {
            return resolveMap(maps.get(currentIndex + 1));
        }
        return resolveMap(previewFirstMapOfNextCycle(maps.get(currentIndex)));
    }

    public void advance() {
        if (maps.size() == 1) {
            return;
        }

        MapSlot previousMap = maps.get(currentIndex);
        if (currentIndex < maps.size() - 1) {
            currentIndex++;
            releaseLoadedMaps();
            return;
        }

        shuffleCycle(previousMap);
        currentIndex = 0;
        releaseLoadedMaps();
    }

    public int getCurrentMapNumber() {
        return currentIndex + 1;
    }

    public int getMapCount() {
        return maps.size();
    }

    public String getCurrentMapId() {
        return maps.get(currentIndex).mapId;
    }

    public List<String> getMapIdsInOrder() {
        List<String> ids = new ArrayList<String>(maps.size());
        for (int i = 0; i < maps.size(); i++) {
            ids.add(maps.get(i).mapId);
        }
        return ids;
    }

    public boolean restoreOrder(List<String> orderedMapIds, String currentMapId) {
        if (orderedMapIds == null
                || orderedMapIds.isEmpty()
                || currentMapId == null) {
            return false;
        }
        Map<String, MapSlot> slotsById = new LinkedHashMap<String, MapSlot>();
        for (int i = 0; i < maps.size(); i++) {
            MapSlot slot = maps.get(i);
            slotsById.put(slot.mapId, slot);
        }

        ArrayList<MapSlot> restored = new ArrayList<MapSlot>(maps.size());
        Map<String, Boolean> savedIds = new LinkedHashMap<String, Boolean>();
        int restoredCurrentIndex = -1;
        for (int i = 0; i < orderedMapIds.size(); i++) {
            String mapId = orderedMapIds.get(i);
            if (mapId == null || savedIds.put(mapId, Boolean.TRUE) != null) {
                return false;
            }
            MapSlot slot = slotsById.remove(mapId);
            if (slot == null) {
                if (mapId.equals(currentMapId)) {
                    return false;
                }
                continue;
            }
            restored.add(slot);
            if (mapId.equals(currentMapId)) {
                restoredCurrentIndex = restored.size() - 1;
            }
        }
        if (restoredCurrentIndex < 0) {
            return false;
        }
        restored.addAll(slotsById.values());

        maps.clear();
        maps.addAll(restored);
        currentIndex = restoredCurrentIndex;
        releaseLoadedMaps();
        return true;
    }

    public void releaseLoadedMaps() {
        if (mapLoader == null) {
            return;
        }
        for (int i = 0; i < maps.size(); i++) {
            maps.get(i).map = null;
        }
    }

    int getLoadedMapCount() {
        int loaded = 0;
        for (int i = 0; i < maps.size(); i++) {
            if (maps.get(i).map != null) {
                loaded++;
            }
        }
        return loaded;
    }

    private ArenaMap resolveMap(MapSlot slot) {
        if (slot.map == null) {
            slot.map = mapLoader.load(slot.mapId);
            if (slot.map == null) {
                throw new IllegalStateException("Map loader returned null for " + slot.mapId);
            }
        }
        return slot.map;
    }

    private void shuffleCycle(MapSlot lastMap) {
        if (random == null) {
            Collections.shuffle(maps);
        } else {
            Collections.shuffle(maps, random);
        }

        if (lastMap != null && maps.size() > 1 && maps.get(0) == lastMap) {
            int swapIndex = 1 + randomIndex(maps.size() - 1);
            Collections.swap(maps, 0, swapIndex);
        }
    }

    private MapSlot previewFirstMapOfNextCycle(MapSlot lastMap) {
        ArrayList<MapSlot> preview = new ArrayList<MapSlot>(maps);
        if (random == null) {
            Collections.shuffle(preview);
        } else {
            Collections.shuffle(preview, random);
        }

        if (preview.size() > 1 && preview.get(0) == lastMap) {
            int swapIndex = 1 + randomIndex(preview.size() - 1);
            Collections.swap(preview, 0, swapIndex);
        }

        return preview.get(0);
    }

    private int randomIndex(int boundExclusive) {
        if (random != null) {
            return random.nextInt(boundExclusive);
        }
        return MathUtils.random(boundExclusive - 1);
    }

    private static final class MapSlot {
        private final String mapId;
        private ArenaMap map;

        private MapSlot(String mapId, ArenaMap map) {
            this.mapId = mapId;
            this.map = map;
        }
    }
}
