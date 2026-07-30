package com.github.jbescos.gameplay;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.badlogic.gdx.utils.Array;
import java.util.Arrays;
import java.util.Random;
import org.junit.Test;

public class MapProgressionTest {
    @Test
    public void lazyProgressionLoadsOnlyCurrentMapAndReleasesItOnAdvance() {
        Array<String> mapIds = new Array<String>();
        mapIds.add("map-a");
        mapIds.add("map-b");
        final int[] loadCount = {0};
        MapProgression progression =
                MapProgression.lazy(
                        mapIds,
                        new MapProgression.MapLoader() {
                            @Override
                            public ArenaMap load(String mapId) {
                                loadCount[0]++;
                                return createMap(mapId);
                            }
                        },
                        new Random(7L));

        assertEquals(0, progression.getLoadedMapCount());
        ArenaMap first = progression.getCurrentMap();
        assertSame(first, progression.getCurrentMap());
        assertEquals(1, loadCount[0]);
        assertEquals(1, progression.getLoadedMapCount());

        progression.advance();
        assertEquals(0, progression.getLoadedMapCount());
        progression.getCurrentMap();
        assertEquals(2, loadCount[0]);
        assertEquals(1, progression.getLoadedMapCount());

        progression.releaseLoadedMaps();
        assertEquals(0, progression.getLoadedMapCount());
    }

    @Test
    public void eagerProgressionRetainsSuppliedMapsForTraining() {
        ArenaMap first = createMap("map-a");
        ArenaMap second = createMap("map-b");
        Array<ArenaMap> maps = new Array<ArenaMap>();
        maps.add(first);
        maps.add(second);

        MapProgression progression = new MapProgression(maps, new Random(11L));

        assertEquals(2, progression.getLoadedMapCount());
        progression.releaseLoadedMaps();
        assertEquals(2, progression.getLoadedMapCount());
        progression.advance();
        assertEquals(2, progression.getLoadedMapCount());
    }

    @Test
    public void restoresSavedMapOrderAndPosition() {
        Array<String> mapIds = new Array<String>();
        mapIds.add("map-a");
        mapIds.add("map-b");
        mapIds.add("map-c");
        MapProgression progression =
                MapProgression.lazy(
                        mapIds,
                        new MapProgression.MapLoader() {
                            @Override
                            public ArenaMap load(String mapId) {
                                return createMap(mapId);
                            }
                        },
                        new Random(19L));

        assertTrue(
                progression.restoreOrder(
                        Arrays.asList("map-c", "map-a", "map-b"),
                        "map-a"));
        assertEquals(Arrays.asList("map-c", "map-a", "map-b"), progression.getMapIdsInOrder());
        assertEquals("map-a", progression.getCurrentMapId());
        assertEquals(2, progression.getCurrentMapNumber());
        assertFalse(
                progression.restoreOrder(
                        Arrays.asList("map-c", "map-a", "map-a"),
                        "map-a"));
        assertEquals("map-a", progression.getCurrentMapId());
    }

    private static ArenaMap createMap(String id) {
        return ArenaMap.builder(id, id)
                .solid(ArenaShape.rectangle(0f, 0f, 2f, 2f))
                .spawn(new SpawnPoint(0f, 0f, 0f))
                .build();
    }
}
