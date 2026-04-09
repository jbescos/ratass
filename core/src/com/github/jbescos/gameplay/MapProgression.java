package com.github.jbescos.gameplay;

import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.utils.Array;
import java.util.ArrayList;
import java.util.Collections;

public final class MapProgression {
    private final ArrayList<ArenaMap> maps = new ArrayList<ArenaMap>();
    private int currentIndex;

    public MapProgression(Array<ArenaMap> maps) {
        if (maps == null || maps.size == 0) {
            throw new IllegalArgumentException("Map progression requires at least one map.");
        }
        for (int i = 0; i < maps.size; i++) {
            this.maps.add(maps.get(i));
        }
        shuffleCycle(null);
    }

    public ArenaMap getCurrentMap() {
        return maps.get(currentIndex);
    }

    public ArenaMap getNextMap() {
        if (maps.size() == 1) {
            return maps.get(0);
        }
        if (currentIndex + 1 < maps.size()) {
            return maps.get(currentIndex + 1);
        }
        return previewFirstMapOfNextCycle(maps.get(currentIndex));
    }

    public void advance() {
        if (maps.size() == 1) {
            return;
        }

        if (currentIndex < maps.size() - 1) {
            currentIndex++;
            return;
        }

        ArenaMap lastMap = maps.get(currentIndex);
        shuffleCycle(lastMap);
        currentIndex = 0;
    }

    public int getCurrentMapNumber() {
        return currentIndex + 1;
    }

    public int getMapCount() {
        return maps.size();
    }

    private void shuffleCycle(ArenaMap lastMap) {
        Collections.shuffle(maps);

        if (lastMap != null && maps.size() > 1 && maps.get(0) == lastMap) {
            int swapIndex = 1 + MathUtils.random(maps.size() - 2);
            Collections.swap(maps, 0, swapIndex);
        }
    }

    private ArenaMap previewFirstMapOfNextCycle(ArenaMap lastMap) {
        ArrayList<ArenaMap> preview = new ArrayList<ArenaMap>(maps);
        Collections.shuffle(preview);

        if (preview.size() > 1 && preview.get(0) == lastMap) {
            int swapIndex = 1 + MathUtils.random(preview.size() - 2);
            Collections.swap(preview, 0, swapIndex);
        }

        return preview.get(0);
    }
}
